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

package teamdash.sync;

import static net.sourceforge.processdash.team.TeamDataConstants.TEAM_DATA_DIRECTORY;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class ExtRefreshCoordinatorTrigger extends TinyCGIBase {

    protected void writeContents() throws IOException {
        if (doRefresh())
            out.write("OK");
        else
            out.write("FAIL");
    }

    private boolean doRefresh() {
        // get the filesystem-based team data directory for this project
        SimpleData sd = getDataContext().getSimpleValue(TEAM_DATA_DIRECTORY);
        if (sd == null || !sd.test())
            return false;
        File dir = new File(sd.format());

        // get the "zealous" setting from the query parameter
        boolean zealous = !parameters.containsKey("lazy");

        // get the timeout duration, either from a query parameter or default
        int timeout;
        try {
            timeout = Integer.parseInt(getParameter("timeout"));
        } catch (Exception e) {
            timeout = 30;
        }

        // perform a refresh of the directory and return the result
        return ExtRefreshCoordinator.runExtRefresh(dir, zealous, timeout);
    }

}
