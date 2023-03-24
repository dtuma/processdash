// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup.move;

import static net.sourceforge.processdash.tool.bridge.bundle.FileBundleConstants.BUNDLE_SUBDIR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Calendar;
import java.util.Properties;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.sync.HierarchySynchronizer;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleDirectory;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMigrator;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.bundle.WBSBackupBundler;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.quicklauncher.TeamToolsVersionManager;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RobustFileWriter;

public class CloudStorageProjectWorker extends MoveProjectWorker {

    private DataContext data;

    public CloudStorageProjectWorker(DashboardContext ctx, String projectPrefix,
            String projectID, boolean isMaster, String oldTeamDir,
            String newTeamDir) {
        // inline constants in the next few lines are used to document the
        // intent of literals that would otherwise be "magic"
        super(DONT_SAVE_NEW_PATHS_TO_DATA, projectPrefix,
                DISABLE_MASTER_LINK_REWRITING, projectID,
                USE_NEW_MOVE_MESSAGE_TYPE, isMaster, oldTeamDir, newTeamDir,
                DONT_WRITE_UNC_PATH);
        this.data = ctx.getData().getSubcontext(projectPrefix);
    }


    /**
     * Perform advance checks on the source dir to ensure it can be migrated
     */
    public void validateSourceData(boolean quick) throws MoveProjectException {
        try {
            CloudStorageDatasetMigrator.validateSourceData(oldTeamDataDir,
                quick, TeamDataDirStrategy.INSTANCE, "settings.xml");
            File oldTeamDissDir = new File(oldTeamDataDir,
                    TeamDataConstants.DISSEMINATION_DIRECTORY);
            CloudStorageDatasetMigrator.validateSourceData(oldTeamDissDir,
                quick, TeamDataDirStrategy.INSTANCE);
        } catch (MoveProjectException mpe) {
            throw mpe.append("projectPrefix", projectPrefix);
        }
    }


    @Override
    public void run() throws MoveProjectException {
        // check to see if this project has already been copied to the desired
        // cloud storage directory. If so, do nothing
        if (isDataAlreadyMigrated())
            return;

        // call the superclass logic to copy data to cloud storage
        try {
            super.run();
        } catch (MoveProjectException mpe) {
            throw new MoveProjectException(mpe.query) //
                    .append("projectPrefix", projectPrefix);
        }

        // if the copy was successful, record this in the data repository
        data.putValue(CLOUD_DIRECTORY, StringData.create(newTeamDir));
    }


    private boolean isDataAlreadyMigrated() {
        SimpleData sd = data.getSimpleValue(CLOUD_DIRECTORY);
        return (sd != null && newTeamDir.equalsIgnoreCase(sd.format())
                && getMarkerFile().exists());
    }


    @Override
    protected void checkIfDirectoryAlreadyExists() {
        if (dataDirAlreadyExists(newTeamDataDir)) {
            // our parent class threw an exception if an alphanumeric project
            // directory was already present. But it was only moving a single
            // project, so it was fine to abort in that case. For us, each abort
            // introduces risks to the atomicity of the overall migration. So
            // instead, we will silently move any existing directory out of the
            // way. This let our operation proceed and avoids losing any data.
            File parent = newTeamDataDir.getParentFile();
            String backupName = newTeamDataDir.getName() + "-backup";
            for (int i = 0; i < 10; i++) {
                String newName = backupName + (i == 0 ? "" : "-" + i);
                File newDir = new File(parent, newName);
                if (!newDir.exists() && newTeamDataDir.renameTo(newDir)) {
                    return;
                }
            }
            throw new MoveProjectException("cannotCreateDir") //
                    .append("path", newTeamDataDir.getPath())
                    .append("projectPrefix", projectPrefix);
        }
    }


    @Override
    protected void copyFilesToNewDirectoryImpl() {
        // if a move file is left over from a previous migration, delete it so
        // it doesn't get included in our bundles
        makeWritable(getMoveFile()).delete();

        // copy the files to the new directory, bundling them at the same time
        try {
            FileBundleMigrator.migrate(oldTeamDataDir, newTeamDataDir,
                TeamDataDirStrategy.INSTANCE, FileBundleMode.Sync,
                SOURCE_DIR_IS_ALREADY_LOCKED_FLAG);
        } catch (Exception e) {
            logError("performing cloud storage migration", e);
            throw new MoveProjectException("bundleError") //
                    .append("path", newTeamDataDir.getPath())
                    .append("projectPrefix", projectPrefix);
        }

        // migrate the backup subdirectory to retain WBS change history
        try {
            File oldBackupDir = new File(oldTeamDataDir, "backup");
            File newBundleDir = new File(newTeamDataDir, BUNDLE_SUBDIR);
            if (oldBackupDir.isDirectory()) {
                // create bundles for historical WBS zip backup files
                new WBSBackupBundler().run(oldBackupDir, newBundleDir, false);

                // run a pack operation to reduce the total number of files
                int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                new FileBundleDirectory(newBundleDir).packBundlesThroughMonth(
                    System.currentTimeMillis() - today * DateUtils.DAYS);
            }
        } catch (IOException ioe) {
            // loss of backups/history is not critical. log and continue
            logError("copying WBS history", ioe);
        }
    }


