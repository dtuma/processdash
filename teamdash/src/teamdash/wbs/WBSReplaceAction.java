// Copyright (C) 2012-2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import static teamdash.wbs.WBSFilenameConstants.HISTORY_SUBDIR;
import static teamdash.wbs.WBSFilenameConstants.SETTINGS_FILENAME;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.TempFileFactory;

import teamdash.wbs.ChangeHistory.Entry;

public class WBSReplaceAction extends AbstractAction {

    private WBSEditor wbsEditor;

    private WBSOpenFileAction openAction;

    private static final Logger logger = Logger
            .getLogger(WBSReplaceAction.class.getName());

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.File_Replace");

    public WBSReplaceAction(WBSEditor wbsEditor, WBSOpenFileAction openAction) {
        super(resources.getString("Menu"));
        this.wbsEditor = wbsEditor;
        this.openAction = openAction;
        setEnabled(editable());
    }

    public void actionPerformed(ActionEvent e) {
        if (!editable()) {
            setEnabled(false);

        } else if (confirmBeginOperation() && editable()) {
            File srcFile = promptForFile();
            if (srcFile != null && editable())
                confirmChangesAndReplaceFromFile(srcFile);
        }
    }

    private boolean editable() {
        return !wbsEditor.readOnly;
    }

    private boolean confirmBeginOperation() {
        String title = resources.getString("Confirm.Title");
        String[] message = resources.getStrings("Confirm.Message");
        int userChoice = JOptionPane.showConfirmDialog(wbsEditor.frame,
            message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return (userChoice == JOptionPane.YES_OPTION);
    }

    private File promptForFile() {
        return openAction.promptForExistingFile(resources
                .getString("Dialog_Title"));
    }

    private void confirmChangesAndReplaceFromFile(File srcFile) {
        // extract the files into a temporary directory
        File replacementDir;
        try {
            replacementDir = extractChanges(srcFile);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to extract files", ioe);
            displayUnableToExtractError();
            return;
        }

        // check to see what changes would be lost. Alert the user and confirm.
        List<Entry> lostChanges = getChangesThatWillBeLost(replacementDir);
        if (confirmLostChanges(srcFile, lostChanges))
            // if the user agrees, make the change.
            new WorkerThread(srcFile, replacementDir).start();
    }

    private File extractChanges(File srcFile) throws IOException {
        // create a temporary directory
        File extractDirectory = TempFileFactory.get().createTempDirectory(
            "pdash-wbs-replace", ".tmp");

        // open the ZIP file
        InputStream in = new FileInputStream(srcFile);
        try {
            ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                    in));
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null) {
                if (!e.isDirectory()) {
                    String filename = e.getName();
                    File f = new File(extractDirectory, filename);
                    if (filename.indexOf('/') != -1)
                        f.getParentFile().mkdirs();
                    FileUtils.copyFile(zipIn, f);
                }
            }
        } finally {
            FileUtils.safelyClose(in);
        }

        // the settings.xml file in the replacement data might name a master
        // project. we don't need that information to perform the replacement
        // operation, and it would just slow down the loading of the replacement
        // team project; so we temporarily move that file out of the way.
        maybeRename(extractDirectory, SETTINGS_FILENAME, "X"+SETTINGS_FILENAME);

        return extractDirectory;
    }

    private void displayUnableToExtractError() {
        String title = resources.getString("Error_Title");
        String[] message = resources.getStrings("Extraction_Error_Message");
        JOptionPane.showMessageDialog(wbsEditor.frame, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Get a list of past edits to the current team project that will be
     * overwritten if the replacement proceeds.
     */
    private List<Entry> getChangesThatWillBeLost(File replacementDir) {
        List<Entry> wbsChanges = wbsEditor.changeHistory.getEntries();
        Entry e = getMostRecentCommonChange(replacementDir);
        if (e == null) {
            // there was no common change ancestor. Return a list of all the
            // changes that have ever been made to this WBS. Insert a "null"
            // entry at the beginning of the list as a signal that this
            // condition has occurred.
            if (!wbsChanges.isEmpty())
                wbsChanges.add(0, null);
            return wbsChanges;
        }

        // make a list of the entries in the current team project that follow
        // the most common change ancestor.
        List<Entry> result = new ArrayList<Entry>();
        for (int i = wbsChanges.size();  i-- > 0; ) {
            Entry w = wbsChanges.get(i);
            if (w.getUid().equals(e.getUid()))
                break;
            result.add(0, w);
        }
        return result;
    }

    /**
     * Search through the change history of the current team project and the
     * replacement project, and find the most recent change entry that they
     * have in common.  If they have no changes in common, returns null.
     */
    private Entry getMostRecentCommonChange(File replacementDir) {
        // make a list of the change UIDs for the main WBS.
        Set<String> wbsEntryUids = new HashSet<String>();
        for (Entry e : wbsEditor.changeHistory.getEntries())
            wbsEntryUids.add(e.getUid());

        // Load the change history for the replacement file.
        ChangeHistory replacementHist = new ChangeHistory(replacementDir);
        List<Entry> replacementChanges = replacementHist.getEntries();

        // find the latest change history that is shared in common.
        for (int i = replacementChanges.size(); i-- > 0;) {
            Entry e = replacementChanges.get(i);
            String uid = e.getUid();
            if (wbsEntryUids.contains(uid))
                return e;
        }

        // no common change entry was found.
        return null;
    }

    /**
     * Display a dialog to the user warning them about changes they are about
     * to overwrite, and get confirmation that they wish to continue.
     */
    private boolean confirmLostChanges(File srcFile, List<Entry> lostChanges) {
        if (lostChanges == null || lostChanges.isEmpty())
            return true;

        boolean sameWbs = true;
        if (lostChanges.get(0) == null) {
            lostChanges.remove(0);
            sameWbs = false;
        }

        Object[] items = new Object[lostChanges.size()];
        for (int i = 0;  i < items.length;  i++) {
            Entry e = lostChanges.get(i);
            items[i] = resources.format("Lost_Changes.Item_FMT", e.getUser(),
                e.getTimestamp());
        }

        JList list = new JList(items);
        JScrollPane sp = new JScrollPane(list);
        Dimension d = list.getPreferredSize();
        d.height = Math.min(d.height + 5, 200);
        sp.setPreferredSize(d);

        String title = resources.getString("Lost_Changes.Title");
        Object[] message = new Object[] {
                resources.formatStrings("Lost_Changes.Header_FMT",
                        srcFile.getPath()), " ",
                resources.getStrings(sameWbs
                        ? "Lost_Changes.Same_WBS_Message"
                        : "Lost_Changes.Different_WBS_Message"),
                sp,
                resources.getString("Lost_Changes.Footer")
        };
        int userChoice = JOptionPane.showConfirmDialog(wbsEditor.frame,
            message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return userChoice == JOptionPane.YES_OPTION;
    }

    /**
     * Our WBS ZIP files contain historical snapshots that can be used as the
     * parent in a 3-way merge operation.  However, these snapshots are not
     * created on every save.  Since we just restored data from one WBS to
     * another, we've created a new branch point when these two WBSes looked
     * identical.  Make sure our ZIP file contains a snapshot of what the
     * data looked like at this branch point.
     */
    private void maybeAddSnapshotToSrcZip(File srcZip, File replacementDir)
            throws IOException {
        // get the unique ID of the change we just used in the replacement
        ChangeHistory changeHistory = new ChangeHistory(replacementDir);
        Entry lastChange = changeHistory.getLastEntry();
        if (lastChange == null)
            return;

        // check to see if the source ZIP already included a snapshot
        // corresponding to that change
        String changeFileName = HISTORY_SUBDIR + "/" + lastChange.getUid()
                + ".zip";
        File snapshotFile = new File(replacementDir, changeFileName);
        if (snapshotFile.isFile())
            return;

        // Make a list of the files that should be included in the snapshot
        List<String> newSnapshotFiles = FileUtils.listRecursively(
            replacementDir, DashboardBackupFactory.WBS_FILE_FILTER);
        if (newSnapshotFiles == null || newSnapshotFiles.isEmpty())
            return;

        // If we renamed the settings.xml file, change the name back before
        // we rebuild the ZIP file.
        maybeRename(replacementDir, "X"+SETTINGS_FILENAME, SETTINGS_FILENAME);

        // rebuild the source ZIP file and add a new snapshot
        RobustFileOutputStream rOut = new RobustFileOutputStream(srcZip);
        try {
            ZipOutputStream zipOut = new ZipOutputStream(
                    new BufferedOutputStream(rOut));

            // add the existing file contents back to the ZIP
            List<String> existingFiles = FileUtils.listRecursively(
                replacementDir, null);
            addToZip(replacementDir, existingFiles, zipOut);

            // create a new snapshot and add it to the ZIP
            zipOut.putNextEntry(new ZipEntry(changeFileName));
            ZipOutputStream historyZip = new ZipOutputStream(zipOut);
            addToZip(replacementDir, newSnapshotFiles, historyZip);
            historyZip.finish();
            zipOut.closeEntry();

            zipOut.finish();
            zipOut.flush();
        } catch (IOException ioe) {
            rOut.abort();
            throw ioe;
        }

        rOut.close();
    }

    private void addToZip(File dir, List<String> filenames,
            ZipOutputStream zipOut) throws IOException {
        for (String filename : filenames) {
            File file = new File(dir, filename);
            if (file.isFile()) {
                ZipEntry e = new ZipEntry(filename);
                e.setTime(file.lastModified());
                zipOut.putNextEntry(e);
                FileUtils.copyFile(file, zipOut);
                zipOut.closeEntry();
            }
        }
    }

    private void maybeRename(File dir, String filename, String newName) {
        File f = new File(dir, filename);
        if (f.isFile())
            f.renameTo(new File(dir, newName));
    }

    private class WorkerThread extends SwingWorker {

        private File replacementFile;

        private File replacementDir;

        private JDialog waitDialog;

        private Exception errorEncountered;

        public WorkerThread(File replacementFile, File replacementDir) {
            this.replacementFile = replacementFile;
            this.replacementDir = replacementDir;
            this.waitDialog = WBSEditor.createWaitDialog(wbsEditor.frame,
                resources.getString("Working"));
        }

        @Override
        public void start() {
            super.start();
            waitDialog.setVisible(true);
        }

        @Override
        public Object construct() {
            logger.info("Initiating replacement of WBS data from file "
                    + replacementFile);

            final TeamProject replacementProject = new TeamProject(
                    replacementDir, "replacement");

            tweakIncomingSettings(replacementProject.getUserSettings());

            try {
                // tell the WBS Editor to perform the replacement.
                wbsEditor.replaceDataFrom(replacementProject);
            } catch (Exception e) {
                // we had a problem with the data replacement.
                logger.log(Level.SEVERE,
                    "Encountered exception when replacing data from file"
                            + replacementFile, e);
                errorEncountered = e;
            }

            try {
                // Try adding a snapshot to the replacement file if needed
                maybeAddSnapshotToSrcZip(replacementFile, replacementDir);

                // finally, delete our temporary directory.
                FileUtils.deleteDirectory(replacementDir, true);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Encountered exception when "
                        + "cleaning up after data replacement", e);
            }

            if (errorEncountered == null)
                logger.info("Successfully completed replacing data from file "
                        + replacementFile);

            return null;
        }

        /**
         * We generally do not want to alter the "user settings" during a
         * replace data operation. This method discards the incoming settings
         * and replaces them with the current settings from the team project.
         */
        private void tweakIncomingSettings(Properties incomingSettings) {
            incomingSettings.clear();
            incomingSettings.putAll(wbsEditor.teamProject.getUserSettings());
        }

        @Override
        public void finished() {
            waitDialog.dispose();

            if (errorEncountered != null)
                ExceptionDialog.show(wbsEditor.frame,
                    resources.getString("Error_Title"),
                    resources.getStrings("Replacement_Error_Message"), " ",
                    errorEncountered);
        }

    }

}
