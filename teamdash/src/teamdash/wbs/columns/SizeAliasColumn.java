package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class SizeAliasColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    private static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);

    DataTableModel dataModel;
    String accountingID;
    String[] sizeUnits;
    int [] columns;

    public SizeAliasColumn(DataTableModel m, String name,
                           String accountingID, String [] sizeUnits)
    {
        this.dataModel = m;
        this.columnName = this.columnID = name;
        this.accountingID = accountingID;

        this.sizeUnits = sizeUnits;
        columns = new int[sizeUnits.length];
        dependentColumns = new String[sizeUnits.length];
        for (int i = 0;   i < sizeUnits.length;   i++) {
            columns[i] = -1;
            dependentColumns[i] = accountingID + sizeUnits[i];
        }
    }

    public boolean recalculate() { return true; }


    public void storeDependentColumn(String ID, int columnNumber) {
        for (int i = sizeUnits.length;   i-- > 0; )
            if (ID.equals(dependentColumns[i])) {
                columns[i] = columnNumber;
                return;
            }
    }

    protected String getSizeUnit(WBSNode node) {
        return (String) SizeTypeColumn.SIZE_METRICS.get(node.getType());
    }

    protected int getSizeColumn(WBSNode node) {
        String unit = getSizeUnit(node);
        if (unit == null) return -1;
        for (int i = sizeUnits.length;   i-- > 0; )
            if (unit.equals(sizeUnits[i]))
                return columns[i];
        return -1;
    }

    public boolean isCellEditable(WBSNode node) {
        int column = getSizeColumn(node);
        return (column == -1 ? false : dataModel.isCellEditable(node, column));
    }

    public Object getValueAt(WBSNode node) {
        int column = getSizeColumn(node);
        if (column == -1) return BLANK;
        return dataModel.getValueAt(node, column);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        int column = getSizeColumn(node);
        if (column != -1)
            dataModel.setValueAt(aValue, node, column);
    }
    public void resetDependentColumns() {
        for (int i = 0;   i < columns.length;   i++)
            columns[i] = -1;
    }

}
