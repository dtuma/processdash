// Copyright (C) 2006-2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskDependencyResolver;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JTreeTable;

public class TaskDependencyCellEditor extends AbstractCellEditor implements
        TableCellEditor {

    /** The Task and Schedule dialog we are working for */
    private TaskScheduleDialog parent;

    /** The dashboard context we are working within */
    private DashboardContext dashboardContext;

    /** The full-path name of the task node we are editing */
    private String taskName;

    /** The list of dependencies we are editing for that node */
    private List dependencies;

    /** Whether a change was made during the current editing session */
    private boolean madeChange;

    private static final Resources resources = Resources
            .getDashBundle("EV.Dependencies");

    public TaskDependencyCellEditor(TaskScheduleDialog parent,
            DashboardContext dashboardContext) {
        this.parent = parent;
        this.dashboardContext = dashboardContext;
        buildUIComponents();
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        this.taskName = null;
        this.dependencies = new ArrayList();
        this.madeChange = false;

        JTreeTable jTreeTable = (JTreeTable) table;
        JTree jTree = jTreeTable.getTree();
        TreePath path = jTree.getPathForRow(row);
        EVTask node = null;
        if (path != null) {
            if (path.getLastPathComponent() instanceof EVTask)
                node = (EVTask) path.getLastPathComponent();
            this.taskName = node.getFullName();
        }

        if (value instanceof Collection) {
            for (Iterator i = ((Collection) value).iterator(); i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof EVTaskDependency) {
                    EVTaskDependency d = (EVTaskDependency) obj;
                    // reverse/collab dependencies aren't editable - skip them
                    if (!d.isReverse() && !d.isCollab())
                        dependencies.add(d);
                }
            }
        }

        // lookup the cell renderer for this row/column.  It will probably
        // be a task DependencyCellRenderer.  If so, copy the icon it is
        // displaying so we have a similar appearance.
        Component rend = table.getCellRenderer(row, column)
                .getTableCellRendererComponent(table, value, isSelected, false,
                        row, column);
        if (rend instanceof JLabel)
            button.setIcon(((JLabel) rend).getIcon());
        else
            button.setIcon(null);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                button.doClick();
            }});

        return button;
    }

    public Object getCellEditorValue() {
        return dependencies;
    }

    public boolean isCellEditable(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
            return ((MouseEvent) anEvent).getClickCount() > 1;
        }
        return true;
    }


    /// variables used by the user interface logic

    private JButton button;

    private DependencyTableModel dependencyTableModel;

    private JTable dependencyTable;

    private JScrollPane scrollPane;

    private Object[] dialogComponents;


    private void buildUIComponents() {
        // first, create the button we'll use as our cell editor
        button = new JButton();
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "buttonClicked"));

        // now, create the components we'll show in the dialog.
        List components = new LinkedList();

        components.add("path label");
        components.add(Box.createRigidArea(new Dimension(5, 5)));

        dependencyTableModel = new DependencyTableModel();
        dependencyTable = new JTable(dependencyTableModel);
        dependencyTable.setDefaultRenderer(Object.class,
                new DependencyTableCellRenderer());
        scrollPane = new JScrollPane(dependencyTable);
        components.add(scrollPane);
        components.add(Box.createRigidArea(new Dimension(5, 5)));


        Box b = Box.createHorizontalBox();

        b.add(Box.createHorizontalGlue());
        b.add(new JButton(new AddAction()));
        b.add(Box.createHorizontalGlue());
        b.add(new JButton(new RemoveAction()));
        b.add(Box.createHorizontalGlue());
        components.add(b);
        components.add(Box.createRigidArea(new Dimension(5, 5)));
        components.add(new Separator());

        dialogComponents = components.toArray();
    }

    public void buttonClicked() {
        dialogComponents[0] = taskName;
        dependencyTableModel.refresh();
        Dimension preferredSize = dependencyTable.getPreferredSize();
        preferredSize.height = Math.max(80, preferredSize.height + 55);
        scrollPane.setPreferredSize(preferredSize);

        int userResponse = JOptionPane.showConfirmDialog(button,
                dialogComponents, resources.getString("Window_Title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (userResponse == JOptionPane.OK_OPTION && madeChange) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    parent.evScheduleChanged();
                }});
            stopCellEditing();
        } else {
            cancelCellEditing();
        }
    }


    private class DependencyTableModel extends AbstractTableModel {

        public String getColumnName(int column) {
            return resources.getString("TaskList.Columns.Depn.Tooltip");
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return (dependencies == null ? 0 : dependencies.size());
        }

        public Object getValueAt(int row, int col) {
            if (isValidRow(row))
                return dependencies.get(row);
            else
                return null;
        }

        public void removeDependency(int row) {
            if (isValidRow(row)) {
                madeChange = true;
                dependencies.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public void addDependency(EVTaskDependency d) {
            if (d != null && !dependencies.contains(d)) {
                madeChange = true;
                int newRowNum = dependencies.size();
                dependencies.add(d);
                fireTableRowsInserted(newRowNum, newRowNum);
            }
        }

        public void refresh() {
            fireTableDataChanged();
        }

        private boolean isValidRow(int row) {
            return (dependencies != null && row >= 0 && row < dependencies
                    .size());
        }
    }

    private class DependencyTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            EVTaskDependency d = (EVTaskDependency) value;
            String name = null;
            if (d != null) {
                name = d.getDisplayName();
                if (name == null)
                    name = resources.getString("Dependency.Unresolved.Text");
            }

            Component result = super.getTableCellRendererComponent(table, name,
                    isSelected, hasFocus, row, column);

            if (d != null && d.isUnresolvable()) {
                setForeground(Color.red);
                setToolTipText(resources
                        .getString("Dependency.Unresolved.Explanation"));
            } else {
                setForeground(Color.black);
                setToolTipText(null);
            }

            return result;
        }

    }

    private class RemoveAction extends AbstractAction implements
            ListSelectionListener {

        public RemoveAction() {
            super(resources.getString("Buttons.Remove_Dependency"));
            dependencyTable.getSelectionModel().addListSelectionListener(this);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            int[] selRows = dependencyTable.getSelectedRows();
            if (selRows != null)
                for (int i = selRows.length; i-- > 0;)
                    dependencyTableModel.removeDependency(selRows[i]);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(dependencyTable.getSelectedRowCount() > 0);
        }
    }

    private class AddAction extends AbstractAction {

        public AddAction() {
            super(resources.getDlgString("Buttons.Add_Dependency"));
        }

        public void actionPerformed(ActionEvent e) {
            String taskListName = chooseTaskList();
            EVTaskDependency d = chooseTask(taskListName);
            dependencyTableModel.addDependency(d);
        }

        private String chooseTaskList() {
            String[] taskListNames = EVTaskList.findTaskLists(dashboardContext
                    .getData(), false, true);
            String[] taskListDisplayNames = getDisplayNames(taskListNames);
            JList taskLists = new JList(taskListDisplayNames);
            new JOptionPaneClickHandler().install(taskLists);
            JScrollPane scrollPane = new JScrollPane(taskLists);
            scrollPane.setPreferredSize(new Dimension(200, 200));
            Object message = new Object[] {
                    resources.getString("Add.Select_Schedule_Instructions"),
                    scrollPane };
            int userReponse = JOptionPane.showConfirmDialog(dependencyTable,
                    message, resources.getString("Add.Window_Title"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (userReponse == JOptionPane.OK_OPTION) {
                int selIndex = taskLists.getSelectedIndex();
                if (selIndex != -1)
                    return taskListNames[selIndex];
            }

            return null;
        }

        private String[] getDisplayNames(String[] taskListNames) {
            String[] result = new String[taskListNames.length];
            for (int i = result.length; i-- > 0;)
                result[i] = EVTaskList.cleanupName(taskListNames[i]);
            return result;
        }

        private EVTaskDependency chooseTask(String taskListName) {
            if (taskListName == null)
                return null;

            EVTaskList tl = EVTaskList.openExisting(taskListName,
                    dashboardContext.getData(),
                    dashboardContext.getHierarchy(), dashboardContext
                            .getCache(), false);
            if (tl == null)
                return null;

            JTree tree = new JTree(tl);
            tree.expandRow(0);
            for (int i = tree.getRowCount(); i-- > 0;)
                tree.expandRow(i);
            tree.setRootVisible(false);
            tree.setToggleClickCount(3);
            new JOptionPaneClickHandler().install(tree);
            tree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);

            JScrollPane scrollPane = new JScrollPane(tree);
            scrollPane.setPreferredSize(new Dimension(200, 200));
            Object message = new Object[] {
                    resources.getString("Add.Select_Task_Instructions"),
                    scrollPane };
            int userReponse = JOptionPane.showConfirmDialog(dependencyTable,
                    message, resources.getString("Add.Window_Title"),
                    JOptionPane.OK_CANCEL_OPTION);
            if (userReponse == JOptionPane.OK_OPTION) {
                TreePath path = tree.getSelectionPath();
                if (path == null)
                    return null;
                EVTask task = (EVTask) path.getLastPathComponent();
                String taskID = EVTaskDependencyResolver.getIdForTask(task,
                        tl.getID());
                System.out.println("taskID is " + taskID);
                if (taskID == null)
                    return null;
                EVTaskDependency d = new EVTaskDependency(taskID, task
                        .getFullName());
                d.setTaskListName(taskListName);
                return d;
            }
            return null;
        }
    }

    private class Separator extends JSeparator {

        public void addNotify() {
            super.addNotify();
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof Dialog)
                ((Dialog) w).setResizable(true);
        }

    }

}
