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

package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;

public class SizeColumnGroup extends NullDataColumn
        implements CalculatedDataColumn {

    public static final String PLAN = "All Plan Size Columns";

    public static final String ACTUAL = "All Actual Size Columns";

    public SizeColumnGroup(boolean plan) {
        this.columnID = getColumnID(plan);
    }

    @Override
    public boolean recalculate() {
        return true;
    }

    @Override
    public void storeDependentColumn(String ID, int columnNumber) {}

    public static String getColumnID(boolean plan) {
        return plan ? PLAN : ACTUAL;
    }

}
