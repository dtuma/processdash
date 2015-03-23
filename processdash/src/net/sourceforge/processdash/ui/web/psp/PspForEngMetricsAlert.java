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


import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.ui.web.reports.snippets.MetricsAlert;

public class PspForEngMetricsAlert extends MetricsAlert {

    @Override
    protected boolean alertIsDisabled() {
        if (testPspLevel(getParameter("PspLevel"), getDataContext()))
            return false;
        else
            return true;
    }

    @Override
    protected boolean supportClipboard(AlertType type) {
        return true;
    }

    protected static boolean testPspLevel(String pspLevel, DataContext data) {
        int pos = PspForEngBase.PSP_LEVELS.indexOf(pspLevel);
        if (pos < 1)
            return true;

        for (int i = pos; i < PspForEngBase.PSP_LEVELS.size(); i++) {
            if (data.getSimpleValue(PspForEngBase.PSP_LEVELS.get(i)) != null)
                return true;
        }

        return false;
    }

}
