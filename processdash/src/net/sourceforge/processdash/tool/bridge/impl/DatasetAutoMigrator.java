// Copyright (C) 2012 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;

/**
 * This class assists an individual with the task of migrating their personal
 * data (stored on their local hard drive) into the Enterprise Server.
 */
public class DatasetAutoMigrator {

    public static final String ENABLED_SYSPROP = DatasetAutoMigrator.class
            .getName() + ".enabled";
    public static final String FORCE_MIGRATE = "force";

    public interface DialogParentSource {
        Component getDialogParent();
    }

    /**
     * Possibly migrate a preexisting dataset into the bridged dataset that
     * makes up the current working directory.
     * 
     * When a user first opens their personal dataset from the PDES, and that
     * personal dataset is empty, a special system property is set.  This
     * class checks for that system property and potentially initiates an
     * operation to import their preexisting historical data into the server.
     * 
     * This method could potentially call System.exit() if the user asks not
     * to continue.
     * 
     * @return true if data was migrated, false otherwise.
     */
    public static boolean maybeRun(WorkingDirectory workingDir,
            DialogParentSource dps) {
        // check to ensure that this class has been enabled.
        if (!StringUtils.hasValue(System.getProperty(ENABLED_SYSPROP)))
            return false;

        // if the current working directory represents a dataset that has
        // already been migrated into the enterprise server, it is not empty
        // and should not be overwritten
        if (Settings.getVal(ALREADY_MIGRATED_SETTING) != null)
            return false;

        // Doublecheck to make certain we have a bridged working directory
        if (!(workingDir instanceof BridgedWorkingDirectory))
            return false;

        // Abort if we are in read-only mode
        if (Settings.isReadOnly())
            return false;

        // Ensure that we have a write lock on the working directory
        try {
            workingDir.assertWriteLock();
        } catch (Exception e) {
            return false;
        }

        // find the directory that currently contains migratable personal data
        File sourceDir = getDirectoryToMigrate();
        if (sourceDir == null)
            return false;

        // Move forward and possibly perform the migration
        return new DatasetAutoMigrator((BridgedWorkingDirectory) workingDir,
                sourceDir, dps).run();
    }

    private static File getDirectoryToMigrate() {
        // look in the settings written by the Process Dashboard installer to
        // find the directory where personal data is being stored.
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        String userDataDirName = prefs.get(DATA_PATH, null);
        if (!StringUtils.hasValue(userDataDirName))
            return null;

        // Check to see if the directory exists
        File userDataDir = new File(userDataDirName);
        if (!userDataDir.isDirectory())
            return null;

        // Look for known files that indicate activity.  Abort if not present
        for (String filename : EXPECTED_FILES)
            if (new File(userDataDir, filename).isFile() == false)
                return null;

        // If the directory contains a teamServer.xml file, it has already
        // been migrated somewhere
        File teamServerFile = new File(userDataDir,
                TeamServerPointerFile.FILE_NAME);
        if (teamServerFile.exists())
            return null;

        // The directory looks OK to migrate. Return it.
        return userDataDir;
    }



    private BridgedWorkingDirectory workingDir;

    private File sourceDir;

    private DialogParentSource dps;

    private FileConcurrencyLock sourceDirLock;

    private ResourceBundle resources;

    private boolean enableOfflineMode;

    public DatasetAutoMigrator(BridgedWorkingDirectory workingDir,
            File sourceDir, DialogParentSource dps) {
        this.workingDir = workingDir;
        this.sourceDir = sourceDir;
        this.dps = dps;
        this.sourceDirLock = new FileConcurrencyLock(new File(sourceDir,
                DashboardInstanceStrategy.LOCK_FILE_NAME));
        this.resources = ResourceBundle
                .getBundle("Templates.resources.ProcessDashboard");
    }

    private boolean run() {
        if (promptUserToConfirmMigration()) {
            // if the user agrees, migrate their data into the server.
            new MigrateDataWorker().start();
            return true;

        } else {
            // the user does not want to migrate their data. Ask if they
            // want to continue and open their server-based dataset. If not,
            // exit the application.
            if (promptUserToContinueAndCreateDataset() == false)
                exitApplication();
            return false;
        }
    }

    private class MigrateDataWorker extends SwingWorker {

        JDialog progressDialog;

