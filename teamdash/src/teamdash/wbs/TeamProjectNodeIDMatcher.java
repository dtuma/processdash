// Copyright (C) 2012-2013 Tuma Solutions, LLC
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

import java.util.Map;

import teamdash.wbs.columns.MilestoneColumn;
import teamdash.wbs.columns.TaskDependencyColumn;

public class TeamProjectNodeIDMatcher {

    /**
     * In preparation for a 3-way merge, examine the data for the WBS, for the
     * workflows, and for the milestones. Reassign node IDs in the incoming team
     * project as needed to ensure the best match. Also, update the values of
     * affected WBS node attributes based on these remappings.
     * 
     * After this method, IDs in the incoming team project may have changed.
     * 
     * @return true if any node IDs were changed, false otherwise.
     */
    public static boolean performMatch(TeamProject base, TeamProject main,
            TeamProject incoming) {

        // First, remap node IDs in the workflow model
        Map<Integer, Integer> workflowIDMappings = matchWBS(
            base.getWorkflows(), main.getWorkflows(), incoming.getWorkflows());

        // Next, remap node IDs in the proxies model
        Map<Integer, Integer> proxyIDMappings = matchWBS(base.getProxies(),
            main.getProxies(), incoming.getProxies());

        // Next, remap node IDs in the milestones model
        Map<Integer, Integer> milestoneIDMappings = matchWBS(
            base.getMilestones(), main.getMilestones(),
            incoming.getMilestones());

        // Next, remap node IDs in the main WBS
        Map<Integer, Integer> wbsIDMappings = matchWBS(base.getWBS(),
            main.getWBS(), incoming.getWBS(), WBS_ALIAS_ATTRS);

        // Finally, apply these remapped IDs to the affected attributes in
        // the main WBS.
        incoming.getWBS().remapFilteredNodeIDs(wbsIDMappings);
        WorkflowUtil.remapWorkflowSourceIDs(incoming.getWBS(), workflowIDMappings);
        MilestoneColumn.remapNodeIDs(incoming.getWBS(), milestoneIDMappings);
        TaskDependencyColumn.remapNodeIDs(incoming.getWBS(), incoming
                .getProjectID(), wbsIDMappings);

        // return true if any node IDs were remapped
        return !workflowIDMappings.isEmpty()
                || !milestoneIDMappings.isEmpty()
                || !wbsIDMappings.isEmpty();
    }

    private static Map<Integer, Integer> matchWBS(WBSModel baseWBS,
            WBSModel mainWBS, WBSModel incomingWBS, String... aliasAttrs) {
        WBSNodeIDMatcher matcher = new WBSNodeIDMatcher(baseWBS, mainWBS,
                incomingWBS, aliasAttrs);
        return matcher.getRemappedIDs();
    }

    private static final String[] WBS_ALIAS_ATTRS = {
        MasterWBSUtil.MASTER_NODE_ID,
    };

}
