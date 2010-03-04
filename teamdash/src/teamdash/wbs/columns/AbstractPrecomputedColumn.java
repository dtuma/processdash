// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class AbstractPrecomputedColumn extends AbstractNumericColumn implements
        CalculatedDataColumn {

    private String attrName;

    protected AbstractPrecomputedColumn(String columnID, String columnName,
            String attrName, String dependentColumnID) {
        this.columnID = columnID;
        this.columnName = columnName;
        this.attrName = attrName;
        if (dependentColumnID != null)
            this.dependentColumns = new String[] { dependentColumnID };
    }

    @Override
    public Object getValueAt(WBSNode node) {
        double time = node.getNumericAttribute(attrName);
        return new NumericDataValue(time, false, !(time > 0), null);
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

}
