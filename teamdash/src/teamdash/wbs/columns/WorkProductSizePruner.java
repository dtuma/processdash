// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.util.Collection;

import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class WorkProductSizePruner implements Pruner {

    private TeamProcess teamProcess;

    private Collection sizesToKeep;

    public WorkProductSizePruner(TeamProcess teamProcess, Collection sizesToKeep) {
        this.teamProcess = teamProcess;
        this.sizesToKeep = sizesToKeep;
    }

    public boolean shouldPrune(WBSNode node) {
        Object size = SizeTypeColumn.getWorkProductSizeMetric(node, teamProcess);
        return size == null || !sizesToKeep.contains(size);
    }

    @Override
    public boolean shouldIncludeHidden(WBSNode node) {
        String type = node.getType();
        return TeamProcess.isCodeTask(type) //
                || TeamProcess.isProbeTask(type) //
                || TeamProcess.isPSPTask(type);
    }

}
