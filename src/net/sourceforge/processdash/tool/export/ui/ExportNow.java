// Copyright (C) 2002-2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.ui;


import java.io.IOException;
import java.util.Date;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.export.mgr.CompletionStatus;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HttpException;



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
                + "<title>${ExportExportingDataDots}</title>\n");
        writeRedirectInstruction(uri, 1);
        interpOut("</head>\n"
                + "<body><h1>${ExportExportingDataDots}</h1>\n"
                + "${ExportExportingMessage}\n"
                + "</body></html>");
    }


    /** Export the data, and tell the user the results.
     */
    private void run() {
        CompletionStatus result = null;
        if (parameters.containsKey("all"))
            DashController.exportAllData();
        else
            result = DashController.exportDataForPrefix(getPrefix());

        if (result == null || CompletionStatus.SUCCESS.equals(result.getStatus())) {
            interpOut("<HTML><HEAD><TITLE>${ExportComplete}</TITLE></HEAD>\n"
                    + "<BODY><H1>${ExportComplete}</H1>\n");
            out.println(HTMLUtils.escapeEntities(resources.format(
                    "ExportDataComplete_FMT", new Date())));

            if (result != null && result.isCloudStorage()
                    && Settings.isPersonalMode() && !isPersonalProject()) {
                out.print("<p style='margin-top:30px; font-style:italic'>" //
                        + "<b>" + resources.getHTML("ExportNote") + "</b>&nbsp;"
                        + resources.getHTML("ExportDataCloud") + "</p>");
            }

            out.println("</BODY></HTML>");

        } else if (CompletionStatus.NO_WORK_NEEDED.equals(result.getStatus())) {
            interpOut("<HTML><HEAD><TITLE>${ExportNotNeeded.Title}</TITLE></HEAD>\n"
                    + "<BODY><H1>${ExportNotNeeded.Title}</H1>\n"
                    + "${ExportNotNeeded.Message}"
                    + "</BODY></HTML>");

        } else {
            interpOut("<HTML><HEAD><TITLE>${ExportError.Title}</TITLE></HEAD>\n"
                    + "<BODY><H1>${ExportError.Title}</H1>\n");

            if (result.getException() instanceof HttpException.Forbidden) {
                String username = HttpAuthenticator.getLastUsername();
                out.println(HTMLUtils.escapeEntities(resources.format(
                    "ExportError.Server_Perm_FMT", username)));

            } else if (result.getTarget() != null
                    && result.getException() instanceof IOException) {
                String target = result.getTarget().toString();
                String resKey;
                if (TeamServerSelector.isUrlFormat(target))
                    resKey = "ExportError.Server_IO_FMT";
                else if (result.isCloudStorage())
                    resKey = "ExportError.Cloud_IO_FMT";
                else
                    resKey = "ExportError.IO_FMT";
                out.println(HTMLUtils.escapeEntities(resources.format(
                        resKey, target)));
            } else {
                out.println(resources.getHTML("ExportError.Message"));
                if (result != null && result.getException() != null) {
                    out.print("<PRE>");
                    result.getException().printStackTrace(out);
                    out.print("</PRE>");
                }
            }

            out.println("</BODY></HTML>");
        }
    }


    private boolean isPersonalProject() {
        SimpleData sd = getDataContext()
                .getSimpleValue(TeamDataConstants.PERSONAL_PROJECT_FLAG);
        return sd != null && sd.test();
    }

    private void interpOut(String htmlTemplate) {
        out.print(resources.interpolate(htmlTemplate, HTMLUtils.ESC_ENTITIES));
    }

    private static final Resources resources = Resources
            .getDashBundle("ImportExport");

}
