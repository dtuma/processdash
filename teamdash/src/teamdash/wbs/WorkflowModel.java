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

package teamdash.wbs;

import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WorkflowLabelColumn;
import teamdash.wbs.columns.WorkflowNotesColumn;
import teamdash.wbs.columns.WorkflowNumPeopleColumn;
import teamdash.wbs.columns.WorkflowPercentageColumn;
import teamdash.wbs.columns.WorkflowRateColumn;
import teamdash.wbs.columns.WorkflowScriptColumn;
import teamdash.wbs.columns.WorkflowSizeUnitsColumn;

/** A customized DataTableModel containing only the columns pertinent
 * to editing workflows.
 */
public class WorkflowModel extends DataTableModel {


    public WorkflowModel(WBSModel workflows, TeamProcess teamProcess) {
        super(workflows, null, teamProcess, null, null, null, null);
    }

    /** override and create only the columns we're interested in.
     */
    @Override
    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess,
                                    WorkflowWBSModel workflows,
                                    MilestonesWBSModel milestones,
                                    TaskDependencySource dependencySource,
                                    String currentUser)
    {
        addDataColumn(new WBSNodeColumn(wbsModel));
        addDataColumn(new WorkflowPercentageColumn(wbsModel));
        addDataColumn(new WorkflowRateColumn(this));
        addDataColumn(new WorkflowSizeUnitsColumn(this, teamProcess));
        addDataColumn(new WorkflowNumPeopleColumn(wbsModel));
        addDataColumn(new WorkflowLabelColumn(this));
        addDataColumn(new WorkflowNotesColumn());

        if (supportsURLs())
            addDataColumn(new WorkflowScriptColumn());
    }

    public boolean supportsURLs() {
        WBSNode root = wbsModel.getRoot();
        String createdWithVersion = (String) root
                .getAttribute(WBSModel.CREATED_WITH_ATTR);
        if (XMLUtils.hasValue(createdWithVersion))
            return VersionUtils.compareVersions(MIN_URL_VERSION,
                createdWithVersion) <= 0;
        else
            return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        super.setValueAt(value, rowIndex, columnIndex);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    private static final String MIN_URL_VERSION = "3.9.0";

    public static final String WORKFLOW_SOURCE_IDS_ATTR = "workflowSourceIDs";

    public static final PatternList WORKFLOW_ATTRS = new PatternList()
            .addLiteralStartsWith("Workflow ")
            .addLiteralEquals(TeamTimeColumn.RATE_ATTR)
            .addLiteralEquals(WorkflowSizeUnitsColumn.ATTR_NAME)
            .addLiteralEquals(WorkflowNumPeopleColumn.ATTR_NAME)
            .addLiteralEquals(TaskLabelColumn.VALUE_ATTR)
            .addLiteralEquals(NotesColumn.VALUE_ATTR);

}
