// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;


public class SvnLOCDiff extends LOCDiffReportGenerator {

    private File baseDirectory = null;

    public SvnLOCDiff(List languageFilters) {
        this(languageFilters, ".");
    }

     public SvnLOCDiff(List languageFilters, String dir) {
         super(languageFilters);
         setBaseDirectory(dir);
     }

     public void setBaseDirectory(String baseDirectory) {
         this.baseDirectory = new File(baseDirectory);
     }

    protected Collection getFilesToCompare() throws IOException {
        List result = new ArrayList();

        getOpenedFilesToCompare(result);
        filterMovedDeletions(result);

        return result;
    }

    private void getOpenedFilesToCompare(List fileList) throws IOException {
        String[] cmd = new String[] { "svn", "status" };
        Process proc = Runtime.getRuntime().exec(cmd, null, baseDirectory);
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = OPENED_FILE_PATTERN.matcher(line);
            if (m.matches()) {
                String changeType = m.group(1);
                boolean hasHistory = "+".equals(m.group(2));
                String filename = m.group(3);
                File f = new File(baseDirectory, filename);
                if (!f.isDirectory())
                    fileList.add(new SvnFile(filename, hasHistory, changeType));
            }
        }
    }
    private static final Pattern OPENED_FILE_PATTERN = Pattern.compile
        ("([AMD])..(.).\\s+(.*)");

    /** When a file is moved or renamed, it shows up as deleted from its original
     * location and "added with history" to its new location.  We'd prefer these
     * files to appear as a single entry in the report we generate.  This method
     * determines whether any of the deleted files in the list fit that description,
     * and removes them from the list.
     */
    private void filterMovedDeletions(List fileList) {
        List deletedFiles = new ArrayList();
        List historyFiles = new ArrayList();
        for (Iterator i = fileList.iterator(); i.hasNext();) {
            SvnFile f = (SvnFile) i.next();
            if (f.getType() == DELETED)
                deletedFiles.add(f);
            if (f.getHasHistory())
                historyFiles.add(f);
        }
        if (!deletedFiles.isEmpty() && !historyFiles.isEmpty()) {
            for (Iterator i = historyFiles.iterator(); i.hasNext();) {
                SvnFile histFile = (SvnFile) i.next();
                for (Iterator j = deletedFiles.iterator(); j.hasNext();) {
                    SvnFile deletedFile = (SvnFile) j.next();
                    if (histFile.isMovedFrom(deletedFile)) {
                        fileList.remove(deletedFile);
                        deletedFiles.remove(deletedFile);
                        break;
                    }
                }
            }
        }
    }

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;

    private class SvnFile implements FileToCompare {

        protected String filename;
        protected boolean hasHistory;
        protected int type;

        public SvnFile(String filename, boolean hasHistory, String type) {
            this.filename = filename;
            this.hasHistory = hasHistory;
            if ("A".equals(type))
                this.type = ADDED;
            else if ("D".equals(type))
                this.type = DELETED;
            else if ("M".equals(type))
                this.type = MODIFIED;
            else
                throw new IllegalArgumentException("Unrecognized Svn change type '"+type+"'");
        }

        public int getType() {
            return type;
        }

        public boolean getHasHistory() {
            return hasHistory;
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            if (type == ADDED && hasHistory == false)
                return null;

            File clientFile = new File(baseDirectory, filename);
            File svnDir = new File(clientFile.getParentFile(), ".svn");
            File textBaseDir = new File(svnDir, "text-base");
            File baseFile = new File(textBaseDir, clientFile.getName() + ".svn-base");
            return new FileInputStream(baseFile);
        }

        public InputStream getContentsAfter() throws IOException {
            if (type == DELETED)
                return null;
            File clientFile = new File(baseDirectory, filename);
            return new FileInputStream(clientFile);
        }

        private String fileUrl;
        private String fileRev;
        private String historyUrl;
        private String historyRev;

        public boolean isMovedFrom(SvnFile file) {
            if (hasHistory == false)
                return false;

            getInfo();
            file.getInfo();

            if (historyUrl == null || historyRev == null)
                return false;
            return (historyUrl.equals(file.fileUrl) && historyRev.equals(file.fileRev));
        }

        private void getInfo() {
            if (fileUrl != null)
                return;

            try {
                String[] cmd = new String[] { "svn", "info", filename };
                Process proc = Runtime.getRuntime().exec(cmd, null, baseDirectory);
                BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("URL: ")) fileUrl = line.substring(5);
                    else if (line.startsWith("Revision: ")) fileRev = line.substring(10);
                    else if (line.startsWith("Copied From URL: ")) historyUrl = line.substring(17);
                    else if (line.startsWith("Copied From Rev: ")) historyRev = line.substring(17);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }


    public static void main(String[] args) {
        SvnLOCDiff diff = new SvnLOCDiff(HardcodedFilterLocator.getFilters());
        args = collectOptions(args);
        diff.setOptions(args[0]);
        diff.addChangeListener(new StdOutChangeListener());

        if (args.length == 2) {
            diff.setBaseDirectory(args[1]);

        } else if (args.length > 2) {
            printUsage();
            return;
        }

        try {
            File out = diff.generateDiffs();
            Browser.launch(out.toURL().toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + SvnLOCDiff.class.getName() + " [directory]");
    }
}