    @Override
    protected void tweakOldProjectFiles() {
        // write the marker file and make files read only, to prevent other
        // clients from changing the files we just migrated
        writeMarkerFile();
        makeFilesReadOnly();
    }



    /**
     * Modify source files to finalize the migration.
     * 
     * This is performed as a separate step from {@link #run()} to assist with
     * repeatability / atomicity: if the migration encounters an error and the
     * user has to retry, we don't want these modifications to be published to
     * the new team data directory.
     */
    public void finish() {
        try {
            writeMoveFile();
            writeMinWbsVersion();
            writeUpgradeNeededNote();
        } catch (IOException ioe) {
            logError("finishing cloud storage migration", ioe);
        }
    }

    private void writeMinWbsVersion() throws IOException {
        File f = makeWritable(new File(oldTeamDataDir, "user-settings.ini"));
        Properties p = new Properties();

        // load the current properties from the file
        InputStream in = null;
        try {
            if (f.isFile()) {
                in = new BufferedInputStream(new FileInputStream(f));
                p.load(in);
            }
        } finally {
            FileUtils.safelyClose(in);
        }

        // store a new min version requirement. This will cause older versions
        // of the WBS Editor to display an "upgrade needed" message
        p.put(TeamToolsVersionManager.WBS_EDITOR_VERSION_REQUIREMENT,
            FileBundleMode.Sync.getMinVersions().get("teamToolsB"));

        // write the updated properties to the file
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new RobustFileOutputStream(f));
            p.store(out, null);
        } finally {
            FileUtils.safelyClose(out);
        }
    }

    private void writeUpgradeNeededNote() throws IOException {
        // rewrite the projDump.xml file so the root node of the project has
        // an "upgrade needed" note. Older versions of the dashboard (which do
        // not understand the new-style moved-data messages) will sync this
        // note down for the user to read.
        File f = makeWritable(new File(oldTeamDataDir, "projDump.xml"));
        BufferedReader in = null;
        RobustFileWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader( //
                    new FileInputStream(f), "UTF-8"));
            out = new RobustFileWriter(f, "UTF-8");

            String line;
            while ((line = in.readLine()) != null) {
                out.write(line);
                out.write("\n");
                if (line.startsWith("<project "))
                    writeUpgradeNeededNote(out);
            }
            out.flush();

        } catch (IOException ioe) {
            if (out != null)
                out.abort();
            throw ioe;

        } finally {
            FileUtils.safelyClose(in);
            FileUtils.safelyClose(out);
            f.setReadOnly();
        }
    }

    private void writeUpgradeNeededNote(Writer out) throws IOException {
        Resources res = Resources.getDashBundle("Bundler.Cloud");
        out.write("     <note author='Cloud Storage Migration Wizard");
        out.write("' timestamp='@" + System.currentTimeMillis());
        out.write("' format='text'>[" + CLOUD_STORAGE_UPGRADE_NEEDED_URL + " ");
        for (int i = 10; i-- > 0;)
            out.write(UPGRADE_BULLET_CHAR);
        out.write(" " + res.getHTML("Upgrade_Link_Name") + "]\n");
        out.write(HTMLUtils.escapeEntities(res.format("Upgrade_Note_FMT", //
            newTeamDirENC)));
        out.write("</note>\n");
    }



    // ask our superclass NOT to write the new team directory paths into the
    // data repository
    private static final DashboardContext DONT_SAVE_NEW_PATHS_TO_DATA = null;

    // ask our superclass NOT to adjust the paths in the settings.xml files for
    // master projects and their subprojects. (This task will be handled later
    // when we change all team directories to ".")
    private static final String DISABLE_MASTER_LINK_REWRITING = null;

    // ask our superclass to write the moved-data message in a way that will
    // only be picked up by newer dashboards (so they can understand and resolve
    // the [Shared Folder] encoding we write there)
    private static final String USE_NEW_MOVE_MESSAGE_TYPE = "";

    // don't write a UNC path into the moved-data message
    private static final String DONT_WRITE_UNC_PATH = null;

    // a data element for recording the cloud storage directory we've moved to
    static final String CLOUD_DIRECTORY = "Team_Directory_Cloud";

    // we don't need the FileBundleMigrator to lock the source directory,
    // because we've locked it ourselves in this class
    private static boolean SOURCE_DIR_IS_ALREADY_LOCKED_FLAG = false;

    // a URL to direct team members to if they are on an old dashboard version
    private static final String CLOUD_STORAGE_UPGRADE_NEEDED_URL = //
            "https://www.processdash.com/cloudStorageUpgradeNeeded?" //
                    + HierarchySynchronizer.WBS_ALERT_TOKEN;

    // a right-arrow bullet for drawing attention to the upgrade link
    private static final String UPGRADE_BULLET_CHAR = "&#x2B9E;";

}
