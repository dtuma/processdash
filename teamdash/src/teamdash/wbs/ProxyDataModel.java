// Copyright (C) 2014-2015 Tuma Solutions, LLC
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
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.ProxyRateColumn;
import teamdash.wbs.columns.ProxySizeColumn;
import teamdash.wbs.columns.ProxyTimeColumn;
import teamdash.wbs.columns.WBSNodeColumn;

public class ProxyDataModel extends DataTableModel {

    public ProxyDataModel(ProxyWBSModel proxies, TeamProcess process) {
        super(proxies, null, process, null, proxies, null, null, null, null);
    }

    /**
     * override and create only the columns we're interested in.
     */
    @Override
    protected void buildDataColumns(TeamMemberList teamList,
            TeamProcess teamProcess, WorkflowWBSModel workflows,
            ProxyWBSModel proxies, MilestonesWBSModel milestones,
            CustomColumnSpecs columns, TaskDependencySource dependencySource,
            String currentUser) {
        addDataColumn(new WBSNodeColumn(proxies));
        ProxySizeColumn size = new ProxySizeColumn(this, teamProcess);
        ProxyRateColumn rate = new ProxyRateColumn(this, size);
        ProxyTimeColumn time = new ProxyTimeColumn(this, size, rate);
        rate.setTimeColumn(time);
        addDataColumn(size);
        addDataColumn(rate);
        addDataColumn(time);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        super.setValueAt(value, rowIndex, columnIndex);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public ProxyWBSModel getWBSModel() {
        return (ProxyWBSModel) super.getWBSModel();
    }

}
