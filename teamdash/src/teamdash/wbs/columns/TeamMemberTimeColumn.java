// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

import java.util.Map;

import javax.swing.table.TableCellRenderer;

import teamdash.merge.ui.MergeConflictNotification;
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

    private String assignWithZeroAttr, assignWithZeroInheritedAttr;

    public TeamMemberTimeColumn(DataTableModel dataModel,
                                TeamMember teamMember) {
        super(dataModel,
              teamMember.getInitials(),
              getColumnID(teamMember));
        this.teamMember = teamMember;
        this.assignWithZeroAttr = getMemberAssignedZeroAttrName(teamMember);
        this.assignWithZeroInheritedAttr = "_" + assignWithZeroAttr;
        setConflictAttributeName(topDownAttrName);
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
        value.isInvisible = (value.value == 0 && value.errorMessage == null //
                && isAssignedWithZero(node) == false);
        return new TeamMemberTime(value, teamMember.getColor());
    }

    @Override
    protected double recalc(WBSNode node) {
        node.removeAttribute(assignWithZeroInheritedAttr);
        double result = super.recalc(node);
        if (result > 0) {
            node.removeAttribute(assignWithZeroAttr);
        } else {
            WBSNode parent = wbsModel.getParent(node);
            if (parent != null && isAssignedWithZero(node))
                parent.setAttribute(assignWithZeroInheritedAttr, "t");
        }
        return result;
    }

    private boolean isAssignedWithZero(WBSNode node) {
        return node.getAttribute(assignWithZeroAttr) != null
                || node.getAttribute(assignWithZeroInheritedAttr) != null;
    }

    @Override
    protected boolean attemptToRepairTopDownBottomUpMismatch(WBSNode node,
            double topDownValue, double bottomUpValue, WBSNode[] children,
            int numToInclude) {
        // when a leaf node is assigned to individuals and is later subdivided,
        // this almost always results in a top-down-bottom-up mismatch. Users
        // rarely see or fix these mismatches, because they are only visible on
        // the rarely-used planned time tab. To alleviate this problem, this
        // method silently clears erroneous top-down estimates once bottom-up
        // estimates have been entered.
        if (bottomUpValue > 0) {
            node.removeAttribute(topDownAttrName);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void adjustConflictNotification(MergeConflictNotification mcn) {
        mcn.setMessageKey("Wbs.Team_Member_Time");
        mcn.putAttribute("teamMemberName", teamMember.getName());
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
     * and retrieve data. Therefore, when the initials have changed for
     * several team members, it is necessary to rename all of the data
     * appropriately. This method performs that operation.
     */
    public static void changeInitials(WBSModel wbsModel,
            Map<String, String> initialsToChange) {
        // update time data for any team members whose initials have changed.
        // since it is possible for two team members to have swapped initials
        // (or for larger cycles to exist), we perform the changes in 2 steps:
        //   (1) Rename the data from the old initials to something which is
        //       not a legal initials value, and is thus safe to avoid
        //       collisions with real data.
        //   (2) Rename the data from its interim value to the new initials.

        for (String oldInitials : initialsToChange.keySet()) {
            TeamMemberTimeColumn.changeInitials
                (wbsModel, oldInitials, oldInitials + " ");
        }

        for (Map.Entry<String, String> me : initialsToChange.entrySet()) {
            String oldInitials = me.getKey();
            String newInitials = me.getValue();
            TeamMemberTimeColumn.changeInitials
                (wbsModel, oldInitials + " ", newInitials);
        }
    }

    /** Instances of this column use a team member's initials to store
     * and retrieve data. Therefore, when a team member's initials change,
     * it is necessary to rename all of the data appropriately. This method
     * performs that operation.
     */
    public static void changeInitials(WBSModel wbsModel,
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
        changeInitials(wbsModel, wbsModel.getRoot(),
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

    public static final String TEAM_MEMBER_TIME_SUFFIX =
        getTopDownAttrName(ATTR_SUFFIX);

}
