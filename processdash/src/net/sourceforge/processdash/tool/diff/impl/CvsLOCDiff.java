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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.StringUtils;

public class CvsLOCDiff extends LOCDiffReportGenerator {

    private File baseDirectory = null;

    private String cvsBaseDir = null;

    public CvsLOCDiff(List languageFilters) {
        this(languageFilters, ".");
    }

    public CvsLOCDiff(List languageFilters, String dir) {
        super(languageFilters);
        setBaseDirectory(dir);
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = new File(baseDirectory);
    }

    private InputStream execCVS(String[] cmd) throws IOException {
        Process proc = Runtime.getRuntime().exec(cmd, null, baseDirectory);
        return proc.getInputStream();
    }

    protected Collection getFilesToCompare() throws IOException {
        List result = new ArrayList();

        getCvsBaseDir();
        getDeletedModifiedFilesToCompare(result);
        getAddedFilesToCompare(result);
        Collections.sort(result);

        return result;
    }

    private void getCvsBaseDir() {
        try {
            File cvsDir = new File(baseDirectory, "CVS");
            File rootFile = new File(cvsDir, "Root");
            BufferedReader in = new BufferedReader(new FileReader(rootFile));
            String cvsRoot = in.readLine();
            Matcher m = CVSROOT_LINE_PATTERN.matcher(cvsRoot);
            if (m.find())
                cvsBaseDir = m.group(1) + "/";

            File repositoryFile = new File(cvsDir, "Repository");
            in = new BufferedReader(new FileReader(repositoryFile));
            String repositoryPath = in.readLine();
            cvsBaseDir = cvsBaseDir + repositoryPath + "/";
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    private static final Pattern CVSROOT_LINE_PATTERN = Pattern
            .compile(".*:([^:]+)$");

    private void getDeletedModifiedFilesToCompare(List fileList)
            throws IOException {
        String[] cmd = new String[] { "cvs", "-Q", "status" };
        BufferedReader in = new BufferedReader
            (new InputStreamReader(execCVS(cmd)));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = FILE_LINE_PATTERN.matcher(line);
            if (m.matches()) {
                String simpleFilename = m.group(2);
                String status = m.group(3);
                parseCvsStatus(in, simpleFilename);
                if (filename != null && revision != null && isRelevant(status))
                    fileList.add(new CvsFile(filename, revision, status));
            }
        }
    }
    private static final Pattern FILE_LINE_PATTERN = Pattern
            .compile("File: (no file )?(.*\\S)\\s+Status: (.+)");

    private String filename = null;
    private String revision = null;
    private void parseCvsStatus(BufferedReader in, String simpleFilename)
            throws IOException {
        filename = revision = null;

        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("=========="))
                return;

            Matcher m = REVISION_LINE_PATTERN.matcher(line);
            if (m.matches() && m.group(3).endsWith(simpleFilename)) {
                revision = m.group(2);
                filename = cleanupFilename(m.group(3));
            }
        }
    }
    private static final Pattern REVISION_LINE_PATTERN = Pattern
            .compile("\\s*(RCS Version|Repository revision):\\s+(\\S+)\\s+(.*),v");

    private String cleanupFilename(String filename) {
        filename = StringUtils.findAndReplace(filename, "/Attic", "");
        if (cvsBaseDir != null) {
            int pos = filename.indexOf(cvsBaseDir);
            if (pos != -1)
                filename = filename.substring(pos + cvsBaseDir.length());
        }
        return filename;
    }

    private void getAddedFilesToCompare(List result) throws IOException {
        getAddedFilesToCompare(result, baseDirectory, "");
    }

    private void getAddedFilesToCompare(List result, File dir, String prefix)
            throws IOException {
        File cvsDir = new File(dir, "CVS");
        File entriesFile = new File(cvsDir, "Entries");
        BufferedReader in = new BufferedReader(new FileReader(entriesFile));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = ADDED_ENTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String filename = prefix + "/" + m.group(1);
                result.add(new CvsFile(filename.substring(1), "0",
                        STATUS_LOCALLY_ADDED));
            } else {
                m = DIRECTORY_ENTRY_PATTERN.matcher(line);
                if (m.matches()) {
                    String subdirName = m.group(1);
                    getAddedFilesToCompare(result, new File(dir, subdirName),
                            prefix + "/" + subdirName);
                }
            }
        }
    }
    private static final Pattern ADDED_ENTRY_PATTERN = Pattern
            .compile("^/(.*)/0/dummy timestamp//$");
    private static final Pattern DIRECTORY_ENTRY_PATTERN = Pattern
            .compile("^D/(.*)////$");

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;

    private static final String STATUS_NEEDS_MERGE = "Needs Merge";
    private static final String STATUS_LOCALLY_MODIFIED = "Locally Modified";
    private static final String STATUS_LOCALLY_ADDED = "Locally Added";
    private static final String STATUS_LOCALLY_REMOVED = "Locally Removed";
    private static final String STATUS_UNRESOLVED_CONFLICT = "Unresolved Conflict";

    private static final String[][] STATUSES = {
            { STATUS_NEEDS_MERGE, Integer.toString(MODIFIED) },
            { STATUS_LOCALLY_MODIFIED, Integer.toString(MODIFIED) },
            { STATUS_LOCALLY_ADDED, Integer.toString(ADDED) },
            { STATUS_LOCALLY_REMOVED, Integer.toString(DELETED) },
            { STATUS_UNRESOLVED_CONFLICT, Integer.toString(MODIFIED) }, };

    private static int getStatusType(String status) {
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i][0].equals(status)) {
                return Integer.parseInt(STATUSES[i][1]);
            }
        }
        return -1;
    }

    private static boolean isRelevant(String status) {
        return getStatusType(status) != -1;
    }

    private class CvsFile implements FileToCompare, Comparable {

        protected String filename;
        protected String version;
        protected int type;

        public CvsFile(String filename, String version, String status) {
            this.filename = filename;
            this.version = version;
            this.type = getStatusType(status);

            if (this.type == -1)
                throw new IllegalArgumentException(
                        "Unrecognized cvs change type '" + status + "'");
            else if (this.version == null && this.type != ADDED)
                throw new IllegalArgumentException(
                        "Must supply version number for '" + filename
                                + "' with status '" + status + "'");
            else if (STATUS_UNRESOLVED_CONFLICT.equals(status))
                System.err
                        .println("Warning - unresolved conflict: " + filename);
        }

        public int getType() {
            return type;
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            if (type == ADDED)
                return null;

            String[] cmd = new String[] { "cvs", "-Q", "update", "-p",
                    "-r" + version, filename };
            return execCVS(cmd);
        }

        public InputStream getContentsAfter() throws IOException {
            if (type == DELETED)
                return null;

            File clientFile = new File(baseDirectory, filename);
            return new FileInputStream(clientFile);
        }

        public int compareTo(Object o) {
            CvsFile that = (CvsFile) o;
            return this.filename.compareTo(that.filename);
        }

    }

    public static void main(String[] args) {
        CvsLOCDiff diff = new CvsLOCDiff(HardcodedFilterLocator.getFilters());
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
        System.out.println("Usage: java " + CvsLOCDiff.class.getName()
                + " [directory]");
    }
}
