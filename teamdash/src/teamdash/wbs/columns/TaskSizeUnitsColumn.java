package teamdash.wbs.columns;

import teamdash.TeamProcess;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TaskSizeUnitsColumn extends AbstractDataColumn
implements CalculatedDataColumn {

    private WBSModel wbsModel;
    private TeamProcess teamProcess;

    public TaskSizeUnitsColumn(WBSModel wbsModel, TeamProcess teamProcess) {
        this.wbsModel = wbsModel;
        this.teamProcess = teamProcess;
        this.columnName = "Units";
        this.columnID = COLUMN_ID;
        this.preferredWidth = 80;
    }

    public Class getColumnClass() { return TaskSizeUnitsColumn.class; }

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
        if (!isCellEditable(node)) return null;
        String result = (String) node.getAttribute(ATTR_NAME);
        if (valueIsEmpty(result))
            result = getDefaultValue(node);
        return result;
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
    public void storeDependentColumn(String ID, int columnNumber) {}

}
