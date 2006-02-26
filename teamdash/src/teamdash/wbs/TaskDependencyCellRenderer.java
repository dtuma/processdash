package teamdash.wbs;

import java.awt.Component;

import javax.swing.JTable;

/** Table cell renderer for task dependency data.
 * 
 * This renderer accepts a comma-separated list of node IDs.  It looks up
 * their full names using a dependency source, and these names as a list
 * in a table cell.
 */
public class TaskDependencyCellRenderer extends DataTableCellRenderer {

    /** The model describing tasks whose nodeIDs may be referenced */
    private TaskDependencySource dependencySource;

    public TaskDependencyCellRenderer(TaskDependencySource dependencySource) {
        this.dependencySource = dependencySource;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Object displayValue = getDisplayValue(value);
        return super.getTableCellRendererComponent(table, displayValue,
                isSelected, hasFocus, row, column);
    }

    public Object getDisplayValue(Object value) {
        boolean isReadOnly = false;
        if (value instanceof ReadOnlyValue) {
            value = ((ReadOnlyValue) value).value;
            isReadOnly = true;
        }

        if (value == null)
            return "";

        String nodeIDs = (String) value;
        if (nodeIDs.length() == 0)
            return "";

        StringBuffer buf = new StringBuffer();
        boolean hadError = false;

        String[] ids = nodeIDs.split(",");
        for (int i = 0; i < ids.length; i++) {
            String display = dependencySource.getDisplayNameForNode(ids[i]);
            if (TaskDependencySource.UNKNOWN_NODE_DISPLAY_NAME.equals(display))
                hadError = true;
            buf.append(TASK_SEPARATOR).append(display);
        }

        Object result = buf.substring(TASK_SEPARATOR.length());
        if (isReadOnly)
            result = new ReadOnlyValue(result);
        if (hadError)
            result = new ErrorValue(result, "Dependent task not found",
                    ErrorValue.ERROR);
        return result;
    }

    private static final String TASK_SEPARATOR = "  \u25AA  ";

}
