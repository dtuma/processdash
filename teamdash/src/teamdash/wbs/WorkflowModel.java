package teamdash.wbs;

import teamdash.TeamMemberList;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WorkflowNumPeopleColumn;
import teamdash.wbs.columns.WorkflowPercentageColumn;
import teamdash.wbs.columns.WorkflowRateColumn;

public class WorkflowModel extends DataTableModel {

//    private TeamProcess teamProcess;

    public WorkflowModel(WBSModel workflows, TeamProcess teamProcess) {
        super(workflows, null, teamProcess);
//        this.teamProcess = teamProcess;
    }

    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess)
    {
        addDataColumn(new WBSNodeColumn(wbsModel));
        addDataColumn(new WorkflowPercentageColumn(wbsModel));
        addDataColumn(new WorkflowRateColumn(this));
        addDataColumn(new TaskSizeUnitsColumn(this, teamProcess));
        addDataColumn(new WorkflowNumPeopleColumn(wbsModel));
    }

}
