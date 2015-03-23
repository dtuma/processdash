// Copyright (C) 2012 Tuma Solutions, LLC
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

/**
 * Saves a copy of the WBS and associated data structures into a specially
 * structured ZIP file.
 * 
 * This class supports several main end-user scenarios:
 * <ul>
 * 
 * <li><b>Network Loss:</b> When the team data directory is on a network server
 * and that server cannot be reached, users can save a copy of their changes so
 * they are not lost.</li>
 * 
 * <li><b>Alternative Plans:</b> When a team wants to create multiple
 * alternative plans during a launch, they can save each one into a separate ZIP
 * file.</li>
 * 
 * <li><b>What-If Scenarios:</b> During a project, a team leader might wish
 * to explore various replanning experiments without altering the team's
 * real plan.</li>
 * 
 * <li><b>Offline Work:</b> When an individual wants to take a copy of the WBS
 * and work with it offline, they can save it into a ZIP.</li>
 * 
 * </ul>
 */
public class WBSSaveAsWorker implements WBSFilenameConstants {

    private WBSEditor wbsEditor;

    private boolean disconnected;

    private HashSet<String> zipFileEntries;

    public WBSSaveAsWorker(WBSEditor e) {
        this.wbsEditor = e;
    }

    public File run() throws IOException {
        // create an output ZIP stream to contain the data we will be writing.
        File zipFile = TempFileFactory.get().createTempFile(
            "pdash-wbs-save-as", ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(zipFile)));

        // initialize the data structure for tracking content that has been
        // added to the ZIP.
        zipFileEntries = new HashSet<String>();

        // test to see if the team project directory is reachable.
        checkConnectivity();

        // write necessary data into the output ZIP file.
        copyPastHistoryIntoZip(zipOut);
        writeBaseSnapshotIntoZip(zipOut);
        saveCurrentDataIntoZip(zipOut);
        saveSupplementalDataIntoZip(zipOut);

