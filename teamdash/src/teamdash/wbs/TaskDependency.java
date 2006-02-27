package teamdash.wbs;

/**
 * Holds information about a task which is a dependency of another task.
 */
public class TaskDependency {

    /** The ID of the task being depended upon */
    public String nodeID;

    /** The full name of the task being depended upon */
    public String displayName;

    /**
     * True if, to the best of our knowledge, the task being depended upon does
     * not exist
     */
    public boolean hasError;

    public TaskDependency(String nodeID, String displayName) {
        this.nodeID = nodeID;
        this.displayName = displayName;
        this.hasError = false;
    }

    /**
     * Update this dependency with information from the given source.
     * 
     * @return true if the display name of this node was changed.
     */
    public boolean update(TaskDependencySource source) {
        String newDislayName = source.getDisplayNameForNode(nodeID);
        if (newDislayName == TaskDependencySource.UNKNOWN_NODE_DISPLAY_NAME) {
            hasError = true;
            return false;

        } else {
            hasError = false;
            if (newDislayName.equals(displayName))
                return false;
            else {
                displayName = newDislayName;
                return true;
            }
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof TaskDependency) {
            TaskDependency that = (TaskDependency) obj;
            return this.nodeID.equals(that.nodeID);
        }
        return false;
    }

    public int hashCode() {
        return nodeID.hashCode();
    }

    public String toString() {
        return displayName;
    }


}
