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
        this.columnID = "Workflow Percentage";
    }

    public boolean isCellEditable(WBSNode node) {
        return (wbsModel.isLeaf(node));
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

/*
    public void setValueAt(Object aValue, WBSNode node) {
        String s = (aValue == null ? "100" : String.valueOf(aValue));
        s = s.trim();
        //if (s.endsWith("%"))  s = s.substring(0, s.length()-1).trim();
        try {
            Number n = FORMATTER.parse(s);
            if (n.doubleValue() > 0 && n.doubleValue() <= 1)
                node.setAttribute(ATTR_NAME, n);
        } catch (ParseException e) {}
    }
*/
    private static final String ATTR_NAME = "Workflow Percentage";
    //private static final NumberFormat FORMATTER =
        //      NumberFormat.getPercentInstance();


}
