
package teamdash.wbs.columns;

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



    public void setValueAt(Object aValue, WBSNode node) {
        System.out.println("setValueAt("+aValue+")");
        if (node == null) return;

        if ("".equals(aValue)) {
            userChangingValue(node, Double.NaN);
            node.setAttribute(topDownAttrName, null);
        } else {

            // parse the value we were given to obtain a double.
            double newValue = NumericDataValue.parse(aValue);
            if (Double.isNaN(newValue)) return;

            // look up the old value of this node.
            double oldValue = NumericDataValue.parse(getValueAt(node));
            if (equal(oldValue, newValue))
                return;             // if no change was made, return.

            // if this node has children, try multiplying all their values
            // by an appropriate ratio to keep the top-down and bottom-up
            // values in sync.
            maybeMultiplyValues(node,  newValue / oldValue);

            // save the new top-down value for this node.
            userChangingValue(node, newValue);
            node.setNumericAttribute(topDownAttrName, newValue);
        }

        // recalculate the tree.
        recalculate();
    }

    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    protected double recalc(WBSNode node) {
        double topDownValue = node.getNumericAttribute(topDownAttrName);

        WBSNode[] children = wbsModel.getChildren(node);
        int numToInclude = filterChildren(children);

        double result;

        if (numToInclude == 0) {
            // this is a leaf. The bottom up value equals the top-down value.
            if (Double.isNaN(topDownValue)) topDownValue = 0;
            node.setNumericAttribute(bottomUpAttrName, topDownValue);
            node.setAttribute(inheritedAttrName, null);
            result = topDownValue;

        } else {
            // this node has children.  Recursively calculate the
            // bottom-up value from those of the children.
            double bottomUpValue = 0;
            double childValue;
            for (int i = 0;   i < numToInclude;   i++) {
                childValue = recalc(children[i]);
                bottomUpValue += childValue;
            }
            // save the bottom-up attribute value we calculated.
            node.setNumericAttribute(bottomUpAttrName, bottomUpValue);
            node.setAttribute(inheritedAttrName, null);

            // if the top-down and bottom-up match, silently erase the
            // superfluous top-down estimate.
            if (equal(topDownValue, bottomUpValue))
                node.setAttribute(topDownAttrName, null);

            if (equal(bottomUpValue, 0))
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

    protected void maybeMultiplyValues(WBSNode node, double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) return;
        if (equal(ratio, 0)) return;
        if (topDownBottomUpMismatch(node)) return;

        multiplyValue(node, ratio);
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


    public void storeDependentColumn(String ID, int columnNumber) { }

    protected static String getTopDownAttrName(String id) {
        return id.replace('_', '-') + " (Top Down)";
    }
    protected static String getBottomUpAttrName(String id) {
        return id.replace('_', '-') + " (Bottom_Up)";
    }
    protected static String getInheritedAttrName(String id) {
        return id.replace('_', '-') + " (Inherited_)";
    }
}
