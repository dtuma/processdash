package teamdash.wbs;

/**
 * An interface for providing information about tasks that a project can list as
 * its dependencies.
 */
public interface TaskDependencySource {

    public static final String UNKNOWN_NODE_DISPLAY_NAME = "?????";

    /**
     * Return a string that can be displayed to the user to represent the
     * dependent task with the given ID.
     * 
     * If no task can be found with the given ID, returns
     * {@link #UNKNOWN_NODE_DISPLAY_NAME}.
     */
    public String getDisplayNameForNode(String nodeId);

    /**
     * Return the nodeID that should be used to describe a dependency on a
     * particular WBS node.
     */
    public String getNodeID(WBSNode node);

    /**
     * Return a tree of tasks which can be listed as dependencies.
     * 
     * The resulting object may not be up-to-date until the
     * {@link #updateTaskTree()} method is called.
     */
    public WBSModel getTaskTree();

    /**
     * Make certain that the tree returned by {@link #getTaskTree()} is as
     * up-to-date as possible.
     */
    public void updateTaskTree();

}
