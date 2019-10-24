// Copyright (C) 2019 Tuma Solutions, LLC
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
import java.util.Set;

import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.SizeDataColumn;
import teamdash.wbs.columns.SizeTypeColumn;

/**
 * When a team project has been relaunched, this makes changes to the WBS in the
 * old (pre-relaunch) project.
 */
public class RelaunchWorkerOld {

    public static final String RELAUNCH_PROJECT_SETTING = "cleanupRelaunched";

    public static final String RELAUNCHED_PROJECT_SETTING = "projectRelaunched";


    private TeamProject teamProject;

    private WBSModel wbs;


    public RelaunchWorkerOld(TeamProject teamProject, DataTableModel data) {
        this.teamProject = teamProject;
        this.wbs = teamProject.getWBS();
    }

    public void run() {
        try {
            runImpl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runImpl() {
        clearRelaunchedNodeSizeData();
        recalcWBS();
        cleanUp();
    }


    /**
     * In a project with WBS-managed size, size data is moved forward into the
     * relaunched WBS wherever possible. This method deletes the relaunched size
     * data from the old WBS to avoid double-counting.
     */
    private void clearRelaunchedNodeSizeData() {
        // this logic should only run if the old WBS is using WBS-managed size
        if (SizeTypeColumn.isUsingNewSizeDataColumns(wbs) == false)
            return;

        // make a list of the nodes that survived relaunch, and appear in the
        // relaunched WBS
        Set<WBSNode> relaunchedNodes = new HashSet<WBSNode>();
        getRelaunchedNodes(relaunchedNodes, wbs.getRoot());

        // the relaunched nodes will carry their size data forward with them,
        // so we must clear those duplicate size values from the nodes in the
        // old WBS. (This prevents master rollups from summing the data twice)
        SizeDataColumn.clearSizeDataNodeValues(wbs,
            teamProject.getTeamProcess(), true, true, relaunchedNodes);
    }

    private boolean getRelaunchedNodes(Set<WBSNode> relaunchedNodes,
            WBSNode wbsNode) {
        // keep this logic in this method in sync with the logic in the
        // RelaunchWorkerNew.listCompletedItems() method
        boolean thisNodeComplete;
        WBSNode[] children = wbs.getChildren(wbsNode);
        if (children.length == 0) {
            // check if leaf tasks have been completed
            thisNodeComplete = PercentCompleteColumn.isComplete(wbsNode);

        } else {
            // parent elements are complete if all of their children were
            thisNodeComplete = true;
            for (WBSNode child : children) {
                if (getRelaunchedNodes(relaunchedNodes, child) == false)
                    thisNodeComplete = false;
            }
        }

        // incomplete nodes get moved forward into the relaunched WBS
        if (thisNodeComplete == false)
            relaunchedNodes.add(wbsNode);

        return thisNodeComplete;
    }


    /** fire a WBS event to force the recalculation of all data columns */
    private void recalcWBS() {
        wbs.fireTableRowsUpdated(0, 0);
    }


    /**
     * Update settings that triggered the relaunch cleanup operation.
     */
    private void cleanUp() {
        teamProject.getUserSettings().remove(RELAUNCH_PROJECT_SETTING);
        teamProject.getUserSettings().put(RELAUNCHED_PROJECT_SETTING, "true");
    }

}
