package teamdash.wbs;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WorkflowNumPeopleColumn;
import teamdash.wbs.columns.WorkflowPercentageColumn;
import teamdash.wbs.columns.WorkflowRateColumn;

/** A customized DataTableModel containing only the columns pertinent
 * to editing workflows.
 */
public class WorkflowModel extends DataTableModel {


    public WorkflowModel(WBSModel workflows, TeamProcess teamProcess) {
        super(workflows, null, teamProcess, null, null);
    }

    /** override and create only the columns we're interested in.
     */
    @Override
    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess,
                                    TaskDependencySource dependencySource,
                                    String currentUser)
    {
        addDataColumn(new WBSNodeColumn(wbsModel));
        addDataColumn(new WorkflowPercentageColumn(wbsModel));
        addDataColumn(new WorkflowRateColumn(this));
        addDataColumn(new TaskSizeUnitsColumn(this, teamProcess));
        addDataColumn(new WorkflowNumPeopleColumn(wbsModel));
    }

}
