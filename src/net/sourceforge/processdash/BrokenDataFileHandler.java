// Copyright (C) 1998-2022 Tuma Solutions, LLC
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


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.AbstractWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.ErrorReporter;
import net.sourceforge.processdash.ui.lib.JLinkLabel;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;


// class LostDataFiles contains information and methods that are used to
// deal with lost or damaged data files.

public class BrokenDataFileHandler {

    private static final String DOWNLOAD_URL = "http://www.processdash.com/download";

    public static final String SHARE_MCF_URL = "http://www.tuma-solutions.com/metricsFrameworkEditor.html#share";

    /**
     * prefix used by {@link RobustFileOutputStream} to mark a temp file
     */
    private static final String TEMP_FILE_PREFIX = RobustFileOutputStream.OUT_PREFIX;

    /**
     * prefix used by {@link RobustFileOutputStream} to mark a backup file
     */
    private static final String BACKUP_FILE_PREFIX = RobustFileOutputStream.BACKUP_PREFIX;

    /**
     * a list of temporary files left around by a crashed RobustFile operation
     */
    private File[] lostFiles;

    /**
     * a collection of files that contain null bytes
     */
    private Set<File> nullByteFiles;

    /**
     * a mapping whose keys are data file prefixes, and whose values are the
     * names of data files which could not be found.
     */
    private Map<String, String> missingDataFiles;

    // the constructor
    public BrokenDataFileHandler() {
        lostFiles = null;
        missingDataFiles = new HashMap<String, String>();
    }

