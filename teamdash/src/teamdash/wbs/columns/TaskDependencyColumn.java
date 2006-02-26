package teamdash.wbs.columns;

import java.util.Map;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TaskDependencyCellEditor;
import teamdash.wbs.TaskDependencyCellRenderer;
import teamdash.wbs.TaskDependencySource;
import teamdash.wbs.WBSNode;

/**
 * A column which can be used to display and edit task dependencies.
 */
public class TaskDependencyColumn extends AbstractDataColumn implements
        CalculatedDataColumn, CustomEditedColumn, CustomRenderedColumn {

    /** The ID we use for this column in the data model */
    static final String COLUMN_ID = "Dependencies";

    /** The name for this column */
    private static final String COLUMN_NAME = "Task Dependencies";

    /** The attribute this column uses to store its data on WBS nodes */
    public static final String ATTR_NAME = "Dependencies";

    /** The data model to which this column belongs */
    protected DataTableModel dataModel;

    /** The model describing tasks that we can depend upon */
    private TaskDependencySource dependencySource;

    /**
     * An icon map that can be used for displaying the tree of potentially
     * dependent tasks.
     */
    private Map iconMap;

    public TaskDependencyColumn(DataTableModel dataModel,
            TaskDependencySource dependencySource, Map iconMap) {
        this.dataModel = dataModel;
        this.dependencySource = dependencySource;
        this.iconMap = iconMap;
        this.columnID = COLUMN_ID;
        this.columnName = COLUMN_NAME;
        this.preferredWidth = 200;
    }

    public boolean isCellEditable(WBSNode node) {
        return true;
    }

    public Object getValueAt(WBSNode node) {
        Object result = node.getAttribute(ATTR_NAME);
        if (node.getIndentLevel() == 0 || node.isReadOnly())
            result = new ReadOnlyValue(result);
        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        node.setAttribute(ATTR_NAME, aValue);
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {
    }

    public TableCellEditor getCellEditor() {
        return new TaskDependencyCellEditor(dependencySource, iconMap);
    }

    public TableCellRenderer getCellRenderer() {
        return new TaskDependencyCellRenderer(dependencySource);
    }

}
