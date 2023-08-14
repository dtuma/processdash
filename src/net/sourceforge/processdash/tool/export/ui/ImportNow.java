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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



public class ImportNow extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("ImportExport");

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        // DataImporter.refreshPrefix() doesn't expect a hierarchy prefix as
        // input - instead, it wants an import prefix (e.g. "/Import_sf7a7s").
        // We don't have a consistent means of determining that prefix, so
        // we'll just refresh all imported data.
        List<String> refreshedFiles = DataImporter.refreshPrefixWithFeedback("/");

        out.println("<html><head>");

        if (parameters.containsKey("redirectToReferrer")) {
            String referer = (String) env.get("HTTP_REFERER");
            if (StringUtils.hasValue(referer)) {
                String uri = HTMLUtils.appendQuery(referer, "rl");
                writeRedirectInstruction(uri, 4);
            }
        }

        String type = (refreshedFiles.isEmpty() ? "Not_Needed" : "Complete");
        String header = StringUtils.findAndReplace(HEADER_HTML, "TYPE", type);
        out.print(resources.interpolate(header, HTMLUtils.ESC_ENTITIES));

        if (!refreshedFiles.isEmpty()) {
            out.print("<ul>\n");
            for (String file : refreshedFiles) {
                out.print("<li>");
                out.print(HTMLUtils.escapeEntities(file));
                out.print("</li>\n");
            }
            out.print("</ul>\n");

        } else if (Settings.isTeamMode() && Settings.isCloudStorage()) {
            out.print("<p style='margin-top:30px; font-style:italic'>" //
                    + "<b>" + resources.getHTML("ExportNote") + "</b>&nbsp;"
                    + resources.getHTML("Import.Not_Needed.Cloud_Message")
                    + "</p>");
        }

        out.print("</body></html>");
    }

    private static final String HEADER_HTML =
            "<title>${Import.TYPE.Title}</title>\n"
            + "<link rel='stylesheet' type='text/css' href='/style.css'>\n"
            + "</head><body>\n" //
            + "<h1>${Import.TYPE.Title}</h1>\n" //
            + "${Import.TYPE.Message}\n";

}