        @Override
        public void start() {
            buildProgressDialog();
            super.start();
            progressDialog.setVisible(true);
        }

        private void buildProgressDialog() {
            progressDialog = new JDialog((Frame) null,
                    getRes("Migrate.Progress.Title"), true);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            JPanel p = new JPanel(new BorderLayout(10, 10));
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            p.add(new JLabel(getRes("Migrate.Progress.Message")),
                BorderLayout.NORTH);
            p.add(progressBar,BorderLayout.CENTER);

            progressDialog.getContentPane().add(p);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(null);
        }

        @Override
        public Object construct() {
            migrateData();
            return null;
        }

        @Override
        public void finished() {
            progressDialog.dispose();
        }

    }

    private void migrateData() {
        try {
            ensureDirectoryIsNotInUse();
            checkForOfflineModeSupport();
            copyDataFiles();
            updateSettings();
            flushWorkingData();
            finalizeOldDataDirectory();
        } catch (IOException ioe) {
            // if an error occurs while copying files, clear the sync timestamp.
            // This will ensure that the bridged working directory does not
            // attempt to preserve any of the changes we just made to the local
            // working directory
            workingDir.clearSyncTimestamp();

            // show an error message to the user, then exit.
            showMigrationExceptionDialog(ioe);
            exitApplication();
        }
    }

    private void ensureDirectoryIsNotInUse() {
        // Ensure that no other process has the source directory locked.
        // If an application is running, ask the user to close it.
        while (true) {
            try {
                sourceDirLock.acquireLock("Dataset Auto Migrator");
                return;
            } catch (LockFailureException lfe) {
                if (promptUserToCloseRunningApplication() == false)
                    exitApplication();
            }
        }
    }

