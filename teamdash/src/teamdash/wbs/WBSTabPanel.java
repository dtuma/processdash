
package teamdash.wbs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

public class WBSTabPanel extends JPanel {


    WBSJTable wbsTable;
    DataJTable dataTable;
    JScrollPane scrollPane;
    JTabbedPane tabbedPane;
    JSplitPane splitPane;
    ArrayList tableColumnModels = null;
    GridBagLayout layout;


    public WBSTabPanel(WBSModel wbs, DataTableModel data, Map iconMap) {

        wbsTable = new WBSJTable(wbs, iconMap);
        dataTable = new DataJTable(data);
        wbsTable.setSelectionModel(dataTable.getSelectionModel());
        dataTable.setRowHeight(wbsTable.getRowHeight());

        scrollPane = new JScrollPane(dataTable,
                                     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().addAdjustmentListener
            (new ScrollListener());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (System.getProperty("java.version").startsWith("1.3"))
            scrollPane.getVerticalScrollBar().setBorder
                (BorderFactory.createMatteBorder(0, 0, 0, 1, Color.darkGray));

        scrollPane.setRowHeaderView(wrapWBSTable(wbsTable));
        wbsTable.setScrollableDelegate(dataTable);

        makeSplitter();
        makeTabs();
        tableColumnModels = new ArrayList();

        this.setOpaque(false);
        layout = new GridBagLayout();
        this.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = 10;
        c.insets.top = 25;
        c.insets.bottom = 1;
        this.add(splitPane);
        layout.setConstraints(splitPane, c);

        c.insets.top = 30;
        c.insets.bottom = 10;
        this.add(scrollPane);
        layout.setConstraints(scrollPane, c);

        c.insets.left = 215;//!@#
        c.insets.top = c.insets.bottom = c.insets.right = 0;
        this.add(tabbedPane);
        layout.setConstraints(tabbedPane, c);

        //makeColumnModels();

    }

    public void addTab(String tabName, String[] columnNames) {

        AbstractTableModel tableModel =
            (AbstractTableModel) dataTable.getModel();
        TableColumnModel columnModel = new DefaultTableColumnModel();

        for (int i = 0;   i < columnNames.length;   i++) {
            int columnIndex = tableModel.findColumn(columnNames[i]);
            if (columnIndex == -1)
                throw new IllegalArgumentException
                    ("No column named " + columnNames[i]);
            TableColumn tableColumn = new TableColumn(columnIndex);
            tableColumn.setHeaderValue(columnNames[i]);
            columnModel.addColumn(tableColumn);
        }

        // add a new tab.
        tableColumnModels.add(columnModel);
        tabbedPane.add(tabName, new EmptyComponent(new Dimension(10, 10)));
    }

    private Component wrapWBSTable(JTable t) {
        JPanel result = new JPanel();
        result.setOpaque(false);

        GridBagLayout layout = new GridBagLayout();
        result.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        //c.weightx = c.weighty = 1.0;
        //c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.top = c.insets.bottom = 0;
        c.insets.right = 20;
        c.anchor = GridBagConstraints.NORTHWEST;
        result.add(t);
        layout.setConstraints(t, c);

        JComponent filler = new EmptyComponent(new Dimension(0, 0));
        c.gridy = 1;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        result.add(filler);
        layout.setConstraints(filler, c);

        return result;
    }
    private Component old_wrapWBSTable(JTable t) {
        Box box = Box.createHorizontalBox();
        box.add(wbsTable);
        box.add(Box.createHorizontalStrut(20));
        box.setBackground(null);
        return box;
    }


    private void makeSplitter() {
        splitPane = new MagicSplitter
            (JSplitPane.HORIZONTAL_SPLIT, false,
             new EmptyComponent(new Dimension(70, 70)),
             new EmptyComponent(new Dimension(70, 70)));
        splitPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        splitPane.setDividerLocation(205);
        //splitPane.setOpaque(false);
        //splitPane.setVisible(false);
        splitPane.addPropertyChangeListener
            (splitPane.DIVIDER_LOCATION_PROPERTY, new DividerListener());
        //splitPane.setMaximumSize(new Dimension(1000, 1000));
        //splitPane.setDividerSize(3);
    }

    private class MagicSplitter extends JSplitPane {
        public MagicSplitter(int newOrientation,
                             boolean newContinuousLayout,
                             Component newLeftComponent,
                             Component newRightComponent) {
            super(newOrientation, newContinuousLayout,
                  newLeftComponent, newRightComponent);
            setOpaque(false);
        }
        private Component divider;
        protected void addImpl(Component comp, Object constraints, int index){
            if (constraints != null &&
                (constraints.equals(JSplitPane.DIVIDER)))
                divider = comp;
            super.addImpl(comp, constraints, index);
        }
        public boolean contains(int x, int y) {
            int l = getDividerLocation();
            int diff = x-l;
            return (diff > 0 && diff < getDividerSize());
            //return divider.contains(x-l, y);
        }
    }

    private class DividerListener implements java.beans.PropertyChangeListener
    {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            int dividerLocation = ((Number) evt.getNewValue()).intValue();
            TableColumn col = wbsTable.getColumnModel().getColumn(0);
            col.setMaxWidth(dividerLocation - 5);
            col.setMinWidth(dividerLocation - 5);
            col.setPreferredWidth(dividerLocation - 5);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.weightx = c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets.left = dividerLocation + 10;
            c.insets.top = c.insets.bottom = c.insets.right = 0;
            layout.setConstraints(tabbedPane, c);
            WBSTabPanel.this.revalidate();
        }
    }




