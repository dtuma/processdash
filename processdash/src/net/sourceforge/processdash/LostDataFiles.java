// Copyright (C) 1998-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash;


import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;


// class LostDataFiles contains information and methods that are used to
// deal with lost or damaged data files.

public class LostDataFiles implements FilenameFilter {

    /**
     * prefix used by {@link RobustFileOutputStream} to mark a temp file
     */
    private static final String TEMP_FILE_PREFIX = RobustFileOutputStream.OUT_PREFIX;

    /**
     * prefix used by {@link RobustFileOutputStream} to mark a backup file
     */
    private static final String BACKUP_FILE_PREFIX = RobustFileOutputStream.BACKUP_PREFIX;


    private File[] lostFiles; // contains the list of lost data files

    // the constructor
    public LostDataFiles() {
        lostFiles = null;
    }

    // implements the accept method for the FilenameFilter used by
    // File.listFiles().  A file matches if it looks like a temporary file
    // left around by the RobustFileOutputStream logic.
    public boolean accept(File location, String filename) {
        return filename.startsWith(TEMP_FILE_PREFIX);
    }

    /**
     * Make a list of files that appear to be lost or corrupt.
     */
    public void findLostFiles(String searchDir) {
        File searchFile = new File(searchDir);

        // First make sure we have a directory, then get a directory list
        // that matches the accept method
        if (searchFile.isDirectory()) {
            lostFiles = searchFile.listFiles(this);
        }
    }

    /**
     * Attempt to repair problems found, if any exist.
     * 
     * If no problems were found or if the repair is successful, returns true.
     * 
     * If the repair is unsuccessful, displays a warning dialog to the user
     * about the problem, and asks how they wish to proceed. If the user decides
     * to ignore the problem, returns true. Otherwise, returns false.
     */
    public boolean repair(ProcessDashboard dash) {
        if (lostFiles == null || lostFiles.length == 0)
            return true;

        int unrepairedCount = 0;
        for (int i = 0; i < lostFiles.length; i++) {
            File file = lostFiles[i];
            if (repairFile(file))
                lostFiles[i] = null;
            else
                unrepairedCount++;
        }
        if (unrepairedCount == 0)
            return true;

        // If there are unrepaired files, display an error dialog
        ProcessDashboard.dropSplashScreen();
        Resources r = Resources.getDashBundle("ProcessDashboard.Errors");
        String message = printOut() + "\n" + r.getString("Lost_Data_Message");
        String title = r.getString("Lost_Data_Title");
        int response = JOptionPane.showConfirmDialog(dash, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

        return response == JOptionPane.YES_OPTION;
    }

    private boolean repairFile(File f) {
        if (f.getName().startsWith(BACKUP_FILE_PREFIX))
            return fixBackupFile(f);
        else
            return fixTempFile(f);
    }

    private boolean fixBackupFile(File backupFile) {
        File origFile = getFileWithoutPrefix(backupFile, BACKUP_FILE_PREFIX);
        if (origFile == null)
            return false;  // shouldn't happen

        if (origFile.exists()) {
            backupFile.delete();
            return true;
        }

        try {
            FileUtils.renameFile(backupFile, origFile);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean fixTempFile(File tempFile) {
        File origFile = getFileWithoutPrefix(tempFile, TEMP_FILE_PREFIX);
        if (origFile == null)
            return false;  // shouldn't happen

        tempFile.delete();
        return true;
    }

    private File getFileWithoutPrefix(File file, String prefix) {
        if (file == null)
            return null;
        else if (file.getName().startsWith(prefix))
            return new File(file.getParent(), file.getName().substring(
                prefix.length()));
        else
            return null;
    }

    // printOut converts the data in lostFiles into a single printable string
    public String printOut() {
        StringBuilder result = new StringBuilder();

        for (File file : lostFiles)
            if (file != null)
                result.append(file.getName()).append("\n");

        return result.toString();
    }

}
