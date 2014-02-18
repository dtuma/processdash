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

import teamdash.team.TeamMember;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.columns.TeamMemberColumnManager.TeamMemberColumn;

public class TeamMemberActualTimeColumn extends AbstractPrecomputedColumn
        implements CalculatedDataColumn, TeamMemberColumn {

    private static final String RESULT_ATTR_SUFFIX = "-Actual_Time";
    private static final String NODE_ATTR_SUFFIX = "@Actual_Node_Time";
    private static final String SUBTAKS_ATTR_SUFFIX = "@Actual_Node_Subtasks";

    private String initials;

    public TeamMemberActualTimeColumn(TeamMember teamMember) {
        super(getColumnID(teamMember), teamMember.getInitials(),
            getResultDataAttrName(teamMember), TeamActualTimeColumn.COLUMN_ID);
        this.initials = teamMember.getInitials();
    }

    /** Return true if this column can be reused to display data for the
     * given team member */
    public boolean validFor(TeamMember t) {
        return initials.equals(t.getInitials());
    }

    public void setTeamMember(TeamMember t) {}


    public static final String getColumnID(TeamMember m) {
        return m.getInitials() + "-Actual-Time";
    }

    public static final String getNodeDataAttrName(TeamMember m) {
        return m.getInitials() + NODE_ATTR_SUFFIX;
    }

    public static String getSubtaskDataAttrName(TeamMember m) {
        return m.getInitials() + SUBTAKS_ATTR_SUFFIX;
    }

    static final String getResultDataAttrName(TeamMember m) {
        return getResultDataAttrName(m.getInitials());
    }

    static final String getResultDataAttrName(String initials) {
        return initials + RESULT_ATTR_SUFFIX;
    }

}
