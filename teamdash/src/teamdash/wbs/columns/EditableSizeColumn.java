package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class EditableSizeColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    static final String ATTR_NAME = "Misc Size";

    DataTableModel dataModel;
    int newChangedColumn;

    public EditableSizeColumn(DataTableModel m) {
        this.dataModel = m;
        this.columnName = this.columnID = "Size";
        this.dependentColumns = new String[] { "N&C" };
    }

    public boolean recalculate() { return true; }
    public boolean isCellEditable(WBSNode node) {
        NumericDataValue value = (NumericDataValue) getValueAt(node);
        return (value == null || value.isEditable);
    }


    public void storeDependentColumn(String ID, int columnNumber) {
        if ("N&C".equals(ID))
            newChangedColumn = columnNumber;
    }


    public Object getValueAt(WBSNode node) {
        NumericDataValue value =
            (NumericDataValue) dataModel.getValueAt(node, newChangedColumn);
        if (value != null && value.isEditable)
            return value;
        if (SizeTypeColumn.SIZE_METRICS.get(node.getType()) != null)
            return value;

        double number = node.getNumericAttribute(ATTR_NAME);
        if (!Double.isNaN(number))
            return new NumericDataValue(number);
        else
            return new NumericDataValue(0,true,true,null);
    }


    public void setValueAt(Object aValue, WBSNode node) {
        NumericDataValue value =
            (NumericDataValue) dataModel.getValueAt(node, newChangedColumn);
        if (value != null && value.isEditable)
            dataModel.setValueAt(aValue, node, newChangedColumn);
        else
            node.setAttribute(ATTR_NAME, aValue);
    }

    public void resetDependentColumns() {
        newChangedColumn = -1;
    }

}
