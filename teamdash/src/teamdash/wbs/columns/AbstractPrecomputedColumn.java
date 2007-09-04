package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class AbstractPrecomputedColumn extends AbstractNumericColumn implements
        CalculatedDataColumn {

    private String attrName;

    protected AbstractPrecomputedColumn(String columnID, String columnName,
            String attrName, String dependentColumnID) {
        this.columnID = columnID;
        this.columnName = columnName;
        this.attrName = attrName;
        if (dependentColumnID != null)
            this.dependentColumns = new String[] { dependentColumnID };
    }

    @Override
    public Object getValueAt(WBSNode node) {
        double time = node.getNumericAttribute(attrName);
        return new NumericDataValue(time, false, !(time > 0), null);
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

}
