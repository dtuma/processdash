package teamdash.wbs;

import teamdash.TeamMemberList;
import teamdash.TeamProcess;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WorkflowPercentageColumn;
import teamdash.wbs.columns.WorkflowRateColumn;

public class WorkflowModel extends DataTableModel {

    private TeamProcess teamProcess;

    public WorkflowModel(WBSModel workflows, TeamProcess teamProcess) {
        super(workflows, null);
        this.teamProcess = teamProcess;
    }

    protected void buildDataColumns(TeamMemberList teamList) {
        addDataColumn(new WBSNodeColumn(wbsModel));
        addDataColumn(new WorkflowPercentageColumn(wbsModel));
        addDataColumn(new WorkflowRateColumn(wbsModel));
        addDataColumn(new TaskSizeUnitsColumn(wbsModel, teamProcess));
    }

}
