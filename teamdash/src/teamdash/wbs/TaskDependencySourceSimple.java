package teamdash.wbs;

import java.util.HashMap;
import java.util.Map;

/**
 * A source of dependency information for a standalone team project.
 * 
 * If the project is part of a master project, use
 * {@link teamdash.wbs.TaskDependencySourceMaster} instead.
 * 
 */
public class TaskDependencySourceSimple extends TaskDependencySourceAbstract {

    /** For efficiency, a map of nodeID -> WBSNode for nodes in the wbs */
    private Map nodeCache;

    public TaskDependencySourceSimple(TeamProject project) {
        super(project.getWBS(), project.getProjectID());
        this.nodeCache = new HashMap();
    }

    public String getDisplayNameForNode(String nodeId) {
        return getDisplayNameForNode(getTaskTree(), nodeCache, nodeId);
    }

    public void updateTaskTree() {
        // nothing to do.
    }
}