        // finalize the ZIP file and return it.
        zipOut.finish();
        zipOut.close();
        return zipFile;
    }

    /**
     * Test to see if we are working with a "network lost" user scenario. If so,
     * set a flag.
     * 
     * Although the other operations in this class should theoretically complete
     * without error in that scenario, the network I/O timeouts could make the
     * "Save As" operation take an exceedingly long time. Setting this flag
     * allows those other operations to abort before performing an operation
     * that would trigger a network timeout.
     */
    private void checkConnectivity() {
        File srcDir = getTeamProjectSourceDirectory();
        this.disconnected = !srcDir.isDirectory();
    }

    /**
     * Look in the original working directory. If it was itself extracted from a
     * "Save As Zip" file, it will contain a subdirectory with a set of
     * historical ZIP files. Copy those files into our ZIP to retain the
     * complete history.
     */
    private void copyPastHistoryIntoZip(ZipOutputStream zipOut)
            throws IOException {
        if (disconnected)
            return;

        File srcDir = getTeamProjectSourceDirectory();
        File origHistory = new File(srcDir, HISTORY_SUBDIR);
        File[] origHistoryFiles = origHistory.listFiles();
        if (origHistoryFiles == null) {
            // In the "network loss" use case, the listFiles() method will
            // return null. Alternatively, if the current WBS was not opened
            // from a ZIP, the history subdirectory will not exist, and
            // listFiles() will also return null. In either case, we can simply
            // do nothing.
        } else {
            for (File f : origHistoryFiles) {
                String name = HISTORY_SUBDIR + "/" + f.getName();
                zipOut.putNextEntry(zipEntry(name, f.lastModified()));
                FileUtils.copyFile(f, zipOut);
                zipOut.closeEntry();
            }
        }
    }

    /**
     * When we perform a Save As operation, we are potentially introducing a
     * branch point allowing two simultaneous editing sessions to proceed
     * independently (one in the original WBS, and one in the saved copy). Make
     * sure we have a record of the original contents of the WBS (from the most
     * recent successful save operation in the original WBS) as they appeared
     * immediately before the branch.
     */
    private void writeBaseSnapshotIntoZip(ZipOutputStream zipOut)
            throws IOException {
        if (disconnected)
            return;

        // get the unique ID of the most recently saved change.
        ChangeHistory.Entry lastChange = wbsEditor.changeHistory.getLastEntry();
        if (lastChange == null)
            return;

        // check to see if we have already included this file in our ZIP
        String changeFileName = HISTORY_SUBDIR + "/" + lastChange.getUid()
                + ".zip";
        if (zipFileEntries.contains(changeFileName))
            return;

        // make a list of the files we need to back up
        File srcDir = getTeamProjectSourceDirectory();
        List<String> filesToBackup = FileUtils.listRecursively(srcDir,
            DashboardBackupFactory.WBS_FILE_FILTER);
        if (filesToBackup == null || filesToBackup.isEmpty())
            return;

        // create a new entry in our top-level ZIP file for this snapshot
        zipOut.putNextEntry(zipEntry(changeFileName, -1));

        // create a nested ZIP output stream to contain the snapshotted files
        ZipOutputStream historyZip = new ZipOutputStream(zipOut);
        for (String filename : filesToBackup) {
            File file = new File(srcDir, filename);
            ZipEntry e = new ZipEntry(filename);
            e.setTime(file.lastModified());
            historyZip.putNextEntry(e);
            FileUtils.copyFile(file, historyZip);
            historyZip.closeEntry();
        }
        historyZip.finish();

        // finalize the entry in our top-level ZIP file.
        zipOut.closeEntry();
    }

    /**
     * Write current data into the ZIP file for the WBS and its accompanying
     * data structures.
     * 
     * Note that the WBS may have "dirty" changes in-memory that have not been
     * saved to the working directory yet. So we must save the in-memory
     * structures instead of consulting the files in the working directory.
     */
    private void saveCurrentDataIntoZip(ZipOutputStream zipOut)
            throws IOException {
        // Create a temporary directory, and save all current data to it.
        File tempDir = TempFileFactory.get().createTempDirectory("wbs-save-as",
            "");
        boolean success = wbsEditor.teamProject.saveCopy(tempDir);
        if (!success)
            throw new IOException("Unable to save project files");
        wbsEditor.tabPanel.saveTabs(new File(tempDir, CUSTOM_TABS_FILE));

        // save a copy of the change history, and potentially update it if we
        // just saved dirty data.
        File changeHistFile = new File(tempDir, CHANGE_HISTORY_FILE);
        wbsEditor.changeHistory.write(changeHistFile);
        if (wbsEditor.isDirty()) {
            ChangeHistory newHist = new ChangeHistory(changeHistFile);
            newHist.addEntry(wbsEditor.owner);
            newHist.write(changeHistFile);
        }

        // copy all of these files into our output ZIP file.
        copyFilesToZip(tempDir, zipOut);

        // Now, delete the temporary directory.
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException ioe) {}
    }

    /**
     * The working directory contains additional files that are not actively
     * written by the WBS Editor (for example, "*-data.pdash" files). These
     * files are helpful during the editing process, so we copy them into the
     * output ZIP file as well.
     */
    private void saveSupplementalDataIntoZip(ZipOutputStream zipOut) {
        try {
            if (!disconnected)
                copyFilesToZip(getTeamProjectSourceDirectory(), zipOut);
        } catch (IOException e) {
            // if the source directory cannot be reached,
            e.printStackTrace();
        }
    }

    /**
     * Look in a directory and copy WBS-related files into our output ZIP.
     * 
     * If the output ZIP already contained a copy of a particular file, it will
     * not be rewritten a second time.
     */
    private void copyFilesToZip(File fromDir, ZipOutputStream toZip)
            throws IOException {
        List<String> filenames = FileUtils.listRecursively(fromDir,
            TeamDataDirStrategy.INSTANCE.getFilenameFilter());
        for (String filename : filenames) {
            if (!zipFileEntries.contains(filename)) {
                File file = new File(fromDir, filename);
                toZip.putNextEntry(zipEntry(filename, file.lastModified()));
                FileUtils.copyFile(file, toZip);
                toZip.closeEntry();
            }
        }
    }

    /**
     * @return the directory where project data is being stored.
     */
    private File getTeamProjectSourceDirectory() {
        return wbsEditor.teamProject.getStorageDirectory();
    }

    /**
     * Convenience routine to create a ZipEntry object, and simultaneously
     * record the entry name in the list of files that have been written to our
     * output ZIP.
     */
    private ZipEntry zipEntry(String name, long modTime) {
        ZipEntry e = new ZipEntry(name);
        if (modTime > 0)
            e.setTime(modTime);
        zipFileEntries.add(name);
        return e;
    }

}
