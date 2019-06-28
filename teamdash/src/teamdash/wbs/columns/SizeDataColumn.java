// Copyright (C) 2019 Tuma Solutions, LLC
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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomNamedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class SizeDataColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn, CustomNamedColumn {


    public class Value extends NumericDataValue {

        /** The size value that is entered directly on a given node */
        public double nodeValue;

        /** The sum of sizes for this node and its children */
        public double bottomUp;

        /** A size value inherited from the parent component */
        public double inherited;

        public Value(double nodeValue, double bottomUp, double inherited) {
            super(inherited > 0 ? inherited : bottomUp);
            this.nodeValue = nodeValue;
            this.bottomUp = bottomUp;
            this.inherited = inherited;
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

    private String metricName;

    protected String nodeValueAttrName, bottomUpAttrName, inheritedAttrName,
            restoreCandidateAttrName;


    public SizeDataColumn(DataTableModel dataModel, String metricName,
            boolean plan) {
        this.wbsModel = dataModel.getWBSModel();
        this.metricName = metricName;

        this.columnID = getColumnID(metricName, plan);
        this.columnName = resources.format(
            plan ? "Planned_Size.Name_FMT" : "Actual_Size.Name_FMT",
            metricName);

        String attr = (plan ? "Added-" : "Actual-")
                + metricName.replace('_', '-');
        nodeValueAttrName = TopDownBottomUpColumn.getTopDownAttrName(attr);
        bottomUpAttrName = TopDownBottomUpColumn.getBottomUpAttrName(attr);
        inheritedAttrName = TopDownBottomUpColumn.getInheritedAttrName(attr);
        restoreCandidateAttrName = bottomUpAttrName + " Restore Candidate";
        setConflictAttributeName(nodeValueAttrName);
    }

    public String getCustomColumnName() {
        return columnName;
    }

    public boolean isCellEditable(WBSNode node) {
        return node != null && node.getIndentLevel() > 0;
    }

    public Object getValueAt(WBSNode node) {
        double nodeValue = node.getNumericAttribute(nodeValueAttrName);
        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);
        double inheritedValue = node.getNumericAttribute(inheritedAttrName);
        return new Value(nodeValue, bottomUpValue, inheritedValue);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        System.out.println("setValueAt(" + aValue + ")");
        if (!isCellEditable(node))
            return;

        if ("".equals(aValue)) {
            node.setAttribute(nodeValueAttrName, null);

        } else if (isNoOpEdit(aValue, node)) {
            // if this was an editing session and no change was made, return.
            return;

        } else {
            // parse the value we were given to obtain a double.
            double newValue = NumericDataValue.parse(aValue);
            if (Double.isNaN(newValue))
                return;

            node.setNumericAttribute(nodeValueAttrName, newValue);
        }
    }

    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    protected double recalc(WBSNode node) {
        double result;
        double nodeValue = node.getNumericAttribute(nodeValueAttrName);
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
                if (metricName.equals(probeTaskSizeMetric))
                    return child;
            }

            // if this is a PSP task, and this column is showing LOC, return it
            if (TeamProcess.isPSPTask(type) && metricName.equals("LOC")) {
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

        private Map<String, String> lowerCaseCache;

        SizeValueRenderer() {
            setHorizontalAlignment(RIGHT);
            lowerCaseCache = new HashMap<String, String>();
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            String display = "", tooltip = null;

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
                        lowerCase(metricName), nodeType);
                    display += " (" + nodeValue + ")";
                }
            }

            setForeground(
                table.isCellEditable(row, column) ? Color.black : Color.gray);

            setToolTipText(tooltip);
            return super.getTableCellRendererComponent(table, display,
                isSelected, hasFocus, row, column);
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
    }



    public static String getColumnID(String metric, boolean plan) {
        // return column IDs that are backward-compatible with the names of
        // old-style size columns (from earlier versions of the dashboard). This
        // makes it easier for calculated columns to declare their dependencies
        if (plan)
            return SizeAccountingColumnSet.getNCID(metric);
        else
            return SizeActualDataColumn.getColumnID(metric, false);
    }

}
