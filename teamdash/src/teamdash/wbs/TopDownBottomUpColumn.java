
package teamdash.wbs;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.text.NumberFormat;

public class TopDownBottomUpColumn implements DataColumn, TableModelListener
{

    private DataTableModel dataModel;
    private WBSModel wbsModel;
    private String name;
    private String topDownAttrName, bottomUpAttrName;
    protected double fuzzFactor = 0.05;

    public TopDownBottomUpColumn(DataTableModel dataModel, String name) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.name = name;
        topDownAttrName = name + " (Top Down)";
        bottomUpAttrName = name + " (Bottom_Up)";
        recalc();
        wbsModel.addTableModelListener(this);
    }

    protected String formatValue(double value) {
        return FORMATTER.format(value);
    }

    protected double parseValue(Object aValue) {
        if (aValue instanceof Number)
            return ((Number) aValue).doubleValue();
        if (aValue == null)
            return 0;
        try {
            return Double.parseDouble(aValue.toString());
        } catch (NumberFormatException nfe) { }

        return Double.NaN;
    }

    public String getColumnName() { return name; }

    public Class getColumnClass() { return String.class; }

    public boolean isCellEditable(int rowIndex) { return true; }

    public Object getValueAt(int rowIndex) {
        WBSNode node = wbsModel.getNodeForRow(rowIndex);
        double value = getValueForNode(node);
        String result = formatValue(value);
        if (topDownBottomUpMismatch(node)) {
            String bottomUp =
                formatValue(node.getNumericAttribute(bottomUpAttrName));
            String errMsg =
                "top-down/bottom-up mismatch (bottom-up = " + bottomUp + ")";
            return new ErrorValue(result, errMsg);
        } else
            return result;
    }

    public void setValueAt(Object aValue, int rowIndex) {
        System.out.println("setValueAt("+aValue+")");

        // look up the node in question.
        WBSNode node = wbsModel.getNodeForRow(rowIndex);
        if (node == null) return;

        if ("".equals(aValue))
            node.setAttribute(topDownAttrName, null);
        else {

            // parse the value we were given to obtain a double.
            double newValue = parseValue(aValue);
            if (Double.isNaN(newValue)) return;

            // look up the old value of this node.
            double oldValue = getValueForNode(node);
            if (equal(oldValue, newValue))
                return;             // if no change was made, return.

            // if this node has children, try multiplying all their values
            // by an appropriate ratio to keep the top-down and bottom-up
            // values in sync.
            maybeMultiplyValues(node,  newValue / oldValue);

            // save the new top-down value for this node.
            node.setNumericAttribute(topDownAttrName, newValue);
        }

        // recalculate the tree.
        recalc();
        fireRecalcEvent();
    }

    private void recalc() { recalc(wbsModel.getRoot()); }

    private double recalc(WBSNode node) {
        double topDownValue = node.getNumericAttribute(topDownAttrName);

        WBSNode[] children = wbsModel.getChildren(node);

        if (children.length == 0) {
            // this is a leaf. The bottom up value equals the top-down value.
            if (Double.isNaN(topDownValue)) topDownValue = 0;
            node.setNumericAttribute(bottomUpAttrName, topDownValue);
            return topDownValue;

        } else {
            // this node has children.  Recursively calculate the
            // bottom-up value from those of the children.
            double bottomUpValue = 0;
            double childValue;
            for (int i = children.length;   i-- > 0; ) {
                childValue = recalc(children[i]);
                bottomUpValue += childValue;
            }
            // save the bottom-up attribute value we calculated.
            node.setNumericAttribute(bottomUpAttrName, bottomUpValue);

            // if the top-down and bottom-up match, silently erase the
            // superfluous top-down estimate.
            if (equal(topDownValue, bottomUpValue))
                node.setAttribute(topDownAttrName, null);

            if (equal(bottomUpValue, 0))
                return (Double.isNaN(topDownValue) ? 0 : topDownValue);
            else
                return bottomUpValue;
        }
    }

    protected boolean topDownBottomUpMismatch(WBSNode node) {
        double topDownValue = node.getNumericAttribute(topDownAttrName);
        if (Double.isNaN(topDownValue)) return false;

        double bottomUpValue = node.getNumericAttribute(bottomUpAttrName);
        if (equal(topDownValue, bottomUpValue)) return false;

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
                child.setNumericAttribute(topDownAttrName, topDownValue);
            }

            multiplyValue(child, ratio);
        }
    }


    protected boolean equal(double a, double b) {
        return Math.abs(a - b) < fuzzFactor;
    }

    protected double getValueForNode(WBSNode node) {
        if (node == null) return 0;
        double value = node.getNumericAttribute(topDownAttrName);
        if (Double.isNaN(value))
            value = node.getNumericAttribute(bottomUpAttrName);
        if (Double.isNaN(value))
            value = 0;

        return value;
    }

    public void tableChanged(TableModelEvent e) { recalc(); }

    protected int myColumnIndex = -1;
    public void fireRecalcEvent() {
        if (myColumnIndex == -1)
            myColumnIndex = dataModel.findColumn(name);
        dataModel.fireTableChanged
            (new TableModelEvent(dataModel, 0, Integer.MAX_VALUE,
                                 myColumnIndex, TableModelEvent.UPDATE));
    }

    protected static final NumberFormat FORMATTER =
        NumberFormat.getNumberInstance();
        static {
            FORMATTER.setMaximumFractionDigits(1);
        }
}
