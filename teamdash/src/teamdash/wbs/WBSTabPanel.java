
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/** Class to display the WBS editor panel
 */
public class WBSTabPanel extends JPanel {


    WBSJTable wbsTable;
    DataJTable dataTable;
    JScrollPane scrollPane;
    JTabbedPane tabbedPane;
    JSplitPane splitPane;
    ArrayList tableColumnModels = new ArrayList();
    GridBagLayout layout;


        /** Create a WBSTabPanel */
    public WBSTabPanel(WBSModel wbs, DataTableModel data, Map iconMap) {
        setOpaque(false);
        setLayout(layout = new GridBagLayout());

                // build the components to display in this panel
        makeTables(wbs, data, iconMap);
        makeSplitter();
        makeScrollPane();
        makeTabbedPane();
    }


        /** Add a tab to the tab panel
         * @param tabName The name to display on the tab
         * @param columnNames The columns to display when this tab is selected
         * @throws IllegalArgumentException if <code>columnNames</code> names
         *    a column which cannot be found
         */
    public void addTab(String tabName, String[] columnNames)
        throws IllegalArgumentException
    {

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

                // add the newly created table model to the tableColumnModels list
        tableColumnModels.add(columnModel);
        // add the new tab. (Note: the addition of the first tab triggers
        // an automatic tab selection event, which will effectively install
        // the tableColumnModel we just created.)
        tabbedPane.add(tabName, new EmptyComponent(new Dimension(10, 10)));
    }


        /** Create the JTables and perform necessary setup */
    private void makeTables(WBSModel wbs, DataTableModel data, Map iconMap) {
        // create the WBS table to display the hierarchy
        wbsTable = new WBSJTable(wbs, iconMap);
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
        splitPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        splitPane.setDividerLocation(205);
        splitPane.addPropertyChangeListener
            (JSplitPane.DIVIDER_LOCATION_PROPERTY, new DividerListener());
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
        scrollPane = new JScrollPane(dataTable,
                                     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // remove the borders from the scroll pane
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (System.getProperty("java.version").startsWith("1.3"))
                // need to add an explicit border for the scroll bar in Java 1.3
            scrollPane.getVerticalScrollBar().setBorder
                (BorderFactory.createMatteBorder(0, 0, 0, 1, Color.darkGray));

        // make the WBS table the "row header view" of the scroll pane.
        scrollPane.setRowHeaderView(wrapWBSTable(wbsTable));
        // don't paint over the splitter bar when we repaint.
        scrollPane.setOpaque(false);

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


        /** Since JTables aren't used to being put in the "row header view" of
         * a scroll pane, we have to place it in a panel that provides some
         * guidance about how the table should lay itself out
         */
    private Component wrapWBSTable(JTable t) {
        JPanel result = new JPanel();
        result.setOpaque(false);

        GridBagLayout layout = new GridBagLayout();
        result.setLayout(layout);

                // add the table to the grid in the top left position.  The table's
                // preferred width will drive the preferred width of the JPanel, so
                // we don't have to worry about "fill" modes.
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.insets.left = c.insets.top = c.insets.bottom = 0;
        c.insets.right = 20;    // leave empty space for the splitter bar
        c.anchor = GridBagConstraints.NORTHWEST;
        result.add(t);
        layout.setConstraints(t, c);

                // add an invisible component below the table that grows to absorb
                // the remaining space.  This ensures that the JPanel will be as tall
                // as the viewport view, and that the table will be top-justified
                // within the panel.
        JComponent filler = new EmptyComponent(new Dimension(0, 0));
        c.gridy = 1;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        result.add(filler);
        layout.setConstraints(filler, c);

        return result;
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
        public MagicSplitter(int newOrientation, boolean newContinuousLayout,
                                                 int firstCompMinSize, int secondCompMinSize) {
            super(newOrientation, newContinuousLayout,
                  new EmptyComponent(new Dimension(firstCompMinSize,
                                                                                                   firstCompMinSize)),
                  new EmptyComponent(new Dimension(secondCompMinSize,
                                                                                   secondCompMinSize)));
            setOpaque(false);
        }
        /** Limit contains() to the area owned by the splitter bar.  This
         * allows mouse events (e.g. clicks, mouseovers) to "pass through"
         * our invisible component areas, to the real components underneath.
         */
        public boolean contains(int x, int y) {
            int l = getDividerLocation();
            int diff = x-l;
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
             col.setMaxWidth(dividerLocation - 5);
             col.setMinWidth(dividerLocation - 5);
             col.setPreferredWidth(dividerLocation - 5);

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

}
