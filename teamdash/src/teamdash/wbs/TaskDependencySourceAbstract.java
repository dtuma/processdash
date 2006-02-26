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
        if (result == null) {
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
