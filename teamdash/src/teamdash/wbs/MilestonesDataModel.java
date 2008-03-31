package teamdash.wbs;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.WBSNodeColumn;

public class MilestonesDataModel extends DataTableModel {

    public MilestonesDataModel(MilestonesWBSModel milestones) {
        super(milestones, null, null, milestones, null, null);
    }

    /** override and create only the columns we're interested in.
     */
    @Override
    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess,
                                    MilestonesWBSModel milestones,
                                    TaskDependencySource dependencySource,
                                    String currentUser)
    {
        addDataColumn(new WBSNodeColumn(milestones));
        addDataColumn(new MilestoneCommitDateColumn());
        addDataColumn(new MilestoneColorColumn(milestones));
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        super.setValueAt(value, rowIndex, columnIndex);
        fireTableCellUpdated(rowIndex, columnIndex);
    }



}
