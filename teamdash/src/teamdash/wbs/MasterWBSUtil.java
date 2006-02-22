package teamdash.wbs;

import java.util.Iterator;
import java.util.List;


public class MasterWBSUtil {

    public static final String MASTER_PARENT_ID = "masterParentID";

    public static final String MASTER_NODE_ID = "masterNodeID";

    public static final String PROJECT_NODE_ID = "projectNodeID";


    private static class NodeIDComparator implements WBSNodeComparator {

        public boolean nodesMatch(WBSNode a, WBSNode b) {
            return (a == b || a.getUniqueID() == b.getUniqueID()
                    || attrEquals(a, b, MASTER_NODE_ID) || attrEquals(a, b,
                    PROJECT_NODE_ID));
        }

    }

    public static final WBSNodeComparator NODE_ID_COMPARATOR = new NodeIDComparator();


    public static int[] mergeFromMaster(WBSModel master,
            String masterProjectID, WBSModel dest) {
        WBSModel working = new WBSModel();
        working.copyFrom(master);

        visitWBS(working, new MasterNodeTweaker(masterProjectID));

        return dest.mergeWBSModel(working, MASTER_NODE_MERGER,
                MASTER_NODE_COMPARATOR);
    }



    private static class MasterNodeMerger implements WBSNodeMerger {

        public void mergeNodes(WBSNode src, WBSNode dest) {
            dest.setReadOnly(src != null);
            copyAttr(src, dest, MASTER_NODE_ID, true);
            copyAttr(src, dest, MASTER_PARENT_ID, true);
            if (src != null)
                dest.setName(src.getName());
        }

    }

    private static final MasterNodeMerger MASTER_NODE_MERGER = new MasterNodeMerger();

    private static class MasterNodeComparator implements WBSNodeComparator {

        public boolean nodesMatch(WBSNode src, WBSNode dest) {
            if (attrEquals(src, dest, MASTER_NODE_ID))
                return true;

            if (isEqual(src.getName(), dest.getName())
                    && isEqual(src.getType(), dest.getType()))
                return true;

            return false;
        }

    }

    private static final MasterNodeComparator MASTER_NODE_COMPARATOR = new MasterNodeComparator();


    private static class MasterNodeTweaker implements WBSNodeVisitor {
        String id;

        public MasterNodeTweaker(String id) {
            this.id = id;
        }

        public void visit(WBSNode parent, WBSNode child) {
            if (parent != null) {
                Object parentID = parent.getAttribute(MASTER_NODE_ID);
                child.setReadOnly(true);
                child.setAttribute(MASTER_NODE_ID, id + ":"
                        + child.getUniqueID());
                child.setAttribute(MASTER_PARENT_ID, parentID);
            }
        }

    }

    public static int[] mergeFromSubproject(WBSModel subproject,
            String subprojectID, String shortName, List teamMemberInitials,
            WBSModel dest) {
        WBSModel working = new WBSModel();
        working.copyFrom(subproject);
        SubprojectNodeTweaker worker = new SubprojectNodeTweaker(shortName,
                subprojectID, teamMemberInitials);
        visitWBS(working, worker);
        return dest.mergeWBSModel(working, worker, MASTER_NODE_COMPARATOR);
    }

    private static class SubprojectNodeTweaker implements WBSNodeVisitor, WBSNodeMerger {
        String shortName;

        String projectID;

        List memberInitials;

        public SubprojectNodeTweaker(String shortName, String projectID,
                List memberInitials) {
            this.shortName = shortName;
            this.projectID = projectID;
            this.memberInitials = memberInitials;
        }

        public void visit(WBSNode parent, WBSNode node) {
            node.setReadOnly(false);

            String nodeID = projectID + ":" + node.getUniqueID();
            node.setAttribute(PROJECT_NODE_ID, nodeID);
            node.setAttribute("Time (Top Down)", null);

            double assignedTime = 0;
            for (Iterator i = memberInitials.iterator(); i.hasNext();) {
                String initials = (String) i.next();
                double memberTime = node.getNumericAttribute(initials
                        + "-Time (Top Down)");
                if (memberTime > 0)
                    assignedTime += memberTime;
            }
            if (assignedTime > 0)
                node.setNumericAttribute(shortName + "-Time (Top Down)",
                        assignedTime);
        }

        public void mergeNodes(WBSNode src, WBSNode dest) {
            copyAttr(src, dest, shortName + "-Time (Top Down)", true);
            // FIXME: currently, if two subproject define a node with
            // the same name, this is not merging a lot of the information.
            // for example, the final node will have a single projectNodeID.
        }

    }

    public static void visitWBS(WBSModel wbs, WBSNodeVisitor visitor) {
        visitor.visit(null, wbs.getRoot());
        visitWBS(wbs, wbs.getRoot(), visitor);
    }

    private static void visitWBS(WBSModel wbs, WBSNode parent,
            WBSNodeVisitor visitor) {
        WBSNode[] children = wbs.getChildren(parent);
        for (int i = 0; i < children.length; i++) {
            visitor.visit(parent, children[i]);
            visitWBS(wbs, children[i], visitor);
        }
    }

    private static void copyAttr(WBSNode src, WBSNode dest, String attrName, boolean copyNull) {
        if (dest != null) {
            Object attrVal = (src == null ? null : src.getAttribute(attrName));
            if (attrVal != null || copyNull)
                dest.setAttribute(attrName, attrVal);
        }
    }

    private static boolean attrEquals(WBSNode a, WBSNode b, String attrName) {
        return isEqual(a.getAttribute(attrName), b.getAttribute(attrName));
    }

    private static boolean isEqual(Object a, Object b) {
        return (a != null && a.equals(b));
    }
}
