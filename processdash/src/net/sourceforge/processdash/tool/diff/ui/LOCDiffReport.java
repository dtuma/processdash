// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


import java.io.IOException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIHighVolume;
import net.sourceforge.processdash.tool.diff.AbstractLanguageFilter;
import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.tool.diff.LOCDiff;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;



public class LOCDiffReport extends TinyCGIBase implements TinyCGIHighVolume {

    static Resources resources = Resources.getDashBundle("LOCDiff");

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
        out.println("<HTML><HEAD><TITLE>" +
                    resources.getString("Options_Title") +
                    "</TITLE></HEAD><BODY><H1>" +
                    resources.getString("Options_Title") +
                    "</H1>\n");
        LOCDiff.printFiltersAndOptions(getTinyWebServer(), out);
        out.println("</BODY></HTML>");
    }

    protected void compareFiles() throws IOException {
        parseMultipartFormData();

        // Compare the two files in question.
        LOCDiff diff = new LOCDiff(getTinyWebServer(),
                                   getFileContents('A'),
                                   getFileContents('B'),
                                   getParameter("FILEB"),
                                   getParameter("options"));

        printHeader(diff);
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
    protected void printHeader(LOCDiff diff) throws IOException {
        // print HTML header.
        out.print("<html><head><title>");
        out.print(resources.getString("Report.Title"));
        out.println("</title><style>\n"+
                    "    @media print { .doNotPrint { display: none } }\n"+
                    "</style></head><body>");

        // print page heading.
        String filenameA = getParameter("FILEA");
        String filenameB = getParameter("FILEB");
        if (filenameA != null && filenameA.length() > 0 &&
            filenameB != null && filenameB.length() > 0) {
            out.print("<h1>");
            out.print(resources.format("Report.Diff_HTML_FMT",
                                       HTMLUtils.escapeEntities(filenameA),
                                       HTMLUtils.escapeEntities(filenameB)));
            out.println("</h1>");
        } else {
            out.print("<h1>");
            out.print(resources.getString("Report.Title"));
            out.println("</h1>");
        }

        // print line describing the language filter in use
        LanguageFilter filter = diff.getFilter();
        String filterHTML = "</i><tt><b>"
            + AbstractLanguageFilter.getFilterName(filter)
            + "</b></tt><i>";
        String options = getParameter("options");
        String key = "Report.Using_Filter_FMT";
        if (options != null && options.length() > 0)
            key = "Report.Using_Filter_Options_FMT";
        out.print("<p><i>");
        out.print(resources.format(key, filterHTML,
                                   "</i><tt><b>"+options+"</b></tt><i>"));
        out.println("</i></p>");

        // print any caveats about this filter's operation.
        out.println("<span class='doNotPrint'>");
        out.flush();
        filter.service(inStream, outStream, env);
        out.println("</span>");
    }

    /** print out the resulting metrics. */
    private void printMetrics(LOCDiff diff) {
        out.println("<table name=METRICS BORDER>");
        printMetricsRow("Base",            diff.getBase());
        printMetricsRow("Deleted",         diff.getDeleted());
        printMetricsRow("Modified",        diff.getModified());
        printMetricsRow("Added",           diff.getAdded());
        printMetricsRow("New_And_Changed", diff.getAdded() + diff.getModified());
        printMetricsRow("Total",           diff.getTotal());
        out.println("</table>");
    }

    /** print out one row of metrics */
    protected void printMetricsRow(String headerKey, int count) {
        out.print("<tr><td>");
        out.print(resources.getHTML("Report." + headerKey));
        out.print(":&nbsp;</td><td>");
        out.print(count);
        out.println("</td></tr>");
    }
}
