// Copyright (C) 2005-2008 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;


public class FileSystemLOCDiff extends LOCDiffReportGenerator {

    private File compareA;
    private File compareB;
    private boolean caseInsensitive;

    public FileSystemLOCDiff(List languageFilters) {
        super(languageFilters);
    }

    public FileSystemLOCDiff(List languageFilters, File compareA, File compareB) {
        super(languageFilters);
        setCompareA(compareA);
        setCompareB(compareB);
    }

    public void setCompareA(File compareA) {
        this.compareA = compareA;
    }

    public void setCompareB(File compareB) {
        this.compareB = compareB;
    }

    protected Collection getFilesToCompare() {
        if (compareB.isDirectory()) {
            showIdenticalRedlines = false;
            caseInsensitive = testCaseInsensitive();
            TreeSet result = new TreeSet();
            listAllFiles(result, compareB, compareB);
            listAllFiles(result, compareA, compareA);
            return result;
        } else {
            skipIdentical = false;
            showIdenticalRedlines = true;
            return Collections.singleton
                (new SimpleFileComparison(compareA, compareB));
        }
    }

    private boolean testCaseInsensitive() {
        File test1 = new File(compareB, "test.txt");
        File test2 = new File(compareB, "TEST.TXT");
        return test1.equals(test2);
    }

    private void listAllFiles(TreeSet result, File dir, File baseDir) {
        String basePath = "";
        if (baseDir != null)
            basePath = baseDir.getAbsolutePath() + File.separator;
        listAllFiles(result, dir, basePath);
    }

    private void listAllFiles(TreeSet result, File dir, String basePath) {
        if (!isDir(dir) || dir.getName().startsWith("."))
            return;
        File [] files = dir.listFiles();
        for (int i = files.length;   i-- > 0; ) {
            String filename = files[i].getName();
            if (!".".equals(filename) && !"..".equals(filename)
                    && !filename.endsWith("~")) {
                if (isFile(files[i])) {
                    filename = files[i].getAbsolutePath();
                    if (filename.startsWith(basePath))
                        filename = filename.substring(basePath.length());
                    result.add(new FileInDirectory(filename));
                } else if (isDir(files[i])) {
                    listAllFiles(result, files[i], basePath);
                }
            }
        }
    }


    private class SimpleFileComparison implements FileToCompare {
        private File fileA, fileB;

        public SimpleFileComparison(File fileA, File fileB) {
            this.fileA = fileA;
            this.fileB = fileB;
        }

        public String getFilename() {
            return fileB.getName();
        }

        public InputStream getContentsBefore() throws IOException {
            return openFile(fileA);
        }

        public InputStream getContentsAfter() throws IOException {
            return openFile(fileB);
        }

        private InputStream openFile(File f) throws IOException {
            if (isFile(f))
                return new FileInputStream(f);
            else
                return null;
        }
    }

    private class FileInDirectory implements FileToCompare,
            Comparable<FileInDirectory> {

        private String filename;
        private String cmpName;

        public FileInDirectory(String filename) {
            this.filename = filename;

            if (caseInsensitive)
                cmpName = filename.toLowerCase();
            else
                cmpName = filename;
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            return openFile(compareA);
        }

        public InputStream getContentsAfter() throws IOException {
            return openFile(compareB);
        }

        private InputStream openFile(File dir) throws IOException {
            if (!isDir(dir))
                return null;
            File file = new File(dir, filename);
            if (isFile(file))
                return new FileInputStream(file);
            else
                return null;
        }


        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof FileInDirectory)) return false;
            FileInDirectory that = (FileInDirectory) obj;
            return this.cmpName.equals(that.cmpName);
        }

        public int hashCode() {
            return cmpName.hashCode();
        }

        public int compareTo(FileInDirectory that) {
            return this.cmpName.compareTo(that.cmpName);
        }
    }


    protected boolean isFile(File f) { return (f != null && f.isFile()); }
    protected boolean isDir(File f)  { return (f != null && f.isDirectory()); }


    public static void main(String[] args) {
        FileSystemLOCDiff diff = new FileSystemLOCDiff
            (HardcodedFilterLocator.getFilters());
        args = collectOptions(args);
        diff.setOptions(args[0]);
        diff.addChangeListener(new StdOutChangeListener());

        if (args.length == 2) {
            diff.setCompareB(new File(args[1]));

        } else if (args.length == 3) {
            diff.setCompareA(new File(args[1]));
            diff.setCompareB(new File(args[2]));

        } else {
            printUsage();
            return;
        }

        try {
            File out = diff.generateDiffs();
            Browser.launch(out.toURI().toURL().toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    protected static void printUsage() {
        System.out.println("Usage: java " +
                FileSystemLOCDiff.class.getName() + "[options] fileA [fileB]");
    }
}
