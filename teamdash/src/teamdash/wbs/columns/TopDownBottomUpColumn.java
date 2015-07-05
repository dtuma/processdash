// Copyright (C) 2002-2015 Tuma Solutions, LLC
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


package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.List;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

/** A numeric column containing data which should sum up hierarchically.
 */
public class TopDownBottomUpColumn extends AbstractNumericColumn
    implements CalculatedDataColumn
{


    protected DataTableModel dataModel;
    protected WBSModel wbsModel;
    protected String topDownAttrName, bottomUpAttrName, inheritedAttrName;
    protected Pruner pruner = null;
    protected boolean hideInheritedValues = false;

    public TopDownBottomUpColumn(DataTableModel dataModel,
                                     String name, String id) {
        this(dataModel, name, id, null);
    }
    public TopDownBottomUpColumn(DataTableModel dataModel,
                                 String name, String id, Pruner p) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = name;
        this.columnID = id;
        this.pruner = p;

        id = id.replace('_', '-');
        topDownAttrName   = getTopDownAttrName(id);
        bottomUpAttrName  = getBottomUpAttrName(id);
        inheritedAttrName = getInheritedAttrName(id);
    }
    protected void setPruner(Pruner p) {
        pruner = p;
        recalculate();
    }

    public void setHideInheritedValues(boolean b) {
        hideInheritedValues = b;
    }

    public boolean isCellEditable(WBSNode node) {
        if (node == null) return false;

        if (node.getAttribute(inheritedAttrName) != null) return false;

        return true;
    }


    public Object getValueAt(WBSNode node) {

        // if this node has an inherited value, return it.
        double inheritedValue = node.getNumericAttribute(inheritedAttrName);
        if (!Double.isNaN(inheritedValue))
            return new NumericDataValue
                (inheritedValue, false, hideInheritedValues, null);

        double topDownValue  = node.getNumericAttribute(topDownAttrName);
        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);

        // if there is no top-down value, return the bottom-up value, or zero.
        if (Double.isNaN(topDownValue))
            return new NumericDataValue
                (Double.isNaN(bottomUpValue) ? 0 : bottomUpValue);

        // if the top-down and bottom up values match, return them.
        if (equal(topDownValue, bottomUpValue))
            return new NumericDataValue(topDownValue);

        // return a "mismatch" object
        String errMsg = "top-down/bottom-up mismatch (bottom-up = " +
            NumericDataValue.format(bottomUpValue) + ")";
        return new NumericDataValue
            (topDownValue, true, false, errMsg, bottomUpValue);
    }



    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        System.out.println("setValueAt("+aValue+")");
        if (node == null) return;

        if ("".equals(aValue)) {
            userChangingValue(node, Double.NaN);
            node.setAttribute(topDownAttrName, null);

        } else if (isNoOpEdit(aValue, node)) {
            // if this was an editing session and no change was made, return.
            return;

        } else {

            // parse the value we were given to obtain a double.
            double newValue = NumericDataValue.parse(aValue);
            if (Double.isNaN(newValue)) return;

            // look up the old value of this node.
            double oldValue = NumericDataValue.parse(getValueAt(node));
            if (newValue == oldValue)
                return;

            // if this node has children, try multiplying all their values
            // by an appropriate ratio to keep the top-down and bottom-up
            // values in sync.
            maybeMultiplyValues(node, newValue, oldValue);

            // save the new top-down value for this node.
            userChangingValue(node, newValue);
            node.setNumericAttribute(topDownAttrName, newValue);
        }

        // ask the data model to recalculate this column.
        dataModel.columnChanged(this);
    }

    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    protected double recalc(WBSNode node) {
        double topDownValue = node.getNumericAttribute(topDownAttrName);

        WBSNode[] children = wbsModel.getReorderableChildren(node);
        int numToInclude = filterChildren(children);

        double result;

        if (numToInclude == 0) {
            // this is a leaf. The bottom up value equals the top-down value.
            if (Double.isNaN(topDownValue)) topDownValue = 0;
            else if (topDownValue == 0) node.removeAttribute(topDownAttrName);
            node.setNumericAttribute(bottomUpAttrName, topDownValue);
            node.setAttribute(inheritedAttrName, null);
            result = topDownValue;

        } else {
            // this node has children.  Recursively calculate the
            // bottom-up value from those of the children.
            double bottomUpValue = sumUpChildValues(children, numToInclude);

            // if we have a mismatch, make an attempt to fix it.
            if (!Double.isNaN(topDownValue)
                    && !equal(topDownValue, bottomUpValue)) {
                boolean fixWasMade = attemptToRepairTopDownBottomUpMismatch(
                    node, topDownValue, bottomUpValue, children, numToInclude);
                if (fixWasMade)
                    bottomUpValue = sumUpChildValues(children, numToInclude);
            }

            // save the bottom-up attribute value we calculated.
            node.setNumericAttribute(bottomUpAttrName, bottomUpValue);
            node.setAttribute(inheritedAttrName, null);

            // if the top-down and bottom-up match, silently erase the
            // superfluous top-down estimate.
            if (equal(topDownValue, bottomUpValue))
                node.setAttribute(topDownAttrName, null);

            if (bottomUpValue == 0)
                result = (Double.isNaN(topDownValue) ? 0 : topDownValue);
            else
                result = bottomUpValue;
        }

        // set the inherited value for pruned children.
        double inheritedVal = (Double.isNaN(result) ? 0 : result);
        for (int i = numToInclude;   i < children.length;   i++)
            setInheritedValue(children[i], inheritedVal);

        return result;
    }

    protected double sumUpChildValues(WBSNode[] children, int numToInclude) {
        double bottomUpValue = 0;
        for (int i = 0;   i < numToInclude;   i++) {
            double childValue = recalc(children[i]);
            bottomUpValue += childValue;
        }
        return bottomUpValue;
    }

    /**
     * If possible, make a column-dependent attempt to repair a
     * top-down-bottom-up mismatch.
     * 
     * @param node the node that has the mismatch
     * @param topDownValue the top down value for the mismatched node
     * @param bottomUpValue the bottom-up value that was calculated before
     *       this method was called
     * @param children a list of children underneath the node
     * @param numToInclude the number of initial items in the children array
     *       that represent non-pruned children
     *
     * @return true if any changes were made to children during this repair
     *    attempt
     */
    protected boolean attemptToRepairTopDownBottomUpMismatch(
            WBSNode node, double topDownValue, double bottomUpValue,
            WBSNode[] children, int numToInclude) {
        return false;
    }

    protected void setInheritedValue(WBSNode node, double value) {
        node.setAttribute(bottomUpAttrName, null);
        node.setNumericAttribute(inheritedAttrName, value);

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0;   i < children.length;   i++)
            setInheritedValue(children[i], value);
    }

    protected int filterChildren(WBSNode[] children) {
        int len = children.length;
        int left = 0;
        int right = len - 1;

        while (true) {
            // find the leftmost child that needs to be pruned.
            while (left < len && !shouldPrune(children[left]))
                left++;

            if (left >= right) break;

            // find the rightmost child that doesn't need pruning.
            while (right > left && shouldPrune(children[right]))
                right--;

            if (left < right) {
                WBSNode temp = children[left];
                children[left] = children[right];
                children[right] = temp;
                left++; right--;
            }
        }

        return left;
    }

    /** Returns true if the given node should not participate in the
     * bottom-up calculation.
     *
     * Nodes that are pruned from the calculation (and all the
     * children of pruned nodes) will have neither their "top down"
     * nor their "bottom up" attribute set.  Instead, they will have
     * their "inherited" attribute set to the value of their nearest
     * ancestor.
     */
    protected boolean shouldPrune(WBSNode node) {
        if (pruner == null)
            return false;
        else
            return pruner.shouldPrune(node);
    }

    protected boolean topDownBottomUpMismatch(WBSNode node) {
        // if this node is the descendant of a pruned node, it doesn't
        // have any error.
        if (node.getAttribute(inheritedAttrName) != null) return false;

        // if this node has no top-down value, there is no mismatch.
        double topDownValue = node.getNumericAttribute(topDownAttrName);
        if (Double.isNaN(topDownValue)) return false;

        // if this node's bottom up value matches the top down value, there
        // is no mismatch.
        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);
        if (equal(topDownValue, bottomUpValue)) return false;

        // we have a mismatch
        return true;
    }

    protected void maybeMultiplyValues(WBSNode node, double newValue, double oldValue) {
        if (topDownBottomUpMismatch(node)) return;

        double ratio = newValue / oldValue;
        if (!Double.isNaN(ratio) &&
            !Double.isInfinite(ratio) &&
            ratio != 0) {
            multiplyValue(node, ratio);
        } else {
            WBSNode delegate = getSingleLeafForNode(node, oldValue != 0);
            if (delegate != null) {
                // We have found a single leaf where the change should be made.
                // Go ahead and set its bottom up value. (This will eventually
                // be set by the recalculation logic, but this line allows the
                // getValueAt to return as a non-error value in the meantime.)
                delegate.setNumericAttribute(bottomUpAttrName, newValue);
                if (delegate != node) {
                    // if the delegate is different from the target node, make
                    // the change on the delegate. (If they are the same, these
                    // lines are unnecessary because they will be performed by
                    // the setValueAt logic after this method returns.)
                    userChangingValue(delegate, newValue);
                    delegate.setNumericAttribute(topDownAttrName, newValue);
                }
            }
        }
    }

    protected WBSNode getSingleLeafForNode(WBSNode node, boolean withValue) {
        ArrayList leaves = new ArrayList();
        getLeavesForNode(node, withValue, leaves);
        if (leaves.size() == 1)
            return (WBSNode) leaves.get(0);
        else
            return selectSingleLeafForNode(leaves);
    }

    protected WBSNode selectSingleLeafForNode(List<WBSNode> multipleLeaves) {
        return null;
    }

    protected void getLeavesForNode(WBSNode node, boolean withValue, ArrayList result) {
        WBSNode[] children = wbsModel.getReorderableChildren(node);
        int numToInclude = filterChildren(children);

        if (numToInclude == 0) {  // this is a leaf.
            double val = node.getNumericAttribute(topDownAttrName);
            if (withValue == false || val > 0)
                result.add(node);

        } else for (int i = 0;   i < numToInclude;   i++)
            getLeavesForNode(children[i], withValue, result);
    }

    /** Recurse over all the children of <code>node</code>, and multiply
     * their top-down values by ratio.
     *
     * (Note: this does not modify the top-down estimate of the node
     * itself).
     */
    protected void multiplyValue(WBSNode node, double ratio) {
        WBSNode[] children = wbsModel.getChildren(node);
        WBSNode child;
        double topDownValue;
        for (int i = children.length;   i-- > 0; ) {
            child = children[i];
            topDownValue = child.getNumericAttribute(topDownAttrName);
            if (!Double.isNaN(topDownValue)) {
                topDownValue *= ratio;
                userChangingValue(child, topDownValue);
                child.setNumericAttribute(topDownAttrName, topDownValue);
            }

            multiplyValue(child, ratio);
        }
    }

    /** When the user edits and changes a value, this function is called
     * for each affected node before the change is made. */
    protected void userChangingValue(WBSNode node, double value) {}


    @Override
    public Object getConflictDisplayValue(String value, WBSNode node) {
        if (value == null || value.length() == 0)
            return 0;
        else
            return Double.valueOf(value);
    }


    public void storeDependentColumn(String ID, int columnNumber) { }

    protected static String getTopDownAttrName(String id) {
        return (id.replace('_', '-') + " (Top Down)").intern();
    }
    protected static String getBottomUpAttrName(String id) {
        return ('_' + id.replace('_', '-') + " (Bottom_Up)").intern();
    }
    protected static String getInheritedAttrName(String id) {
        return ('_' + id.replace('_', '-') + " (Inherited_)").intern();
    }

    public static final String TOP_DOWN_ATTR_SUFFIX = getTopDownAttrName("");

}
