package teamdash.wbs.columns;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowRateColumn extends AbstractNumericColumn {

    private WBSModel wbsModel;

    public WorkflowRateColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = "Rate";
        this.columnID = "Workflow Rate";
    }

    public boolean isCellEditable(WBSNode node) {
        return (wbsModel.isLeaf(node));
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : "");
    }

    protected double getValueForNode(WBSNode node) {
        double d = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(d)) d = 0;
        return d;
    }

    protected void setValueForNode(double value, WBSNode node) {
        node.setNumericAttribute(ATTR_NAME, value);
    }


    private static final String ATTR_NAME = "Workflow Rate";

}
