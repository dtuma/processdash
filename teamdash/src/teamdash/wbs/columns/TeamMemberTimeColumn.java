package teamdash.wbs.columns;

import teamdash.TeamMember;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamMemberTime;
import teamdash.wbs.WBSModel;
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

    public boolean validFor(TeamMember t) {
        return teamMember.getInitials().equals(t.getInitials());
    }

    void setTeamMember(TeamMember t) {
        teamMember = t;
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

    public static void changeInitials(DataTableModel dataModel,
                                      String oldInitials,
                                      String newInitials) {
        changeInitials(dataModel.getWBSModel(),
                       dataModel.getWBSModel().getRoot(),
                       oldInitials+"-Time",
                       newInitials+"-Time");
    }

    private static void changeInitials(WBSModel wbsModel, WBSNode node,
                                       String oldID, String newID) {
        node.setAttribute(getTopDownAttrName(newID),
                          node.getAttribute(getTopDownAttrName(oldID)));
        node.setAttribute(getTopDownAttrName  (oldID), null);
        node.setAttribute(getBottomUpAttrName (oldID), null);
        node.setAttribute(getInheritedAttrName(oldID), null);

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = children.length;   i-- > 0; )
            changeInitials(wbsModel, children[i], oldID, newID);
    }

}
