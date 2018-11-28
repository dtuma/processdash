// Copyright (C) 2018 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import static teamdash.wbs.columns.TeamTimeColumn.TEAM_TIME_ATTR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

/**
 * Sometimes a user will apply a workflow to a component, then delete a few
 * tasks that aren't required for the given component. This class is designed to
 * pull the time from those deleted tasks, and spread it across the remaining
 * tasks based on their phase percentages.
 */
public class WorkflowDeletedTimeSpreader {

    private WBSModel wbs;

    private DataTableModel data;

    private WorkflowWBSModel workflows;

    private Map<WBSNode, Double> timesToSpread;

    public WorkflowDeletedTimeSpreader(WBSModel wbs, DataTableModel data,
            WorkflowWBSModel workflows, List<WBSNode> deletedNodes) {
        this.wbs = wbs;
        this.data = data;
        this.workflows = workflows;
        if (workflows != null && deletedNodes != null)
            addTimeFromDeletedNodes(deletedNodes);
    }

    private void addTimeFromDeletedNodes(List<WBSNode> deletedNodes) {
        timesToSpread = new HashMap<WBSNode, Double>();

        // calculate the deepest indentation level
        int maxIndent = 0;
        for (WBSNode node : deletedNodes)
            maxIndent = Math.max(maxIndent, node.getIndentLevel());

        // start deleting nodes, starting with the deepest first
        for (int indent = maxIndent; indent > 0; indent--) {
            for (WBSNode node : deletedNodes) {
                if (node.getIndentLevel() == indent)
                    addTimeFromDeletedNode(node);
            }
        }
    }

    private void addTimeFromDeletedNode(WBSNode nodeToDelete) {
        // if we previously deleted children of this node, fetch that time
        Double childTime = timesToSpread.remove(nodeToDelete);

        // check to see if this node came from applying a workflow. If it is
        // not a workflow step or workflow-internal subcomponent, abort.
        String workflowStepName = WorkflowUtil.getWorkflowStepName(nodeToDelete,
            workflows, null);
        if (workflowStepName == null)
            return;

        // calculate the amount of time allocated to this node
        double nodeTime = nodeToDelete.getNumericAttribute(TEAM_TIME_ATTR);
        double totalTime = sum(childTime, nodeTime);

        // reallocate the time to the parent of this node
        if (totalTime > 0) {
            WBSNode parent = wbs.getParent(nodeToDelete);
            Double parentTime = timesToSpread.get(parent);
            double newParentTime = sum(parentTime, totalTime);
            timesToSpread.put(parent, newParentTime);
        }
    }

    private double sum(Double a, double b) {
        double result = 0;
        if (a != null)
            result += a.doubleValue();
        if (b > 0)
            result += b;
        return result;
    }

    public boolean respreadDeletedTime() {
        // if we have nothing to do, abort
        if (timesToSpread == null || timesToSpread.isEmpty())
            return false;

        // find the TeamTimeColumn in the data model
        int col = data.findColumn(TeamTimeColumn.COLUMN_ID);
        if (col == -1)
            return false;
        TeamTimeColumn ttc = (TeamTimeColumn) data.getColumn(col);

        // add time to each of the nodes in question
        boolean madeChange = false;
        for (Entry<WBSNode, Double> e : timesToSpread.entrySet()) {
            WBSNode node = e.getKey();
            Double time = e.getValue();

            // if the node is no longer present in the WBS model, skip it
            if (wbs.getIndexOfNode(node) < 0)
                continue;

            // if time has been logged to any subtasks, skip this node
            if (TeamActualTimeColumn.hasActualTime(node))
                continue;

            // if some tasks under the node have been marked complete, skip it
            if (PercentCompleteColumn.isNonzeroPercentComplete(node))
                continue;

            if (wbs.isLeaf(node)) {
                // if all tasks were deleted from underneath the node, just
                // store the deleted time on the node itself. This makes it
                // possible to delete workflow tasks from underneath a component
                // without losing the top-level time estimate.
                ttc.setValueAt(time, node);

            } else {
                // in the regular case, reallocate the deleted time to the other
                // workflow tasks that appear underneath this component.
                ttc.addTimeToWorkflowTasks(node, time);
            }
            madeChange = true;
        }

        // let our caller know if a change was made
        return madeChange;
    }

}
