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
/*            if (Double.isNaN(value) || value < 0) return;

            double nc = value, added, modified;
            added = NumericDataValue.parse
                (dataModel.getValueAt(node, addedColumn));
            modified = NumericDataValue.parse
                (dataModel.getValueAt(node, modifiedColumn));

            if (nc > modified) {
                // when the user edit "new & changed", try to accomodate them
                // by altering the "added" value.
                added = nc - modified;
                dataModel.setValueAt(new Double(added), node, addedColumn);
            } else {
                // if the strategy above would result in a negative value for
                // "added", then zero out added and decrease the modified value
                dataModel.setValueAt(new Double(0),  node, addedColumn);
                dataModel.setValueAt(new Double(nc), node, modifiedColumn);
            }
        }
    }
*/
}
