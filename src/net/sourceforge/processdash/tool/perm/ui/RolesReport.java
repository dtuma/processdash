// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm.ui;

import java.io.IOException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class RolesReport extends TinyCGIBase {

    private static final Resources resources = RolesEditor.resources;

    @Override
    protected void writeContents() throws IOException {
        String title = resources.getHTML("RolesReport.Title");

        out.println("<html><head>");
        out.println("<title>" + title + "</title>");
        out.println("<style>");
        out.println("    h2 { margin-bottom: 0px }");
        out.println("    ul { margin-top: 0px }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<h1>" + title + "</h1>");
        out.println(resources.getHTML("RolesReport.Header"));

        for (Role r : PermissionsManager.getInstance().getAllRoles()) {
            // print the name of the role as a heading
            out.print("<h2>");
            out.print(HTMLUtils.escapeEntities(r.getName()));
            out.println("</h2>");

            // print a list of the permissions in the role
            out.println("<ul>");
            boolean sawItem = false;
            for (Permission p : r.getPermissions()) {
                if (!p.isInactive()) {
                    printPermissionItem(p);
                    sawItem = true;
                }
            }

            // if no permissions were found, print a "no permission" bullet
            if (!sawItem)
                out.print("<li><i>" + resources.getHTML("No_Permission")
                        + "</i></li>");

            out.println("</ul>");
        }

        out.println("</body></html>");
    }

    private void printPermissionItem(Permission p) {
        out.print("<li>");
        out.print(HTMLUtils.escapeEntities(p.toString()));
        out.println("</li>");
    }

}
