package teamdash.wbs;

import java.util.HashMap;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * A source of dependency information for a standalone team project.
 * 
 * If the project is part of a master project, use
 * {@link teamdash.wbs.TaskDependencySourceMaster} instead.
 * 
 */
public class TaskDependencySourceSimple extends TaskDependencySourceAbstract
        implements TableModelListener {

    /** The WBS of the project that is using this dependency source. */
    private WBSModel liveWbs;

    /** True if we have received a change event from the liveWBS since we
     * last refreshed our dependency tree. */
    private boolean liveWbsHasChanged;

    /** For efficiency, a map of nodeID -> WBSNode for nodes in the wbs */
    private Map nodeCache;

    public TaskDependencySourceSimple(TeamProject project) {
        super(new WBSModel(), project.getProjectID());

        this.liveWbs = project.getWBS();
        this.liveWbs.addTableModelListener(this);
        this.liveWbsHasChanged = true;
        this.nodeCache = new HashMap();

        updateTaskTree();
    }

    public String getDisplayNameForNode(String nodeId) {
        return getDisplayNameForNode(liveWbs, nodeCache, nodeId);
    }

    public void updateTaskTree() {
        if (liveWbsHasChanged) {
            getTaskTree().copyFrom(liveWbs);
            liveWbsHasChanged = false;
        }
    }

    public void tableChanged(TableModelEvent e) {
        liveWbsHasChanged = true;
    }
}
