package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.Arrays;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;


/** A table cell editor for editing task dependencies.
 * 
 * This editor pops up a dialog box, allowing the user to view and edit task
 * dependencies.
 */
public class TaskDependencyCellEditor extends AbstractCellEditor implements
        TableCellEditor {

    /** The model describing tasks that we can depend upon */
    private TaskDependencySource dependencySource;

    /** The full-path name of the node we are editing */
    private String path;

    /** If non-null, an error message that should be displayed in lieu of
     * opening the dependency editing dialog.  */
    private String[] errMsg;

    /** The comma-separated list of node IDs specifying the task dependencies
     * that we are editing.  */
    private String value;


    public TaskDependencyCellEditor(TaskDependencySource dependencySource,
            Map iconMap) {
        this.dependencySource = dependencySource;
        buildUIComponents(iconMap);
    }

    public Object getCellEditorValue() {
        return value;
    }

    public boolean isCellEditable(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
            return ((MouseEvent) anEvent).getClickCount() > 1;
        }
        return true;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        DataTableModel dataModel = (DataTableModel) table.getModel();
        WBSModel wbsModel = dataModel.getWBSModel();
        WBSNode node = wbsModel.getNodeForRow(row);
        if (node.getIndentLevel() == 0)
            this.errMsg = ROOT_NODE_ERROR;
        else if (value instanceof ReadOnlyValue)
            this.errMsg = READ_ONLY_ERROR;
        else {
            this.path = wbsModel.getFullName(node);
            this.value = (String) value;
            loadUIData();
        }

        button.setText(renderer.getDisplayValue(value).toString());
        return button;
    }


    public void buttonClicked() {
        if (errMsg != null) {
            JOptionPane.showMessageDialog(null, errMsg,
                    "Operation Not Allowed", JOptionPane.ERROR_MESSAGE);
            errMsg = null;
            cancelCellEditing();
            return;
        }

        int userResponse = JOptionPane.showConfirmDialog(null,
                dialogComponents, "Edit Task Predecessors",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

        if (closedProgrammatically || userResponse == JOptionPane.OK_OPTION) {
            unloadUIData();
            stopCellEditing();
        } else {
            cancelCellEditing();
        }
    }

    /// variables used by the user interface logic

    private JButton button;

    private TaskDependencyCellRenderer renderer;

    private DependencyTableModel dependencies;

    private JTable dependencyTable;

    private JScrollPane scrollPane;

    private Object[] dialogComponents;

    private boolean closedProgrammatically;



    private void buildUIComponents(Map iconMap) {
        if (dialogComponents != null)
            return;

        button = new JButton();
        button.setBackground(Color.white);
        button.setFont(UIManager.getFont("TextField.font"));
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "buttonClicked"));

        List components = new LinkedList();

        components.add("path label"); // will be replaced by loadUIData()
        components.add(Box.createRigidArea(new Dimension(5, 5)));

        dependencies = new DependencyTableModel();
        dependencyTable = new JTable(dependencies);
        renderer = new TaskDependencyCellRenderer(dependencySource);
        dependencyTable.setDefaultRenderer(Object.class, renderer);
        scrollPane = new JScrollPane(dependencyTable);
        components.add(scrollPane);
        components.add(Box.createRigidArea(new Dimension(5, 5)));

        WBSJTable taskTree = new WBSJTable(dependencySource.getTaskTree(),
                iconMap);
        taskTree.setEditingEnabled(false);
        AddAction addAction = new AddAction(taskTree);
        taskTree.addMouseListener(new WBSClickHandler(addAction));

        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        b.add(new JButton(addAction));
        b.add(Box.createHorizontalGlue());
        b.add(new JButton(new RemoveAction(dependencyTable)));
        b.add(Box.createHorizontalGlue());
        components.add(b);
        components.add(Box.createRigidArea(new Dimension(5, 5)));

        JScrollPane sp = new JScrollPane(taskTree);
        sp.setPreferredSize(new Dimension(300, 200));
        components.add(sp);

        dialogComponents = components.toArray();
    }

    private void loadUIData() {
        dialogComponents[0] = path;
        dependencies.setValue(value);
        Dimension preferredSize = dependencyTable.getPreferredSize();
        preferredSize.height = Math.max(80, preferredSize.height + 55);
        scrollPane.setPreferredSize(preferredSize);
        dependencySource.updateTaskTree();
        closedProgrammatically = false;
    }

    private void unloadUIData() {
        value = dependencies.getValue();
    }


    /** A simple table model for holding the list of dependencies we are
     * currently editing.
     */
    private class DependencyTableModel extends DefaultTableModel {

        public DependencyTableModel() {
            addColumn("Dependencies");
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void setValue(String value) {
            setRowCount(0);
            if (value != null) {
                String[] idList = value.split(",");
                for (int i = 0; i < idList.length; i++)
                    addRow(new Object[] { idList[i] });
            }
        }

        public String getValue() {
            if (getRowCount() == 0) {
                return null;
            } else {
                StringBuffer newValue = new StringBuffer();
                for (int i = 0; i < getRowCount(); i++) {
                    Object id = getValueAt(i, 0);
                    newValue.append(",").append(id);
                }
                return newValue.substring(1);
            }
        }

        public void addDependency(String nodeID) {
            for (int i = 0; i < getRowCount(); i++) {
                Object o = getValueAt(i, 0);
                if (nodeID.equals(o))
                    return;
            }
            addRow(new Object[] { nodeID });
        }
    }


    /** Adds dependencies for the WBS nodes currently selected in the
     * dependency tree.
     */
    private class AddAction extends AbstractAction implements
            ListSelectionListener {

        private WBSJTable wbs;

        public AddAction(WBSJTable wbs) {
            super("Add");
            setEnabled(false);
            this.wbs = wbs;
            wbs.getSelectionModel().addListSelectionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            WBSModel model = (WBSModel) wbs.getModel();
            int[] rows = wbs.getSelectedRows();
            for (int i = rows.length; i-- > 0;) {
                WBSNode node = model.getNodeForRow(rows[i]);
                String nodeID = dependencySource.getNodeID(node);
                dependencies.addDependency(nodeID);
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            int[] selRows = wbs.getSelectedRows();
            if (selRows.length == 0)
                setEnabled(false);
            else if (selRows.length == 1)
                setEnabled(selRows[0] != 0);
            else
                setEnabled(true);
        }

    }


    /** Removes the dependencies that are currently selected in the
     * dependency table
     */
    private class RemoveAction extends AbstractAction implements
            ListSelectionListener {

        private JTable table;

        public RemoveAction(JTable table) {
            super("Remove");
            setEnabled(false);
            this.table = table;
            table.getSelectionModel().addListSelectionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            Arrays.sort(rows);
            for (int i = rows.length; i-- > 0;)
                dependencies.removeRow(rows[i]);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(table.getSelectedRowCount() > 0);
        }
    }


    /** As a convenience to users, allows double- and triple-clicks on the
     * WBS tree to perform commonly needed actions.
     */
    private class WBSClickHandler extends MouseAdapter {

        private AddAction addAction;

        public WBSClickHandler(AddAction addAction) {
            this.addAction = addAction;
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2)
                addAction.actionPerformed(null);

            else if (e.getClickCount() == 3) {
                try {
                    SwingUtilities.getWindowAncestor((Component) e.getSource())
                            .dispose();
                    closedProgrammatically = true;
                } catch (Exception ex) {
                }
            }
        }
    }


    private static final String[] ROOT_NODE_ERROR = new String[] {
            "Sorry, you cannot define task dependencies",
            "for the root node of your team project." };

    private static final String[] READ_ONLY_ERROR = new String[] {
            "Sorry, you cannot define task dependencies",
            "for this node, because it is read-only." };
}