    private class ScrollListener implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
            splitPane.repaint();
        }
    }

    /*
    private void makeColumnModels() {
        tcms[0] = new DefaultTableColumnModel();
        tcms[1] = new DefaultTableColumnModel();
        tcms[2] = new DefaultTableColumnModel();
        for (int i=1;   i<10;   i++) {
            TableColumn tc = newTableColumn(i);
            if ((i&1) == 0)
,                tcms[0].addColumn(tc);
            else
                tcms[1].addColumn(tc);
            tcms[2].addColumn(tc);
        }
    }
    */


    private Component makeTabs() {
        /*Box h = Box.createHorizontalBox();
            h.add(Box.createHorizontalStrut(90));*/

        tabbedPane = new JTabbedPane();
        //tabbedPane.add("Even", null);
        //tabbedPane.add("Odd", null);
        //tabbedPane.add("All", null);
        //tabbedPane.setSelectedIndex(2);

        //System.out.println("bounds="+tabbedPane.getBoundsAt(0));
        //h.add(tabbedPane);

        tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int whichTab = tabbedPane.getSelectedIndex();
                    TableColumnModel newModel =
                        (TableColumnModel) tableColumnModels.get(whichTab);
                    dataTable.setColumnModel(newModel);
                } } );

        return tabbedPane; //h;
    }
    private class EmptyComponent extends JComponent {
        private Dimension d, m;
        public EmptyComponent(Dimension d) {
            this(d, new Dimension(3000,3000));
        }
        public EmptyComponent(Dimension d, Dimension m) {
            this.d = d;
            this.m = m;
        }
        public Dimension getMaximumSize() { return m; }
        public Dimension getMinimumSize() { return d; }
        public Dimension getPreferredSize() { return d; }
        public boolean isOpaque() { return false; }
        public void paint(Graphics g) {}
    }

    /*
    public JTable makeTable(TableModel m, int[] columns) {
        JTable t = new JTable(m);
        TableColumnModel tcm = t.getColumnModel();
    column:
        for (int c=m.getColumnCount();   c-- > 0; ) {
            for (int i=columns.length;   i-- > 0; )
                if (columns[i] == c) continue column;
            tcm.removeColumn(tcm.getColumn(c));
        }
        return t;
    }

    public JMenuBar buildMenuBar() {
        JMenuBar result = new JMenuBar();
        JMenu options = new JMenu("Columns");
        options.add(new JMenuItem(new EvenColumnsAction()));
        options.add(new JMenuItem(new OddColumnsAction()));
        options.add(new JMenuItem(new AllColumnsAction()));
        result.add(options);
        return result;
    }
    */


    private class ColumnAction extends AbstractAction {
        TableColumnModel tcm = null;
        public ColumnAction(String name) { super(name); }
        public void actionPerformed(ActionEvent e) {
            dataTable.setColumnModel(tcm);
            dataTable.updateUI();
            //scrollPane.setColumnHeaderView(dataTable.getTableHeader());
        }
    }
    /*

        public static JScrollPane configure(JTable table1) {
            JScrollPane pane = new JScrollPane(table1);

            // Build a second table with one column from the table1
            // and remove that column from table1.
            JTable table2 = new JTable(table1.getModel());
            table2.setRowSelectionAllowed(false); //looks better
            table2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            TableColumnModel tcm1 = table1.getColumnModel();
            TableColumnModel tcm2 = new DefaultTableColumnModel();
            TableColumn col = tcm1.getColumn(0);
            //make it gray (header-like)
            col.setCellRenderer(table2.getTableHeader().getDefaultRenderer());
            tcm1.removeColumn(col);
            tcm2.addColumn(col);
            table2.setColumnModel(tcm2);    // Install new column model
            table2.setPreferredScrollableViewportSize(table2.getPreferredSize());

            // Put the second table in the row header
            pane.setRowHeaderView(table2);

            // Put the header of the second table in the top left corner
            JTableHeader th = table2.getTableHeader();
            //nail it down
            th.setReorderingAllowed(false);
            th.setResizingAllowed(false);
            pane.setCorner(JScrollPane.UPPER_LEFT_CORNER, th);
            return pane;
}



     */

    private TableColumn newTableColumn(int i) {
        TableColumn result = new TableColumn(i);
        if (i != 7)
            result.setHeaderValue(Integer.toString(i));
        return result;
    }

    /*
    private class EvenColumnsAction extends ColumnAction {
        public EvenColumnsAction() {
            super("Even");
            tcm = new DefaultTableColumnModel();
            tcm.addColumn(newTableColumn(2));
            tcm.addColumn(newTableColumn(4));
            tcm.addColumn(newTableColumn(6));
            tcm.addColumn(newTableColumn(8));
        }
    }

    private class OddColumnsAction extends ColumnAction {
        public OddColumnsAction() {
            super("Odd");
            tcm = new DefaultTableColumnModel();
            tcm.addColumn(newTableColumn(1));
            tcm.addColumn(newTableColumn(3));
            tcm.addColumn(newTableColumn(5));
            tcm.addColumn(newTableColumn(7));
            tcm.addColumn(newTableColumn(9));
        }
    }

    private class AllColumnsAction extends ColumnAction {
        public AllColumnsAction() {
            super("All");
            tcm = new DefaultTableColumnModel();
            for (int i=1;  i < 10;  i++)
                tcm.addColumn(newTableColumn(i));
        }
    }

    private class BogusData extends AbstractTableModel {
        public String getColumnName(int column) {
            return Integer.toString(column);
        }
        public int getColumnCount() { return 10; }
        public Class getColumnClass(int columnIndex) { return String.class; }
        public int getRowCount() { return 100; }
        public Object getValueAt(int rowIndex, int columnIndex) {
            return Integer.toString(rowIndex * 10 + columnIndex);
        }
    }
    */
}
