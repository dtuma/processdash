// Copyright (C) 2019 Tuma Solutions, LLC
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

import java.util.regex.Matcher;

import teamdash.team.TeamMember;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;

public class SizeOwnerColumn extends AbstractDataColumn
        implements CalculatedDataColumn {

    public static final String COLUMN_ID = "Size Owner";


    private DataTableModel dataModel;

    private int resourcesCol;

    public SizeOwnerColumn(DataTableModel dataModel) {
        this.dataModel = dataModel;
        this.columnID = this.columnName = COLUMN_ID;
        this.dependentColumns = new String[] {
                TeamTimeColumn.RESOURCES_COL_ID };
        resetDependentColumns();
    }

    public void storeDependentColumn(String columnId, int columnNumber) {
        if (TeamTimeColumn.RESOURCES_COL_ID.equals(columnId))
            resourcesCol = columnNumber;
    }

    public void resetDependentColumns() {
        resourcesCol = -1;
    }

    public boolean recalculate() {
        return true;
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    public void setValueAt(Object aValue, WBSNode node) {}

    public Object getValueAt(WBSNode node) {
        if (resourcesCol == -1)
            return null;

        while (node != null) {
            // get the value in the "Assigned To" column
            Object resources = dataModel.getValueAt(node, resourcesCol);
            if (resources != null) {

                // The "Assigned To" column displays the person with the most
                // hours first. If more than one person shares top billing on a
                // leaf component/task, it lists them in the order they were
                // entered. So we can return the first set of initials that
                // appears, and get the "best" owner of this component
                Matcher m = TeamMember.INITIALS_PATTERN
                        .matcher(resources.toString());
                if (m.find())
                    return m.group();
            }

            // If this node has no owner, look at the parent. (This covers
            // cases like an unassigned leaf subcomponent that was created
            // purely for the purposes of size data entry.)
            node = node.getWbsModel().getParent(node);
        }

        return null;
    }

}
