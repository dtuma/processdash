// Copyright (C) 2019-2020 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package teamdash.wbs.columns;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomNamedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.SizeMetric;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer;

public class SizeDataColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn, CustomNamedColumn {

    /**
     * When a PSP/PROBE task is assigned to more than one person, the
     * {@link PlanTimeWatcher} will set this attribute on the node (to "LOC" for
     * a PSP task, or the task size units for the PROBE task). If a node is not
     * a PSP/PROBE task, or if it is assigned to less than two people, the node
     * will not have a value for this attribute.
     */
    public static final String PROBE_MULTI_FLAG_ATTR = "_ PROBE Multi Flag";

    /**
     * When the planning step in a PSP/PROBE task is marked complete, the
     * {@link WBSSynchronizer} will set this attribute on the node to indicate
     * that planned size should be read-only.
     */
    public static final String PROBE_PLAN_LOCKED_FLAG_ATTR = "@ Plan Size Locked";

    /**
     * When a PSP task is marked complete, the {@link WBSSynchronizer} will set
     * this attribute on the node to indicate that actual LOC should be
     * read-only.
     */
    public static final String PROBE_ACTUAL_LOCKED_FLAG_ATTR = "@ Actual Size Locked";



    public static class Value extends NumericDataValue {

        /** The size value that is entered directly on a given node */
        public double nodeValue;

        /** The sum of sizes for this node and its children */
        public double bottomUp;

        /** A size value inherited from the parent component */
        public double inherited;

        /** The time when the last reverse sync occurred for this value */
        public String revSyncTime;

        /** An optional tooltip explaining why this value is read only */
        public String readOnlyReason;

        public Value(double nodeValue, double bottomUp, double inherited,
                String revSyncTime, String readOnlyReason) {
            super(inherited > 0 ? inherited : bottomUp);
            this.nodeValue = nodeValue;
            this.bottomUp = bottomUp;
            this.inherited = inherited;
            this.revSyncTime = revSyncTime;
            this.readOnlyReason = readOnlyReason;
            this.isEditable = (readOnlyReason == null);
            if (inherited > 0) {
                this.isInvisible = true;
                this.isEditable = false;
            }
        }

        @Override
        public String toString() {
            // this override to the toString method causes:
            // - editing sessions to start with the nodeValue
            // - edits to be no-ops if they re-save the nodeValue
            // - copy/paste operations to transfer the nodeValue
            return format(nodeValue);
        }

    }



    protected WBSModel wbsModel;

    private TeamProcess teamProcess;

    private String metricID;

    private boolean plan;

    private Map<String, String> lowerCaseCache;

    protected String nodeValueAttrName, bottomUpAttrName, inheritedAttrName,
            syncTimestampAttrName, restoreCandidateAttrName, pdashSumAttrName;


    public SizeDataColumn(DataTableModel dataModel, TeamProcess teamProcess,
            String metricID, boolean plan) {
        this.wbsModel = dataModel.getWBSModel();
        this.teamProcess = teamProcess;
        this.metricID = metricID;
        this.plan = plan;

        this.lowerCaseCache = new HashMap<String, String>();
        this.columnID = getColumnID(metricID, plan);
        this.affectedColumns = new String[] {
                SizeColumnGroup.getColumnID(plan) };

        String attr = getAttrBaseName(metricID, plan);
        nodeValueAttrName = TopDownBottomUpColumn.getTopDownAttrName(attr);
        bottomUpAttrName = TopDownBottomUpColumn.getBottomUpAttrName(attr);
        inheritedAttrName = TopDownBottomUpColumn.getInheritedAttrName(attr);
        syncTimestampAttrName = attr + REV_SYNC_SUFFIX;
        restoreCandidateAttrName = bottomUpAttrName + " Restore Candidate";
        pdashSumAttrName = SizeActualDataColumn.getNodeAttrName(metricID, plan);
        setConflictAttributeName(nodeValueAttrName);
    }

