// Copyright (C) 2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;

import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class IndivProjectSummaryRedirect extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {
        String selfUri = (String) env.get("SCRIPT_PATH");
        int end = selfUri.lastIndexOf('/');
        int beg = selfUri.lastIndexOf('/', end - 1);

        StringBuilder newUri = new StringBuilder();
        newUri.append(selfUri.substring(0, beg));
        newUri.append("/cms");
        newUri.append(selfUri.substring(beg, end + 1));

        boolean isPersonalProject = (null != getDataContext()
                .getSimpleValue(TeamDataConstants.PERSONAL_PROJECT_FLAG));
        if (isPersonalProject)
            newUri.append("rollup_plan_summary");
        else
            newUri.append("indiv_plan_summary");

        out.write("Location: " + newUri + "\r\n\r\n");
    }

}
