package teamdash.wbs.columns;

import teamdash.TeamMember;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamMemberTime;
import teamdash.wbs.WBSNode;

public class TeamMemberTimeColumn extends TopDownBottomUpColumn {

    private TeamMember teamMember;

    public TeamMemberTimeColumn(DataTableModel dataModel,
                                TeamMember teamMember) {
        super(dataModel,
              teamMember.getInitials(),
              teamMember.getInitials() + "-Time");
        this.teamMember = teamMember;
    }

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

}
