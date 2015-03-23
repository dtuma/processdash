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

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.MilestoneDeferredColumn;
import teamdash.wbs.columns.MilestoneVisibilityColumn;
import teamdash.wbs.columns.WBSNodeColumn;

public class MilestonesDataModel extends DataTableModel {

    public MilestonesDataModel(MilestonesWBSModel milestones) {
        super(milestones, null, null, null, null, milestones, null, null);
    }

    /** override and create only the columns we're interested in.
     */
    @Override
    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess,
                                    WorkflowWBSModel workflows,
                                    ProxyWBSModel proxies,
                                    MilestonesWBSModel milestones,
                                    TaskDependencySource dependencySource,
                                    String currentUser)
    {
        addDataColumn(new WBSNodeColumn(milestones));
        addDataColumn(new MilestoneCommitDateColumn());
        addDataColumn(new MilestoneColorColumn(milestones));
        addDataColumn(new MilestoneVisibilityColumn());
        addDataColumn(new MilestoneDeferredColumn());
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        super.setValueAt(value, rowIndex, columnIndex);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public MilestonesWBSModel getWBSModel() {
        return (MilestonesWBSModel) super.getWBSModel();
    }

}
