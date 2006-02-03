// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2006 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.ui;


import java.io.IOException;
import java.util.Date;


import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;



public class ExportNow extends TinyCGIBase {



    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        if (parameters.containsKey("run"))
                run();
        else
                printWaitPage();
    }


    /** Print a page asking the user to wait. This page includes an
     * HTTP "refresh" instruction that will initiate the export operation.
     */
    private void printWaitPage() {
        String uri = (String) env.get("REQUEST_URI");
        uri = uri + (uri.indexOf('?') == -1 ? "?run" : "&run");
        interpOut("<html><head>\n"
                        + "<title>${ExportExportingDataDots}</title>\n"
                        + "<meta http-equiv='Refresh' content='1;URL=" + uri + "'>\n"
                        + "</head>\n"
                        + "<body><h1>${ExportExportingDataDots}</h1>\n"
                        + "${ExportExportingMessage}\n"
                        + "</body></html>");
    }


    /** Export the data, and tell the user the results.
     */
        private void run() {
                DashController.exportData(getPrefix());

        interpOut("<HTML><HEAD><TITLE>${ExportComplete}</TITLE></HEAD>\n"
                        + "<BODY><H1>${ExportComplete}</H1>\n");
        out.println(HTMLUtils.escapeEntities(resources.format(
                        "ExportDataComplete_FMT", new Date())));
        out.println("</BODY></HTML>");
        }


        private void interpOut(String htmlTemplate) {
                out.print(resources.interpolate(htmlTemplate, HTMLUtils.ESC_ENTITIES));
        }

        private static final Resources resources = Resources
                        .getDashBundle("ImportExport");

}