    private void checkForOfflineModeSupport() {
        // when we migrate data into the enterprise server, the user will end
        // up with two shortcuts: an old shortcut that launches the version of
        // the dashboard that is installed on their local hard drive, and a
        // new shortcut that dynamically downloads the application from the
        // server.  Until they delete the old shortcut, they could possibly
        // end up using it from time to time to open their data.  If their old
        // version is older than 1.14.7, it will not properly handle the
        // "Work Offline" logic; so to avoid problems we only want to put the
        // dataset into offline mode if their installed version is 1.14.7 or
        // higher.  The simplest way to detect this is to look at the log.txt
        // file in the source directory; it will contain a line near the top
        // of the file indicating the version number that was used the last
        // time the data was opened.
        try {
            File logFile = new File(sourceDir, "log.txt");
            BufferedReader in = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = in.readLine()) != null) {
                int versionTokenPos = line.indexOf(VERSION_TOKEN);
                if (versionTokenPos != -1) {
                    String version = line.substring(versionTokenPos
                            + VERSION_TOKEN.length()).trim();
                    if (VersionUtils.compareVersions(version,
                        MIN_OFFLINE_VERSION) >= 0)
                        enableOfflineMode = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
    }
    private static final String VERSION_TOKEN = "Process Dashboard version ";
    private static final String MIN_OFFLINE_VERSION = "1.14.7";

    private void copyDataFiles() throws IOException {
        // Copy all important data files from the source directory to the
        // working directory.
        File destDir = workingDir.getDirectory();
        List<String> files = FileUtils.listRecursively(sourceDir,
            DashboardInstanceStrategy.INSTANCE.getFilenameFilter());
        for (String filename : files) {
            File srcFile = new File(sourceDir, filename);
            File destFile = new File(destDir, filename);
            RobustFileOutputStream out = new RobustFileOutputStream(destFile);
            FileUtils.copyFile(srcFile, out);
            out.close();
        }
    }

    private void updateSettings() {
        // a new pspdash.ini file has been copied into our current working
        // directory. Reload the values from that file.
        InternalSettings.maybeReload();

        // write a new setting to indicate that we've performed the migration.
        InternalSettings.set(ALREADY_MIGRATED_SETTING, Long.toString(System
                .currentTimeMillis()));
    }

    private void flushWorkingData() {
        try {
            // Enable offline mode by default, because is most similar to the
            // mode of operation the user is accustomed to.
            if (enableOfflineMode)
                workingDir.setOfflineLockEnabled(true);
        } catch (Exception e) {
            // ignore exceptions. If the lock stays in online mode, that's OK.
        }

        try {
            // Flush all of our new files to the server.
            workingDir.flushData();
        } catch (Exception e) {
            // ignore exceptions. If we are unable to flush the data, it will
            // presumably get flushed later by the bridged worker.
        }
    }

    private void finalizeOldDataDirectory() {
        writeTeamServerPointerFile();
        writeMarkerFile();
        makeFilesReadOnly();
        sourceDirLock.releaseLock(true);
    }

    private void writeTeamServerPointerFile() {
        try {
            TeamServerPointerFile f = new TeamServerPointerFile(sourceDir);
            f.addServerEntry(workingDir.getDescription());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void writeMarkerFile() {
        try {
            File marker = new File(sourceDir, MARKER_FILE);
            PrintWriter out = new PrintWriter(new FileWriter(marker));
            out.println("This directory is obsolete.  The data it contains");
            out.println("has been moved to the server at");
            out.println(workingDir.getDescription());
            out.println();
            out.println("The contents of this directory have been left intact");
            out.println("while affected users transition to the new directory.");
            out.println("After all users have transitioned, this directory");
            out.println("can be safely deleted.");
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

    protected void makeFilesReadOnly() {
        // Make the files in the old directory read-only. This will cause the
        // dashboard to open in read-only mode.
        for (File f : sourceDir.listFiles())
            f.setReadOnly();
    }


    // Various methods to display dialogs to the user and prompt for
    // confirmation

    private boolean promptUserToConfirmMigration() {
        // if the system property tells us to force the data migration,
        // automatically consent without prompting the user.
        if (FORCE_MIGRATE.equals(System.getProperty(ENABLED_SYSPROP)))
            return true;

        String title = getRes("Migrate.Confirm.Title");
        String messageFmt = getRes("Migrate.Confirm.Message_FMT");
        String[] message = MessageFormat
                .format(messageFmt, sourceDir.getPath()).split("\n");

        String yesOption = getRes("Migrate.Options.Yes_Recommended");
        String noOption = getRes("Migrate.Options.No");
        String[] options = new String[] { yesOption, noOption };

        int userChoice = JOptionPane.showOptionDialog(dp(), message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options, yesOption);
        return (userChoice == 0);
    }

    private boolean promptUserToContinueAndCreateDataset() {
        String title = getRes("Migrate.Create.Title");
        String[] message = getRes("Migrate.Create.Message").split("\n");

        String yesOption = getRes("Migrate.Options.Yes");
        String noOption = getRes("Migrate.Options.No_Recommended");
        String[] options = new String[] { yesOption, noOption };

        int userChoice = JOptionPane.showOptionDialog(dp(), message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options, noOption);
        return (userChoice == 0);
    }

    private boolean promptUserToCloseRunningApplication() {
        String title = getRes("Migrate.Close.Title");
        String[] message = getRes("Migrate.Close.Message").split("\n");

        int userChoice = JOptionPane.showConfirmDialog(dp(), message, title,
            JOptionPane.OK_CANCEL_OPTION);
        return (userChoice == JOptionPane.OK_OPTION);
    }

    private void showMigrationExceptionDialog(IOException ioe) {
        String title = getRes("Migrate.Error.Title");
        String[] message = getRes("Migrate.Error.Message").split("\n");
        ExceptionDialog.show(dp(), title, message, ioe);
    }

    private Component dp() {
        if (dps != null)
            return dps.getDialogParent();
        else
            return null;
    }

    private String getRes(String key) {
        return resources.getString(key);
    }

    private static void exitApplication() {
        System.exit(0);
    }


    // Values used by the Process Dashboard installer to record the directory
    // where personal data is stored.
    private static final String USER_VALUES_PREFS_NODE = //
    "/net/sourceforge/processdash/installer";

    private static final String DATA_PATH = "DATA_PATH";

    // a list of files that indicate activity in a particular directory
    private static final String[] EXPECTED_FILES = { "global.dat", "0.dat",
            "timelog.xml" };

    // a file we write to the source directory to indicate it has been migrated
    static final String MARKER_FILE = "00-This-Directory-is-Obsolete.txt";

    // pspdash.ini setting to record the fact that we've migrated data into
    // this working directory
    private static final String ALREADY_MIGRATED_SETTING = "autoMigrated.date";

}
