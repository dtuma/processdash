
package teamdash;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

public class TabTableTest {

    public static void main(String args[]) {
        new TabTableTest();
    }

    JTable a, b;
    JScrollPane sp;
    JTabbedPane tp;
    JSplitPane split;
    TableColumnModel[] tcms = new TableColumnModel[3];
    JPanel panel;
    GridBagLayout layout;


    public TabTableTest() {
        JFrame frame = new JFrame("Tab Table Test");

        frame.getContentPane().add(makeTable());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
        System.out.println("bounds="+tp.getBoundsAt(0));
    }

    private Component makeSplitter() {
        JSplitPane sp = new MagicSplitter
            (JSplitPane.HORIZONTAL_SPLIT, false,
             new EmptyComponent(new Dimension(70, 70)),
             new EmptyComponent(new Dimension(70, 70)));
        sp.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        sp.setDividerLocation(80);
        //sp.setOpaque(false);
        //sp.setVisible(false);
        sp.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                                     new DividerListener());
        //sp.setMaximumSize(new Dimension(1000, 1000));
        //sp.setDividerSize(3);

        return split = sp;
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
            TableColumn col = a.getColumnModel().getColumn(0);
            col.setMaxWidth(dividerLocation - 5);
            col.setMinWidth(dividerLocation - 5);
            col.setPreferredWidth(dividerLocation - 5);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.weightx = c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets.left = dividerLocation + 10;
            c.insets.top = c.insets.bottom = c.insets.right = 0;
            layout.setConstraints(tp, c);
            panel.revalidate();
        }
    }


    private Component makeTable() {
        TableModel m = new BogusData();

        a = makeTable(m, new int[] { 0 } );
        b = makeTable(m, new int[] { 1,2,3,4,5,6,7,8,9 } );
        a.setSelectionModel(b.getSelectionModel());

        sp = new JScrollPane(b,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().addAdjustmentListener(new ScrollListener());
        sp.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        Box box = Box.createHorizontalBox();
        box.add(a);
        box.add(Box.createHorizontalStrut(20));
        box.setBackground(null);
        sp.setRowHeaderView(box);

        panel = new JPanel();
        panel.setOpaque(false);
        layout = new GridBagLayout();
        panel.setLayout(layout);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = 10;
        c.insets.top = 24;
        c.insets.bottom = 1;

        Component comp = makeSplitter();
        panel.add(comp);
        layout.setConstraints(comp, c);

        c.insets.top = 30;
        c.insets.bottom = 10;
        panel.add(sp);
        layout.setConstraints(sp, c);

        c.insets.left = 90;
        c.insets.top = c.insets.bottom = c.insets.right = 0;
        comp = makeTabs();
        panel.add(comp);
        layout.setConstraints(comp, c);

        makeColumnModels();

        return panel;
    }

    private class ScrollListener implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
            split.repaint();
        }
    }

    private void makeColumnModels() {
        tcms[0] = new DefaultTableColumnModel();
        tcms[1] = new DefaultTableColumnModel();
        tcms[2] = new DefaultTableColumnModel();
        for (int i=1;   i<10;   i++) {
            TableColumn tc = newTableColumn(i);
            if ((i&1) == 0)
                tcms[0].addColumn(tc);
            else
                tcms[1].addColumn(tc);
            tcms[2].addColumn(tc);
        }
    }


    private Component makeTabs() {
        /*Box h = Box.createHorizontalBox();
            h.add(Box.createHorizontalStrut(90));*/

        tp = new JTabbedPane();
        tp.add("Even", null); //new EmptyComponent());
        tp.add("Odd", null); //new EmptyComponent());
        tp.add("All", null); // new EmptyComponent());
        tp.setSelectedIndex(2);

        System.out.println("bounds="+tp.getBoundsAt(0));
        //h.add(tp);

        tp.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    b.setColumnModel(tcms[tp.getSelectedIndex()]); } } );

        return tp; //h;
    }
    private class EmptyComponent extends JComponent {
        private Dimension d, m;
        public EmptyComponent(Dimension d) {
            this.d = d;
            this.m = new Dimension(3000,3000);
        }
        public Dimension getMaximumSize() { return m; }
        public Dimension getMinimumSize() { return d; }
        public Dimension getPreferredSize() { return d; }
        public boolean isOpaque() { return false; }
        public void paint(Graphics g) {}
    }

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

    private class ColumnAction extends AbstractAction {
        TableColumnModel tcm = null;
        public ColumnAction(String name) { super(name); }
        public void actionPerformed(ActionEvent e) {
            b.setColumnModel(tcm);
            b.updateUI();
            //sp.setColumnHeaderView(b.getTableHeader());
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

}
