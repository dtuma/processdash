// Copyright (C) 2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle.ui;

import java.io.IOException;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.bundle.FileBundlePackTask;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundlePackTask.Speed;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class BundlePackPage extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        if (parameters.containsKey("run"))
            performPackOperation();
        else
            printWaitPage();
    }

    private void printWaitPage() {
        String uri = (String) env.get("REQUEST_URI");
        uri = HTMLUtils.appendQuery(uri, "run");
        out.print("<html>\n<head>\n" //
                + "<title>Packing Bundle Directories...</title>\n");
        writeRedirectInstruction(uri, 1);
        out.print("</head>\n<body>\n" //
                + "<h1>Packing Bundle Directories...</h1>\n"
                + "<p>Packing bundle directories. Please wait.</p>\n"
                + "</body>\n</html>");
    }

    private void performPackOperation() throws IOException {
        // parse the speed parameter and create a pack task
        Speed speed = FileBundlePackTask
                .parseSpeedSetting(getParameter("speed"), Speed.NOW);
        FileBundlePackTask packTask = new FileBundlePackTask(
                getDashboardContext(), speed);
        packTask.setIncludeBackups(true);

        // run the operation, either against a project or the whole dashboard
        String path = getPrefix();
        boolean isProject = StringUtils.hasValue(path);
        if (isProject)
            packTask.runForProject(path);
        else
            packTask.run();

        // print results
        List<String> packedDirs = packTask.getPackedDirs();
        if (packedDirs.isEmpty()) {
            out.print("<html>\n<head>\n" //
                    + "<title>No Packing Operations Needed</title>\n"
                    + "</head>\n<body>\n" //
                    + "<h1>No Packing Operations Needed</h1>\n" //
                    + "<p>This " + (isProject ? "project" : "dashboard")
                    + " does not contain any bundled directories that"
                    + " need packing.</p>\n" //
                    + "</body>\n</html>");

        } else {
            out.print("<html>\n<head>\n" //
                    + "<title>Bundle Directory Packing Complete</title>\n"
                    + "</head>\n<body>\n"
                    + "<h1>Bundle Directory Packing Complete</h1>\n"
                    + "<p>The dashboard packed bundles in the following "
                    + "directories:</p><ul>");
            for (String dir : packedDirs) {
                out.write("<li>" + HTMLUtils.escapeEntities(dir) + "</li>\n");
            }
            out.print("</ul>\n</body>\n</html>");
        }
    }

}
