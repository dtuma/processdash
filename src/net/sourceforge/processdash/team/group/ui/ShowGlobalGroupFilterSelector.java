// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group.ui;

import java.io.IOException;

import javax.swing.JMenuBar;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class ShowGlobalGroupFilterSelector extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        getMainGroupFilterMenu().doClick();
        DashController.printNullDocument(out);
    }

    private GroupFilterMenu getMainGroupFilterMenu() {
        ProcessDashboard dash = (ProcessDashboard) getDashboardContext();
        JMenuBar menuBar = dash.getConfigurationMenus();
        for (int i = menuBar.getMenuCount(); i-- > 0;) {
            Object item = menuBar.getMenu(i);
            if (item instanceof GroupFilterMenu)
                return (GroupFilterMenu) item;
        }
        return null;
    }

}
