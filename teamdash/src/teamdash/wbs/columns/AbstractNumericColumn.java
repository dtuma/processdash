package teamdash.wbs.columns;

import java.text.NumberFormat;

import teamdash.wbs.DataColumn;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public abstract class AbstractNumericColumn implements DataColumn {

    protected String columnID, columnName;
    protected double fuzzFactor = 0.05;
    protected String[] dependentColumns = null;

    public String getColumnID() { return columnID; }
    public String getColumnName() { return columnName; }
    public Class getColumnClass() { return String.class; }
    protected String getErrorAt(WBSNode node) { return null; }
    protected boolean isCellVisible(WBSNode node) { return true; }

    protected abstract double getValueForNode(WBSNode node);
    protected abstract void setValueForNode(double value, WBSNode node);


    public Object getValueAt(WBSNode node) {

        double value = getValueForNode(node);
        Object result = formatValue(value);

        if (isCellVisible(node) == false)
            result = new ReadOnlyInvisibleValue(result);
        else if (isCellEditable(node) == false)
            result = new ReadOnlyValue(result);

        String errorMsg = getErrorAt(node);
        if (errorMsg != null)
            result = new ErrorValue(result, errorMsg);

        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        setValueForNode(parseValue(aValue), node);
    }

    protected static final NumberFormat FORMATTER =
        NumberFormat.getNumberInstance();
    static {
        FORMATTER.setMaximumFractionDigits(1);
    }

    protected String formatValue(double value) {
        return FORMATTER.format(value);
    }

    protected double parseValue(Object aValue) {
        // unwrap special values
        if (aValue instanceof ErrorValue)
            aValue = ((ErrorValue) aValue).value;
        if (aValue instanceof ReadOnlyInvisibleValue)
            aValue = ((ReadOnlyInvisibleValue) aValue).invisibleValue;
        if (aValue instanceof ReadOnlyValue)
            aValue = ((ReadOnlyValue) aValue).value;

        if (aValue instanceof Number)
            return ((Number) aValue).doubleValue();
        if (aValue == null || "".equals(aValue))
            return 0;
        try {
            return Double.parseDouble(aValue.toString());
        } catch (NumberFormatException nfe) { }

        return Double.NaN;
    }

    protected boolean equal(double a, double b) {
        return Math.abs(a - b) < fuzzFactor;
    }

    public String[] getDependentColumnIDs() { return dependentColumns; }
    public String[] getAffectedColumnIDs() { return null; }
}
