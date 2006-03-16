package teamdash.wbs.columns;

import java.util.Iterator;
import java.util.Map;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TaskDependency;
import teamdash.wbs.TaskDependencyCellEditor;
import teamdash.wbs.TaskDependencyCellRenderer;
import teamdash.wbs.TaskDependencyList;
import teamdash.wbs.TaskDependencySource;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

/**
 * A column which can be used to display and edit task dependencies.
 */
public class TaskDependencyColumn extends AbstractDataColumn implements
        CalculatedDataColumn, CustomEditedColumn, CustomRenderedColumn {

    /** The ID we use for this column in the data model */
    public static final String COLUMN_ID = "Dependencies";

    /** The name for this column */
    private static final String COLUMN_NAME = "Task Dependencies";

    /**
     * The attribute this column uses to store the list of dependent nodeIDs for
     * a WBS node
     */
    public static final String ID_LIST_ATTR = "Dependencies";

    /**
     * The attribute this column uses to store the name of a dependent task,
     * whose nodeID appeared in the list held by ID_ATTR
     */
    public static final String NAME_ATTR_PREFIX = "Dependency Name ";

    /** An attribute for holding working data about dependencies */
    private static final String TASK_LIST_ATTR = "Dependency_List";


    /** The data model to which this column belongs */
    private DataTableModel dataModel;

    /** The model describing tasks that we can depend upon */
    private TaskDependencySource dependencySource;

    /** Whether we should unequivocally return true on our next recalculation */
    private boolean needsRecalc;

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
        TaskDependencyList list = (TaskDependencyList) node
                .getAttribute(TASK_LIST_ATTR);
        Object result = list;
        if (node.getIndentLevel() == 0 || node.isReadOnly())
            result = new ReadOnlyValue(result);
        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        TaskDependencyList list;

        if (node.getIndentLevel() == 0 || node.isReadOnly())
            return;
        else if (aValue instanceof TaskDependencyList || aValue == null)
            list = (TaskDependencyList) aValue;
        else if (aValue instanceof String) {
            list = TaskDependencyList.valueOf((String) aValue);
            needsRecalc = true;
        } else
            return;

        node.setAttribute(TASK_LIST_ATTR, list);
        writeDependenciesToNode(node, list);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
    }

    public TableCellEditor getCellEditor() {
        return new TaskDependencyCellEditor(dependencySource, iconMap);
    }

    public TableCellRenderer getCellRenderer() {
        return new TaskDependencyCellRenderer();
    }

    public boolean recalculate() {
        boolean result = needsRecalc;
        needsRecalc = false;
        WBSModel wbs = dataModel.getWBSModel();
        WBSNode[] allNodes = wbs.getDescendants(wbs.getRoot());
        for (int i = 0; i < allNodes.length; i++) {
            if (recalculate(allNodes[i]))
                result = true;
        }
        return result;
    }

    /** Recalculate data for one node. Return true if changes were made. */
    private boolean recalculate(WBSNode n) {
        boolean result = false;
        // retrieve the list of TaskDependencies for this node.
        TaskDependencyList list = (TaskDependencyList) n
                .getAttribute(TASK_LIST_ATTR);
        if (list != null) {
            // if that list exists, update each of its tasks.
            if (list.update(dependencySource))
                result = true;

        } else {
            // the list doesn't exist. Try recalculating it.
            list = readDependenciesForNode(n);
            if (list != null) {
                // the list wasn't there, but it needs to be. Save it.
                n.setAttribute(TASK_LIST_ATTR, list);
                result = true;
            }
        }

        return result;
    }

    /**
     * Read the data contained in the ID_LIST and NAME attributes, and construct
     * a list of TaskDependency objects. If this task has no dependencies,
     * returns null.
     */
    private TaskDependencyList readDependenciesForNode(WBSNode node) {
        String idList = (String) node.getAttribute(ID_LIST_ATTR);
        if (idList == null || idList.length() == 0)
            return null;

        TaskDependencyList result = new TaskDependencyList();
        String[] ids = idList.split(",");
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            String name = (String) node.getAttribute(NAME_ATTR_PREFIX + i);
            TaskDependency d = new TaskDependency(id, name);
            d.update(dependencySource);
            node.setAttribute(NAME_ATTR_PREFIX + i, d.displayName);
            result.add(d);
        }
        return result;
    }

    /**
     * Save information from the given list of TaskDependency objects into the
     * ID_LIST and NAME attributes of the given node.
     */
    private void writeDependenciesToNode(WBSNode node, TaskDependencyList list) {
        int pos = 0;
        if (list == null || list.isEmpty()) {
            node.setAttribute(ID_LIST_ATTR, null);
        } else {
            StringBuffer idList = new StringBuffer();
            for (Iterator i = list.iterator(); i.hasNext();) {
                TaskDependency d = (TaskDependency) i.next();
                idList.append(",").append(d.nodeID);
                node.setAttribute(NAME_ATTR_PREFIX + (pos++), d.displayName);
            }
            node.setAttribute(ID_LIST_ATTR, idList.substring(1));
        }
        for (int j = 0; j < 10; j++)
            node.setAttribute(NAME_ATTR_PREFIX + (pos++), null);
    }

}
