// Copyright (C) 2007-2016 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.tool.redact.RedactFilterer;
import net.sourceforge.processdash.tool.redact.ui.RedactFilterConfigDialog;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XorOutputStream;

public class SaveBackupAction extends AbstractAction {

    private DataContext dataContext;

    private BackupCoordinator backupCoordinator = null;

    private File lastBackupDirectory;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard");

    public SaveBackupAction(DashboardContext dashContext) {
        super(resources.getString("Menu.Save_Backup"));
        this.dataContext = dashContext.getData();
        RedactFilterer.setDashboardContext(dashContext);
    }

    public void actionPerformed(ActionEvent e) {
        if (backupCoordinator != null) {
            backupCoordinator.beep();
        } else {
            backupCoordinator = new BackupCoordinator();
            backupCoordinator.start();
        }
    }

    private class BackupCoordinator extends Thread {

        ProgressDialog progressDialog;

        File destFile;

        Set<String> redactFilters;

        public BackupCoordinator() {
            progressDialog = new ProgressDialog();
        }

        public void beep() {
            if (progressDialog.isShowing())
                progressDialog.toFront();

            Toolkit.getDefaultToolkit().beep();
        }

        @Override
        public void run() {
            try {
                doBackup();
            } finally {
                doOnSwingThread("finished");
            }
        }

        private void doBackup() {
            // request dirty GUIs to prompt for save BEFORE we display our
            // "Save As" dialog.
            DashController.saveDirtyGuiData();

            // get a backup operation started and running.
            BackupTask backupTask = new BackupTask();
            backupTask.start();

            // while the backup is running, ask the user where the backup
            // should be saved.
            doOnSwingThread("promptForDestFile");

            // if the user pressed the cancel button, abort.
            if (destFile == null)
                return;

            doOnSwingThread("showProgressDialog");

            try {
                makeCopyOfBackup(backupTask, destFile);
            } catch (Exception e) {
                e.printStackTrace();
                doOnSwingThread("showErrorMessage");
                return;
            }

            doOnSwingThread("showSuccess");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }

        @SuppressWarnings("unused")
        public void promptForDestFile() {
            promptForDestFile(getDefaultFilename());
        }

        private void promptForDestFile(String defaultFilename) {
            redactFilters = null;
            destFile = getDestFile(defaultFilename);
            if (destFile != null && destFile.getName().endsWith(RPDBK)) {
                redactFilters = RedactFilterConfigDialog.promptForFilters();
                if (redactFilters == null)
                    promptForDestFile(removeFilenameSuffix(destFile.getName()));
            }
        }

        @SuppressWarnings("unused")
        public void showProgressDialog() {
            progressDialog.setLocationRelativeTo(null);
            progressDialog.setVisible(true);
        }

        private void makeCopyOfBackup(BackupTask backupTask, File dest)
                throws Exception {
            backupTask.join();

            File backupFile = backupTask.getBackupFile();
            if (backupFile == null)
                throw new Exception("Couldn't backup data");

            OutputStream out = new BufferedOutputStream(new FileOutputStream(
                    dest));
            if (dest.getName().toLowerCase().endsWith(PDBK))
                out = new XorOutputStream(out,
                        CompressedInstanceLauncher.PDASH_BACKUP_XOR_BITS);

            if (redactFilters == null)
                FileUtils.copyFile(backupFile, out);
            else
                new RedactFilterer(redactFilters).doFilter(backupFile, out);
            out.close();
        }

        @SuppressWarnings("unused")
        public void showErrorMessage() {
            String[] message = resources
                    .getStrings("Save_Backup.Error.Message");
            String title = resources.getString("Save_Backup.Error.Title");
            JOptionPane.showMessageDialog(null, message, title,
                JOptionPane.ERROR_MESSAGE);
        }

        @SuppressWarnings("unused")
        public void showSuccess() {
            progressDialog.showSuccess();
        }

        @SuppressWarnings("unused")
        public void finished() {
            progressDialog.dispose();
            backupCoordinator = null;
        }

