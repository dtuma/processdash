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

package net.sourceforge.processdash.tool.diff.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.LOCDiffReportGenerator;
import net.sourceforge.processdash.ui.Browser;


public class PerforceLOCDiff extends LOCDiffReportGenerator {

    protected String changelist = "default";
    private JDialog workingDialog = null;

    public PerforceLOCDiff(List languageFilters) {
        super(languageFilters);
    }

    public void run() {

        try {
            File outFile = File.createTempFile("diff", ".htm");
            if (workingDialog != null)
                outFile.deleteOnExit();

            generateDiffs(outFile, getFilesToCompare());
            Browser.launch(outFile.toURL().toString());

            if (workingDialog != null)
                workingDialog.hide();

        } catch (IOException ioe) {
//            beep(null);
            ioe.printStackTrace();
        } catch (UserCancel uc) {}
    }



    private Collection getFilesToCompare() throws IOException {
        List result = new ArrayList();

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
                String action = m.group(2);
                result.add(new PerforceFile(filename, action));
            }
        }
        return result;
    }
    private static final Pattern OPENED_FILE_PATTERN = Pattern.compile
        ("(.*) - (edit|add|delete) (default change|change ([0-9]+)) .*");

    private static final int ADDED = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;

    private class PerforceFile implements FileToCompare {

        private String filename;
        private int type;

        public PerforceFile(String filename, String type) {
            this.filename = filename;
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
            String[] cmd = new String[] { "p4", "print", "-q", filename };
            Process proc = Runtime.getRuntime().exec(cmd);
            return proc.getInputStream();
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
            String fname = filename;
            if (type == ADDED) {
                int pos = fname.lastIndexOf('#');
                if (pos != -1)
                    fname = fname.substring(0, pos);
            }
            String[] cmd = new String[] { "p4", "fstat", fname };
            Process proc = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("... clientFile "))
                    return line.substring(15);
            }
            return null;
        }

    }

    protected class UserCancel extends RuntimeException {}

    public static void main(String[] args) {

        PerforceLOCDiff dlg = new PerforceLOCDiff
            (HardcodedFilterLocator.getFilters());

        if (args.length == 0) {
//            dlg.showDialog();
            dlg.run();

        } else if (args.length == 1) {
            dlg.changelist = args[0];
            dlg.run();

        } else {
            printUsage();

        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " +
                FileSystemLOCDiffDialog.class.getName() + " [changelist]");
    }
}
