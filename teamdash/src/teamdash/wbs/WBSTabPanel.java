package teamdash.wbs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import teamdash.TeamProcess;

/** Class to display the WBS editor panel
 */
public class WBSTabPanel extends JPanel {

    public static final String TEAM_MEMBER_TIMES_ID = "TeamMemberTimes";

    WBSJTable wbsTable;
    DataJTable dataTable;
    JScrollPane scrollPane;
    JTabbedPane tabbedPane;
    JSplitPane splitPane;
    JToolBar toolBar;
    UndoList undoList;
    ArrayList tableColumnModels = new ArrayList();
    GridBagLayout layout;

    /** Create a WBSTabPanel */
    public WBSTabPanel(WBSModel wbs, DataTableModel data,
                       TeamProcess teamProcess) {
        this(wbs, data, teamProcess.getIconMap(),
             teamProcess.getNodeTypeMenu());
    }

    /** Create a WBSTabPanel */
    public WBSTabPanel(
        WBSModel wbs,
        DataTableModel data,
        Map iconMap,
        JMenu iconMenu) {
        setOpaque(false);
        setLayout(layout = new GridBagLayout());

        undoList = new UndoList(wbs);
        undoList.setForComponent(this);

        // build the components to display in this panel
        makeTables(wbs, data, iconMap, iconMenu);
        makeSplitter();
        makeScrollPane();
        makeTabbedPane();
        makeToolBar();

        // manually set the initial divider location, to trigger the
        // size coordination logic.
        splitPane.setDividerLocation(200);
    }

    /** Add a tab to the tab panel
     * @param tabName The name to display on the tab
     * @param columnNames The columns to display when this tab is selected
     * @throws IllegalArgumentException if <code>columnNames</code> names
     *    a column which cannot be found
     */
    public void addTab(
        String tabName,
        String columnIDs[],
        String[] columnNames)
        throws IllegalArgumentException {

        DataTableModel tableModel = (DataTableModel) dataTable.getModel();
        TableColumnModel columnModel = new DefaultTableColumnModel();

        for (int i = 0; i < columnNames.length; i++) {
            if (TEAM_MEMBER_TIMES_ID.equals(columnIDs[i])) {
                tableModel.addTeamMemberTimes(columnModel);
                continue;
            }

            int columnIndex = tableModel.findColumn(columnIDs[i]);
            if (columnIndex == -1)
                columnIndex = tableModel.findColumn(columnNames[i]);
            if (columnIndex == -1)
                throw new IllegalArgumentException(
                    "No column with ID " + columnIDs[i]);

            TableColumn tableColumn = new TableColumn(columnIndex);
            String name = columnNames[i];
            if (name == null)
                name = tableModel.getColumn(columnIndex).getColumnName();
            tableColumn.setHeaderValue(name);
            tableColumn.setIdentifier(columnIDs[i]);
            columnModel.addColumn(tableColumn);
        }

        // add the newly created table model to the tableColumnModels list
        tableColumnModels.add(columnModel);
        // add the new tab. (Note: the addition of the first tab triggers
        // an automatic tab selection event, which will effectively install
        // the tableColumnModel we just created.)
        tabbedPane.add(tabName, new EmptyComponent(new Dimension(10, 10)));
    }


    /** Get a list of actions for editing the work breakdown structure */
    public Action[] getEditingActions() {
        Action[] tableActions = wbsTable.getEditingActions();
        Action[] result = new Action[tableActions.length + 2];
        System.arraycopy(tableActions, 0, result, 2, tableActions.length);
        result[0] = undoList.getUndoAction();
        result[1] = undoList.getRedoAction();
        return result;
    }


    /** Get an action capable of inserting a workflow into the work breakdown
     * 	structure */
    public Action getInsertWorkflowAction(WBSModel workflows) {
        return wbsTable.getInsertWorkflowAction(workflows);
    }



    /** Create the JTables and perform necessary setup */
    private void makeTables(
        WBSModel wbs,
        DataTableModel data,
        Map iconMap,
        JMenu iconMenu) {
        // create the WBS table to display the hierarchy
        wbsTable = new WBSJTable(wbs, iconMap, iconMenu);
        // create the table to display hierarchy data
        dataTable = new DataJTable(data);
        // link the tables together so they have the same scrolling behavior,
        // selection model, and row height.
        wbsTable.setScrollableDelegate(dataTable);
        wbsTable.setSelectionModel(dataTable.getSelectionModel());
        dataTable.setRowHeight(wbsTable.getRowHeight());
    }

    /** Create and install the splitter component. */
    private void makeSplitter() {
        splitPane =
            new MagicSplitter(JSplitPane.HORIZONTAL_SPLIT, false, 70, 70);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setDividerLocation(205);
        splitPane.addPropertyChangeListener(
            JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new DividerListener());
        //splitPane.setDividerSize(3);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = 10;
        c.insets.top = 25;
        c.insets.bottom = 1;
        add(splitPane);
        layout.setConstraints(splitPane, c);
    }

