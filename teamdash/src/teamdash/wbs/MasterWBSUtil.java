// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.util.PatternList;

import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.ProxySizeColumn;
import teamdash.wbs.columns.SizeDataColumn;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WorkflowSizeUnitsColumn;


public class MasterWBSUtil {

    public static final String MASTER_PARENT_ID = "masterParentID";

    public static final String MASTER_NODE_ID = "masterNodeID";

    public static final String PROJECT_NODE_ID = "projectNodeID";

    public static final String SOURCE_NODE_ID = "sourceNodeID";


    private static class NodeIDComparator implements WBSNodeComparator {

        public boolean nodesMatch(WBSNode a, WBSNode b) {
            return (a == b || a.getUniqueID() == b.getUniqueID()
                    || attrEquals(a, b, MASTER_NODE_ID) || attrEquals(a, b,
                    PROJECT_NODE_ID));
        }

    }

    public static final WBSNodeComparator NODE_ID_COMPARATOR = new NodeIDComparator();


    public static boolean isMasterNode(WBSNode node) {
        return node.getAttribute(MASTER_NODE_ID) != null;
    }

    public static void removeMasterNodeAttrs(WBSNode node) {
        if (isMasterNode(node)) {
            node.removeAttribute(MASTER_NODE_ID);
            node.removeAttribute(MASTER_PARENT_ID);
            node.setReadOnly(false);
        }
    }


    public static int[] mergeFromMaster(TeamProject proj) {
        ImportDirectory iDir = proj.getMasterProjectDirectory();
        if (iDir == null)
            return null;

        TeamProject masterProject = new TeamProject(iDir.getDirectory(), "");
        String masterProjectID = masterProject.getProjectID();
        if (masterProjectID == null)
            return null;

        // merge milestone information
        mergeFromMaster(masterProject.getMilestones(), masterProjectID,
                proj.getMilestones());

        // make the change
        int[] insertedRows = mergeFromMaster(masterProject.getWBS(),
            masterProjectID, proj.getWBS(), getExtraMasterAttrs(masterProject));
        return insertedRows;
    }

    private static List<String> getExtraMasterAttrs(TeamProject masterProject) {
        String setting = masterProject.getUserSetting("copyMasterWbsAttributes");
        if (setting == null || setting.trim().length() == 0)
            return null;

        List<String> result = new ArrayList<String>();
        for (String item : setting.split(",")) {
            String attr = item.trim();
            if (attr.length() > 0)
                result.add(attr);
        }
        return result;
    }

    public static int[] mergeFromMaster(WBSModel master,
            String masterProjectID, WBSModel dest) {
        return mergeFromMaster(master, masterProjectID, dest, null);
    }

    public static int[] mergeFromMaster(WBSModel master,
            String masterProjectID, WBSModel dest,
            List<String> extraMasterAttrs) {
        WBSModel working = new WBSModel();
        working.copyFrom(master);

        visitWBS(working, new MasterNodeTweaker(masterProjectID));

        // copy the master node ID for the overall project
        copyAttr(working.getRoot(), dest.getRoot(), MASTER_NODE_ID, true);
        // copy everything else
        return dest.mergeWBSModel(working,
                new MasterNodeMerger(extraMasterAttrs),
                MASTER_NODE_COMPARATOR, true);
    }



    private static class MasterNodeMerger implements WBSNodeMerger {

        private List<String> extraMasterAttrs;

        public MasterNodeMerger(List<String> extraMasterAttrs) {
            this.extraMasterAttrs = extraMasterAttrs;
        }

