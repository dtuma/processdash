// Copyright (C) 2009-2011 Tuma Solutions, LLC
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
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.net.cms.ViewSinglePageAssembler;
import net.sourceforge.processdash.net.http.WebServer;

public class PspForEngQuickLinks extends PspForEngBase {

    private enum Mode { TOC, SinglePage };

    @Override
    protected void writeContents() throws IOException {
        Mode mode = getMode();
        if (mode == null)
            return;

        String level = getPspLevel();
        if (level == null)
            return;

        String target = (mode == Mode.SinglePage ? "quickLink" : "contents");

        out.print("<hr/><b>Quick Links:</b><br/>");
        printLink("quickLink", level, "summary.htm", "Plan Summary");
        if (!level.startsWith("psp0")) {
            printLink(target, level, "sizeest.class", "Size Est. Template");
            printLink(target, null, "reports/probe/probe.class?page=report",
                "PROBE Report");
        }
        printLink(target, PARENT, "pspForEng/studata", "Export STUDATA");
    }

    private Mode getMode() {
        if ("toc".equals(getParameter("mode")))
            return Mode.TOC;
        else if (ViewSinglePageAssembler.isSinglePageView(env))
            return Mode.SinglePage;
        else
            return null;
    }

    private String getPspLevel() {
        DataContext data = getDataContext();
        for (String level : PSP_LEVELS) {
            if (data.getSimpleValue(level) != null)
                return level.toLowerCase();
        }
        return null;
    }

    private void printLink(String target, String level, String uri, String name)
            throws IOException {
        out.print("<a target='");
        out.print(target);
        out.print("' href='");
        String path = getPrefix();
        if (level == PARENT)
            path = DataRepository.chopPath(path);
        out.print(WebServer.urlEncodePath(path));
        out.print("//");
        if (level != null && level != PARENT) {
            out.print(level);
            out.print("/");
        }
        out.print(uri);
        out.print("'>");
        out.print(name);
        out.println("</a><br/>");
    }

    private static final String PARENT = "PARENT";
}
