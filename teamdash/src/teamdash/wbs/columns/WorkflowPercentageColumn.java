package teamdash.wbs.columns;

//import java.text.NumberFormat;
//import java.text.ParseException;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowPercentageColumn extends AbstractNumericColumn {

    private WBSModel wbsModel;

    public WorkflowPercentageColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = "%";
        this.columnID = COLUMN_ID;
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : null);
    }

    protected double getValueForNode(WBSNode node) {
        double d = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(d)) d = 100;
            return d;
    }

    protected void setValueForNode(double value, WBSNode node) {
        if (value > 0 && value <= 100)
            node.setNumericAttribute(ATTR_NAME, value);
    }


    private static final String ATTR_NAME = "Workflow Percentage";
    static final String COLUMN_ID = ATTR_NAME;

}
