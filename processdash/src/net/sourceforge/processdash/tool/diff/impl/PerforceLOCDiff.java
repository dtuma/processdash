// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.diff.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator.FileToCompare;
import net.sourceforge.processdash.tool.diff.ui.FileSystemLOCDiffDialog;
import net.sourceforge.processdash.ui.Browser;


public class PerforceLOCDiff extends LOCDiffReportGenerator {

    protected String changelist = "default";
    private JDialog workingDialog = null;

    public PerforceLOCDiff(List languageFilters) {
        super(languageFilters);
    }

    public PerforceLOCDiff(List languageFilters, String changelist) {
        super(languageFilters);
        setChangelist(changelist);
    }

    public void setChangelist(String changelist) {
        this.changelist = changelist;
    }

    protected Collection getFilesToCompare() throws IOException {
        List result = new ArrayList();

        getOpenedFilesToCompare(result);

        if (result.isEmpty())
            getSubmittedFilesToCompare(result);

        return result;
    }

    private void getOpenedFilesToCompare(List result) throws IOException {
        String[] cmd = new String[] { "p4", "opened", "-c", changelist };
        Process proc = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = OPENED_FILE_PATTERN.matcher(line);
            if (!m.matches()) {
                System.err.println("Unrecognized output from p4 opened: '"+line+"'");
            } else {
                String filename = m.group(1);
                int revNum = Integer.parseInt(m.group(2));
                String action = m.group(3);
                result.add(new PerforceFile(filename, revNum, action));
            }
        }
    }
    private static final Pattern OPENED_FILE_PATTERN = Pattern.compile
        ("(//.*)\\#([0-9]+) - (edit|add|delete) (default change|change ([0-9]+)) .*");

    private void getSubmittedFilesToCompare(List result) throws IOException {
        String[] cmd = new String[] { "p4", "describe", "-s", changelist };
        Process proc = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = SUBMITTED_FILE_PATTERN.matcher(line);
            if (m.matches()) {
                String filename = m.group(1);
                int revNum = Integer.parseInt(m.group(2));
                String action = m.group(3);
                result.add(new SubmittedPerforceFile(filename, revNum, action));
            }
        }
    }
    private static final Pattern SUBMITTED_FILE_PATTERN = Pattern.compile
        ("\\.\\.\\. (//.*)#([0-9]+) (edit|add|delete)");

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;

    private class PerforceFile implements FileToCompare {

        protected String filename;
        protected int revNum;
        protected int type;

        public PerforceFile(String filename, int revNum, String type) {
            this.filename = filename;
            this.revNum = revNum;
            if ("add".equals(type))
                this.type = ADDED;
            else if ("delete".equals(type))
                this.type = DELETED;
            else if ("edit".equals(type))
                this.type = MODIFIED;
            else
                throw new IllegalArgumentException("Unrecognized Perforce change type '"+type+"'");
        }

        public String getFilename() {
            return filename;
        }

        public InputStream getContentsBefore() throws IOException {
            if (type == ADDED)
                return null;
            return getFileFromPerforce(filename, revNum);
        }

        public InputStream getContentsAfter() throws IOException {
            if (type == DELETED)
                return null;
            String clientFilename = getClientFilename();
            if (clientFilename == null)
                return null;
            else
                return new FileInputStream(clientFilename);
        }

        private String getClientFilename() throws IOException {
            String[] cmd = new String[] { "p4", "fstat", filename };
            Process proc = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("... clientFile "))
                    return line.substring(15);
            }
            return null;
        }

        protected InputStream getFileFromPerforce(String filename, int revNum) throws IOException {
            if (revNum == 0)
                return null;

            String[] cmd = new String[] { "p4", "print", "-q", filename + "#" + revNum };
            Process proc = Runtime.getRuntime().exec(cmd);
            return proc.getInputStream();
        }

    }

    private class SubmittedPerforceFile extends PerforceFile {

        public SubmittedPerforceFile(String filename, int revNum, String type) {
            super(filename, revNum, type);
        }

        public InputStream getContentsBefore() throws IOException {
            return getFileFromPerforce(filename, revNum-1);
        }
        public InputStream getContentsAfter() throws IOException {
            return getFileFromPerforce(filename, revNum);
        }
    }


    public static void main(String[] args) {
        PerforceLOCDiff diff = new PerforceLOCDiff
            (HardcodedFilterLocator.getFilters());

        if (args.length == 1) {
            diff.setChangelist(args[0]);

        } else if (args.length > 1) {
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
        System.out.println("Usage: java " +
                FileSystemLOCDiffDialog.class.getName() + " [changelist]");
    }
}
