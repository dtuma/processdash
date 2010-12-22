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

import static teamdash.wbs.columns.TeamTimeColumn.getMemberAssignedZeroAttrName;

import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamMemberTime;
import teamdash.wbs.TeamMemberTimeCellRenderer;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamMemberColumnManager.TeamMemberColumn;

/** A column for displaying the amount of time an individual is spending on
 * each node in the work breakdown structure.
 *
 * This inherits "top-down-bottom-up" column editing behaviors.
 */
public class TeamMemberTimeColumn extends TopDownBottomUpColumn
    implements CustomRenderedColumn, TeamMemberColumn
{

    public static final String ATTR_SUFFIX = "-Time";

    private TeamMember teamMember;

    public TeamMemberTimeColumn(DataTableModel dataModel,
                                TeamMember teamMember) {
        super(dataModel,
              teamMember.getInitials(),
              getColumnID(teamMember));
        this.teamMember = teamMember;
    }

    /** Return true if this column can be reused to display data for the
     * given team member */
    public boolean validFor(TeamMember t) {
        return teamMember.getInitials().equals(t.getInitials());
    }

    /** Update the team member object for this column.
     */
    public void setTeamMember(TeamMember t) {
        if (!validFor(t))
            throw new IllegalArgumentException("Invalid team member");
        teamMember = t;
    }

    /** Translate the value in this column into a TeamMemberTime object,
     * with an associated color. */
    public Object getValueAt(WBSNode node) {
        NumericDataValue value = (NumericDataValue) super.getValueAt(node);
        return new TeamMemberTime(value, teamMember.getColor());
    }

    public Class getColumnClass() {
        return TeamMemberTime.class;
    }

    public String[] getAffectedColumnIDs() {
        return AFFECTED_COLUMN;
    }

    private static final String[] AFFECTED_COLUMN = { "Time" };

    public TableCellRenderer getCellRenderer() {
        return CELL_RENDERER;
    }

    private static final TableCellRenderer CELL_RENDERER =
        new TeamMemberTimeCellRenderer();


    /** Instances of this column use a team member's initials to store
     * and retrieve data. Therefore, when a team member's initials change,
     * it is necessary to rename all of the data appropriately. This method
     * performs that operation.
     */
    public static void changeInitials(DataTableModel dataModel,
                                      String oldInitials,
                                      String newInitials) {
        // calculate the node attribute names applicable to this rename
        String oldID = oldInitials + ATTR_SUFFIX;
        String newID = newInitials + ATTR_SUFFIX;
        String oldTopAttr = getTopDownAttrName(oldID);
        String newTopAttr = getTopDownAttrName(newID);
        String oldBtmAttr = getBottomUpAttrName(oldID);
        String oldInherAttr = getInheritedAttrName(oldID);
        String oldZeroAttr = getMemberAssignedZeroAttrName(oldInitials);
        String newZeroAttr = getMemberAssignedZeroAttrName(newInitials);

        // perform the change, starting at the root of the wbs.
        changeInitials(dataModel.getWBSModel(),
                       dataModel.getWBSModel().getRoot(),
                       oldTopAttr, newTopAttr, oldBtmAttr, oldInherAttr,
                       oldZeroAttr, newZeroAttr);
    }

    private static void changeInitials(WBSModel wbsModel, WBSNode node,
            String oldTopAttr, String newTopAttr, String oldBtmAttr,
            String oldInherAttr, String oldZeroAttr, String newZeroAttr) {
        node.setAttribute(newTopAttr, node.getAttribute(oldTopAttr));
        node.setAttribute(oldTopAttr, null);
        node.setAttribute(oldBtmAttr, null);
        node.setAttribute(oldInherAttr, null);

        node.setAttribute(newZeroAttr, node.getAttribute(oldZeroAttr));
        node.setAttribute(oldZeroAttr, null);

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = children.length;   i-- > 0; )
            changeInitials(wbsModel, children[i], oldTopAttr, newTopAttr,
                oldBtmAttr, oldInherAttr, oldZeroAttr, newZeroAttr);
    }

    public static String getColumnID(TeamMember teamMember) {
        return teamMember.getInitials() + ATTR_SUFFIX;
    }

    public static String getMemberNodeDataAttrName(TeamMember m) {
        return getTopDownAttrName(getColumnID(m));
    }

}
