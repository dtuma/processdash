package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class SizeAliasColumn implements CalculatedDataColumn {
    private static final ReadOnlyValue BLANK = new ReadOnlyValue("");

    DataTableModel dataModel;
    String name;
    String accountingID;
    String[] sizeUnits;
    int [] columns;
    String[] dependentColumns;

    public SizeAliasColumn(DataTableModel m, String name,
                           String accountingID, String [] sizeUnits)
    {
        this.dataModel = m;
        this.name = name;
        this.accountingID = accountingID;

        this.sizeUnits = sizeUnits;
        columns = new int[sizeUnits.length];
        dependentColumns = new String[sizeUnits.length];
        for (int i = 0;   i < sizeUnits.length;   i++) {
            columns[i] = -1;
            dependentColumns[i] = accountingID + sizeUnits[i];
        }
    }

    public String getColumnID() { return name; }
    public String getColumnName() { return name; }
    public Class getColumnClass() { return String.class; }
    public String[] getDependentColumnIDs() { return dependentColumns; }
    public String[] getAffectedColumnIDs() { return null; }
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

}
