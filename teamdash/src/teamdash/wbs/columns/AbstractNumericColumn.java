package teamdash.wbs.columns;

import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public abstract class AbstractNumericColumn extends AbstractDataColumn {

    protected double fuzzFactor = 0.05;


    public Class getColumnClass() { return NumericDataValue.class; }

    //eventually delete these.
    protected final String getErrorAt(WBSNode node) { return null; }
    protected final boolean isCellVisible(WBSNode node) { return true; }


    protected double getValueForNode(WBSNode node) { return 0; }


    public Object getValueAt(WBSNode node) {
        return new NumericDataValue(getValueForNode(node));
    }


    protected void setValueForNode(double value, WBSNode node) { }

    public void setValueAt(Object aValue, WBSNode node) {
        setValueForNode(NumericDataValue.parse(aValue), node);
    }


    protected boolean equal(double a, double b) {
        return Math.abs(a - b) < fuzzFactor;
    }

}
