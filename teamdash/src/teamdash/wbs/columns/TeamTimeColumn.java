package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.IntList;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class TeamTimeColumn extends AbstractNumericColumn
        implements CalculatedDataColumn {

    DataTableModel dataModel;
    IntList includedColumns;

    public TeamTimeColumn(DataTableModel m) {
        this.dataModel = m;
        this.columnID = this.columnName = "Time";
        this.includedColumns = new IntList();
    }

    public boolean recalculate() { return true; }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (!includedColumns.contains(columnNumber))
            includedColumns.add(columnNumber);
    }

    public void resetDependentColumns() {
        includedColumns = new IntList();
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }


    public Object getValueAt(WBSNode node) {
        double topDown = 0;
        double bottomUp = 0;
        NumericDataValue oneVal;
        int col;
        for (int i = includedColumns.size();   i-- > 0; ) {
            col = includedColumns.get(i);
            oneVal = (NumericDataValue) dataModel.getValueAt(node, col);
            topDown += safe(oneVal.value);
            bottomUp += safe(oneVal.expectedValue);
        }

        String errMsg = null;
        if (topDown != bottomUp)
            errMsg = "top-down/bottom-up mismatch (bottom-up = " +
                NumericDataValue.format(bottomUp) + ")";

        return new NumericDataValue(topDown, true, false, errMsg, bottomUp);
    }

    private double safe(double v) {
        return (Double.isNaN(v) ? 0 : v);
    }

    protected void setValueForNode(double value, WBSNode node) { }

}