        private void doOnSwingThread(final String method) {
            try {
                final Method m = BackupCoordinator.class.getMethod(method);
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        try {
                            m.invoke(BackupCoordinator.this);
                        } catch (Exception e) {
                        }
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private class ProgressDialog extends JDialog {

        JLabel prompt;

        JProgressBar progressBar;

        public ProgressDialog() {
            setTitle(resources.getString("Save_Backup.Progress.Title"));
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            prompt = new JLabel(resources
                    .getString("Save_Backup.Progress.Prompt"));
            prompt.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 10));
            panel.add(prompt, BorderLayout.NORTH);

            this.progressBar = new JProgressBar();
            this.progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.CENTER);

            panel.add(Box.createHorizontalStrut(200), BorderLayout.SOUTH);
            getContentPane().add(panel);
            pack();
        }

        public void showSuccess() {
            String finished = resources
                    .getString("Save_Backup.Progress.Finished");
            setTitle(finished);
            prompt.setText(finished);
            progressBar.setIndeterminate(false);
            progressBar.setValue(progressBar.getMaximum());
        }
    }

    private class BackupTask extends Thread {

        private File backupFile = null;

        @Override
        public void run() {
            backupFile = DashController.backupData();
        }

        public File getBackupFile() {
            return backupFile;
        }

    }

    private File getDestFile(String defaultFilename) {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setSelectedFile(new File(getDefaultDirectory(fc),
                defaultFilename));
        fc.setDialogTitle(resources.getString("Save_Backup.Window_Title"));
        String defaultType = Settings.getVal(TYPE_PREF, ZIP);
        for (String type : BACKUP_FILE_TYPES) {
            ExampleFileFilter filter = makeFilter(type);
            fc.addChoosableFileFilter(filter);
            if (type.equalsIgnoreCase(defaultType))
                fc.setFileFilter(filter);
        }

        if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        File dest = fc.getSelectedFile();
        saveDefaultDirectory(dest);
        if (dest == null)
            return null;

        ExampleFileFilter ff = (ExampleFileFilter) fc.getFileFilter();
        File result = ff.maybeAppendExtension(dest);
        String resultType = ff.getExtension(result);
        if (StringUtils.hasValue(resultType))
            InternalSettings.set(TYPE_PREF, resultType);
        return result;
    }

    public File selectBackupFile(Component parent, String title) {
        JFileChooser fc = new JFileChooser();
        if (lastBackupDirectory != null)
            fc.setCurrentDirectory(lastBackupDirectory);
        fc.setDialogTitle(title);
        ExampleFileFilter ff = makeFilter(PDBK);
        for (String type : BACKUP_FILE_TYPES)
            ff.addExtension(type);
        ff.addFilename("state");
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);

        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
            return null;

        File result = fc.getSelectedFile();
        saveDefaultDirectory(result);
        return result;
    }

    private void saveDefaultDirectory(File f) {
        if (f != null) {
            lastBackupDirectory = f.getParentFile();
            if (lastBackupDirectory != null)
                InternalSettings.set(DIR_PREF, lastBackupDirectory.getPath());
        }
    }

    private File getDefaultDirectory(JFileChooser fc) {
        if (lastBackupDirectory != null)
            return lastBackupDirectory;

        String path = Settings.getVal(DIR_PREF);
        if (StringUtils.hasValue(path)) {
            File result = new File(path);
            if (result.isDirectory())
                return result;
        }

        return fc.getCurrentDirectory();
    }

    private String getDefaultFilename() {
        String owner = ProcessDashboard.getOwnerName(dataContext);
        if (owner == null)
            owner = "";
        else
            owner = FileUtils.makeSafe(owner) + "-";

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date date = EVCalculator.getFixedEffectiveDate();
        return "pdash-" + owner + fmt.format(date != null ? date : new Date());
    }

    private String removeFilenameSuffix(String filename) {
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1)
            return filename;
        else
            return filename.substring(0, dotPos);
    }

    private ExampleFileFilter makeFilter(String ext) {
        String descr = resources.getString("Save_Backup." + ext
                + ".File_Description");
        return new ExampleFileFilter(ext, descr);
    }

    private static final String PDBK = CompressedInstanceLauncher.PDASH_BACKUP_EXTENSION;
    private static final String RPDBK = RedactFilterer.REDACTED_PDASH_BACKUP_EXTENSION;
    private static final String ZIP = "zip";
    private static final String[] BACKUP_FILE_TYPES = { PDBK, RPDBK, ZIP };

    private static final String DIR_PREF = "userPref.saveBackup.dir";
    private static final String TYPE_PREF = "userPref.saveBackup.type";

}
