// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumnModel;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.CustomColumnManager;
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.LabelSource;
import teamdash.wbs.columns.MilestoneColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.PhaseColumn;
import teamdash.wbs.columns.PlanTimeWatcher;
import teamdash.wbs.columns.ProxyEstBucketColumn;
import teamdash.wbs.columns.ProxyEstTypeColumn;
import teamdash.wbs.columns.SizeTypeColumn;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TaskSizeColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamMemberColumnManager;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WbsNodeAttributeSource;


public class WBSDataModel extends DataTableModel<WBSModel> {

    /** A list of the columns which are sources of label data */
    private Set<Integer> labelSources;

    /** A list of the columns which are sources of attribute data */
    private Set<Integer> attrSources;

    /** The manager of integrations with external systems */
    private ExternalSystemManager extSysMgr;

    /** Object which manages columns for team members */
    private TeamMemberColumnManager memberColumnManager;

    /** Object which manages custom columns */
    private CustomColumnManager customColumnManager;


    public WBSDataModel(WBSModel wbsModel, TeamMemberList teamList,
            TeamProcess teamProcess, WorkflowWBSModel workflows,
            ProxyWBSModel proxies, MilestonesWBSModel milestones,
            CustomColumnSpecs customColumns, ExternalSystemManager extSysMgr,
            TaskDependencySource dependencySource, String currentUser) {
        super(wbsModel);
        labelSources = new HashSet<Integer>();
        attrSources = new HashSet<Integer>();
        this.extSysMgr = extSysMgr;
        buildDataColumns(teamList, teamProcess, workflows, proxies, milestones,
            customColumns, dependencySource, currentUser);
        initializeColumnDependencies();
    }

    /** Add a single data column to the data model */
    @Override
    public int addDataColumn(DataColumn column) {
        int newColumnIndex = super.addDataColumn(column);
        if (column instanceof LabelSource)
            labelSources.add(newColumnIndex);
        if (column instanceof WbsNodeAttributeSource)
            attrSources.add(newColumnIndex);
        return newColumnIndex;
    }

    /** Remove a single data column from the data model */
    @Override
    public int removeDataColumn(DataColumn column) {
        int pos = super.removeDataColumn(column);
        labelSources.remove(pos);
        attrSources.remove(pos);
        return pos;
    }

    public Integer[] getLabelSourceColumns() {
        return labelSources.toArray(new Integer[labelSources.size()]);
    }

    public Integer[] getAttributeSourceColumns() {
        return attrSources.toArray(new Integer[attrSources.size()]);
    }

    public final ExternalSystemManager getExternalSystemManager() {
        return extSysMgr;
    }

    /** Add time columns for each team member to the given column model. */
    public void addTeamMemberPlanTimes(TableColumnModel columnModel) {
        memberColumnManager.addPlanTimesToColumnModel(columnModel);
    }

    /** Add time columns for each team member to the given column model. */
    public void addTeamMemberActualTimes(TableColumnModel columnModel) {
        memberColumnManager.addActualTimesToColumnModel(columnModel);
    }

    /** Get a list of the column numbers for each team member column. */
    public IntList getTeamMemberColumnIDs() {
        IntList result = new IntList();
        Iterator i = memberColumnManager.getPlanTimeColumns().iterator();
        while (i.hasNext())
            result.add(findIndexOfColumn(i.next()));

        return result;
    }

    /** Register a listener that should be notified about team member column
     * changes */
    public void addTeamMemberColumnListener(ChangeListener l) {
        memberColumnManager.addTeamMemberColumnListener(l);
    }

    /** Add all custom columns to the given column model. */
    public void addCustomColumns(TableColumnModel columnModel) {
        customColumnManager.addColumnsToColumnModel(columnModel);
    }

    public CustomColumnManager getCustomColumnManager() {
        return customColumnManager;
    }

    /** Create a set of data columns for this data model. */
    private void buildDataColumns(TeamMemberList teamList,
            TeamProcess teamProcess, WorkflowWBSModel workflows,
            ProxyWBSModel proxies, MilestonesWBSModel milestones,
            CustomColumnSpecs projectColumns,
            TaskDependencySource dependencySource, String currentUser)
    {
        addDataColumn(new WBSNodeColumn(wbsModel));
        SizeTypeColumn.createSizeColumns(this, teamProcess);
        addDataColumn(new PhaseColumn(this, teamProcess, workflows));
        memberColumnManager = new TeamMemberColumnManager(this, workflows,
                teamList);
        addDataColumn(new TaskSizeColumn(this, teamProcess));
        addDataColumn(new TaskSizeUnitsColumn(this, teamProcess));
        addDataColumn(new TeamTimeColumn(this, milestones,
                teamList.isSinglePersonTeam()));
        addDataColumn(new TeamActualTimeColumn(this, milestones, teamList));
        addDataColumn(new TaskLabelColumn(this));
        addDataColumn(new MilestoneColumn(this, milestones));
        addDataColumn(new ProxyEstTypeColumn(this, proxies));
        addDataColumn(new ProxyEstBucketColumn(this, proxies, teamProcess));
        addDataColumn(new TaskDependencyColumn(this, dependencySource,
                teamProcess.getIconMap()));
        addDataColumn(new NotesColumn(currentUser));
        addDataColumn(new ErrorNotesColumn(currentUser));
        addDataColumn(new PlanTimeWatcher(this, teamProcess));
        extSysMgr.createDataColumns(this);
        customColumnManager = new CustomColumnManager(this, projectColumns,
                teamProcess.getProcessID());
    }

}