        public void mergeNodes(WBSNode src, WBSNode dest) {
            dest.setReadOnly(src != null);
            copyAttr(src, dest, MASTER_NODE_ID, true);
            copyAttr(src, dest, MASTER_PARENT_ID, true);
            copyAttr(src, dest, MilestoneCommitDateColumn.MASTER_VALUE_ATTR, true);
            if (src != null) {
                dest.setName(src.getName());
                copyAttr(src, dest, TaskDependencyColumn.ID_LIST_ATTR, true);
                if (extraMasterAttrs != null) {
                    for (String attrName : extraMasterAttrs)
                        copyAttr(src, dest, attrName, true);
                }
            }
        }

    }

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
            child.setAttribute(MilestoneCommitDateColumn.MASTER_VALUE_ATTR,
                child.getAttribute(MilestoneCommitDateColumn.VALUE_ATTR));
            child.removeAttributes(SIZE_DATA_ATTRS);
        }

    }

    public static int[] mergeFromSubproject(WBSModel subproject,
            String subprojectID, String shortName, String subprojectInitials,
            List teamMemberInitials, boolean useShortNamesInRollup,
            WBSModel dest) {
        WBSModel working = new WBSModel();
        working.copyFrom(subproject);
        SubprojectNodeTweaker worker = new SubprojectNodeTweaker(shortName,
                subprojectID, subprojectInitials, teamMemberInitials,
                useShortNamesInRollup, isManagedSizeMismatch(subproject, dest));
        visitWBS(working, worker);
        return dest.mergeWBSModel(working, worker, MASTER_NODE_COMPARATOR,
            false);
    }

    private static class SubprojectNodeTweaker implements WBSNodeVisitor, WBSNodeMerger {

        String shortName;

        String projectID;

        String subprojectTimeAttr;

        boolean useShortNamesInRollup;

        boolean clearSizeData;

        public SubprojectNodeTweaker(String shortName, String projectID,
                String projectInitials, List memberInitials,
                boolean useShortNamesInRollup, boolean clearSizeData) {
            this.shortName = shortName;
            this.projectID = projectID;
            this.subprojectTimeAttr = projectInitials + MEMBER_TIME_SUFFIX;
            this.useShortNamesInRollup = useShortNamesInRollup;
            this.clearSizeData = clearSizeData;
        }

        public void visit(WBSNode parent, WBSNode node) {
            if (useShortNamesInRollup
                    && node.getAttribute(MASTER_NODE_ID) == null) {
                String newName = node.getName() + " (" + shortName + ")";
                node.setName(newName);
            }

            node.setReadOnly(false);

            if (clearSizeData && SIZE_DATA_ATTRS != null)
                node.removeAttributes(SIZE_DATA_ATTRS);

            String nodeID = projectID + ":" + node.getUniqueID();
            node.setAttribute(PROJECT_NODE_ID, nodeID);
            Object teamTime = node.getAttribute("Time (Top Down)");
            node.removeAttributes(DISCARD_ATTR_PAT);
            node.setAttribute(subprojectTimeAttr, teamTime);
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

        private static final String MEMBER_TIME_SUFFIX = "-Time (Top Down)";
        private static final PatternList DISCARD_ATTR_PAT = new PatternList()
                .addLiteralEquals(TeamTimeColumn.NUM_PEOPLE_ATTR)
                .addLiteralEquals(TeamTimeColumn.TPP_ATTR)
                .addLiteralEndsWith(MEMBER_TIME_SUFFIX);

    }

    public static Set<String> mergeSizeMetricsFromSubproject(TeamProject src,
            TeamProject dest) {
        // create data structures for tracking
        Set<String> sizeMetricNames = new HashSet<String>();
        Set<String> sizeMetricSrcAttrs = new HashSet<String>();
        Map<String, String> idChanges = new HashMap();

        // iterate over the size metrics in the source subproject
        for (SizeMetric srcMetric : src.getSizeMetrics().getMetrics()) {

            // add the metric to the size metrics model in the dest project
            SizeMetric destMetric = dest.getSizeMetrics()
                    .getMetric(srcMetric.getName(), true);

            // if the dest project assigned this metric a different ID than the
            // source, record this discrepancy
            if (!srcMetric.getMetricID().equals(destMetric.getMetricID()))
                idChanges.put(srcMetric.getMetricID(),
                    destMetric.getMetricID());

            // keep track of all the size metric names/attrs we've seen
            sizeMetricNames.add(srcMetric.getName());
            sizeMetricSrcAttrs.add(SizeDataColumn
                    .getNodeValueAttrName(srcMetric.getMetricID(), true));
            sizeMetricSrcAttrs.add(SizeDataColumn
                    .getNodeValueAttrName(srcMetric.getMetricID(), false));
        }

        // discard any leftover, invisible size data from metrics that were
        // deleted from the source project in the past
        for (WBSNode node : src.getWBS().getWbsNodes())
            node.discardAttributes(SIZE_DATA_ATTRS, sizeMetricSrcAttrs);

        // if any metric IDs changed, propagate those changes as needed
        if (!idChanges.isEmpty()) {
            ProxySizeColumn.remapNodeIDs(src.getProxies(), idChanges);
            WorkflowSizeUnitsColumn.remapNodeIDs(src.getWorkflows(), idChanges);
            SizeDataColumn.remapSizeDataAttrs(src.getWBS(), idChanges);
        }

        // return the list of size metric names we saw
        return sizeMetricNames;
    }

    private static final PatternList SIZE_DATA_ATTRS = new PatternList()
            .addLiteralEquals("Actual-LOC (Top Down)")
            .addLiteralEquals("Added-LOC (Top Down)")
            .addLiteralStartsWith("Actual-Size-")
            .addLiteralStartsWith("Added-Size-");

    /**
     * @return true if WBS-managed size is in effect for one of the given WBSes,
     *         but not for the other
     */
    private static boolean isManagedSizeMismatch(WBSModel a, WBSModel b) {
        boolean aSizeFlag = isUsingNewSizeDataColumns(a);
        boolean bSizeFlag = isUsingNewSizeDataColumns(b);
        return aSizeFlag != bSizeFlag;
    }

    private static boolean isUsingNewSizeDataColumns(WBSModel wbs) {
        return wbs.getRoot().getAttribute("WBS Managed Size Data") != null;
    }

    public static String getNodeID(WBSNode node, String projectID) {
        String result = (String) node.getAttribute(MASTER_NODE_ID);
        if (result == null)
            result = (String) node.getAttribute(PROJECT_NODE_ID);
        if (result == null)
            result = projectID + ":" + node.getUniqueID();
        return result;
    }

    public static String getNodeIDs(WBSNode node, String projectID) {
        String masterNodeID = (String) node.getAttribute(MASTER_NODE_ID);

        String projectNodeID = (String) node.getAttribute(PROJECT_NODE_ID);
        if (projectNodeID == null)
            projectNodeID = projectID + ":" + node.getUniqueID();

        if (masterNodeID == null)
            return projectNodeID;
        else
            return projectNodeID + "," + masterNodeID;
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
