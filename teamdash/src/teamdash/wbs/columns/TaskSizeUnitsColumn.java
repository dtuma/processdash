package teamdash.wbs.columns;

import teamdash.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TaskSizeUnitsColumn extends AbstractDataColumn {

    private WBSModel wbsModel;
    private TeamProcess teamProcess;

    public TaskSizeUnitsColumn(WBSModel wbsModel, TeamProcess teamProcess) {
        this.wbsModel = wbsModel;
        this.teamProcess = teamProcess;
        this.columnName = "Units";
        this.columnID = ATTR_NAME;
    }

    public Class getColumnClass() { return TaskSizeUnitsColumn.class; }

    public boolean isCellEditable(WBSNode node) {
        boolean result = (wbsModel.isLeaf(node));
        System.out.println("task is cell editable: "+result);
        return result;
    }

    public Object getValueAt(WBSNode node) {
        if (!isCellEditable(node)) return null;
        String result = (String) node.getAttribute(ATTR_NAME);
        if (result == null) {
            // TODO - ask the team process for the size metric.
        }
        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue == null)
            node.setAttribute(ATTR_NAME, null);
        else
            node.setAttribute(ATTR_NAME, String.valueOf(aValue));
    }

    private static final String ATTR_NAME = "Task Size Units";

}