    /**
     * Make a list of files that appear to be lost or corrupt.
     */
    public void findCorruptFiles(String searchDir, WorkingDirectory workingDir) {
        File searchFile = new File(searchDir);

        // First make sure we have a directory, then get a list of files in
        // that directory that appear to have been left around by the
        // RobustFileOutputStream logic.
        if (searchFile.isDirectory()) {
            lostFiles = searchFile.listFiles(new FilenameFilter() {
                public boolean accept(File location, String filename) {
                    return filename.startsWith(TEMP_FILE_PREFIX);
                }
            });
        }

        // get a list of files that contain null bytes, for later investigation
        if (workingDir instanceof AbstractWorkingDirectory) {
            nullByteFiles = ((AbstractWorkingDirectory) workingDir)
                    .getFilesWithNullBytes();
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
    public boolean repairCorruptFiles(Component dialogParent) {
        if (repairLostFiles(dialogParent) == false)
            return false;

        repairFilesWithNullBytes(dialogParent);
        return true;
    }

    private boolean repairLostFiles(Component dialogParent) {
        if (lostFiles == null || lostFiles.length == 0)
            return true;

        // sort the files so that backups are first, followed by temp files.
        // this allows our repair logic to prefer applying a backup-based
        // repair, as it is more reliable than a temp-file-based repair.
        Arrays.sort(lostFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return getFileType(f1) - getFileType(f2);
            }
            private int getFileType(File f) {
                return (isBackupFile(f) ? 1 : 2);
            }
        });

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
        Object message = new Object[] { new JOptionPaneTweaker.ToFront(),
                getCorruptFileStr(), r.getString("Lost_Data_Message") };
        String title = r.getString("Lost_Data_Title");
        int response = JOptionPane.showConfirmDialog(dialogParent, message,
            title, JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

        return response == JOptionPane.YES_OPTION;
    }

    private boolean repairFile(File f) {
        if (isBackupFile(f))
            return fixBackupFile(f);
        else
            return fixTempFile(f);
    }

    private boolean isBackupFile(File f) {
        return f.getName().startsWith(BACKUP_FILE_PREFIX);
    }

    private boolean fixBackupFile(File backupFile) {
        File origFile = getFileWithoutPrefix(backupFile, BACKUP_FILE_PREFIX);
        if (origFile == null)
            return false;  // shouldn't happen

        if (origFile.exists()) {
            if (startsWithNullByte(origFile)) {
                // if the original file is showing signs of corruption, delete
                // it and keep the backup file instead.
                origFile.delete();
            } else {
                // if we have an intact original file in place, we can delete
                // the backup.
                backupFile.delete();
                return true;
            }
        }

        // move the backup file back into the place of the original.
        return safelyRename(backupFile, origFile);
    }

    private boolean fixTempFile(File tempFile) {
        File origFile = getFileWithoutPrefix(tempFile, TEMP_FILE_PREFIX);
        if (origFile == null)
            return false;  // shouldn't happen

        if (startsWithNullByte(origFile)) {
            // our original file appears to be corrupted, so it isn't worth
            // keeping.  If the corruption occurred as a result of work by the
            // Robust writer, that would suggest that the temporary file had
            // been written completely; so it is likely that the temporary file
            // contains more reliable data than the corrupted original. Replace
            // the corrupted original with the temp file.
            origFile.delete();
            return safelyRename(tempFile, origFile);

        } else {
            tempFile.delete();
            return true;
        }
    }

    private File getFileWithoutPrefix(File file, String prefix) {
        if (file == null)
            return null;
        else if (file.getName().startsWith(prefix))
            return RobustFileOutputStream.getOriginalFile(file);
        else
            return null;
    }

    private boolean startsWithNullByte(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            return (in.read() == 0);
        } catch (IOException e) {
            return false;
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    private boolean safelyRename(File srcFile, File destFile) {
        try {
            System.out.println("**************** REPAIRING CORRUPT "
                    + destFile.getName() + " with data from "
                    + srcFile.getName() + " *****************");
            FileUtils.renameFile(srcFile, destFile);
            if (nullByteFiles != null)
                nullByteFiles.remove(destFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // printOut converts the data in lostFiles into a single printable string
    public String getCorruptFileStr() {
        StringBuilder result = new StringBuilder();

        for (File file : lostFiles)
            if (file != null)
                result.append(file.getName()).append("\n");

        return result.toString();
    }



    /**
     * If any files have been corrupted with null bytes, attempt to find
     * replacements from a backup. If successful, display a warning to the user
     * about what just happened.
     */
    private void repairFilesWithNullBytes(Component dialogParent) {
        if (nullByteFiles == null || nullByteFiles.isEmpty())
            return;

        Set<String> repairedNullFiles = new TreeSet<String>();
        for (File file : nullByteFiles) {
            if (shouldRepairNull(file) && repairNullByteFile(file))
                repairedNullFiles.add("        " + file.getPath());
        }

        if (repairedNullFiles.isEmpty())
            return;

        // If we repaired any null files, display a warning. (No warning is
        // displayed for files that could not be repaired; these will trigger a
        // warning later when the corrupt data is encountered by the dashboard.)
        ProcessDashboard.dropSplashScreen();
        Resources r = Resources.getDashBundle("ProcessDashboard.Errors");
        String title = r.getString("Repaired_Null.Title");
        Object message = new Object[] { r.getString("Repaired_Null.Header"),
                StringUtils.join(repairedNullFiles, "\n"), " ",
                new JOptionPaneTweaker.ToFront(),
                r.getString("Repaired_Null.Footer") };
        JOptionPane.showMessageDialog(dialogParent, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private boolean shouldRepairNull(File f) {
        String name = f.getName().toLowerCase(Locale.ENGLISH);
        for (String extension : FILE_TYPES_FOR_NULL_REPAIR)
            if (name.endsWith(extension))
                return startsWithNullByte(f);

        return false;
    }

    private static final String[] FILE_TYPES_FOR_NULL_REPAIR = { ".dat",
            ".def", ".xml", "pspdash.ini", "state" };

    private boolean repairNullByteFile(File f) {
        InputStream in = findReplacementFromBackup(f);
        if (in == null)
            return false;

        try {
            FileUtils.copyFile(in, f);
            return true;
        } catch (IOException ioe) {
            return false;
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    private InputStream findReplacementFromBackup(File f) {
        // look for a "backup" subdirectory
        File backupDir = new File(f.getParentFile(), "backup");
        if (!backupDir.isDirectory())
            return null;

        // find a list of ZIP file in that directory, and look inside each
        // one (starting with the most recent) for a copy of the given file.
        File[] backupZipFiles = backupDir.listFiles(new FilenameFilter() {
            public boolean accept(File location, String name) {
                return name.startsWith("pdash-") && name.endsWith(".zip");
            }
        });
        Arrays.sort(backupZipFiles);
        for (int i = backupZipFiles.length; i-- > 0;) {
            InputStream zipFileIn = null;
            try {
                File zip = backupZipFiles[i];
                ZipInputStream in = new ZipInputStream(new BufferedInputStream(
                        zipFileIn = new FileInputStream(zip)));
                ZipEntry e;
                while ((e = in.getNextEntry()) != null) {
                    if (e.getName().equalsIgnoreCase(f.getName())) {
                        PushbackInputStream pb = new PushbackInputStream(in, 1);
                        int b = pb.read();
                        if (b == 0)
                            // this ZIP contains a corrupt version of the file.
                            // abort and try the next ZIP.
                            break;

                        // otherwise, we've found a viable historical file.
                        System.out.println("*************** REPAIRING CORRUPT "
                                + f.getName() + " with data from backup/"
                                + zip.getName() + " *****************");
                        pb.unread(b);
                        return pb;
                    }
                }
            } catch (Exception ex) {
                // if a given ZIP file is damaged or corrupt, skip it and look
                // through the previous ZIP files to scan further back in time
            }
            FileUtils.safelyClose(zipFileIn);
        }

        // no replacement file was found in the recent backups.
        System.out.println("*************** FILE " + f.getName()
                + " APPEARS TO BE CORRUPT *****************");
        return null;
    }

    public void logMissingDataFileError(String prefix, String filename) {
        missingDataFiles.put(prefix, filename);
    }

    public List<String> getMissingDataPrefixes() {
        return new ArrayList<String>(missingDataFiles.keySet());
    }

    public void showMissingDataFileWarnings() {
        // no missing data files? Great! Do nothing and return.
        if (missingDataFiles.isEmpty())
            return;

        // prepare to display an error dialog.
        ProcessDashboard.dropSplashScreen();
        Resources resources = Resources.getDashBundle("ProcessDashboard.Errors");

        // find the errors which can be attributed to missing MCFs.
        Map<String, String> warnings = new HashMap(missingDataFiles);
        Map<String, List<String>> mcfProjects = findMissingMcfProjects(warnings);

        // If they are missing one of the standard MCFs, print a message
        // telling them to download and run the dashboard installer.
        showMcfWarning(resources, "Missing_MCF.TSP_Footer", "TSP",
            mcfProjects.remove("TSP"), DOWNLOAD_URL);
        showMcfWarning(resources, "Missing_MCF.TSP_Footer", "PDSSD",
            mcfProjects.remove("PDSSD"), DOWNLOAD_URL);

        // If they are missing a custom MCF, print a message telling them
        // to obtain it from their team leader and install it.
        String resKey = CompressedInstanceLauncher.isRunningFromCompressedData()
                ? "Missing_MCF.Custom_Footer_Zip_FMT"
                : "Missing_MCF.Custom_Footer_FMT";
        for (Map.Entry<String, List<String>> e : mcfProjects.entrySet())
            showMcfWarning(resources, resKey, e.getKey(), e.getValue(),
                SHARE_MCF_URL);

        // if any other files are missing, display a more generic error.
        ErrorReporter errorReporter = new ErrorReporter(
                resources.getString("Broken_Data_Title"),
                resources.getStrings("Broken_Data_Header"),
                resources.getStrings("Broken_Data_Footer"));
        for (String dataPrefix : warnings.keySet())
            errorReporter.logError(dataPrefix);
        errorReporter.done();
    }

    private Map<String, List<String>> findMissingMcfProjects(
            Map<String, String> warnings) {
        Map<String, List<String>> result = new HashMap();

        Pattern mcfFilenamePattern = Pattern
                .compile("<(.+)/(master|team|indiv|rollup).*_dataFile.txt>");

        for (Iterator i = warnings.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, String> e = (Map.Entry<String, String>) i.next();
            String dataPrefix = e.getKey();
            String missingFile = e.getValue();
            Matcher m = mcfFilenamePattern.matcher(missingFile);
            if (m.matches()) {
                i.remove();
                if (missingFile.contains("_root_")) {
                    String pid = m.group(1);
                    List<String> projectList = result.get(pid);
                    if (projectList == null)
                        result.put(pid, projectList = new ArrayList());
                    projectList.add(dataPrefix);
                }
            }
        }

        return result;
    }

    private void showMcfWarning(Resources resources, String resKey, String pid,
            List<String> projects, final String url) {
        if (projects == null || projects.isEmpty())
            return;

        String title = resources.getString("Missing_MCF.Title");
        JList projectList = new JList(projects.toArray());
        projectList.setVisibleRowCount(Math.min(projects.size(), 10));
        ActionListener hyperlink = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Browser.launch(url);
            }};
        Object[] message = new Object[] { new JOptionPaneTweaker.ToFront(),
            formatString(resources.format("Missing_MCF.Header_FMT", pid), null),
            BoxUtils.hbox(30, new JScrollPane(projectList)), " ", //
            formatString(resources.format(resKey, pid,
                TemplateLoader.getApplicationTemplateDir()), hyperlink) };
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private Object[] formatString(String str, ActionListener hyperlink) {
        str = StringUtils.findAndReplace(str, "[[[", "<a>");
        str = StringUtils.findAndReplace(str, "]]]", "</a>");
        List result = new ArrayList();
        for (String paragraph : str.split("\n")) {
            for (String line : StringUtils.breakDownString(paragraph, 75, true))
                result.add(formatOneLine(line, hyperlink));
        }
        return result.toArray();
    }

    private Object formatOneLine(String line, ActionListener hyperlink) {
        boolean hasLinkStart = line.contains("<a");
        boolean hasLinkEnd = line.contains("</a");
        if (hasLinkStart || hasLinkEnd) {
            if (!hasLinkStart)
                line = "<a>" + line;
            if (!hasLinkEnd)
                line = line + "</a>";
            return new JLinkLabel(line, hyperlink);
        } else {
            return line;
        }
    }

}
