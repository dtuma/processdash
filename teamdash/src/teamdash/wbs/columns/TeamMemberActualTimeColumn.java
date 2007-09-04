package teamdash.wbs.columns;

import teamdash.team.TeamMember;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.columns.TeamMemberColumnManager.TeamMemberColumn;

public class TeamMemberActualTimeColumn extends AbstractPrecomputedColumn
        implements CalculatedDataColumn, TeamMemberColumn {

    private static final String RESULT_ATTR_SUFFIX = "-Actual_Time";
    private static final String NODE_ATTR_SUFFIX = "@Actual_Node_Time";

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

    static final String getResultDataAttrName(TeamMember m) {
        return m.getInitials() + RESULT_ATTR_SUFFIX;
    }

}
