// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.EventObject;

public class TaskScheduleDialog
    implements EVTask.Listener, EVTaskList.RecalcListener, EVSchedule.Listener
{

    /** Model for the JTreeTable */
    protected EVTaskList model;
    /** TreeTable displaying the task list */
    protected JTreeTable treeTable;
    /** table displaying the schedule */
    protected JTable scheduleTable;
    /** Frame containing everything */
    protected JFrame frame;
    /** the dashboard */
    protected PSPDashboard dash;
    protected String taskListName;

    protected JButton addTaskButton, deleteTaskButton, moveUpButton,
        moveDownButton, addPeriodButton, insertPeriodButton,
        deletePeriodButton, chartButton, reportButton, closeButton,
        saveButton, recalcButton;

    protected JFrame chartDialog = null;


    public TaskScheduleDialog(PSPDashboard dash, String taskListName,
                              boolean createRollup) {
        // Create the frame and set an appropriate icon
        frame = new JFrame("Task and Schedule");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));
        PCSH.enableHelpKey(frame, "UsingTaskSchedule");

        // Create the earned value model.
        model = new EVTaskList(taskListName, dash.data, dash.props,
                               createRollup, true);
        model.recalc();
        model.setNodeListener(this);
        model.addRecalcListener(this);
        model.getSchedule().setListener(this);

        // Create a JTreeTable to display the task list.
        treeTable = new TaskJTreeTable(model);
        treeTable.setShowGrid(true);
        treeTable.setIntercellSpacing(new Dimension(1, 1));
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (treeTable, EVTaskList.toolTips);
                                // set default widths for the columns
        for (int i = 0;  i < EVTaskList.colWidths.length;  i++)
            treeTable.getColumnModel().getColumn(i)
                .setPreferredWidth(EVTaskList.colWidths[i]);
        configureEditor(treeTable);

        // Create a JTable to display the schedule list.
        scheduleTable = new ScheduleJTable(model.getSchedule());
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (scheduleTable, EVSchedule.toolTips);
                                // set default widths for the columns
        for (int i = 0;  i < EVSchedule.colWidths.length;  i++)
            scheduleTable.getColumnModel().getColumn(i)
                .setPreferredWidth(EVSchedule.colWidths[i]);
        configureEditor(scheduleTable);

        boolean isRollup = isRollup();
        if (isRollup) frame.setTitle("Task and Schedule Rollup");

        JScrollPane sp;
        sp = new JScrollPane(treeTable,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(10, 10));
        Box topBox = newVBox
            (sp,
             Box.createVerticalStrut(2),
             buildTaskButtons(isRollup),
             Box.createVerticalStrut(2));

        sp = new JScrollPane(scheduleTable,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(10, 10));
        Box bottomBox = newVBox
            (sp,
             Box.createVerticalStrut(2),
             isRollup ? null : buildScheduleButtons(),
             isRollup ? null : Box.createVerticalStrut(2),
             buildMainButtons(isRollup));

        JSplitPane jsp = new JSplitPane
            (JSplitPane.VERTICAL_SPLIT, true, topBox, bottomBox);
        jsp.setResizeWeight(0.5);
        frame.getContentPane().add(jsp);

        this.dash = dash;
        this.taskListName = taskListName;
        setDirty(false);


        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    confirmClose(true); }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setSize(new Dimension(630, 600));
        frame.show();

        // if the task list is empty, open the add task dialog immediately.
        if (((EVTask) model.getRoot()).isLeaf())
            addTask();
    }

    private boolean isRollup() { return model.isRollup(); }

    private boolean isDirty = false;
    protected void setDirty(boolean dirty) {
        isDirty = dirty;
        saveButton.setEnabled(isDirty);
        closeButton.setText(isDirty ? "Cancel" : "Close");
    }

    protected Component buildTaskButtons(boolean isRollup) {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        addTaskButton = new JButton
            (isRollup ? "Add Schedule..." : "Add Task...");
        addTaskButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addTask(); }});
        result.add(addTaskButton);
        result.add(Box.createHorizontalGlue());

        deleteTaskButton = new JButton
            (isRollup ? "Delete Schedule..." : "Delete Task...");
        deleteTaskButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteTask(); }});
        deleteTaskButton.setEnabled(false);
        result.add(deleteTaskButton);
        result.add(Box.createHorizontalGlue());

        moveUpButton = new JButton
            (isRollup ? "Move Schedule Up" : "Move Task Up");
        moveUpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveTaskUp(); }});
        moveUpButton.setEnabled(false);
        result.add(moveUpButton);
        result.add(Box.createHorizontalGlue());

        moveDownButton = new JButton
            (isRollup ? "Move Schedule Down" : "Move Task Down");
        moveDownButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveTaskDown(); }});
        moveDownButton.setEnabled(false);
        result.add(moveDownButton);
        result.add(Box.createHorizontalGlue());

        treeTable.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableTaskButtons(e); }});
        return result;
    }

    protected Component buildScheduleButtons() {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());

        addPeriodButton = new JButton("Add Row");
        addPeriodButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addScheduleRow(); }});
        result.add(addPeriodButton);
        result.add(Box.createHorizontalGlue());

        insertPeriodButton = new JButton("Insert Row");
        insertPeriodButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    insertScheduleRow(); }});
        insertPeriodButton.setEnabled(false);
        result.add(insertPeriodButton);
        result.add(Box.createHorizontalGlue());

        deletePeriodButton = new JButton("Delete Row");
        deletePeriodButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteScheduleRows(); }});
        deletePeriodButton.setEnabled(false);
        result.add(deletePeriodButton);
        result.add(Box.createHorizontalGlue());

        scheduleTable.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableScheduleButtons(); }});

        return result;
    }

    protected Component buildMainButtons(boolean isRollup) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        result.setBorder(BorderFactory.createRaisedBevelBorder());

        /*
        if (isRollup) {
            recalcButton = new JButton("Refresh");
            recalcButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recalcAll(); }});
            result.add(recalcButton);
        }
        */

        chartButton = new JButton("Chart");
        chartButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showChart(); }});
        result.add(chartButton);

        reportButton = new JButton("Report");
        reportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showHTML(); }});
        result.add(reportButton);

        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    confirmClose(true); }});
        result.add(closeButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    save(); }});
        result.add(saveButton);

        Dimension size = result.getMinimumSize();
        size.width = 2000;
        result.setMaximumSize(size);

        return result;
    }

    private Box newHBox(Component a, Component b) {
        Box result = Box.createHorizontalBox();
        result.add(a); result.add(b);
        return result;
    }

    private Box newVBox(Component a, Component b) {
        return newVBox(a, b, null, null, null); }
    private Box newVBox(Component a, Component b, Component c) {
        return newVBox(a, b, c, null, null); }
    private Box newVBox(Component a, Component b, Component c, Component d) {
        return newVBox(a, b, c, d, null); }
    private Box newVBox(Component a, Component b, Component c, Component d,
                        Component e) {
        Box result = Box.createVerticalBox();
        if (a != null) result.add(a);
        if (b != null) result.add(b);
        if (c != null) result.add(c);
        if (d != null) result.add(d);
        if (e != null) result.add(e);
        return result;
    }

    public void show() { frame.show(); frame.toFront(); }


    private Color mixColors(Color a, Color b, float r) {
        float s = 1.0f - r;
        return new Color((a.getRed()   * r + b.getRed()   * s) / 255f,
                         (a.getGreen() * r + b.getGreen() * s) / 255f,
                         (a.getBlue()  * r + b.getBlue()  * s) / 255f);
    }
    Color editableColor, selectedEditableColor;
    Color expandedColor, automaticColor;

    class TaskJTreeTable extends JTreeTable {

        DefaultTableCellRenderer editable, readOnly;
        TreeTableModel model;

        public TaskJTreeTable(TreeTableModel m) {
            super(m);
            model = m;

            editableColor =
                mixColors(getBackground(), Color.yellow, 0.6f);
            selectedEditableColor =
                mixColors(getSelectionBackground(), editableColor, 0.4f);
            expandedColor =
                mixColors(getBackground(), getForeground(), 0.8f);

            editable = new TaskTableRenderer(selectedEditableColor,
                                             editableColor,
                                             getForeground());

            readOnly = new TaskTableRenderer(getSelectionBackground(),
                                             getBackground(),
                                             expandedColor);
        }


        public TableCellRenderer getCellRenderer(int row, int column) {
            TableCellRenderer result = super.getCellRenderer(row, column);

            if (result instanceof JTreeTable.TreeTableCellRenderer)
                return result;

            if (row < 0) return readOnly;

            TreePath path = getTree().getPathForRow(row);
            if (path != null &&
                model.isCellEditable(path.getLastPathComponent(),
                                     convertColumnIndexToModel(column)))
                return editable;
            else
                return readOnly;
        }

        class TaskTableRenderer extends ShadedTableCellRenderer {
            public TaskTableRenderer(Color sel, Color desel, Color fg) {
                super(sel, desel, fg);
            }
            protected boolean useAltForeground(int row) {
                return getTree().isExpanded(row);
            }
        }

        public Component prepareEditor(TableCellEditor editor,
                                       int row,
                                       int column) {
            Component result = super.prepareEditor(editor, row, column);
            result.setBackground(selectedEditableColor);
            if (result instanceof JTextComponent)
                ((JTextComponent) result).selectAll();
            return result;
        }
    }

    class ScheduleJTable extends JTable {
        DefaultTableCellRenderer editable, readOnly;
        EVSchedule model;

        public ScheduleJTable(EVSchedule model) {
            super(model);
            this.model = model;

            editable = new ScheduleTableRenderer(selectedEditableColor,
                                                 editableColor,
                                                 Color.gray);

            readOnly = new ScheduleTableRenderer(getSelectionBackground(),
                                                 getBackground(),
                                                 Color.gray);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            TableCellRenderer result = super.getCellRenderer(row, column);

            if (result instanceof JTreeTable.TreeTableCellRenderer)
                return result;

            if (row < 0) return readOnly;

            if (model.isCellEditable(row, convertColumnIndexToModel(column)))
                return editable;
            else
                return readOnly;
        }

        class ScheduleTableRenderer extends ShadedTableCellRenderer {
            public ScheduleTableRenderer(Color sel, Color desel, Color fg) {
                super(sel, desel, fg);
            }
            protected boolean useAltForeground(int row) {
                return model.rowIsAutomatic(row);
            }
        }

        public Component prepareEditor(TableCellEditor editor,
                                       int row,
                                       int column) {
            Component result = super.prepareEditor(editor, row, column);
            result.setBackground(selectedEditableColor);
            if (result instanceof JTextComponent)
                ((JTextComponent) result).selectAll();
            return result;
        }
    }

    class ShadedTableCellRenderer extends DefaultTableCellRenderer {
        /** The color to use in this renderer if the cell is selected. */
        Color selectedBackgroundColor;
        /** The color to use in this renderer if the cell is not selected. */
        Color backgroundColor;
        /** The alternate foreground color to use when useAlt returns true. */
        Color altForeground;

        public ShadedTableCellRenderer(Color sel, Color desel, Color fg) {
            selectedBackgroundColor = sel;
            backgroundColor = desel;
            altForeground = fg;
        }

        protected boolean useAltForeground(int row) { return false; }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, column);
            Color bg = null;
            if (isSelected)
                result.setBackground(bg = selectedBackgroundColor);
            else
                result.setBackground(bg = backgroundColor);
            result.setForeground(useAltForeground(row) ?
                                 altForeground : table.getForeground());

            // This step is necessary because the DefaultTableCellRenderer
            // may have incorrectly set the "opaque" flag.
            if (result instanceof JComponent) {
                boolean colorMatch = (bg != null) &&
                    ( bg.equals(table.getBackground()) ) && table.isOpaque();
                ((JComponent)result).setOpaque(!colorMatch);
            }

            return result;
        }

        public void setValue(Object value) {
            if (value instanceof Date) {
                setHorizontalAlignment(SwingConstants.LEFT);
                setText(EVSchedule.formatDate((Date) value));
            } else {
                setHorizontalAlignment(SwingConstants.RIGHT);
                super.setValue(value);
            }
        }
    }


    public void evNodeChanged(EVTask node) {
        TreePath tp = new TreePath(node.getPath());
        int row = treeTable.getTree().getRowForPath(tp);
        if (row != -1) {
            AbstractTableModel model =
                (AbstractTableModel) treeTable.getModel();
            model.fireTableChanged(new TableModelEvent(model, row));
        }
    }

    public void evScheduleChanged() {
        setDirty(true);
        recalcAll();
    }

    protected void recalcAll() {
        // recalculate all the data. This will automatically fire
        // appropriate events on the EVSchedule table.
        model.recalc();
    }

    public void evRecalculated(EventObject e) {

        // We have to manually generate events for the JTreeTable,
        // since it has installed some wrapper object to convert the
        // TreeTableModel into a TableModel.
        AbstractTableModel model =
            (AbstractTableModel) treeTable.getModel();
        model.fireTableChanged(new TableModelEvent(model, 0,
                                                   treeTable.getRowCount()-1));

        // Since rows may have been added or deleted to the schedule, and
        // rows may have changed to or from automatic rows, update the
        // buttons appropriately.
        enableScheduleButtons();
    }

    /** Display a dialog allowing the user to choose a hierarchy task,
     *  then add the selected task to the task list as a child of the
     *  task tree root. */
    protected void addTask() {
        String path = null;
        if (isRollup()) {
            path = chooseTaskList();
        } else {
            NodeSelectionDialog dialog = new NodeSelectionDialog
                (frame, dash.props, "Add Task",
                 "Choose a project/task to add to the task list", "Add", null);
            path = dialog.getSelectedPath();
        }
        if (path != null && model.addTask(path, dash.data, dash.props, true)) {
            treeTable.getTree().expandRow(0);
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }

    private String chooseTaskList() {
        String[] taskListNames =
            EVTaskList.findTaskLists(dash.data, true, true);
        String[] taskListDisplayNames = getDisplayNames(taskListNames);
        JList taskLists = new JList(taskListDisplayNames);
        JScrollPane sp = new JScrollPane(taskLists);
        sp.setPreferredSize(new Dimension(200, 200));
        Object message = new Object[] {
            "Choose a task & schedule template:", sp };
        if (JOptionPane.showConfirmDialog(frame, message, "Add Schedule",
                                          JOptionPane.OK_CANCEL_OPTION)
            == JOptionPane.OK_OPTION) {
            int selIndex = taskLists.getSelectedIndex();
            return (selIndex == -1 ? null : taskListNames[selIndex]);
        }
        return null;
    }
    private String[] getDisplayNames(String[] taskListNames) {
        String[] result = new String[taskListNames.length];
        for (int i = result.length;   i-- > 0;  )
            result[i] = EVTaskList.cleanupName(taskListNames[i]);
        return result;
    }

    /** Delete the currently selected task.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void deleteTask() {
        TreePath selPath = treeTable.getTree().getSelectionPath();
        if (selPath == null || selPath.getPathCount() != 2) return;

        // make the change.
        if (confirmDelete(selPath) && model.removeTask(selPath)) {
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }
    protected boolean confirmDelete(TreePath selPath) {
        EVTask task = (EVTask) selPath.getLastPathComponent();
        String fullName = isRollup() ? task.getName() : task.getFullName();
        String[] message = new String[] {
            "Are you certain you want to delete the " +
                (isRollup() ? "schedule," : "task,"),
            "        '" + fullName + "'",
            "from this task list?" };
        return (JOptionPane.showConfirmDialog
                (frame, message,
                 isRollup() ? "Delete Schedule?" : "Delete Task?",
                 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION);
    }

    /** Swap the currently selected task with its previous sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskUp() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath))) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row));
        }
    }

    /** Swap the currently selected task with its next sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskDown() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath)+1)) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row));
        }
    }

    private class RowSelectionTask implements Runnable {
        private int row;
        public RowSelectionTask(int row) { this.row = row; }
        public void run() {
            if (row != -1) {
                treeTable.getSelectionModel().setSelectionInterval(row, row);
                enableTaskButtons();
            }
        }
    }

    protected int selectedTaskPos(TreePath selPath) {
        if (selPath == null) return -1;

        int pathLen = selPath.getPathCount();
        if (pathLen != 2) return -1; // only adjust children of the root node.
        EVTask selectedTask = (EVTask) selPath.getPathComponent(pathLen-1);
        EVTask parentNode = (EVTask) selPath.getPathComponent(pathLen-2);
        return parentNode.getChildIndex(selectedTask);
    }

    protected void enableTaskButtons(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting())
            SwingUtilities.invokeLater(new EnableTaskButtons());
    }
    private class EnableTaskButtons implements Runnable {
        public void run() { enableTaskButtons(); }
    }

    protected void enableTaskButtons() {
        enableTaskButtons(treeTable.getTree().getSelectionPath());
    }

    protected void enableTaskButtons(TreePath selectionPath) {
        boolean enableDelete = false, enableUp = false, enableDown = false;
        int pos = selectedTaskPos(selectionPath);
        if (pos != -1) {
            int numKids = ((EVTask) model.getRoot()).getNumChildren();

            enableDelete = true;
            enableUp     = (pos > 0);
            enableDown   = (pos < numKids-1);
        }
        deleteTaskButton.setEnabled(enableDelete);
        moveUpButton    .setEnabled(enableUp);
        moveDownButton  .setEnabled(enableDown);
    }


    protected void addScheduleRow() {
        model.getSchedule().addRow();
        setDirty(true);
    }
    protected void insertScheduleRow() {
        int[] rows = scheduleTable.getSelectedRows();
        if (rows == null || rows.length == 0) return;
        model.getSchedule().insertRow(rows[0]);
        setDirty(true);
    }
    protected void deleteScheduleRows() {
        int[] rows = scheduleTable.getSelectedRows();
        if (rows == null || rows.length == 0) return;
        for (int i = rows.length;  i-- > 0; )
            model.getSchedule().deleteRow(rows[i]);
        setDirty(true);
    }

    protected void enableScheduleButtons() {
        if (isRollup()) return;

        boolean enableDelete = false, enableInsert = false;
        int[] rows = scheduleTable.getSelectedRows();
        enableInsert = (rows != null && rows.length > 0);
        if (enableInsert) for (int i = rows.length;  i-- > 0; )
            if (!model.getSchedule().rowIsAutomatic(rows[i])) {
                enableDelete = true; break;
            }

        insertPeriodButton.setEnabled(enableInsert);
        deletePeriodButton.setEnabled(enableDelete);
    }

    protected void close() {
        TaskScheduleChooser.close(taskListName);
        frame.dispose();
        model.setNodeListener(null);
        model.removeRecalcListener(this);
        model.getSchedule().setListener(null);
        model = null;
        treeTable = null;
        scheduleTable = null;
    }

    protected void save() {
        model.save();
        setDirty(false);
    }

    protected void configureEditor(JTable table) {
        //configureEditor(table.getDefaultEditor(String.class));
        //configureEditor(table.getDefaultEditor(Date.class));
    }/*
    protected void configureEditor(TableCellEditor e) {
        if (e instanceof DefaultCellEditor)
            //((DefaultCellEditor)e).setClickCountToStart(1);
            ((DefaultCellEditor)e).addCellEditorListener(this);
            }*/

    public void showChart() {
        if (chartDialog != null && chartDialog.isDisplayable()) {
            chartDialog.show();
            chartDialog.toFront();
        } else
            chartDialog = new TaskScheduleChart(this);
    }

    public static final String CHART_URL = "//reports/ev.class";
    public void showHTML() {
        if (saveOrCancel(true)) {
            String uri = "/" + URLEncoder.encode(taskListName) + CHART_URL;
            Browser.launch(uri);
        }
    }

    private static final Object CONFIRM_CLOSE_MSG =
        "Do you want to save the changes you made to this " +
        "task & schedule template?";

    public void confirmClose(boolean showCancel) {
        if (saveOrCancel(showCancel))
            close();
    }
    public boolean saveOrCancel(boolean showCancel) {
        if (isDirty)
            switch (JOptionPane.showConfirmDialog
                    (frame, CONFIRM_CLOSE_MSG, "Save Changes?",
                     showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false;                 // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                save();                 // save changes.
            }
        return true;
    }

}
