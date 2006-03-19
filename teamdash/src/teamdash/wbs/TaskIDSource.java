package teamdash.wbs;

/**
 * Interface for providing task identification information
 */
public interface TaskIDSource {

    /**
     * Return the nodeID that should be used to describe a particular WBS node.
     */
    public String getNodeID(WBSNode node);

}
