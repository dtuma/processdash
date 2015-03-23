// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

/**
 * A reusable base implementation of the
 * {@link teamdash.wbs.TaskDependencySource} interface.
 */
public abstract class TaskDependencySourceAbstract implements
        TaskDependencySource {

    /**
     * The tree of potentially dependent tasks which we will provide to our
     * clients.
     */
    private WBSModel taskTree;

    /**
     * The ID of the project for which we are providing dependency information
     */
    private String projectID;

    public TaskDependencySourceAbstract(WBSModel taskTree, String projectID) {
        this.taskTree = taskTree;
        this.projectID = projectID;
    }

    protected String getDisplayNameForNode(WBSModel model, Map nodeCache,
            String nodeId) {
        WBSNode node = (WBSNode) nodeCache.get(nodeId);
        if (node == null) {
            refreshNodeMap(model, nodeCache);
            node = (WBSNode) nodeCache.get(nodeId);
        }

        if (node == null)
            return UNKNOWN_NODE_DISPLAY_NAME;

        String result = model.getFullName(node);
        if (result == null || result.startsWith("null/")) {
            nodeCache.remove(nodeId);
            return getDisplayNameForNode(model, nodeCache, nodeId);
        } else
            return result;
    }

    protected void refreshNodeMap(WBSModel model, Map nodeCache) {
        WBSNode[] allNodes = model.getDescendants(model.getRoot());
        for (int i = 0; i < allNodes.length; i++) {
            String nodeID = getNodeID(allNodes[i]);
            nodeCache.put(nodeID, allNodes[i]);
        }
    }

    public String getNodeID(WBSNode node) {
        return MasterWBSUtil.getNodeID(node, projectID);
    }

    public WBSModel getTaskTree() {
        return taskTree;
    }

    protected String getProjectID() {
        return projectID;
    }
}