    /** Create and install the scroll pane component. */
    private void makeScrollPane() {
        // create a vertical scroll bar
        scrollPane =
            new JScrollPane(
                dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // remove the borders from the scroll pane
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // we need to add an explicit border for the scroll bar in Java 1.3,
        // otherwise its right edge vanishes when we remove the border from
        // the scrollPane.
        if (System.getProperty("java.version").startsWith("1.3"))
            scrollPane.getVerticalScrollBar().setBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.darkGray));

        // make the WBS table the "row header view" of the scroll pane.
        scrollPane.setRowHeaderView(wbsTable);
        // don't paint over the splitter bar when we repaint.
        scrollPane.setOpaque(false);
        wbsTable.setOpaque(false);
        scrollPane.getRowHeader().setOpaque(false);

        // add the scroll pane to the panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = c.insets.bottom = 10;
        c.insets.top = 30;
        add(scrollPane);
        layout.setConstraints(scrollPane, c);
    }

    /** Create and install the tabbed pane component. */
    private void makeTabbedPane() {
        tabbedPane = new JTabbedPane();

        tabbedPane.addChangeListener(new TabListener());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.top = c.insets.bottom = c.insets.right = 0;
        c.insets.left = 215;
        add(tabbedPane);
        layout.setConstraints(tabbedPane, c);
    }

    /** Create and install the tool bar component. */
    private void makeToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0,0,0,0));
        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());

        Action[] editingActions = wbsTable.getEditingActions();
        for (int i = 0;   i < editingActions.length;   i++)
            if (editingActions[i].getValue(Action.SMALL_ICON) != null)
                addToolbarButton(editingActions[i]);

        // add the tool bar to the panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets.left = 10;
        c.insets.right = c.insets.bottom = c.insets.top = 0;
        add(toolBar);
        layout.setConstraints(toolBar, c);
    }

    /** Add a button to the beginning of the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        button.setMargin(new Insets(0,0,0,0));
        button.setFocusPainted(false);
        button.setToolTipText((String)a.getValue(Action.NAME));
        button.setText(null);
        toolBar.add(button);
    }

    /** Listen for changes to the tab selection, and install the corresponding
     * table column model. */
    private final class TabListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            int whichTab = tabbedPane.getSelectedIndex();
            TableColumnModel newModel =
                (TableColumnModel) tableColumnModels.get(whichTab);
            dataTable.setColumnModel(newModel);
        }
    }

    /** This component displays a splitter bar (along the lines of JSplitPane)
     * but doesn't display anything on either side of the bar. Instead, these
     * areas are transparent, allowing other components to show through.
     */
    private final class MagicSplitter extends JSplitPane {
        public MagicSplitter(
            int newOrientation,
            boolean newContinuousLayout,
            int firstCompMinSize,
            int secondCompMinSize) {
            super(
                newOrientation,
                newContinuousLayout,
                new EmptyComponent(
                    new Dimension(firstCompMinSize, firstCompMinSize)),
                new EmptyComponent(
                    new Dimension(secondCompMinSize, secondCompMinSize)));
            setOpaque(false);
        }
        /** Limit contains() to the area owned by the splitter bar.  This
         * allows mouse events (e.g. clicks, mouseovers) to "pass through"
         * our invisible component areas, to the real components underneath.
         */
        public boolean contains(int x, int y) {
            int l = getDividerLocation();
            int diff = x - l;
            return (diff > 0 && diff < getDividerSize());
        }
    }

    /** Listen for changes in the position of the divider, and resize other
     * objects in this panel appropriately
     */
    private final class DividerListener implements PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            // get the new location of the divider.
            int dividerLocation = ((Number) evt.getNewValue()).intValue();

            // resize the wbsTable to fit on the left side of the divider
            TableColumn col = wbsTable.getColumnModel().getColumn(0);
            col.setMinWidth(dividerLocation - 5);
            col.setMaxWidth(dividerLocation - 5);
            col.setPreferredWidth(dividerLocation - 5);
            // add an extra 20 pixels to the width of the JScrollPane's
            // row header, to allow space for the splitter bar.
            Dimension d = new Dimension(dividerLocation + 15, 100);
            wbsTable.setPreferredScrollableViewportSize(d);

            // resize the tabbed pane to fit on the right side of the divider.
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.weightx = c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets.left = dividerLocation + 10;
            c.insets.top = c.insets.bottom = c.insets.right = 0;
            layout.setConstraints(tabbedPane, c);

            // revalidate the layout of the tabbed panel.
            WBSTabPanel.this.revalidate();
        }
    }

    /** Display an invisible component with a certain minimum/preferred size.
    */
    private final class EmptyComponent extends JComponent {
        private Dimension d, m;
        public EmptyComponent(Dimension d) {
            this(d, new Dimension(3000, 3000));
        }
        public EmptyComponent(Dimension d, Dimension m) {
            this.d = d;
            this.m = m;
        }
        public Dimension getMaximumSize() {
            return m;
        }
        public Dimension getMinimumSize() {
            return d;
        }
        public Dimension getPreferredSize() {
            return d;
        }
        public boolean isOpaque() {
            return false;
        }
        public void paint(Graphics g) {
        }
    }

}
