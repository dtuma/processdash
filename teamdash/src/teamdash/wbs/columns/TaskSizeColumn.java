package teamdash.wbs.columns;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TaskSizeColumn extends SizeAliasColumn {

    private WBSModel wbsModel;
    private int unitsColumn = -1;

    public TaskSizeColumn(DataTableModel dataModel) {
        super(dataModel, "Task Size", "N&C-", SizeTypeColumn.SIZE_UNITS);
        this.wbsModel = dataModel.getWBSModel();

        int len = this.dependentColumns.length;
        String [] dependentCols = new String[len+1];
        System.arraycopy(this.dependentColumns, 0, dependentCols, 0, len);
        dependentCols[len] = TaskSizeUnitsColumn.COLUMN_ID;
        this.dependentColumns = dependentCols;
        this.preferredWidth = 65;
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (TaskSizeUnitsColumn.COLUMN_ID.equals(ID))
            unitsColumn = columnNumber;
        else
            super.storeDependentColumn(ID, columnNumber);
    }

    public void resetDependentColumns() {
        unitsColumn = -1;
        super.resetDependentColumns();
    }

    protected String getSizeUnit(WBSNode node) {
        Object result = dataModel.getValueAt(node, unitsColumn);
        return (result == null ? null : String.valueOf(result));
    }

    public boolean isCellEditable(WBSNode node) {
        // we only allow the user to edit time for "leaf tasks". If this
        // isn't a leaf task, it's not editable.
        if (!TeamTimeColumn.isLeafTask(wbsModel, node)) return false;

        // if this leaf task is inheriting it's size from some standard
        // size metrics (like LOC), we don't want to let the user edit the
        // inherited value - that would behave in a manner completely unlike
        // what they expected.  So we only let them edit this column if they
        // have chosen a custom size metric for this task.
        return getSizeColumn(node) == -1;
    }

    private boolean valueIsEmpty(Object aValue) {
        if (aValue == null) return true;
        if (aValue instanceof String &&
            ((String) aValue).trim().length() == 0) return true;
        return false;
    }

    public Object getValueAt(WBSNode node) {
        if (!TeamTimeColumn.isLeafTask(wbsModel, node)) return BLANK;

        Object result = super.getValueAt(node);
        if (result != BLANK)
            return new NumericDataValue(NumericDataValue.parse(result), false);

        double miscSize = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(miscSize))
            return null;
        else
            return new NumericDataValue(miscSize);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (!isCellEditable(node)) return;

        node.setNumericAttribute(ATTR_NAME, NumericDataValue.parse(aValue));
    }

    private static final String ATTR_NAME = EditableSizeColumn.ATTR_NAME;

    public boolean recalculate() { return true; }

}
