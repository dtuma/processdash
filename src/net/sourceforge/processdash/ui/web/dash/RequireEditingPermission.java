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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;

import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class RequireEditingPermission extends TinyCGIBase {

    @Override
    protected void writeHeader() {
        out.print("Content-type: application/javascript\r\n\r\n");
    }

    @Override
    protected void writeContents() throws IOException {
        // if we are being called from the nested export, print nothing
        String referer = (String) env.get("HTTP_REFERER");
        if (referer != null && referer.endsWith("/reports/form2html.class"))
            return;

        // get the ID of the required parameter
        String perm = getParameter("perm");
        if (perm == null)
            throw new TinyCGIException(400, "Missing perm parameter");

        // if the user has the parameter in question, write nothing
        if (PermissionsManager.getInstance().hasPermission(perm))
            return;

        // redirect to a page that will export to HTML
        out.print("window.location.replace('/reports/form2html.class');");
        out.flush();
    }

}
