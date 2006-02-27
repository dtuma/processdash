package teamdash.wbs;

import java.awt.Component;

import javax.swing.JTable;

/**
 * Table cell renderer for task dependency data.
 */
public class TaskDependencyCellRenderer extends DataTableCellRenderer {

    public TaskDependencyCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        boolean hasError = false;

        // Note: this logic will not flag errors in read-only dependencies
        if (value instanceof TaskDependency)
            hasError = ((TaskDependency) value).hasError;
        else if (value instanceof TaskDependencyList)
            hasError = ((TaskDependencyList) value).hasError();

        if (hasError)
            value = new ErrorValue(value, "Dependent task not found",
                    ErrorValue.ERROR);

        return super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
    }

}
