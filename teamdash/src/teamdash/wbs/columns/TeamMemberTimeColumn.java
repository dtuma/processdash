package teamdash.wbs.columns;

import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamMemberTime;
import teamdash.wbs.TeamMemberTimeCellRenderer;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

/** A column for displaying the amount of time an individual is spending on
 * each node in the work breakdown structure.
 *
 * This inherits "top-down-bottom-up" column editing behaviors.
 */
public class TeamMemberTimeColumn extends TopDownBottomUpColumn
    implements CustomRenderedColumn
{

    public static final String ATTR_SUFFIX = "-Time";

    private TeamMember teamMember;

    public TeamMemberTimeColumn(DataTableModel dataModel,
                                TeamMember teamMember) {
        super(dataModel,
              teamMember.getInitials(),
              teamMember.getInitials() + ATTR_SUFFIX);
        this.teamMember = teamMember;
    }

    /** Return true if this column can be reused to display data for the
     * given team member */
    public boolean validFor(TeamMember t) {
        return teamMember.getInitials().equals(t.getInitials());
    }

    /** Update the team member object for this column.
     */
    void setTeamMember(TeamMember t) {
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
        // perform the change, starting at the root of the wbs.
        changeInitials(dataModel.getWBSModel(),
                       dataModel.getWBSModel().getRoot(),
                       oldInitials+ATTR_SUFFIX,
                       newInitials+ATTR_SUFFIX);
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
