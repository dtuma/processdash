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
        printRes("<HTML><HEAD><TITLE>${Title}</TITLE>");
        out.print("<STYLE> .indent { margin-left: 1cm } </STYLE></HEAD>");
        printRes("<BODY><H1>${Header}</H1>");

        printRes("<P>${Config_File_Header}");
        out.print("<PRE class='indent'>");
        out.println(DashController.getSettingsFileName());
        out.println("</PRE></P>");

        printRes("<P>${Data_Dir_Header}");
        out.print("<PRE class='indent'>");
        out.println(ProcessDashboard.getDefaultDirectory());
        out.println("</PRE></P>");

        printRes("<P>${Add_On.Header}<br>&nbsp;");

        List packages = TemplateLoader.getPackages();
        if (packages == null || packages.size() < 2)
            printRes("<P class='indent'><i>${Add_On.None}</i>");
        else {
            printRes("<table border class='indent' cellpadding='5'><tr>"
                    + "<th>${Add_On.Name}</th>" //
                    + "<th>${Add_On.Version}</th>"
                    + "<th>${Add_On.Filename}</th></tr>");
            for (Iterator i = packages.iterator(); i.hasNext();) {
                DashPackage pkg = (DashPackage) i.next();
                if ("pspdash".equals(pkg.id))
                    continue;
                out.print("<tr><td>");
                out.print(HTMLUtils.escapeEntities(pkg.name));
                out.print("</td><td>");
                out.print(HTMLUtils.escapeEntities(pkg.version));
                out.print("</td><td>");
                out.print(HTMLUtils
                        .escapeEntities(cleanupFilename(pkg.filename)));
                out.println("</td></tr>");
            }
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
