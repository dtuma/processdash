package teamdash.wbs.columns;

import java.util.Arrays;

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.ItalicCellRenderer;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TaskLabelColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CalculatedDataColumn {

    public static final String COLUMN_ID = "Labels";

    private static final String EXPLICIT_VALUE_ATTR = "Label";
    private static final String INHERITED_VALUE_ATTR = "Inherited_Label";

    private WBSModel wbsModel;

    public TaskLabelColumn(DataTableModel dataModel) {
        this.columnName = this.columnID = COLUMN_ID;
        this.preferredWidth = 200;
        this.wbsModel = dataModel.getWBSModel();
    }

    public boolean recalculate() {
        recalculate(wbsModel.getRoot(), null);
        return true;
    }

    private void recalculate(WBSNode node, String inheritedValue) {
        node.setAttribute(INHERITED_VALUE_ATTR, inheritedValue);
        String nodeValue = (String) node.getAttribute(EXPLICIT_VALUE_ATTR);
        if (nodeValue != null)
            inheritedValue = nodeValue;

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0; i < children.length; i++)
            recalculate(children[i], inheritedValue);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
    }

    public boolean isCellEditable(WBSNode node) {
        return true;
    }

    public Object getValueAt(WBSNode node) {
        String nodeValue = (String) node.getAttribute(EXPLICIT_VALUE_ATTR);
        if (" ".equals(nodeValue))
            return null;
        if (nodeValue != null)
            return nodeValue;

        String inheritedValue = (String) node.getAttribute(INHERITED_VALUE_ATTR);
        if (inheritedValue != null)
            return new ErrorValue(inheritedValue, EFFECTIVE_LABEL_MESSAGE);

        return null;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        String val = (String) aValue;

        if (val == null || val.trim().length() == 0) {
            val = " ";
        } else {
            // labels are separated by commas and/or whitespace.  They also
            // may not contain several other characters (which are reserved
            // for use by the search expression logic)
            String[] labels = val.split("[,\u0000- |()]+");
            Arrays.sort(labels, String.CASE_INSENSITIVE_ORDER);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < labels.length; i++)
                if (labels[i].length() > 0)
                    result.append(", ").append(labels[i]);
            val = result.toString();
            if (val.length() == 0)
                val = " ";
            else
                val = val.substring(2);
        }

        String inheritedValue = (String) node.getAttribute(INHERITED_VALUE_ATTR);
        if (val != null && val.equals(inheritedValue))
            val = null;

        node.setAttribute(EXPLICIT_VALUE_ATTR, val);
    }

    public TableCellRenderer getCellRenderer() {
        return LABEL_RENDERER;
    }

    private static final String EFFECTIVE_LABEL_MESSAGE = "Inherited Value";
    private static final TableCellRenderer LABEL_RENDERER =
        new ItalicCellRenderer(EFFECTIVE_LABEL_MESSAGE);

}
