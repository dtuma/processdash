// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;





/** CGI script to print out the DNS name of the web server.
 */
public class DisplayConfig extends TinyCGIBase {

    private static final Resources resources =
        Resources.getDashBundle("ProcessDashboard.ConfigScript");

    protected void writeContents() throws IOException {
        if (parameters.containsKey("serverName"))
            printServerName();
        else if (parameters.containsKey("config"))
            printConfigFile();
        else
            printUserConfig();

        out.flush();
    }

    private void printServerName() {
        out.print(getTinyWebServer().getHostName(true));
    }

    private void printConfigFile() {
        out.print(DashController.getSettingsFileName());
    }

    private void printUserConfig() {
        boolean brief = parameters.containsKey("brief");

        printRes("<HTML><HEAD><TITLE>${Title}</TITLE>");

        double indentLeftMargin = brief ? 0.3 : 1;
        out.print("<STYLE> .indent { margin-left: " + indentLeftMargin + "cm }");

        if (brief) {
            out.print("body { font-size: small }");
            out.print("sup { font-size: small }");
        }
        out.print("</STYLE>");

        out.print("<HEAD>");
        out.print("<BODY>");

        if (!brief) {
            printRes("<H1>${Header}</H1>");
        }

        // When we want brief info, where're not showing any header. <P> inserts a
        //  line break and if there's no header, the text starts with a line break.
        //  Since it doesn't look so good, we use a <DIV>
        out.print(brief ? "<DIV>" : "<P>");

        printRes("${Config_File_Header}");
        out.print("<PRE class='indent'>");
        out.println(DashController.getSettingsFileName());
        out.println("</PRE>");

        out.print(brief ? "</DIV>" : "</P>");

        printRes("<P>${Data_Dir_Header}");
        out.print("<PRE class='indent'>");
        out.println(ProcessDashboard.getDefaultDirectory());
        out.println("</PRE></P>");

        printRes("<P>${Add_On.Header}");

        List<DashPackage> packages = TemplateLoader.getPackages();

        if (packages == null || packages.size() < 2)
            printRes("<PRE class='indent'><i>${Add_On.None}</i></PRE>");
        else {
                printRes("<br />&nbsp;"
                         + "<table border class='indent' cellpadding='5'><tr>"
                         + "<th>${Add_On.Name}</th>"
                         + "<th>${Add_On.Version}</th>");

                // We want the brief layout to be as compact as possible so we
                //  don't display the "location" column
                if (!brief) {
                    printRes("<th>${Add_On.Filename}</th></tr>");
                }

            for (Iterator<DashPackage> i = packages.iterator(); i.hasNext();) {
                DashPackage pkg = i.next();

                if ("pspdash".equals(pkg.id))
                    continue;

                    out.print("<tr><td>");
                    out.print(HTMLUtils.escapeEntities(pkg.name));
                    out.print("</td><td>");
                    out.print(HTMLUtils.escapeEntities(pkg.version));
                    out.print("</td>");

                    if (!brief) {
                        out.print("<td>" +
                                  HTMLUtils.escapeEntities(cleanupFilename(pkg.filename)) +
                                  "</td>");
                    }

                    out.println("</tr>");
            }
            out.print("</TABLE>");
        }

        out.println("</P>");

        // Showing a link to "more details" if we are in brief mode
        if (brief) {
            printRes("<p><a href=\"/control/showenv.class\">${More_Details}</a></p>");
        }

        out.println("</BODY></HTML>");
    }

    private String cleanupFilename(String filename) {
        if (filename == null)
            return "";
        if (filename.startsWith("file:")) {
            filename = HTMLUtils.urlDecode(filename.substring(5));
            File f = new File(filename);
            filename = f.getPath();
        }
        return filename;
    }

    private void printRes(String text) {
        out.println(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

}
