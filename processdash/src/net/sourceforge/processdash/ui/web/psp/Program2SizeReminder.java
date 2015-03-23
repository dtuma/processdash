// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.reports.snippets.MetricsAlert;
import net.sourceforge.processdash.util.HTMLUtils;

public class Program2SizeReminder extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        // First, look at the project which is invoking this script, and see
        // if it is preceded by a PSP0 program. If not, print nothing and exit.
        DataContext data = getDataContext();
        SimpleData prevProgramVal = data.getSimpleValue("Previous_Program");
        if (!hasValue(prevProgramVal))
            return;

        String prevProgram = prevProgramVal.format();
        SimpleData psp0tag = data.getSimpleValue(prevProgram + "/PSP0");
        if (!hasValue(psp0tag))
            return;

        // Next, check to see if size data has been entered for the preceding
        // PSP0 program. If it has, print nothing and exit.
        SimpleData psp0size = data.getSimpleValue(prevProgram
                + "/New & Changed LOC");
        if (hasValue(psp0size))
            return;

        // At this point, we know this project is preceded by a PSP0 program
        // that has no size data. However, we don't want to print any warnings
        // during the Planning phase of this project, because that would
        // confuse students unnecessarily.  So we look for several signs
        // indicating that they might be in or approaching the Postmortem
        // phase of this project.
        if (hasValue(data.getSimpleValue("Test/Time"))
                || hasValue(data.getSimpleValue("Postmortem/Time"))
                || hasValue(data.getSimpleValue("New & Changed LOC"))) {
            printSizeWarningBlock(prevProgram);
        }
    }

    private void printSizeWarningBlock(String prevProgram) {
        int slashPos = prevProgram.lastIndexOf('/');
        String simpleName = prevProgram.substring(slashPos + 1);

        out.println("<html><head>" + MetricsAlert.HEADER_ITEMS + "</head>");

        out.println("<body><div class='alertError'>");
        out.print("Do not forget to enter the actual size for your "
                + "previous program, <a href=\"");
        out.print(WebServer.urlEncodePath(prevProgram));
        out.print("//psp0/summary.htm?showSizeBlock\" target=\"_blank\">");
        out.print(HTMLUtils.escapeEntities(simpleName));
        out.println("</a>.");
        out.println("</div></body></html>");
    }

    private static boolean hasValue(SimpleData val) {
        return (val != null && val.test());
    }
}
