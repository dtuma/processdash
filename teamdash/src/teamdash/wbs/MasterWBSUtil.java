package teamdash.wbs;

import java.util.Iterator;
import java.util.List;

import teamdash.wbs.columns.TaskDependencyColumn;


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

        // copy the master node ID for the overall project
        copyAttr(working.getRoot(), dest.getRoot(), MASTER_NODE_ID, true);
        // copy everything else
        return dest.mergeWBSModel(working, MASTER_NODE_MERGER,
                MASTER_NODE_COMPARATOR);
    }



    private static class MasterNodeMerger implements WBSNodeMerger {

        public void mergeNodes(WBSNode src, WBSNode dest) {
            dest.setReadOnly(src != null);
            copyAttr(src, dest, MASTER_NODE_ID, true);
            copyAttr(src, dest, MASTER_PARENT_ID, true);
            if (src != null) {
                dest.setName(src.getName());
                copyAttr(src, dest, TaskDependencyColumn.ID_LIST_ATTR, true);
            }
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
                child.setAttribute(MASTER_PARENT_ID, parentID);
            }
            child.setReadOnly(true);
            child.setAttribute(MASTER_NODE_ID, id + ":" + child.getUniqueID());
        }

    }

    public static int[] mergeFromSubproject(WBSModel subproject,
            String subprojectID, String shortName, List teamMemberInitials,
            boolean useShortNamesInRollup, WBSModel dest) {
        WBSModel working = new WBSModel();
        working.copyFrom(subproject);
        SubprojectNodeTweaker worker = new SubprojectNodeTweaker(shortName,
                subprojectID, teamMemberInitials, useShortNamesInRollup);
        visitWBS(working, worker);
        return dest.mergeWBSModel(working, worker, MASTER_NODE_COMPARATOR);
    }

    private static class SubprojectNodeTweaker implements WBSNodeVisitor, WBSNodeMerger {
        String shortName;

        String projectID;

        List memberInitials;

        boolean useShortNamesInRollup;

        public SubprojectNodeTweaker(String shortName, String projectID,
                List memberInitials, boolean useShortNamesInRollup) {
            this.shortName = shortName;
            this.projectID = projectID;
            this.memberInitials = memberInitials;
            this.useShortNamesInRollup = useShortNamesInRollup;
        }

        public void visit(WBSNode parent, WBSNode node) {
            if (useShortNamesInRollup
                    && node.getAttribute(MASTER_NODE_ID) == null) {
                String newName = node.getName() + " (" + shortName + ")";
                node.setName(newName);
            }

            node.setReadOnly(false);

            String nodeID = projectID + ":" + node.getUniqueID();
            node.setAttribute(PROJECT_NODE_ID, nodeID);
            node.setAttribute("Time (Top Down)", null);

            double assignedTime = 0;
            for (Iterator i = memberInitials.iterator(); i.hasNext();) {
                String initials = (String) i.next();
                double memberTime = node.getNumericAttribute(initials
                        + "-Time (Top Down)");
                if (memberTime > 0) {
                    assignedTime += memberTime;
                    node.setAttribute(initials+ "-Time (Top Down)", null);
                }
            }
            if (assignedTime > 0)
                node.setNumericAttribute(shortName + "-Time (Top Down)",
                        assignedTime);
        }

        public void mergeNodes(WBSNode src, WBSNode dest) {
            if (src != null) {
                // merge all top-down numeric estimates (time, size, etc)
                Iterator i = src.listAttributeNames().iterator();
                while (i.hasNext()) {
                    String attrName = (String) i.next();
                    if (attrName.endsWith(" (Top Down)"))
                        sumAttr(src, dest, attrName);
                }

                // merge dependencies
                listMergeAttr(src, dest, TaskDependencyColumn.ID_LIST_ATTR);
            }
        }

    }

    public static String getNodeID(WBSNode node, String projectID) {
        String result = (String) node.getAttribute(MASTER_NODE_ID);
        if (result == null)
            result = (String) node.getAttribute(PROJECT_NODE_ID);
        if (result == null)
            result = projectID + ":" + node.getUniqueID();
        return result;
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

    private static void sumAttr(WBSNode src, WBSNode dest, String attrName) {
        if (dest != null && src != null) {
            double srcVal = src.getNumericAttribute(attrName);
            if (!Double.isNaN(srcVal)) {
                double destVal = dest.getNumericAttribute(attrName);
                destVal = (Double.isNaN(destVal) ? srcVal : srcVal + destVal);
                dest.setNumericAttribute(attrName, destVal);
            }
        }
    }

    private static void listMergeAttr(WBSNode src, WBSNode dest, String attr) {
        if (dest != null && src != null) {
            String srcDepend = (String) src.getAttribute(attr);
            if (srcDepend != null && srcDepend.length() > 0) {
                String destDepend = (String) dest.getAttribute(attr);
                if (destDepend == null || destDepend.length() == 0)
                    destDepend = srcDepend;
                else {
                    destDepend = "," + destDepend + ",";
                    String[] srcItems = srcDepend.split(",");
                    for (int i = 0; i < srcItems.length; i++) {
                        String item = srcItems[i];
                        if (destDepend.indexOf("," + item + ",") == -1)
                            destDepend = destDepend + item + ",";
                    }
                    destDepend = destDepend.substring(1, destDepend.length()-1);
                }
                dest.setAttribute(attr, destDepend);
            }
        }
    }

    private static boolean attrEquals(WBSNode a, WBSNode b, String attrName) {
        return isEqual(a.getAttribute(attrName), b.getAttribute(attrName));
    }

    private static boolean isEqual(Object a, Object b) {
        return (a != null && a.equals(b));
    }
}
