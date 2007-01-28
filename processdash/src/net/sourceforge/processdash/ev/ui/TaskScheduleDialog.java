// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListCached;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.CachedObject;
import net.sourceforge.processdash.net.cache.CachedURLObject;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.NodeSelectionDialog;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.PaintUtils;
import net.sourceforge.processdash.ui.lib.DeferredSelectAllExecutor;
import net.sourceforge.processdash.ui.lib.ErrorReporter;
import net.sourceforge.processdash.ui.lib.JTreeTable;
import net.sourceforge.processdash.ui.lib.ToolTipTableCellRendererProxy;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.util.BooleanArray;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


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
    protected DashboardContext dash;
    protected String taskListName;

    private EVTaskList.FlatTreeModel flatModel = null;
    private TreeTableModel mergedModel = null;
    private TableColumnModel treeColumnModel;
    private TableColumnModel flatColumnModel = null;

    protected JButton addTaskButton, deleteTaskButton, moveUpButton,
        moveDownButton, addPeriodButton, insertPeriodButton,
        deletePeriodButton, chartButton, reportButton, closeButton,
        saveButton, recalcButton, errorButton, indivChartButton,
        collaborateButton;
    protected JCheckBox flatViewCheckbox;
    protected JCheckBox mergedViewCheckbox;

    protected boolean disableTaskPruning;

    protected JFrame chartDialog = null;

    protected Resources resources = Resources.getDashBundle("EV");

    private static Preferences preferences = Preferences.userNodeForPackage(TaskScheduleDialog.class);
    private static final String EXPANDED_NODES_KEY_SUFFIX = "_EXPANDEDNODES";
    private static final String EXPANDED_NODES_DELIMITER = Character.toString('\u0001');

    public TaskScheduleDialog(DashboardContext dash, String taskListName,
                              boolean createRollup) {
        this.dash = dash;
        this.taskListName = taskListName;

        // Create the frame and set an appropriate icon
        frame = new JFrame(resources.getString("Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());
        PCSH.enableHelpKey(frame, "UsingTaskSchedule");

        // Try to open an existing earned value model.
        model = EVTaskList.openExisting
            (taskListName, dash.getData(), dash.getHierarchy(),
             dash.getCache(), true);

        // If the earned value model doesn't already exist, create a new one.
        if (model == null) {
            if (createRollup)
                model = new EVTaskListRollup
                    (taskListName, dash.getData(), dash.getHierarchy(),
                     dash.getCache(),true);
            else
                model = new EVTaskListData
                    (taskListName, dash.getData(), dash.getHierarchy(), true);
        }

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                dash.getData(), dash.getHierarchy(), dash.getCache());
        model.setDependencyCalculator(depCalc);

        model.recalc();
        model.setNodeListener(this);
        model.addRecalcListener(this);
        model.getSchedule().setListener(this);

        // Create a JTreeTable to display the task list.
        treeTable = new TaskJTreeTable(model);
        treeTable.setShowGrid(true);
        treeTable.setIntercellSpacing(new Dimension(1, 1));
        treeTable.getSelectionModel().setSelectionMode
            (ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (treeTable, EVTaskList.toolTips);
                                // set default widths for the columns
        List hiddenCols = new LinkedList();
        if (!Settings.getBool("ev.showCumulativeTaskData", false)) {
            hiddenCols.add(new Integer(EVTaskList.PLAN_CUM_TIME_COLUMN));
            hiddenCols.add(new Integer(EVTaskList.PLAN_CUM_VALUE_COLUMN));
        }
        if (!(model instanceof EVTaskListRollup))
            hiddenCols.add(new Integer(EVTaskList.ASSIGNED_TO_COLUMN));

        int totalWidth = 0;
        treeColumnModel = treeTable.getColumnModel();
        for (int i = 0;  i < EVTaskList.colWidths.length;  i++) {
            int width = EVTaskList.colWidths[i];
            if (hiddenCols.contains(new Integer(i))) {
                width = 0;
                treeColumnModel.getColumn(i).setMinWidth(0);
                treeColumnModel.getColumn(i).setMaxWidth(0);
            }
            treeColumnModel.getColumn(i).setPreferredWidth(width);
            totalWidth += width;
        }
        configureEditor(treeTable);

        // Create a JTable to display the schedule list.
        scheduleTable = new ScheduleJTable(model.getSchedule());
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (scheduleTable, model.getSchedule().getColumnTooltips());
                                // set default widths for the columns
        for (int i = 0;  i < EVSchedule.colWidths.length;  i++)
            scheduleTable.getColumnModel().getColumn(i)
                .setPreferredWidth(EVSchedule.colWidths[i]);
        configureEditor(scheduleTable);

        // possibly hide the direct columns if they aren't needed
        showHideDirectColumns();

        // record user preference for pruning enablement
        disableTaskPruning = Settings.getBool("ev.disablePruning", false);

        boolean isRollup = isRollup();
        if (isRollup)
            frame.setTitle(resources.getString("Window_Rollup_Title"));
        else
            readExpandedNodesPref(model.getID());

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
             isRollup || Settings.isReadOnly() ? null : buildScheduleButtons(),
             isRollup || Settings.isReadOnly() ? null : Box.createVerticalStrut(2),
             buildMainButtons(isRollup));

        JSplitPane jsp = new JSplitPane
            (JSplitPane.VERTICAL_SPLIT, true, topBox, bottomBox);
        jsp.setResizeWeight(0.5);
        frame.getContentPane().add(jsp);

        setDirty(false);


        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    confirmClose(true); }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setSize(new Dimension(totalWidth + 20, 600));
        frame.show();

        // if the task list is empty, open the add task dialog immediately.
        if (((EVTask) model.getRoot()).isLeaf() && Settings.isReadWrite())
            addTask();
        else {
            if (getErrors() != null)
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Map errors = getErrors();
                            highlightErrors(errors);
                            displayErrorDialog(errors);
                        } } );
        }
    }


    private boolean isRollup() {
        return (model instanceof EVTaskListRollup);
    }

    protected boolean isFlatView() {
        return (flatViewCheckbox != null && flatViewCheckbox.isSelected());
    }

    protected boolean isMergedView() {
        return (mergedViewCheckbox != null && mergedViewCheckbox.isSelected());
    }

    private boolean isDirty = false;
    protected void setDirty(boolean dirty) {
        isDirty = dirty;
        saveButton.setEnabled(isDirty);
        closeButton.setText(isDirty ? resources.getString("Cancel")
                                    : resources.getString("Close"));
    }

    protected Component buildTaskButtons(boolean isRollup) {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        addTaskButton = new JButton
            (isRollup ? resources.getString("Buttons.Add_Schedule")
                      : resources.getString("Buttons.Add_Task"));
        addTaskButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addTask(); }});
        if (Settings.isReadWrite())
            result.add(addTaskButton);
        result.add(Box.createHorizontalGlue());

        // button margins: 2 pixels top and bottom, 14 left and right.

        deleteTaskButton = new JButton
            (isRollup ? resources.getString("Buttons.Remove_Schedule")
                      : resources.getString("Buttons.Remove_Task"));
        deleteTaskButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteTask(); }});
        deleteTaskButton.setEnabled(false);
        if (Settings.isReadWrite())
            result.add(deleteTaskButton);
        result.add(Box.createHorizontalGlue());


        moveUpButton = new JButton
            (isRollup ? resources.getString("Buttons.Move_Schedule_Up")
                      : resources.getString("Buttons.Move_Task_Up"));
        moveUpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveTaskUp(); }});
        moveUpButton.setEnabled(false);
        moveUpButton.setMnemonic('U');
        if (Settings.isReadWrite())
            result.add(moveUpButton);
        result.add(Box.createHorizontalGlue());

        moveDownButton = new JButton
            (isRollup ? resources.getString("Buttons.Move_Schedule_Down")
                      : resources.getString("Buttons.Move_Task_Down"));
        moveDownButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveTaskDown(); }});
        moveDownButton.setEnabled(false);
        moveDownButton.setMnemonic('D');
        if (Settings.isReadWrite())
            result.add(moveDownButton);
        result.add(Box.createHorizontalGlue());

        flatViewCheckbox = mergedViewCheckbox = null;
        if (!isRollup) {
            flatViewCheckbox = new JCheckBox
                (resources.getString("Buttons.Flat_View"));
            flatViewCheckbox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        toggleFlatView(); }});
            flatViewCheckbox.setSelected(false);
            flatViewCheckbox.setFocusPainted(false);
            flatViewCheckbox.setMnemonic('F');
            result.add(flatViewCheckbox);
            result.add(Box.createHorizontalGlue());
        } else {
            mergedViewCheckbox = new JCheckBox
                (resources.getString("Buttons.Merged_View"));
            mergedViewCheckbox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        toggleMergedView(); }});
            mergedViewCheckbox.setSelected(false);
            mergedViewCheckbox.setFocusPainted(false);
            mergedViewCheckbox.setMnemonic('M');
            result.add(mergedViewCheckbox);
            result.add(Box.createHorizontalGlue());
        }

        treeTable.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableTaskButtons(e); }});
        return result;
    }


    protected Component buildScheduleButtons() {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());

        addPeriodButton = new JButton
            (resources.getString("Buttons.Add_Schedule_Row"));
        addPeriodButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addScheduleRow(); }});
        result.add(addPeriodButton);
        result.add(Box.createHorizontalGlue());

        insertPeriodButton = new JButton
            (resources.getString("Buttons.Insert_Schedule_Row"));
        insertPeriodButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    insertScheduleRow(); }});
        insertPeriodButton.setEnabled(false);
        result.add(insertPeriodButton);
        result.add(Box.createHorizontalGlue());

        deletePeriodButton = new JButton
            (resources.getString("Buttons.Delete_Schedule_Row"));
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
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createRaisedBevelBorder());

        Box box = Box.createHorizontalBox();
        result.add(newVBox(Box.createVerticalStrut(2),
                           box,
                           Box.createVerticalStrut(2)), BorderLayout.CENTER);
        box.add(Box.createHorizontalStrut(2));

        collaborateButton = new JButton
            (resources.getString("Buttons.Collaborate"));
        collaborateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCollaborationWizard(); }});
        if (Settings.isReadWrite())
            box.add(collaborateButton);

        box.add(Box.createHorizontalGlue());

        /*
        if (isRollup) {
            recalcButton = new JButton("Refresh");
            recalcButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recalcAll(); }});
            box.add(recalcButton);
        }
        */

        errorButton = new JButton(resources.getString("Buttons.Errors"));
        errorButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    displayErrorDialog(getErrors()); }});
        errorButton.setBackground(Color.red);
        errorButton.setFocusPainted(false);
        errorButton.setVisible(getErrors() != null);
        box.add(errorButton);
        box.add(Box.createHorizontalStrut(2));

        if (isRollup) {
            indivChartButton = new JButton
                (resources.getString("Buttons.Indiv_Chart"));
            indivChartButton.setEnabled(false);
            indivChartButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showIndivChart(); }});
            box.add(indivChartButton);
            box.add(Box.createHorizontalStrut(2));
        }

        chartButton = new JButton(resources.getString("Buttons.Chart"));
        chartButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showChart(); }});
        box.add(chartButton);
        box.add(Box.createHorizontalStrut(2));

        reportButton = new JButton(resources.getString("Buttons.Report"));
        reportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showHTML(); }});
        box.add(reportButton);
        box.add(Box.createHorizontalStrut(2));

        closeButton = new JButton(resources.getString("Close"));
        closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    confirmClose(true); }});
        box.add(closeButton);
        box.add(Box.createHorizontalStrut(2));

        saveButton = new JButton(resources.getString("Save"));
        saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    save(); }});
        if (Settings.isReadWrite()) {
            box.add(saveButton);
            box.add(Box.createHorizontalStrut(2));
        }

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

    protected Map getErrors() {
        return model.getSchedule().getMetrics().getErrors();
    }

    protected void highlightErrors(Map errors) {
        if (errors == null || errors.size() == 0) return;
        Iterator i = errors.values().iterator();
        while (i.hasNext()) {
            EVTask t = (EVTask) i.next();
            treeTable.getTree().makeVisible(new TreePath(t.getPath()));
        }
    }

    protected void displayErrorDialog(Map errors) {
        if (errors == null || errors.size() == 0) return;
        ErrorReporter err = new ErrorReporter
            (resources.getString("Error_Dialog.Title"),
             resources.getStrings("Error_Dialog.Head"),
             resources.getStrings("Error_Dialog.Foot"));
        Iterator i = errors.keySet().iterator();
        while (i.hasNext())
            err.logError((String) i.next());
        err.done();
    }


    Color editableColor, selectedEditableColor;
    Color expandedColor, automaticColor;

    class TaskJTreeTable extends JTreeTable {

        DefaultTableCellRenderer editable, readOnly, dependencies;
        TaskDependencyCellEditor dependencyEditor;
        TreeTableModel model;
        BooleanArray cutList;

        public TaskJTreeTable(TreeTableModel m) {
            super(m);
            model = m;
            cutList = new BooleanArray();

            editableColor =
                PaintUtils.mixColors(getBackground(), Color.yellow, 0.6);
            selectedEditableColor =
                PaintUtils.mixColors(getSelectionBackground(), editableColor, 0.4);
            expandedColor =
                PaintUtils.mixColors(getBackground(), getForeground(), 0.8);

            editable = new TaskTableRenderer(selectedEditableColor,
                                             editableColor,
                                             getForeground());

            readOnly = new TaskTableRenderer(getSelectionBackground(),
                                             getBackground(),
                                             expandedColor);
            dependencies = new DependencyCellRenderer(getSelectionBackground(),
                                             getBackground(),
                                             selectedEditableColor,
                                             editableColor);

            dependencyEditor = new TaskDependencyCellEditor(
                    TaskScheduleDialog.this, dash);
            getColumnModel().getColumn(EVTaskList.DEPENDENCIES_COLUMN)
                    .setCellEditor(dependencyEditor);

            TaskJTreeTableCellRenderer r =
                new TaskJTreeTableCellRenderer(getTree().getCellRenderer(),
                        cutList);
            getTree().setCellRenderer(r);
            ToolTipManager.sharedInstance().registerComponent(getTree());
            new ToolTipTimingCustomizer().install(this);
            setTransferHandler(new TransferSupport());
            setAutoscrolls(true);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            int modelCol = convertColumnIndexToModel(column);
            if (modelCol == EVTaskList.DEPENDENCIES_COLUMN)
                return dependencies;

            TableCellRenderer result = super.getCellRenderer(row, column);
            if (result instanceof JTreeTable.TreeTableCellRenderer)
                return result;

            if (row < 0) return readOnly;

            TreePath path = getTree().getPathForRow(row);
            if (path != null &&
                model.isCellEditable(path.getLastPathComponent(), modelCol))
                return editable;
            else
                return readOnly;
        }

        class TaskTableRenderer extends ShadedTableCellRenderer {
            private Font regular = null, bold = null;
            public TaskTableRenderer(Color sel, Color desel, Color fg) {
                super(sel, desel, fg);
            }
            protected boolean useAltForeground(int row) {
                return getTree().isExpanded(row);
            }
            private Font getFont(boolean bold, Component c) {
                if (this.regular == null) {
                    Font base = c.getFont();
                    if (base == null) return null;
                    this.regular = base.deriveFont(Font.PLAIN);
                    this.bold    = base.deriveFont(Font.BOLD);
                }
                return (bold ? this.bold : this.regular);
            }
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component result = super.getTableCellRendererComponent
                    (table, value, isSelected, hasFocus, row, column);
                String errorStr = null;

                TreePath path = getTree().getPathForRow(row);
                EVTask node = null;
                if (path != null) {
                    if (path.getLastPathComponent() instanceof EVTask)
                        node = (EVTask) path.getLastPathComponent();
                    errorStr = ((EVTaskList) model).getErrorStringAt
                        (path.getLastPathComponent(),
                         convertColumnIndexToModel(column));
                }

                if (result instanceof JComponent)
                    ((JComponent) result).setToolTipText(errorStr);

                if (errorStr != null)
                    result.setForeground(errorStr.startsWith(" ")
                                         ? Color.orange : Color.red);
                else if (node != null && node.isUserPruned())
                    result.setForeground(PHANTOM_COLOR);
                else if (node != null && node.isChronologicallyPruned())
                    result.setForeground(SEPIA);

                Font f = getFont(errorStr != null, result);
                if (f != null) result.setFont(f);

                return result;
            }
        }

        class DependencyCellRenderer extends DefaultTableCellRenderer {

            private Color[] colors;

            public DependencyCellRenderer(Color selRO, Color deselRO,
                    Color sel, Color desel) {

                colors = new Color[] { sel, desel, selRO, deselRO };
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component result = super.getTableCellRendererComponent(table,
                        null, isSelected, hasFocus, row, column);

                boolean isEditable = false;
                TreePath path = getTree().getPathForRow(row);
                if (path != null &&
                    model.isCellEditable(path.getLastPathComponent(),
                            table.convertColumnIndexToModel(column)))
                    isEditable = true;

                setBackground(colors[(isSelected ? 0:1) + (isEditable ? 0:2)]);
                setHorizontalAlignment(SwingConstants.CENTER);

                TaskDependencyAnalyzer.GUI analyzer =
                    new TaskDependencyAnalyzer.GUI(value);
                analyzer.syncLabel(this);

                return result;
            }
        }

        public boolean editCellAt(int row, int column, EventObject e) {
            boolean result = super.editCellAt(row, column, e);

            if (result == true && e instanceof MouseEvent)
                DeferredSelectAllExecutor.register(getEditorComponent());

            return result;
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

        public void setTreeTableModel(TreeTableModel treeTableModel) {
            cutList.clear();

            TreeCellRenderer r = getTree().getCellRenderer();

            super.setTreeTableModel(treeTableModel);

            getTree().setCellRenderer(r);
            ToolTipManager.sharedInstance().registerComponent(getTree());
        }


        private void setCutList(int[] rows) {
            cutList.clear();
            if (rows != null)
                for (int i = rows.length; i-- > 0; )
                    cutList.set(rows[i], true);
            if (treeTable != null)
                tableChanged(new TableModelEvent(treeTable.getModel(), 1,
                        treeTable.getRowCount()-1));
        }

        private class TransferSupport extends TransferHandler implements
                ClipboardOwner {

            public TransferSupport() {
                InputMap inputMap = getInputMap();

                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                        Event.CTRL_MASK), "CutCopyNodes");
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                        Event.CTRL_MASK), "CutCopyNodes");
                getActionMap().put("CutCopyNodes", getCopyAction());

                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                        Event.CTRL_MASK), "PasteNodes");
                getActionMap().put("PasteNodes", getPasteAction());
            }

            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (int i = 0; i < flavors.length; i++) {
                    if (flavors[i] == DataFlavor.stringFlavor)
                        return true;
                }
                return false;
            }

            public void exportToClipboard(JComponent comp, Clipboard clip,
                    int action) {
                // we have to override this method, because we need to register
                // a ClipboardOwner when we set the clipboard contents.
                Transferable t = createTransferable(comp);
                if (t != null) {
                    clip.setContents(t, this);
                    exportDone(comp, t, action);
                } else {
                    exportDone(comp, null, NONE);
                }
            }

            protected Transferable createTransferable(JComponent c) {
                if (!isFlatView())
                    return null;

                int[] rows = treeTable.getSelectedRows();
                if (rows == null || rows.length == 0
                        || (rows.length == 1 && rows[0] == 0))
                    return null;

                StringBuffer data = new StringBuffer();
                for (int i = 0; i < rows.length; i++) {
                    Object taskName = treeTable.getValueAt(rows[i],
                            EVTaskList.TASK_COLUMN);
                    data.append(taskName).append('\n');
                }

                if (isEditing())
                    removeEditor();
                setCutList(rows);

                return new StringSelection(data.toString());
            }

            public void lostOwnership(Clipboard clipboard, Transferable t) {
                setCutList(null);
            }

            public boolean importData(JComponent comp, Transferable t) {
                if (!isFlatView())
                    return false;

                int insertionRow = treeTable.getSelectedRow();
                if (insertionRow == -1) return false;
                if (insertionRow == 0) insertionRow = 1;

                String tasks;
                try {
                    tasks = (String) t.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception ex) {
                    return false;
                }

                cutList.clear();

                int[] newSelection = flatModel.insertTasks(tasks, insertionRow-1);
                if (newSelection == null)
                    return false;

                setDirty(true);
                SwingUtilities.invokeLater(new RowSelectionTask(
                        newSelection[0]+1, newSelection[1]+1, false));
                return true;
            }

        }
    }

    class TaskJTreeTableCellRenderer
        extends javax.swing.tree.DefaultTreeCellRenderer {

        private Font regular = null, bold = null;
        private BooleanArray cutList;

        public TaskJTreeTableCellRenderer(TreeCellRenderer r,
                BooleanArray cutList) {
            super();
            this.cutList = cutList;
        }
        private Font getFont(boolean bold, Component c) {
            if (this.regular == null) {
                Font base = c.getFont();
                if (base == null) return null;
                this.regular = base.deriveFont(Font.PLAIN);
                this.bold    = base.deriveFont(Font.BOLD);
            }
            return (bold ? this.bold : this.regular);
        }

        /**
         * Overrides <code>DefaultTreeCellRenderer.getPreferredSize</code> to
         * return a wider preferred size value.
         *
         * Without this method, nodes displayed in bold text are
         * sometimes truncated (with "..." at the end of the label),
         * presumably because DefaultTreeCellRenderer doesn't expect
         * the font to be changing.
         *
         * Since we display in a JTreeTable table and our preferred
         * width is typically ignored entirely, we could really ask
         * for as large a preferred width as we want and not worry
         * that we'll be given too much horizontal space.
         */
        public Dimension getPreferredSize() {
            Dimension        retDimension = super.getPreferredSize();

            if(retDimension != null)
                retDimension = new Dimension((int) (retDimension.width * 1.1),
                                             retDimension.height);
            return retDimension;
        }

        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            Component result = super.getTreeCellRendererComponent
                (tree, value, selected, expanded, leaf, row, hasFocus);
            String errorStr = null;

            // find the EVTask for the indicated row
            TreePath path = tree.getPathForRow(row);
            EVTask node = null;
            if (path != null)
                node = (EVTask) path.getLastPathComponent();

            // in flat mode, display the full name of the node rather
            // than the terminal name
            if (row > 0 && node != null && tree.getModel() == flatModel)
                setText(node.getFullName());

            // retrieve the error string for this node
            if (node != null && model != null)
                errorStr = ((EVTaskList) model).getErrorStringAt
                    (node, EVTaskList.TASK_COLUMN);

            // display the error string if needed
            if (result instanceof JComponent)
                ((JComponent) result).setToolTipText(errorStr);
            if (errorStr != null)
                result.setForeground(errorStr.startsWith(" ")
                                     ? Color.orange : Color.red);
            Font f = getFont(errorStr != null, result);
            if (f != null) result.setFont(f);

            // give pruned nodes a special appearance
            if (node != null && node.isUserPruned()) {
                result.setForeground(PHANTOM_COLOR);
                if (errorStr == null && result instanceof JComponent)
                    ((JComponent) result).setToolTipText
                        (resources.getString("Task.Removed_Tooltip"));
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getPrunedIcon(expanded, leaf));

            // give chronologically pruned nodes a special appearance
            } else if (node != null && node.isChronologicallyPruned()) {
                result.setForeground(SEPIA);
                if (errorStr == null && result instanceof JComponent)
                    ((JComponent) result).setToolTipText
                        (resources.getString
                         ("Task.Previously_Completed_Tooltip"));
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getChronIcon(expanded, leaf));

            // give cut nodes a special appearance
            } else if (row > 0 && cutList.get(row)) {
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getCutLeafIcon());
                if (errorStr == null && result instanceof JComponent) {
                    ((JComponent) result).setToolTipText
                        (resources.getString("Task.Copied_Tooltip"));
                    result.setForeground(Color.BLUE);
                }
            }

            return result;
        }
        private Icon prunedOpenIcon = null;
        private Icon prunedClosedIcon = null;
        private Icon prunedLeafIcon = null;

        private Icon chronOpenIcon = null;
        private Icon chronClosedIcon = null;
        private Icon chronLeafIcon = null;

        private Icon cutLeafIcon = null;

        private Icon getPrunedIcon(boolean expanded, boolean leaf) {
            if (leaf) {
                if (prunedLeafIcon == null)
                    prunedLeafIcon = pruneIcon(getLeafIcon(), getDefaultLeafIcon(), 8);
                return prunedLeafIcon;
            } else if (expanded) {
                if (prunedOpenIcon == null)
                    prunedOpenIcon = pruneIcon(getOpenIcon(), getDefaultOpenIcon(), 8);
                return prunedOpenIcon;
            } else {
                if (prunedClosedIcon == null)
                    prunedClosedIcon = pruneIcon(getClosedIcon(), getDefaultClosedIcon(), 8);
                return prunedClosedIcon;
            }
        }
        private Icon pruneIcon(Icon icon, Icon alt, int y) {
            if (icon == null) icon = alt;
            if (icon == null) return null;
            BufferedIcon result = new BufferedIcon(this, icon);
//            result.applyFilter(new GrayFilter(true, 85));
            result.applyFilter(PHANTOM_FILTER);
            result.setDecorator(new PruneDecorationIcon(), y);
            return result;
        }
        private Icon getChronIcon(boolean expanded, boolean leaf) {
            if (leaf) {
                if (chronLeafIcon == null)
                    chronLeafIcon = chronIcon(getLeafIcon(), getDefaultLeafIcon(), 8);
                return chronLeafIcon;
            } else if (expanded) {
                if (chronOpenIcon == null)
                    chronOpenIcon = chronIcon(getOpenIcon(), getDefaultOpenIcon(), 8);
                return chronOpenIcon;
            } else {
                if (chronClosedIcon == null)
                    chronClosedIcon = chronIcon(getClosedIcon(), getDefaultClosedIcon(), 8);
                return chronClosedIcon;
            }
        }
        private Icon chronIcon(Icon icon, Icon alt, int y) {
            if (icon == null) icon = alt;
            if (icon == null) return null;
            BufferedIcon result = new BufferedIcon(this, icon);
            result.applyFilter(SEPIA_FILTER);
            return result;
        }
        private Icon getCutLeafIcon() {
            if (cutLeafIcon == null) {
                Icon icon = getLeafIcon();
                if (icon == null) icon = getDefaultLeafIcon();
                BufferedIcon buf = new BufferedIcon(this, icon);
                buf.applyFilter(new CutItemFilter(
                        getBackgroundSelectionColor()));
                cutLeafIcon = buf;
            }
            return cutLeafIcon;
        }
    }

    private static class BufferedIcon implements Icon {
        protected Image image = null;
        protected int width = 16, height = 16;
        protected Icon decoration = null;
        private int decorationOffset = 0;

        public BufferedIcon() {}

        public BufferedIcon(Component c, Icon originalIcon) {
            width = originalIcon.getIconWidth();
            height = originalIcon.getIconHeight();
            image = new BufferedImage(width, height,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics imageG = image.getGraphics();
            originalIcon.paintIcon(c, imageG, 0, 0);
            imageG.dispose();
        }

        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        protected void doPaint(Component c, Graphics g) {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) {
                image = new BufferedImage(getIconWidth(), getIconHeight(),
                                          BufferedImage.TYPE_INT_ARGB);
                Graphics imageG = image.getGraphics();
                doPaint(c,imageG);
                imageG.dispose();
            }
            g.drawImage(image, x, y, null);
            if (decoration != null) {
                decoration.paintIcon(c, g, 0, decorationOffset);
            }
        }

        public void applyFilter(RGBImageFilter filter) {
            ImageProducer prod =
                new FilteredImageSource(image.getSource(), filter);
            this.image = Toolkit.getDefaultToolkit().createImage(prod);
        }

        public void setDecorator(Icon decoration, int y) {
            this.decoration = decoration;
            this.decorationOffset = y;
        }
    }

    // filter for creating "error" icons.  Converts to light monochrome.
    private static Color PHANTOM_COLOR = new Color(255*2/3, 255*2/3, 255*2/3);
    private static PhantomFilter PHANTOM_FILTER = new PhantomFilter();
    private static class PhantomFilter extends RGBImageFilter {
        public PhantomFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                             0.59 * ((rgb >> 8) & 0xff) +
                             0.11 * (rgb & 0xff));
            // mix one part gray with two parts white to brighten.
            gray = (gray + 255 + 255) / 3;

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            return (rgb & 0xff000000) | (0x010101 * gray);
        }
    }

    private static Color SEPIA = new Color(159, 141, 114);
    private static SepiaFilter SEPIA_FILTER = new SepiaFilter();
    private static class SepiaFilter extends RGBImageFilter {
        public SepiaFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                             0.59 * ((rgb >> 8) & 0xff) +
                             0.11 * (rgb & 0xff));

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            double p = ((double) gray) / 255.0;
            int r = linear(159, 232, p);
            int g = linear(141, 224, p);
            int b = linear(114, 205, p);

            return (rgb & 0xff000000) | (r<<16) | (g<<8) | b;
        }
        private int linear(int zero, int one, double g) {
            return 0xff & ((int) (zero + g * (one - zero)));
        }
    }

    private static class CutItemFilter extends RGBImageFilter {
        private int replacementRgb;
        public CutItemFilter(Color whiteReplacement) {
            canFilterIndexColorModel = true;
            replacementRgb = whiteReplacement.getRGB();
        }

        public int filterRGB(int x, int y, int rgb) {
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                    0.59 * ((rgb >> 8) & 0xff) +
                    0.11 * (rgb & 0xff));
            if (gray > 200)
                return (rgb & 0xff000000) | replacementRgb;
            else
                return rgb;
        }
    }


    private class PruneDecorationIcon implements Icon {
        public int getIconWidth() { return 7; }
        public int getIconHeight() { return 7; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.red);
            g.drawLine(x,   y,   x+6, y+6);
            g.drawLine(x,   y+1, x+5, y+6);
            g.drawLine(x+1, y,   x+6, y+5);
            g.drawLine(x,   y+6, x+6, y);
            g.drawLine(x,   y+5, x+5, y);
            g.drawLine(x+1, y+6, x+6, y+1);
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

        public boolean editCellAt(int row, int column, EventObject e) {
            boolean result = super.editCellAt(row, column, e);

            if (result == true && e instanceof MouseEvent)
                DeferredSelectAllExecutor.register(getEditorComponent());

            return result;
        }

        public Component prepareEditor(TableCellEditor editor,
                int row, int column) {
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


    public void evNodeChanged(final EVTask node) {
        if (SwingUtilities.isEventDispatchThread())
            handleEvNodeChanged(node);
        else try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { handleEvNodeChanged(node); } } );
        } catch (Exception e) { }
    }

    private void handleEvNodeChanged(EVTask node) {
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

        // Calculating the schedule may mean that direct time columns now
        // need to be displayed or hidden
        showHideDirectColumns();

        // Since rows may have been added or deleted to the schedule, and
        // rows may have changed to or from automatic rows, update the
        // buttons appropriately.
        enableScheduleButtons();

        // Display the error button if necessary.
        maybeDisplayErrorButton();

        // highlight any errors in the EVModel if they exist.
        highlightErrors(getErrors());
    }

    /** Display a dialog allowing the user to choose a hierarchy task,
     *  then add the selected task to the task list as a child of the
     *  task tree root. */
    protected void addTask() {
        if (Settings.isReadOnly()) return;

        String path = null;
        if (isRollup()) {
            path = chooseTaskList();
        } else {
            NodeSelectionDialog dialog = new NodeSelectionDialog
                (frame, dash.getHierarchy(),
                 resources.getString("Add_Task_Dialog.Title"),
                 resources.getString("Add_Task_Dialog.Instructions"),
                 resources.getString("Add_Task_Dialog.Button"),
                 null);
            path = dialog.getSelectedPath();
        }
        if (path != null &&
            model.addTask(path, dash.getData(), dash.getHierarchy(),
                          dash.getCache(), true)) {
            treeTable.getTree().expandRow(0);
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }


    private String chooseTaskList() {
        String[] taskListNames =
            EVTaskList.findTaskLists(dash.getData(), false, true);
        taskListNames = insertRemoveElem
            (taskListNames,
             resources.getString("Import_Schedule.New_Schedule_Option"),
             this.taskListName);
        String[] taskListDisplayNames = EVTaskList.getDisplayNames(taskListNames);
        JList taskLists = new JList(taskListDisplayNames);
        JScrollPane sp = new JScrollPane(taskLists);
        sp.setPreferredSize(new Dimension(200, 200));
        Object message = new Object[] {
            resources.getString("Add_Schedule_Dialog.Instructions"), sp };
        if (JOptionPane.showConfirmDialog
            (frame, message,
             resources.getString("Add_Schedule_Dialog.Title"),
             JOptionPane.OK_CANCEL_OPTION)
            == JOptionPane.OK_OPTION) {
            int selIndex = taskLists.getSelectedIndex();
            if (selIndex == -1)
                return null;
            else if (selIndex == 0)
                return importNewSharedSchedule();
            else
                return taskListNames[selIndex];
        }
        return null;
    }
    private String[] insertRemoveElem(String[] array, String elemToInsert,
            String elemToRemove) {
        String result[];
        if (array == null) {
            result = new String[1];
            result[0] = elemToInsert;
        } else {
            LinkedList list = new LinkedList(Arrays.asList(array));
            list.remove(elemToRemove);
            list.add(0, elemToInsert);
            result = (String[]) list.toArray(new String[list.size()]);
        }
        return result;
    }
    private class FocusHighlighter implements FocusListener {
        JTextField f;
        FocusHighlighter(JTextField field) { f = field; }
        public void focusGained(FocusEvent e) { f.selectAll(); }
        public void focusLost(FocusEvent e) { }
    }

    private String importNewSharedSchedule() {
        String urlStr = "http://";
        String passwordStr = "";
        String errorMessage = null;
        URL u = null;

        while (true) {
            // ask the user for the relevant information to locate the
            // schedule.
            JTextField url = new JTextField(urlStr, 40);
            url.addFocusListener(new FocusHighlighter(url));
            JTextField password = new JPasswordField(passwordStr, 10);
            password.addFocusListener(new FocusHighlighter(password));
            String urlLabel = resources.getString("Import_Schedule.URL_Label");
            String passwordLabel =
                resources.getString("Import_Schedule.Password_Label");
            Object message = new Object[] {
                errorMessage,
                resources.getString("Import_Schedule.Instructions"),
                newHBox(new JLabel("  "+urlLabel+" "), url),
                newHBox(new JLabel("  "+passwordLabel+" "), password) };
            if (JOptionPane.showConfirmDialog
                (frame, message,
                 resources.getString("Import_Schedule.Dialog_Title"),
                 JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
                // if the user didn't hit the OK button, return null.
                return null;

            urlStr = url.getText();
            passwordStr = password.getText();

            if (urlStr == null || urlStr.trim().length() == 0) {
                errorMessage = resources.getString
                    ("Import_Schedule.URL_Missing");
                continue;
            }
            if (urlStr.indexOf("/ev+/") != -1) {
                errorMessage = resources.getString("Import_Schedule.Pub_URL");
                continue;
            }
            try {
                u = new URL(urlStr.trim() + XML_QUERY_SUFFIX);
            } catch (MalformedURLException mue) {
                errorMessage = resources.getString
                    ("Import_Schedule.URL_Invalid");
                continue;
            }
            break;
        }

        // fetch the specified schedule.
        if (passwordStr != null) {
            passwordStr = passwordStr.trim();
            if (passwordStr.length() == 0) passwordStr = null;
        }
        CachedObject importedSchedule =
            new CachedURLObject(dash.getCache(),
                                EVTaskListCached.CACHED_OBJECT_TYPE,
                                u,
                                passwordStr == null ? null : "EV",
                                passwordStr);


        // check to see if there was an error in fetching the schedule.
        errorMessage = importedSchedule.getErrorMessage();
        String remoteName = null;
        if (errorMessage == null) {
            // if we were able to successfully fetch the schedule, try
            // to interpret its contents as an XML schedule.
            remoteName = EVTaskListCached.getNameFromXML
                (importedSchedule.getString("UTF-8"));

            // if we weren't able to interpret the fetched schedule,
            // record an error message.
            if (remoteName == null)
                errorMessage =
                    resources.getString("Import_Schedule.Invalid_Schedule");
        }

        // if there was any error, ask the user if they want to continue.
        if (errorMessage != null) {
            errorMessage = CachedURLObject.translateMessage
                (resources, "Import_Schedule.", errorMessage);
            Object message = new Object[] {
                resources.getString("Import_Schedule.Error.Header"),
                "    " + errorMessage,
                resources.getString("Import_Schedule.Error.Footer") };
            if (JOptionPane.showConfirmDialog
                (frame, message,
                 resources.getString("Import_Schedule.Error.Title"),
                 JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)
                != JOptionPane.YES_OPTION)
                return null;
        }

        // get a local name to use for this schedule.
        String localName = remoteName;
        String owner = (String) importedSchedule.getLocalAttr
            (CachedURLObject.OWNER_ATTR);
        if (owner != null)
            localName = resources.format
                ("Import_Schedule.Default_Name_FMT", localName, owner);

        do {
            localName = (String) JOptionPane.showInputDialog
                (frame,
                 resources.getStrings("Import_Schedule.Name_Dialog.Prompt"),
                 resources.getString("Import_Schedule.Name_Dialog.Title"),
                 JOptionPane.PLAIN_MESSAGE, null, null, localName);
            if (localName != null)
                localName = localName.replace('/', ' ').trim();
        } while (localName == null || localName.length() == 0);
        importedSchedule.setLocalAttr(EVTaskListCached.LOCAL_NAME_ATTR,
                                      localName);

        // return the name of the cached schedule object
        return EVTaskListCached.buildTaskListName(importedSchedule.getID(),
                                                  localName);
    }
    private static final String XML_QUERY_SUFFIX = "?xml";

    /** delete the currently selected task.
     */
    protected void deleteTask() {
        if (Settings.isReadOnly()) return;

        TreePath selPath = treeTable.getTree().getSelectionPath();
        if (selPath == null) return;
        if (isFlatView()) {
            EVTask selectedTask = (EVTask) selPath.getLastPathComponent();
            selPath = new TreePath(selectedTask.getPath());
        }

        int pathLen = selPath.getPathCount();
        if (isRollup()) {
            if (pathLen != 2 || !confirmDelete(selPath)) return;
        } else {
            if (pathLen < 2) return;
            if (pathLen == 2 && !confirmDelete(selPath)) return;
        }

        // make the change.
        if (model.removeTask(selPath)) {
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }


    protected boolean confirmDelete(TreePath selPath) {
        EVTask task = (EVTask) selPath.getLastPathComponent();
        String fullName = isRollup() ? task.getName() : task.getFullName();
        String[] message =
            resources.formatStrings(isRollup()
                                        ? "Confirm.Delete_Schedule.Prompt_FMT"
                                        : "Confirm.Delete_Task.Prompt_FMT",
                                    fullName);
        return (JOptionPane.showConfirmDialog
                (frame, message,
                 isRollup()
                     ? resources.getString("Confirm.Delete_Schedule.Title")
                     : resources.getString("Confirm.Delete_Task.Title"),
                 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION);
    }

    /** Explode the currently selected task.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
    protected void explodeTask() {
        TreePath selPath = treeTable.getTree().getSelectionPath();
        if (selPath == null || selPath.getPathCount() != 2) return;

        // make the change.
        if (confirmExplode(selPath) && model.explodeTask(selPath)) {
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }
    protected boolean confirmExplode(TreePath selPath) {
        EVTask task = (EVTask) selPath.getLastPathComponent();
        String fullName = task.getFullName();
        String[] message = new String[] {
            "By default, the task list is displayed hierarchically.",
            "If this prevents you from arranging tasks in the order",
            "they will be performed, you can \"explode\" items in your",
            "task list.",
            " ",
            "\"Exploding\" a task replaces the hierarchically organized",
            "task with a flat list of all its children.  You can",
            "then rearrange the children at will, but you will no",
            "longer be able to view them in the task list hierarchically.",
            "In addition, items added to that hierarchy in the future",
            "will no longer be automatically added to your task list.",
            " ",
            "Are you certain you want to explode the task,",
            "        '" + fullName + "'",
            "in this task list?" };
        return (JOptionPane.showConfirmDialog
                (frame, message, "Explode Task?",
                 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION);
    }
     */

    /** Swap the currently selected task with its previous sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskUp() {
        if (Settings.isReadOnly())
            return;

        if (isFlatView()) {
            moveTaskUpFlatView();
        } else {
            moveTaskUpTreeView();
        }
    }

    protected void moveTaskUpFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        if (flatModel.moveTasksUp(firstRowNum-1, lastRowNum-1)) {
            setDirty(true);
            SwingUtilities.invokeLater
                (new RowSelectionTask(firstRowNum-1, lastRowNum-1, false));
        }
    }

    private void moveTaskUpTreeView() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath, model))) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row, row, false));
        }
    }


    /** Swap the currently selected task with its next sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskDown() {
        if (Settings.isReadOnly())
            return;

        if (isFlatView()) {
            moveTaskDownFlatView();
        } else {
            moveTaskDownTreeView();
        }
    }

    protected void moveTaskDownFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        if (flatModel.moveTasksDown(firstRowNum-1, lastRowNum-1)) {
            setDirty(true);
            SwingUtilities.invokeLater
                (new RowSelectionTask(firstRowNum+1, lastRowNum+1, true));
        }
    }

    private void moveTaskDownTreeView() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath, model)+1)) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row, row, true));
        }
    }


    private class RowSelectionTask implements Runnable {
        private int firstRow;
        private int lastRow;
        private boolean downward;
        public RowSelectionTask(int firstRow, int lastRow, boolean downward) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.downward = downward;
        }
        public void run() {
            if (firstRow != -1) {
                treeTable.getSelectionModel().setSelectionInterval
                    (firstRow, lastRow);
                enableTaskButtons();

                int rowToShow =
                    (downward
                        ? Math.min(lastRow+1, treeTable.getRowCount()-1)
                        : Math.max(firstRow-1, 0));
                treeTable.scrollRectToVisible
                    (treeTable.getCellRect(rowToShow, 0, true));
            }
        }
    }

    protected int selectedTaskPos(TreePath selPath, TreeModel model) {
        if (selPath == null) return -1;

        int pathLen = selPath.getPathCount();
        if (pathLen != 2) return -1; // only adjust children of the root node.
        EVTask selectedTask = (EVTask) selPath.getPathComponent(pathLen-1);
        EVTask parentNode = (EVTask) selPath.getPathComponent(pathLen-2);
        return model.getIndexOfChild(parentNode, selectedTask);
    }

    protected void enableTaskButtons(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting())
            SwingUtilities.invokeLater(new EnableTaskButtons());
    }
    private class EnableTaskButtons implements Runnable {
        public void run() { enableTaskButtons(); }
    }

    protected void enableTaskButtons() {
        if (treeTable != null) {
            if (isFlatView())
                enableTaskButtonsFlatView();
            else if (isMergedView())
                enableTaskButtonsMergedView();
            else
                enableTaskButtonsTreeView();
        }
    }

    private void enableTaskButtonsFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();

        boolean enableDelete = (disableTaskPruning == false
                && firstRowNum > 0 && firstRowNum == lastRowNum);
        boolean enableUp     = (firstRowNum > 1);
        boolean enableDown   = (lastRowNum > 0 && lastRowNum < treeTable.getRowCount()-1);

        //addTaskButton    .setEnabled(false);
        deleteTaskButton .setEnabled(enableDelete);
        deleteTaskButton .setText(resources.getString("Buttons.Remove_Task"));
        moveUpButton     .setEnabled(enableUp);
        moveDownButton   .setEnabled(enableDown);
    }

    private void enableTaskButtonsMergedView() {
        addTaskButton    .setEnabled(false);
        deleteTaskButton .setEnabled(false);
        moveUpButton     .setEnabled(false);
        moveDownButton   .setEnabled(false);
        indivChartButton .setEnabled(false);
    }


    private void enableTaskButtonsTreeView() {
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        boolean enableDelete = false, enableIndiv = false,
            enableUp = false, enableDown = false, isPruned = false;
        int pos = selectedTaskPos(selectionPath, model);
        if (pos != -1) {
            int numKids = ((EVTask) model.getRoot()).getNumChildren();

            isPruned = false;
            enableDelete = true;
            enableIndiv  = true;
            enableUp     = (pos > 0);
            enableDown   = (pos < numKids-1);

        } else if (!isRollup() && selectionPath != null &&
                   selectionPath.getPathCount() > 2) {
            isPruned =
                ((EVTask) selectionPath.getLastPathComponent()).isUserPruned();
            enableDelete = isPruned || (disableTaskPruning == false);
        }

        addTaskButton    .setEnabled(true);
        deleteTaskButton .setEnabled(enableDelete);
        if (!isRollup())
            deleteTaskButton.setText
                (isPruned
                 ? resources.getString("Buttons.Restore_Task")
                 : resources.getString("Buttons.Remove_Task"));
        moveUpButton     .setEnabled(enableUp);
        moveDownButton   .setEnabled(enableDown);

        if (indivChartButton != null)
            indivChartButton.setEnabled(enableIndiv);
    }

    protected void toggleFlatView() {
        if (!isFlatView()) {
            changeTreeTableModel(model, treeColumnModel);
            treeTable.setDragEnabled(false);
        } else {
            if (flatModel == null) {
                flatModel = model.getFlatModel();
                flatColumnModel = createFlatColumnModel();
            }
            changeTreeTableModel(flatModel, flatColumnModel);
            treeTable.setDragEnabled(true);
        }

        enableTaskButtons();
    }

    private TableColumnModel createFlatColumnModel() {
        DefaultTableColumnModel result = new DefaultTableColumnModel();

        int extraWidth = 0;
        for (int i = 0;   i < treeColumnModel.getColumnCount();  i++) {
            TableColumn c = treeColumnModel.getColumn(i);
            switch (c.getModelIndex()) {
            case EVTaskList.TASK_COLUMN:
            case EVTaskList.PLAN_TIME_COLUMN:
            case EVTaskList.PLAN_DTIME_COLUMN:
            //case EVTaskList.ACT_TIME_COLUMN:
            //case EVTaskList.ACT_DTIME_COLUMN:
            case EVTaskList.PLAN_DATE_COLUMN:
            case EVTaskList.FORECAST_DATE_COLUMN:
            case EVTaskList.DEPENDENCIES_COLUMN:
                result.addColumn(cloneTableColumn(c));
                break;

            default:
                extraWidth += c.getPreferredWidth();
                break;
            }
        }

        TableColumn c = result.getColumn(0);
        c.setPreferredWidth(c.getWidth() + extraWidth);

        return result;
    }
    private TableColumn cloneTableColumn(TableColumn c) {
        TableColumn result = new TableColumn(c.getModelIndex(),
                c.getPreferredWidth(), c.getCellRenderer(),
                c.getCellEditor());
        result.setMaxWidth(c.getMaxWidth());
        result.setMinWidth(c.getMinWidth());
        result.setResizable(c.getResizable());
        result.setHeaderValue(c.getHeaderValue());
        result.setHeaderRenderer(c.getHeaderRenderer());
        result.setIdentifier(c.getIdentifier());
        return result;
    }


    protected void toggleMergedView() {
        if (!isMergedView())
            changeTreeTableModel(model, treeColumnModel);
        else {
            boolean needsExpansionSetup = false;
            if (mergedModel == null) {
                mergedModel = model.getMergedModel();
                needsExpansionSetup = true;
            }
            changeTreeTableModel(mergedModel, treeColumnModel);
            if (needsExpansionSetup)
                readExpandedNodesPref(model.getID());
        }

        enableTaskButtons();
    }

    protected void changeTreeTableModel(TreeTableModel m,
            TableColumnModel columnModel) {
        // changing the TreeTableModel below causes our column model to
        // be completely recreated from scratch.  Unfortunately, this
        // loses all information about column width, tooltips, etc.  To
        // avoid this, we temporarily install a discardable column model.
        // The disruptive changes will be made to it, then we reinstall
        // the desired column model when we're done.
        //    Note that we can do this only because we know that we'll
        // be replacing the TreeTableModel with another one that is
        // exactly compatible.
        treeTable.setColumnModel(new DefaultTableColumnModel());
        treeTable.setTreeTableModel(m);
        treeTable.setColumnModel(columnModel);
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

    protected void showHideDirectColumns() {
        // update the task table
        for (int j = EVTaskList.DIRECT_COLUMN_LIST.length;   j-- > 0; ) {
            int i = EVTaskList.DIRECT_COLUMN_LIST[j];
            showHideColumn(treeColumnModel.getColumn(i),
                           model.getColumnName(i),
                           EVTaskList.colWidths[i]);
        }

        // update the schedule table
        for (int j = EVSchedule.DIRECT_COLUMN_LIST.length;   j-- > 0; ) {
            int i = EVSchedule.DIRECT_COLUMN_LIST[j];
            showHideColumn(scheduleTable.getColumnModel().getColumn(i),
                           model.getSchedule().getColumnName(i),
                           EVSchedule.colWidths[i]);
        }
    }

    protected void showHideColumn(TableColumn col, String name, int width) {

        boolean hide = (name == null || name.endsWith(" "));
        boolean hidden = (col.getMaxWidth() == 0);
        if (hide != hidden) {
            col.setMinWidth(0);
            col.setMaxWidth(hide ? 0 : width);
            col.setPreferredWidth(hide ? 0 : width);
        }
    }

    protected void enableScheduleButtons() {
        if (isRollup() || Settings.isReadOnly()) return;

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

    protected void maybeDisplayErrorButton() {
        errorButton.setVisible(getErrors() != null);
    }

    protected void close() {
        TaskScheduleChooser.close(taskListName);
        frame.dispose();

        if ((!isRollup() && !isFlatView()) ||
            (isRollup() && isMergedView())) {
            saveExpandedNodesPref(model.getID());
        }
        model.setNodeListener(null);
        model.removeRecalcListener(this);
        model.getSchedule().setListener(null);
        model = null;
        treeTable.dispose();
        treeTable = null;
        scheduleTable = null;
    }

    protected void save() {
        model.save();
        setDirty(false);
        displayErrorDialog(getErrors());
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

    public void showCollaborationWizard() {
        if (saveOrCancel(true))
            new TaskScheduleCollaborationWizard(dash, taskListName);
    }

    public void showChart() {
        if (chartDialog != null && chartDialog.isDisplayable()) {
            chartDialog.show();
            chartDialog.toFront();
        } else
            chartDialog = new TaskScheduleChart(model);
    }

    public void showIndivChart() {
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        if (selectionPath == null) return;
        int pos = selectedTaskPos(selectionPath, model);
        if (pos == -1) return;
        EVTaskList indivSchedule =
            ((EVTaskListRollup) model).getSubSchedule(pos);
        new TaskScheduleChart(indivSchedule);
    }

    public static final String CHART_URL = "//reports/ev.class";
    public void showHTML() {
        if (saveOrCancel(true))
            showReport(taskListName);
    }


    public static void showReport(String taskListName) {
        String uri = "/" + HTMLUtils.urlEncode(taskListName) + CHART_URL;
        Browser.launch(uri);
    }


    public void confirmClose(boolean showCancel) {
        if (saveOrCancel(showCancel))
            close();
    }
    public boolean saveOrCancel(boolean showCancel) {
        if (isDirty)
            switch (JOptionPane.showConfirmDialog
                    (frame,
                     resources.getString("Confirm.Close.Prompt"),
                     resources.getString("Confirm.Close.Title"),
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

    private String getExpandedNodesKey(String taskListId) {
        return taskListId + EXPANDED_NODES_KEY_SUFFIX;
    }

    private Set getExpandedNodesPref(String taskListId) {
        String value = preferences.get(getExpandedNodesKey(taskListId), "");
        String[] nodesArray = value.split(EXPANDED_NODES_DELIMITER);
        Set nodesToExpand = new HashSet(Arrays.asList(nodesArray));

        return nodesToExpand;
    }

    private void setExpandedNodesPref(String taskListId, Set value) {
        preferences.put(getExpandedNodesKey(taskListId),
                StringUtils.join(value, EXPANDED_NODES_DELIMITER));
    }

    private void saveExpandedNodesPref(String taskListId) {
        Set expandedNodes = new HashSet();
        getExpandedNodes(expandedNodes, (EVTask) treeTable.getTree().getModel().getRoot());
        setExpandedNodesPref(taskListId, expandedNodes);
    }

    private void getExpandedNodes(Set expandedNodes, EVTask taskNode) {
        boolean isVisible = treeTable.getTree().isVisible(new TreePath(taskNode.getPath()));
        boolean isExpanded = treeTable.getTree().isExpanded(new TreePath(taskNode.getPath()));

        if (isVisible && isExpanded) {
            List ids = taskNode.getTaskIDs();
            if (null == ids || ids.isEmpty())
                expandedNodes.add(taskNode.getFullName());
            else
                expandedNodes.addAll(ids);

            for (int i = 0; i < taskNode.getNumChildren(); i++)
                getExpandedNodes(expandedNodes, taskNode.getChild(i));
        }
    }

    private void readExpandedNodesPref(String taskListId) {
        Set nodesToExpand = getExpandedNodesPref(taskListId);
        expandNodes(nodesToExpand, (EVTask) treeTable.getTree().getModel().getRoot());
    }

    private void expandNodes(Set nodesToExpand, EVTask taskNode) {
        List ids = taskNode.getTaskIDs();
        if ((null != ids && nodesToExpand.removeAll(ids)) ||
                nodesToExpand.remove(taskNode.getFullName())) {
            treeTable.getTree().makeVisible(new TreePath(taskNode.getPath()));
            treeTable.getTree().expandPath(new TreePath(taskNode.getPath()));
        }
        for (int i = 0; i < taskNode.getNumChildren(); i++) {
            expandNodes(nodesToExpand, taskNode.getChild(i));
        }
    }
}
