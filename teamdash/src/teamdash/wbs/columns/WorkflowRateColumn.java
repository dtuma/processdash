package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowRateColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    private DataTableModel dataModel;
    private WBSModel wbsModel;
    private int percentageColumn = -1;

    public WorkflowRateColumn(DataTableModel dataModel) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = "Rate";
        this.columnID = "Workflow Rate";
        this.dependentColumns = new String[] {
            WorkflowPercentageColumn.COLUMN_ID };
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
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


    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return false;
    }
    private void recalc(WBSNode node) {
        if (TeamTimeColumn.isLeafTask(wbsModel, node)) {
            double baseRate = getValueForNode(node);
            double baseProductivity = 1.0 / baseRate;
            double percentage = NumericDataValue.parse
                (dataModel.getValueAt(node, percentageColumn)) / 100.0;
            double effectiveProductivity = percentage * baseProductivity;
            double effectiveRate = 1.0 / effectiveProductivity;
            if (Double.isNaN(effectiveRate) ||
                Double.isInfinite(effectiveRate))
                node.setAttribute(TeamTimeColumn.RATE_ATTR, null);
            else
                node.setNumericAttribute(TeamTimeColumn.RATE_ATTR, effectiveRate);
        }

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0;   i < children.length;   i++)
            recalc(children[i]);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (WorkflowPercentageColumn.COLUMN_ID.equals(ID))
            percentageColumn = columnNumber;
    }

    public void resetDependentColumns() {
        percentageColumn = -1;
    }


    private static final String ATTR_NAME = "Workflow Rate";


}