    @Override
    public String getColumnName() {
        return resources.format(
            plan ? "Planned_Size.Name_FMT" : "Actual_Size.Name_FMT",
            getMetricDisplayName());
    }

    public String getCustomColumnName() {
        return getColumnName();
    }

    public String getMetricDisplayName() {
        return String.valueOf(teamProcess.getSizeMetricMap().get(metricID));
    }

    public Object getValueAt(WBSNode node) {
        double nodeValue = getNodeValue(node);
        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);
        double inheritedValue = node.getNumericAttribute(inheritedAttrName);
        String syncTime = (String) node.getAttribute(syncTimestampAttrName);
        String readOnlyReason = getReadOnlyReason(node);
        return new Value(nodeValue, bottomUpValue, inheritedValue, syncTime,
                readOnlyReason);
    }

    private double getNodeValue(WBSNode node) {
        return node.getNumericAttribute(
            isProbeMulti(node) ? pdashSumAttrName : nodeValueAttrName);
    }

    /**
     * @return a non-null string if this node is read-only; a non-empty string
     *         if we should display a tooltip explaining why
     */
    private String getReadOnlyReason(WBSNode node) {
        if (isProbeMulti(node)) {
            return resources.format(
                plan ? "Size_Data.Multi_PROBE_Plan_Tooltip_FMT"
                     : "Size_Data.Multi_PROBE_Actual_Tooltip_FMT",
                lowerCase(node.getType()), lowerCase(getMetricDisplayName()));

        } else if (isCompletionLocked(node)) {
            return resources.format(
                plan ? "Size_Data.Completion_Locked_Plan_FMT"
                     : "Size_Data.Completion_Locked_Actual_FMT",
                lowerCase(node.getType()), lowerCase(getMetricDisplayName()));

        } else if (node.getIndentLevel() == 0) {
            return "";
        } else {
            return null;
        }
    }

    public boolean isCellEditable(WBSNode node) {
        // the tests in this method must match the tests performed by the
        // getReadOnlyReason method above. When this method returns false,
        // that method must return non-null.
        return node != null //
                && node.getIndentLevel() > 0 //
                && !isCompletionLocked(node) //
                && !isProbeMulti(node);
    }

    /** @return true if this cell is read only because tasks were completed */
    private boolean isCompletionLocked(WBSNode node) {
        String attr = plan ? PROBE_PLAN_LOCKED_FLAG_ATTR
                : PROBE_ACTUAL_LOCKED_FLAG_ATTR;
        if (node.getAttribute(attr) == null)
            return false;

        String units = TaskSizeUnitsColumn.getSizeUnitsForTask(node, teamProcess);
        return metricID.equals(units);
    }

    /** @return true if node is a PSP/PROBE task assigned to multiple people */
    private boolean isProbeMulti(WBSNode node) {
        return metricID.equals(node.getAttribute(PROBE_MULTI_FLAG_ATTR));
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue == PROBE_MULTI_FLAG_ATTR) {
            handleProbeMultiStatusChange(node);
            return;
        }

        System.out.println("setValueAt(" + aValue + ")");
        if (!isCellEditable(node))
            return;

        if ("".equals(aValue)) {
            clearNodeValue(node, topDownEditMode);

        } else if (isNoOpEdit(aValue, node)) {
            // if this was an editing session and no change was made, return.
            return;

        } else {
            // parse the value we were given to obtain a double.
            double newValue = NumericDataValue.parse(aValue);
            if (Double.isNaN(newValue))
                return;

            if (maybeMultipleNodeValues(node, newValue) == false)
                node.setNumericAttribute(nodeValueAttrName, newValue);
        }
    }

    private void handleProbeMultiStatusChange(WBSNode node) {
        if (isProbeMulti(node)) {
            // this node just changed from single-assignee status to multiple
            // assignee status. We'll have to stop displaying our direct value,
            // and display the sum of PDASH values instead. If those numbers
            // differ, and this is a plan column, we don't want the change
            // to alter rate-driven time estimates; so we must clear explicit
            // rates from affected tasks.
            double explicitVal = node.getNumericAttribute(nodeValueAttrName);
            double pdashSum = node.getNumericAttribute(pdashSumAttrName);
            if (plan && !equal(explicitVal, pdashSum, 0.0001)) {
                clearTaskRatesForNodesAffectedByPlanSizeChange(wbsModel,
                    teamProcess, metricID, Collections.singleton(node));
            }

        } else {
            // this node just changed from multiple assignees to a single
            // assignee. For continuity, we'll consider preserving the number
            // that is currently in the cell
            double pdashSum = node.getNumericAttribute(pdashSumAttrName);
            if (pdashSum > 0)
                node.setNumericAttribute(nodeValueAttrName, pdashSum);
        }
    }

    private void clearNodeValue(WBSNode node, boolean recursive) {
        node.setAttribute(nodeValueAttrName, null);

        if (recursive) {
            for (WBSNode child : wbsModel.getChildren(node))
                clearNodeValue(child, true);
        }
    }

    @Override
    protected boolean isNoOpEdit(Object newValue, WBSNode node) {
        if (newValue instanceof String && topDownEditMode) {
            // in top-down editing mode, we need to compare the value to
            // our bottom-up sum rather than the node value
            double oldVal = NumericDataValue.parse(getValueAt(node));
            String oldStr = NumericDataValue.format(oldVal);
            return newValue.equals(oldStr);
        } else {
            return super.isNoOpEdit(newValue, node);
        }
    }

    private boolean maybeMultipleNodeValues(WBSNode node, double newValue) {
        // if we aren't in top-down editing mode, don't multiply values
        if (topDownEditMode == false)
            return false;

        // get the old bottom up value. If zero, there's nothing to multiply
        Value v = (Value) getValueAt(node);
        if (v.bottomUp == 0 || Double.isNaN(v.bottomUp))
            return false;

        if (newValue == 0) {
            // if we've been asked to change the sum to zero, clear all values
            clearNodeValue(node, true);
        } else {
            // otherwise, calculate the ratio and multiply all node values
            double ratio = newValue / v.bottomUp;
            multiplyNodeValues(node, ratio);
        }
        return true;
    }

    private void multiplyNodeValues(WBSNode node, double ratio) {
        // multiply the value on this node, if one is present
        double nodeValue = node.getNumericAttribute(nodeValueAttrName);
        if (nodeValue > 0) {
            nodeValue *= ratio;
            node.setNumericAttribute(nodeValueAttrName, nodeValue);
        }

        // recurse over children
        for (WBSNode child : wbsModel.getChildren(node))
            multiplyNodeValues(child, ratio);
    }



    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    protected double recalc(WBSNode node) {
        double result;
        double nodeValue = getNodeValue(node);
        WBSNode[] children = wbsModel.getChildren(node);

        if (children.length == 0) {
            // this is a leaf. The bottom up value equals the node value.
            if (Double.isNaN(nodeValue))
                nodeValue = maybeRestoreNodeValueForLeaf(node);
            else if (nodeValue == 0)
                node.removeAttribute(nodeValueAttrName);
            node.setNumericAttribute(bottomUpAttrName, nodeValue);
            node.setAttribute(inheritedAttrName, null);
            result = nodeValue;

        } else {
            // this node has children. Recursively calculate the
            // bottom-up value from those of the children.
            double bottomUpValue = sumUpChildValues(node, children);

            // possibly move our parent node estimate to a PSP/PROBE child
            if (nodeValue > 0 && bottomUpValue == 0
                    && reallocateNodeValue(node, nodeValue, children)) {
                bottomUpValue = nodeValue;
                nodeValue = 0;
            }

            // include any value entered on this node.
            if (nodeValue > 0)
                bottomUpValue += nodeValue;
            result = bottomUpValue;

            // save the bottom-up attribute value we calculated.
            node.setNumericAttribute(bottomUpAttrName, bottomUpValue);
            node.setAttribute(inheritedAttrName, null);

            // set the inherited value for child tasks that don't have one.
            if (result > 0) {
                for (WBSNode child : children) {
                    double cbu = child.getNumericAttribute(bottomUpAttrName);
                    if (cbu == 0 && isTask(child))
                        setInheritedValue(child, result);
                }
            }
        }

        return result;
    }

    protected double maybeRestoreNodeValueForLeaf(WBSNode node) {
        // the goal of this method is to improve the user experience when
        // all of the tasks are deleted from underneath a component. In that
        // case, we'd like the component to remember the size it had
        // immediately before the deletion occurred.

        // first, check to see if this task is a candidate for restoration.
        Object oldChildTaskID = node.removeAttribute(restoreCandidateAttrName);
        if (oldChildTaskID == null)
            return 0;

        // next, check to see if that child was truly deleted. (If it's still
        // present in the WBS somewhere, we don't need to rescue its size.)
        if (wbsModel.getNodeMap().containsKey(oldChildTaskID))
            return 0;

        // If so, restore the previous bottom up value, if one was present.
        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);
        if (bottomUpValue > 0) {
            node.setNumericAttribute(nodeValueAttrName, bottomUpValue);
            return bottomUpValue;
        }

        // no bottom up size was present to restore.
        return 0;
    }

    protected double sumUpChildValues(WBSNode parent, WBSNode[] children) {
        double bottomUpValue = 0;
        int taskChildNodeID = -1;

        for (int i = 0; i < children.length; i++) {
            WBSNode child = children[i];

            double childValue = recalc(child);
            if (child.isHidden() == false)
                bottomUpValue += childValue;

            if (isTask(child) && childValue > 0)
                taskChildNodeID = child.getUniqueID();
        }

        // This node is a candidate for bottom-up size restoration if it
        // has a nonzero bottom-up size, and if it has children that are
        // tasks (rather than components). Record this fact, either way.
        if (bottomUpValue > 0 && taskChildNodeID != -1)
            parent.setAttribute(restoreCandidateAttrName, taskChildNodeID);
        else
            parent.removeAttribute(restoreCandidateAttrName);

        return bottomUpValue;
    }

    private boolean isTask(WBSNode node) {
        String type = node.getType();
        return type != null && type.endsWith(" Task");
    }

    protected boolean reallocateNodeValue(WBSNode node, double nodeValue,
            WBSNode[] children) {
        // the goal of this method is to improve the user experience for
        // PSP and PROBE tasks. Users can record size directly on most WBS
        // components, so they may get used to this editing paradigm. But when
        // a component contains a PROBE or PSP task, the sync logic needs the
        // size to be recorded there. This method looks for that pattern and
        // moves the parent size onto the PROBE/PSP child.
        WBSNode delegate = getSizeDelegateChild(children);
        if (delegate == null)
            return false;

        node.setAttribute(nodeValueAttrName, null);
        node.setAttribute(restoreCandidateAttrName, "t");
        delegate.setNumericAttribute(nodeValueAttrName, nodeValue);
        delegate.setNumericAttribute(bottomUpAttrName, nodeValue);
        delegate.setAttribute(inheritedAttrName, null);
        return true;
    }

    private WBSNode getSizeDelegateChild(WBSNode[] children) {
        for (WBSNode child : children) {
            // don't look at read-only or hidden children
            if (!isCellEditable(child) || child.isHidden())
                continue;

            String type = child.getType();

            // if this is a PROBE task, and it uses the same size metric as this
            // column, return it.
            if (TeamProcess.isProbeTask(type)) {
                String probeTaskSizeMetric = TaskSizeUnitsColumn
                        .getSizeUnitsForProbeTask(child);
                if (metricID.equals(probeTaskSizeMetric))
                    return child;
            }

            // if this is a PSP task, and this column is showing LOC, return it
            if (TeamProcess.isPSPTask(type) && metricID.equals("LOC")) {
                return child;
            }
        }

        // we didn't find a size delegate child directly under this node.
        return null;
    }

    protected void setInheritedValue(WBSNode node, double value) {
        node.setAttribute(bottomUpAttrName, null);
        node.setNumericAttribute(inheritedAttrName, value);

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0; i < children.length; i++)
            setInheritedValue(children[i], value);
    }

    public Object getConflictDisplayValue(String value, WBSNode node) {
        if (value == null || value.length() == 0)
            return 0;
        else
            return Double.valueOf(value);
    }

    public void storeDependentColumn(String ID, int columnNumber) {}


    public TableCellRenderer getCellRenderer() {
        return new SizeValueRenderer();
    }



    private class SizeValueRenderer extends DefaultTableCellRenderer {

        SizeValueRenderer() {
            setHorizontalAlignment(RIGHT);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            String display = "", tooltip = null;

            if (!(value instanceof Value))
                return super.getTableCellRendererComponent(table, display,
                    isSelected, hasFocus, row, column);

            Value v = (Value) value;
            if (v.bottomUp > 0) {
                display = NumericDataValue.format(v.bottomUp);

                if (v.nodeValue > 0
                        && !equal(v.nodeValue, v.bottomUp, 0.0001)) {
                    WBSNode node = wbsModel.getNodeForRow(row);
                    String nodeType = node == null ? "element"
                            : lowerCase(wbsModel.filterNodeType(node));
                    String nodeValue = NumericDataValue.format(v.nodeValue);

                    tooltip = resources.format(
                        "Size_Data.Node_Value_Tooltip_FMT", nodeValue, display,
                        lowerCase(getMetricDisplayName()), nodeType);
                    display += " (" + nodeValue + ")";
                }
            }

            if (StringUtils.hasValue(v.readOnlyReason))
                tooltip = v.readOnlyReason;
            setToolTipText(tooltip);

            setForeground(v.isEditable ? Color.black : Color.gray);

            return super.getTableCellRendererComponent(table, display,
                isSelected, hasFocus, row, column);
        }
    }

    private String lowerCase(String s) {
        if (s == null)
            return null;

        String result = lowerCaseCache.get(s);
        if (result != null)
            return result;

        String[] words = s.split(" ");
        for (int i = words.length; i-- > 0;) {
            String w = words[i];
            if (w.equals(w.toUpperCase())) {
                // this word is all upper case; likely an acronym such as
                // LOC, PSP, or PROBE. Leave it alone
            } else {
                words[i] = w.toLowerCase();
            }
        }
        result = StringUtils.join(Arrays.asList(words), " ");
        lowerCaseCache.put(s, result);
        return result;
    }



    /**
     * Normally, this column allows people to edit values on individual nodes,
     * to include attaching a new value to a parent. But when this column is
     * altered by other logic, it may be desirable to use the traditional WBS
     * behavior, where editing a parent alters the children underneath. Other
     * logic can call this method to request that mode, then change it back when
     * they are done.
     */
    public static void setTopDownEditMode(boolean mode) {
        topDownEditMode = mode;
    }

    private static volatile boolean topDownEditMode = false;



    /**
     * Write a reverse synced value into a node, unless the value there is newer
     * than the reverse sync value.
     * 
     * @return true if a change was made
     */
    public static boolean maybeStoreReverseSyncValue(WBSNode node,
            String metricID, boolean plan, String newValue, Date timestamp) {
        // calculate the names of attributes we will use
        String attr = getAttrBaseName(metricID, plan);
        String nodeAttr = TopDownBottomUpColumn.getTopDownAttrName(attr);
        String timeAttr = attr + REV_SYNC_SUFFIX;

        // If the WBS has processed newer reverse sync values for this
        // size data value, ignore the instruction
        Object lastSyncTime = node.getAttribute(timeAttr);
        if (lastSyncTime != null) {
            try {
                Date last = XMLUtils.parseDate(lastSyncTime.toString());
                if (last.compareTo(timestamp) >= 0)
                    return false;
            } catch (Exception e) {}
        }

        // save the new value along with the timestamp
        node.setAttribute(nodeAttr, newValue);
        node.setAttribute(timeAttr, XMLUtils.saveDate(timestamp));
        return true;
    }
    private static final String REV_SYNC_SUFFIX = " (Last Rev Sync Time)";



    /**
     * Iterate over the WBS, and clear size data values entered on any of the
     * given nodes.
     * 
     * @param wbs
     *            the WBS to scan
     * @param process
     *            the team process used by this project
     * @param clearPlanSize
     *            true if planned size values should be cleared
     * @param clearActualSize
     *            true if actual size values should be cleared
     * @param nodesToClear
     *            the nodes whose size data should be cleared
     */
    public static void clearSizeDataNodeValues(WBSModel wbs,
            TeamProcess process, boolean clearPlanSize, boolean clearActualSize,
            Set<WBSNode> nodesToClear) {

        // iterate over each of the plan/actual size metrics in the team process
        for (boolean plan : new boolean[] { true, false }) {
            // if the user doesn't wants this type cleared, skip it
            if ((plan ? clearPlanSize : clearActualSize) == false)
                continue;

            for (String metricID : process.getSizeMetricMap().keySet()) {
                // get the name of the attribute for this metric
                String attr = getNodeValueAttrName(metricID, plan);

                // iterate over the nodes we were given, clearing this attribute
                Set<WBSNode> planChangedNodes = new HashSet<WBSNode>();
                for (WBSNode node : nodesToClear) {
                    // keep track of changes made to planned node size data
                    if (plan && node.getNumericAttribute(attr) > 0)
                        planChangedNodes.add(node);

                    // clear the node size data attribute for this metric
                    node.removeAttribute(attr);
                }

                // if we changed the planned size for any nodes, clear the
                // associated rates from all affected nodes (so their time
                // estimates won't get recalculated)
                if (plan && !planChangedNodes.isEmpty())
                    clearTaskRatesForNodesAffectedByPlanSizeChange(wbs, process,
                        metricID, planChangedNodes);
            }
        }
    }



    /**
     * Scan the WBS for any tasks that inherit their task size (directly or
     * indirectly) from the nodes in question. If found, clear any direct rates
     * for those tasks.
     * 
     * @return true if any rates were cleared
     */
    public static boolean clearTaskRatesForNodesAffectedByPlanSizeChange(
            WBSModel wbs, TeamProcess process, String metricID,
            Set<WBSNode> directlyChangedNodes) {

        // find all ancestors of the changed nodes; their sum was affected too
        Set<WBSNode> allAffectedNodes = new HashSet<WBSNode>();
        for (WBSNode node : directlyChangedNodes) {
            while (node != null) {
                allAffectedNodes.add(node);
                node = wbs.getParent(node);
            }
        }

        // scan the WBS, clearing task rates as needed
        String attr = getNodeValueAttrName(metricID, true);
        return clearRateDrivenTasks(wbs, wbs.getRoot(), process, metricID, attr,
            allAffectedNodes);
    }

    private static boolean clearRateDrivenTasks(WBSModel wbs, WBSNode node,
            TeamProcess process, String metricID, String attr,
            Set<WBSNode> changedNodes) {

        // if this node is not in the "changed" list, and it has a size
        // estimate of its own, that size estimate will shield it from
        // "inheriting" a size from any changed parent. We can prune our
        // depth-first search in that case.
        if (node.getAttribute(attr) != null && !changedNodes.contains(node))
            return false;

        // if this node is a leaf task that uses the same task units as
        // the changed estimate, clear its rate attribute.
        if (TeamTimeColumn.isLeafTask(wbs, node)) {
            String taskUnits = TaskSizeUnitsColumn.getSizeUnitsForTask(node,
                process);
            if (metricID.equals(taskUnits))
                return node.removeAttribute(TeamTimeColumn.RATE_ATTR) != null;
            else
                return false;

        } else {
            // recurse over children
            boolean result = false;
            for (WBSNode child : wbs.getChildren(node)) {
                result = clearRateDrivenTasks(wbs, child, process, metricID,
                    attr, changedNodes) || result;
            }
            return result;
        }
    }



    /**
     * After the IDs of metrics are changed by a merge operation, propagate
     * those ID changes into the attribute names used for holding size data
     * 
     * @param sizeMetricIDMappings
     *            a map whose keys are the old size metricIDs, and whose values
     *            are the new metricIDs
     */
    public static void remapSizeDataAttrs(WBSModel wbs,
            Map<String, String> sizeMetricIDMappings) {
        // if there are no remappings to perform, return
        if (sizeMetricIDMappings == null || sizeMetricIDMappings.isEmpty())
            return;

        // create a map of attribute names that need changing
        Map<String, String> attrRemappings = new HashMap();
        for (Entry<String, String> e : sizeMetricIDMappings.entrySet()) {
            // iterate over planned and actual
            for (boolean plan : BOOLEANS) {
                // remap size attribute names for this plan/actual metric
                String oldAttr = getNodeValueAttrName(e.getKey(), plan);
                String newAttr = getNodeValueAttrName(e.getValue(), plan);
                attrRemappings.put(oldAttr, newAttr);
            }
        }

        // rename the size attributes throughout the model
        wbs.renameAttributes(attrRemappings);
    }


    /**
     * After legacy project data has been loaded, rename the size data
     * attributes from the old legacy names to their new metricID-based names.
     */
    public static void renameLegacySizeDataAttrs(WBSModel wbs,
            Collection<SizeMetric> sizeMetrics) {
        // create a map of attribute names that need changing
        Map<String, String> attrRenames = new HashMap();
        for (SizeMetric sm : sizeMetrics) {
            // no need to rename size data attributes for the LOC metric
            if ("LOC".equals(sm.getMetricID()))
                continue;
            // rename both planned and actual size data attributes
            for (boolean plan : BOOLEANS) {
                // get attribute base names for this plan/actual metric
                String oldBase = getAttrBaseName(sm.getName(), plan);
                String newBase = getAttrBaseName(sm.getMetricID(), plan);
                // remap attributes for top-down size values
                attrRenames.put( //
                    TopDownBottomUpColumn.getTopDownAttrName(oldBase),
                    TopDownBottomUpColumn.getTopDownAttrName(newBase));
                // remap attributes for last sync timestamps
                attrRenames.put( //
                    oldBase + REV_SYNC_SUFFIX, //
                    newBase + REV_SYNC_SUFFIX);
            }
        }

        // rename the size attributes throughout the model
        wbs.renameAttributes(attrRenames);
    }

    private static final boolean BOOLEANS[] = new boolean[] { true, false };



    public static String getColumnID(String metricID, boolean plan) {
        // return column IDs that are backward-compatible with the names of
        // old-style size columns (from earlier versions of the dashboard). This
        // makes it easier for calculated columns to declare their dependencies
        if (plan)
            return SizeAccountingColumnSet.getNCID(metricID);
        else
            return SizeActualDataColumn.getColumnID(metricID, false);
    }

    public static String getNodeValueAttrName(String metricID, boolean plan) {
        String base = getAttrBaseName(metricID, plan);
        return TopDownBottomUpColumn.getTopDownAttrName(base);
    }

    private static String getAttrBaseName(String metricID, boolean plan) {
        return (plan ? "Added-" : "Actual-") + metricID.replace('_', '-');
    }

}
