// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.PSPDiff;
import pspdash.TinyCGIBase;

import java.io.IOException;


public class pspdiff extends TinyCGIBase {

    protected void writeContents() throws IOException {
        if (parameters.get("showOptions") != null)
            // If the query string requested the display of the
            // options page, just do it.
            printOptions();

        else
            // otherwise, compare the files posted to this script
            // (default operation).
            compareFiles();
    }

    protected void printOptions() throws IOException {
        // FIXME.
        out.println("<HTML><BODY>Not yet implemented.</BODY></HTML>");
    }

    protected void compareFiles() throws IOException {
        parseMultipartFormData();

        // Compare the two files in question.
        PSPDiff diff = new PSPDiff(getTinyWebServer(),
                                   getFileContents('A'),
                                   getFileContents('B'),
                                   getParameter("FILEB"),
                                   getParameter("options"));

        printHeader();
        printMetrics(diff);
        out.println("<hr>");
        diff.displayHTMLRedlines(out);
        out.println("</body></html>");

        diff.dispose();
    }

    private String getFileContents(char letter) {
        // check for an uploaded file.
        byte[] fileContents =
            (byte[]) parameters.remove("FILE" + letter + "_CONTENTS");
        if (fileContents != null) return new String(fileContents);

        // if no file was uploaded, check for pasted text.
        String fileText = (String) parameters.get("TEXT" + letter);
        if (fileText != null) return fileText;

        // nothing found! return the empty string.
        return "";
    }

    /** print the HTML header, and initial document info. */
    protected void printHeader() {
        out.println("<html><body>");
    }

    /** print out the resulting metrics. */
    private void printMetrics(PSPDiff diff) {
        out.println("<table name=METRICS BORDER>");
        printMetricsRow("Base",          diff.getBase());
        printMetricsRow("Deleted",       diff.getDeleted());
        printMetricsRow("Modified",      diff.getModified());
        printMetricsRow("Added",         diff.getAdded());
        printMetricsRow("New & Changed", diff.getAdded() + diff.getModified());
        printMetricsRow("Total",         diff.getTotal());
        out.println("</table>");
    }

    /** print out one row of metrics */
    protected void printMetricsRow(String header, int count) {
        out.print("<tr><td>");
        out.print(header);
        out.print(":&nbsp;</td><td>");
        out.print(count);
        out.println("</td></tr>");
    }
}
