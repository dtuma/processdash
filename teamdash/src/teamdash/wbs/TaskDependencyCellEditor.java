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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

    /** The list of TaskDependencies that we are editing.  */
    private TaskDependencyList value;


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

        this.value = null;

        DataTableModel dataModel = (DataTableModel) table.getModel();
        WBSModel wbsModel = dataModel.getWBSModel();
        WBSNode node = wbsModel.getNodeForRow(row);
        if (node.getIndentLevel() == 0)
            this.errMsg = ROOT_NODE_ERROR;
        else if (value instanceof ReadOnlyValue)
            this.errMsg = READ_ONLY_ERROR;
        else {
            this.path = wbsModel.getFullName(node);
            this.value = (TaskDependencyList) WrappedValue.unwrap(value);
            loadUIData();
        }

        this.button.setText(this.value == null ? null : this.value.toString());
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

        closedProgrammatically = false;

        int userResponse = setupAndShowDialog();

        if (closedProgrammatically || userResponse == JOptionPane.OK_OPTION) {
            unloadUIData();
            stopCellEditing();
        } else {
            cancelCellEditing();
        }
    }

    private int setupAndShowDialog() {
        if (dialog == null) {
            dialog = optionPane.createDialog(button, "Edit Task Predecessors");
            dialog.setResizable(true);
        }

        Dimension prefSize = dialog.getPreferredSize();
        if (dialogSize != null) {
            prefSize.width = Math.max(prefSize.width, dialogSize.width);
            prefSize.height = Math.max(prefSize.height, dialogSize.height);
        }
        dialog.setSize(prefSize);
        dialog.setVisible(true);  // this will block until the user finishes.
        dialogSize = dialog.getSize();

        Object value = optionPane.getValue();
        if (value instanceof Integer)
            return ((Integer) value).intValue();
        else
            return JOptionPane.CLOSED_OPTION;
    }

    /// variables used by the user interface logic

    private JButton button;

    private DependencyTableModel dependencies;

    private JTable dependencyTable;

    private JScrollPane scrollPane;

    private boolean closedProgrammatically;

    private JLabel pathLabel;

    private JDialog dialog;

    private JOptionPane optionPane;

    private Dimension dialogSize = null;



    private void buildUIComponents(Map iconMap) {
        // first, create the button we'll use as our cell editor
        button = new JButton();
        button.setBackground(Color.white);
        button.setFont(UIManager.getFont("TextField.font"));
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "buttonClicked"));

        // now, create the components we'll show in the dialog.
        List components = new LinkedList();

        pathLabel = new JLabel("path label");
        components.add(pathLabel);
        components.add(Box.createRigidArea(new Dimension(5, 5)));

        dependencies = new DependencyTableModel();
        dependencyTable = new JTable(dependencies);
        dependencyTable.setDefaultRenderer(Object.class,
                new TaskDependencyCellRenderer());
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

        optionPane = new JOptionPane(components.toArray(),
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        // we can't actually create the dialog yet, because we don't know
        // who the parent window will be.
    }

    private void loadUIData() {
        pathLabel.setText(path);
        dependencies.setValue(value);
        Dimension preferredSize = dependencyTable.getPreferredSize();
        preferredSize.height = Math.max(80, preferredSize.height + 55);
        scrollPane.setPreferredSize(preferredSize);
        dependencySource.updateTaskTree();
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

        public void setValue(TaskDependencyList value) {
            setRowCount(0);
            if (value != null) {
                for (Iterator i = value.iterator(); i.hasNext();)
                    addRow(new Object[] { i.next() });
            }
        }

        public TaskDependencyList getValue() {
            if (getRowCount() == 0) {
                return null;
            } else {
                TaskDependencyList result = new TaskDependencyList();
                for (int i = 0; i < getRowCount(); i++) {
                    result.add(getValueAt(i, 0));
                }
                return result;
            }
        }

        public void addDependency(TaskDependency d) {
            for (int i = 0; i < getRowCount(); i++) {
                Object o = getValueAt(i, 0);
                if (d.equals(o))
                    return;
            }
            addRow(new Object[] { d });
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
            putValue(MNEMONIC_KEY, new Integer('A'));
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
                TaskDependency d = new TaskDependency(nodeID, null);
                d.update(dependencySource);
                dependencies.addDependency(d);
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
            putValue(MNEMONIC_KEY, new Integer('R'));
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
                closedProgrammatically = true;
                dialog.dispose();
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
