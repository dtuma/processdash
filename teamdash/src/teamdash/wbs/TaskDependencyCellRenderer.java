package teamdash.wbs;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JTable;

import teamdash.XMLUtils;

/**
 * Table cell renderer for task dependency data.
 */
public class TaskDependencyCellRenderer extends DataTableCellRenderer {

    public TaskDependencyCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        boolean hasError = false;
        setToolTipText(null);
        String overflowToolTipText = null;

        // Note: this logic will not flag errors in read-only dependencies
        if (value instanceof TaskDependency)
            hasError = ((TaskDependency) value).hasError;
        else if (value instanceof TaskDependencyList) {
            TaskDependencyList list = (TaskDependencyList) value;
            hasError = list.hasError();
            overflowToolTipText = computeOverflowToolTipText(list);
        }

        if (hasError)
            value = new ErrorValue(value, "Dependent task not found",
                    ErrorValue.ERROR);

        Component result = super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        if (overflowToolTipText != null)
            setToolTipText(overflowToolTipText);
        return result;
    }

    private String computeOverflowToolTipText(TaskDependencyList list) {
        if (list.size() < 2)
            return null;

        StringBuffer result = new StringBuffer();
        result.append("<html><body><table border='0'>");
        for (Iterator i = list.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            if (d.hasError)
                result.append("<tr><td color='red'>");
            else
                result.append("<tr><td>");
            result.append(XMLUtils.escapeAttribute(d.displayName));
            result.append("</td></tr>");
        }
        result.append("</table></body></html>");
        return result.toString();
    }

}
