package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TaskSizeUnitsColumn extends AbstractDataColumn
implements CalculatedDataColumn {

    private DataTableModel dataModel;
    private WBSModel wbsModel;
    private TeamProcess teamProcess;
    private int mainSizeUnitsColumn;

    public TaskSizeUnitsColumn(DataTableModel dataModel, TeamProcess teamProcess) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.teamProcess = teamProcess;
        this.columnName = "Units";
        this.columnID = COLUMN_ID;
        this.preferredWidth = 80;
        this.dependentColumns = new String[] { SizeTypeColumn.COLUMN_ID };
        this.mainSizeUnitsColumn = -1;
    }

    public Class getColumnClass() { return TaskSizeUnitsColumn.class; }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (SizeTypeColumn.COLUMN_ID.equals(ID))
            mainSizeUnitsColumn = columnNumber;
    }

    public void resetDependentColumns() {
        mainSizeUnitsColumn = -1;
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    private boolean valueIsEmpty(Object aValue) {
        if (aValue == null) return true;
        if (aValue instanceof String &&
            ((String) aValue).trim().length() == 0) return true;
        return false;
    }

    protected String getDefaultValue(WBSNode node) {
        return teamProcess.getPhaseSizeMetric(node.getType());
    }

    public Object getValueAt(WBSNode node) {
        if (isCellEditable(node)) {
            String result = (String) node.getAttribute(ATTR_NAME);
            if (valueIsEmpty(result))
                result = getDefaultValue(node);
            return result;
        } else if (mainSizeUnitsColumn != -1) {
            return dataModel.getValueAt(node, mainSizeUnitsColumn);
        } else {
            return null;
        }
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (valueIsEmpty(aValue) || aValue.equals(getDefaultValue(node)))
            node.setAttribute(ATTR_NAME, null);
        else
            node.setAttribute(ATTR_NAME, String.valueOf(aValue));
    }

    private static final String ATTR_NAME = "Task Size Units";
    static final String COLUMN_ID = ATTR_NAME;

    public boolean recalculate() { return true; }

}
