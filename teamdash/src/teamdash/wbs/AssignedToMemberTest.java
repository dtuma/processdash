// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.wbs;

import teamdash.team.TeamMember;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

/**
 * A simple test to determine whether an individual is assigned to a particular
 * WBSNode, or to any of its children.
 * 
 * This test will return true if:
 * <ul>
 * 
 * <li>This individual has nonzero planned time for the tested node or any of
 * its descendants</li>
 * 
 * <li>The individual is assigned to the tested node with a zero estimate. (Note
 * that no assigned-with-zero test is performed on all descendants of the tested
 * node)</li>
 * 
 * </ul>
 * 
 * Instances of this class should be discarded and recreated after changes are
 * saved to the team member list; otherwise, they may not continue to work
 * properly.
 */
public class AssignedToMemberTest implements WBSNodeTest {

    private DataColumn indivTimeColumn;

    private String assignedWithZeroAttr;

    public AssignedToMemberTest(DataTableModel data, TeamMember teamMember) {
        // find the data column that holds planned time for this individual
        String columnID = TeamMemberTimeColumn.getColumnID(teamMember);
        int column = data.findColumn(columnID);
        if (column == -1)
            return;

        indivTimeColumn = data.getColumn(column);
        assignedWithZeroAttr = TeamTimeColumn
                .getMemberAssignedZeroAttrName(teamMember);
    }

    public boolean test(WBSNode node) {
        if (indivTimeColumn == null)
            return false;

        if (node.getAttribute(assignedWithZeroAttr) != null)
            return true;

        NumericDataValue plannedTime = (NumericDataValue) indivTimeColumn
                .getValueAt(node);
        return (plannedTime != null && plannedTime.value > 0);
    }

}
