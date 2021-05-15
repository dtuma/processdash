// Copyright (C) 2002-2021 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.IONoSuchElementException;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.AbstractTreeTableModel;
import net.sourceforge.processdash.ui.lib.JTreeTable;
import net.sourceforge.processdash.ui.lib.ToolTipTableCellRendererProxy;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeCardDialog {

    protected JFrame frame;
    TimeCard model;
    protected JTreeTable treeTable;
    DashHierarchy useProps;
    TimeLog timeLog;
    JComboBox monthField;
    JTextField yearField;
    JComboBox formatType;
    JCheckBox hideColumns;
    int format;
    private static final int HOURS_MINUTES = 0;
    private static final int HOURS = 1;
    private static final int MINUTES = 2;

    Resources resources = Resources.getDashBundle("Time");

    public TimeCardDialog(DashHierarchy useProps, TimeLog timeLog,
            Component relativeTo) {
        this.useProps = useProps;
        this.timeLog = timeLog;

        frame = new JFrame(resources.getString("Time_Card.Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);

        model = new TimeCard(useProps, timeLog);
        treeTable = new JTreeTable(model);
        treeTable.setDefaultRenderer(String.class, new TimeCardRenderer());
        treeTable.setShowGrid(true);
        treeTable.setIntercellSpacing(new Dimension(1, 1));
        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        treeTable.setPreferredTreeSizeFollowsRowSize(false);
        treeTable.getTableHeader().setReorderingAllowed(false);
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (treeTable, model.dayNames);

        JScrollPane sp = new JScrollPane
            (treeTable,
             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
             JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                treeTable.getTree().setBorder(BorderFactory.createMatteBorder(0, 0, 0,
                                1, treeTable.getGridColor()));
        JPanel rowHeader = new JPanel(new BorderLayout());
        rowHeader.add(treeTable.getTree());
        sp.setRowHeaderView(rowHeader);

        frame.getContentPane().add("Center", sp);
        frame.getContentPane().add("North", buildTopPanel());
        resizeColumns();
        PCSH.enableHelpKey(frame, "UsingTimeLogEditor.TimeCardView");

        frame.pack();
        frame.setLocationRelativeTo(relativeTo);
        frame.setVisible(true);
    }
    private void setColWidth(TableColumn c, int width) {
        c.setMinWidth(width);
        c.setPreferredWidth(width);
    }
    private void resizeColumns() {
                                // set default widths for the columns
        boolean hideColumns = this.hideColumns.isSelected();
        int i = model.getColumnCount() - 1;
        setColWidth(treeTable.getColumnModel().getColumn(i),
            getPreferredColumnWidth(i, 64));
        int daysInMonth = model.getDaysInMonth();
        while (i-- > 0)
            setColWidth(treeTable.getColumnModel().getColumn(i),
                (i < daysInMonth && (!hideColumns || !model.columnEmpty(i + 1)))
                        ? getPreferredColumnWidth(i, 32) : 0);
    }

    private int getPreferredColumnWidth(int col, int minWidth) {
        Object columnTotal = model.getValueAt(model.getRoot(), col);
        Component c = treeTable.getCellRenderer(0, col)
                .getTableCellRendererComponent(treeTable, columnTotal + "X",
                    false, false, 0, col);
        return Math.max(minWidth, c.getPreferredSize().width);
    }

    private Component buildTopPanel() {
        Box result = Box.createHorizontalBox();

        result.add(new JLabel
            (resources.getString("Time_Card.Month_Label")+" "));
        // We have to build our own month name array because the "official"
        // one contains 13 month names, the last one empty for Gregorian
        // Calendars
        String[] monthNames = new String[12];
        String[] officialMonthNames = (new DateFormatSymbols()).getMonths();
        for (int i=12;  i-- > 0; ) monthNames[i] = officialMonthNames[i];
        monthField = new JComboBox(monthNames);
        dontGrow(monthField);
        monthField.setSelectedIndex(model.getMonth() - Calendar.JANUARY);
        monthField.setMaximumRowCount(12);
        result.add(monthField);
        monthField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recalc(); }});

        yearField = new JTextField(5);
        dontGrow(yearField);
        yearField.setText(Integer.toString(model.getYear()));
        result.add(yearField);
        yearField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { recalc(); }
                public void removeUpdate(DocumentEvent e) { recalc(); }
                public void changedUpdate(DocumentEvent e)  { recalc(); }});

        result.add(Box.createHorizontalGlue());
        result.add(new JLabel(resources.getString("Time_Format.Label")+" "));
        formatType = new JComboBox();
        formatType.addItem(format(75, HOURS_MINUTES));
        formatType.addItem(format(75, HOURS));
        formatType.addItem(format(75, MINUTES));
        dontGrow(formatType);
        result.add(formatType);
        formatType.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    redraw(); }});

        result.add(Box.createHorizontalGlue());
        hideColumns = new JCheckBox();
        result.add(new JLabel(" " + resources.getString
                              ("Time_Card.Hide_Empty_Columns_Label") + " "));
        result.add(hideColumns);
        hideColumns.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    resizeColumns(); }});

        result.add(Box.createHorizontalGlue());
        JButton closeButton = new JButton(resources.getString("Close"));
        dontGrow(closeButton);
        result.add(Box.createVerticalStrut
                   (closeButton.getPreferredSize().height + 4));
        result.add(closeButton);
        closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false); }});

        return result;
    }
    private void dontGrow(JComponent c) {
        c.setMaximumSize(c.getPreferredSize());
    }

    public void hide() { frame.setVisible(false); }
    public void show() { frame.setVisible(true); }

    public void recalc() {
        int month = monthField.getSelectedIndex() + Calendar.JANUARY;
        int year = 0;
        try {
            year = Integer.parseInt(yearField.getText());
        } catch (Exception e) {}
        model.recalc(useProps, timeLog, month, year);
        ((AbstractTableModel) treeTable.getModel()).fireTableDataChanged();
        treeTable.getTree().expandRow(0);
        treeTable.getTree().invalidate();
        resizeColumns();
    }
    public void redraw() {
        format = formatType.getSelectedIndex();
        ((AbstractTableModel) treeTable.getModel()).fireTableDataChanged();
        resizeColumns();
    }




    private class TimeCard extends AbstractTreeTableModel {

        int month, year, daysInMonth, firstDayOfWeek;
        double[] time;
        public String[] dayNames = new String[32];

        private class TimeCardNode {
            TimeCardNode parent;
            String name, fullname;
            double[] time;
            ArrayList children;

            public TimeCardNode(DashHierarchy props) {
                this(null, props, PropertyKey.ROOT);
            }

            private TimeCardNode(TimeCardNode parent,
                                 DashHierarchy props,
                                 PropertyKey key) {
                this.parent = parent;
                this.name = (parent == null
                             ? resources.getString("Time_Card.Daily_Total")
                             : key.name());
                this.fullname = key.path();
                this.time = new double[32];
                this.children = new ArrayList();
                Prop prop = props.pget(key);
                for (int i = 0;   i < prop.getNumChildren();   i++)
                    children.add(new TimeCardNode(this, props,
                                                  prop.getChild(i)));
            }

            public String toString() { return name; }
            public double getTime(int day) { return time[day]; }
            public int getNumChildren() { return children.size(); }
            public boolean isLeaf() { return (getNumChildren() == 0); }
            public TimeCardNode getChild(int which) {
                return (TimeCardNode) children.get(which);
            }
            protected TimeCardNode findChild(String childName) {
                for (int i = getNumChildren();   i-- > 0; ) {
                    TimeCardNode child = getChild(i);
                    if (childName.equals(child.name))
                        return child;
                }
                return null;
            }
            public TimeCardNode getParent() { return parent; }
            public TimeCardNode[] getPath() { return getPathToRoot(this, 0); }
            protected TimeCardNode[] getPathToRoot(TimeCardNode aNode,
                                                   int depth) {
                TimeCardNode[] retNodes;

                if(aNode == null) {
                    if(depth == 0)
                        return null;
                    else
                        retNodes = new TimeCardNode[depth];
                }
                else {
                    depth++;
                    retNodes = getPathToRoot(aNode.getParent(), depth);
                    retNodes[retNodes.length - depth] = aNode;
                }
                return retNodes;
            }

            public void addTime(String path, int day, double minutes) {
                if (pathMatches(path)) {
                    time[0] += minutes;
                    time[day] += minutes;
                    for (int i = getNumChildren();   i-- > 0; )
                        getChild(i).addTime(path, day, minutes);
                }
            }

            private boolean pathMatches(String path) {
                // is my full name a prefix that matches the given path?
                return Filter.pathMatches(path, fullname);
            }

            public void prune() {
                TimeCardNode n;
                for (int i = getNumChildren();   i-- > 0; ) {
                    n = getChild(i);
                    if (n.time[0] == 0)
                        children.remove(i);
                    else
                        n.prune();
                }
            }

            public void copyFrom(TimeCardNode that) {
                this.time = that.time;
                Object[] thisPath = getPath();

                // first, delete nodes that are not present in that node.
                for (int i = this.getNumChildren();   i-- > 0; ) {
                    TimeCardNode child = this.getChild(i);
                    if (that.findChild(child.name) == null) {
                        this.children.remove(i);
                        fireTreeNodesRemoved(TimeCard.this, thisPath,
                                new int[] { i }, new Object[] { child });
                    }
                }

                // next, recurse over children, and update them.
                SortedMap nodeList = new TreeMap();
                for (int i = getNumChildren(); i-- > 0;) {
                    TimeCardNode child = this.getChild(i);
                    child.copyFrom(that.findChild(child.name));
                    nodeList.put(new Integer(i), child);
                }
                if (!nodeList.isEmpty())
                    fireTreeNodesChanged(TimeCard.this, thisPath,
                            getIndexes(nodeList), getNodes(nodeList));

                // finally, insert new nodes if necessary
                if (this.getNumChildren() < that.getNumChildren()) {
                    for (int i = 0; i < that.getNumChildren(); i++) {
                        TimeCardNode child = that.getChild(i);
                        if (this.findChild(child.name) == null) {
                            child.parent = this;
                            this.children.add(i, child);
                            fireTreeNodesInserted(TimeCard.this, thisPath,
                                    new int[] { i }, new Object[] { child });
                        }
                    }
                }
            }
            private int[] getIndexes(SortedMap nodeMap) {
                int[] result = new int[nodeMap.size()];
                int pos = 0;
                for (Iterator i = nodeMap.keySet().iterator(); i.hasNext();)
                    result[pos] = ((Integer) i.next()).intValue();
                return result;
            }

            private Object[] getNodes(SortedMap nodeMap) {
                return nodeMap.values().toArray();
            }
        }

        public TimeCard(DashHierarchy props, TimeLog timeLog) {
            super(null);
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            recalc(props, timeLog, cal.get(Calendar.MONTH),
                   cal.get(Calendar.YEAR));
        }

        public TimeCard(DashHierarchy props, TimeLog timeLog,
                        int month, int year) {
            super(null);
            recalc(props, timeLog, month, year);
        }

        public void recalc(DashHierarchy props, TimeLog timeLog,
                           int month, int year) {
            TimeCardNode root = new TimeCardNode(props);
            this.year = year;
            this.month = month;

            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(year, month, 1,     // year, month, day
                    0, 0, 0);           // hour, minute, second
            Date from = cal.getTime();
            daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            cal.add(Calendar.MONTH, 1);
            Date to = cal.getTime();

            for (int i=32;  i-- > 0; )
                if (i >= daysInMonth)
                    dayNames[i] = null;
                else {
                    cal.set(year, month, i+1);
                    dayNames[i] = dayFormat.format(cal.getTime());
                }

            try {
                Iterator e = timeLog.filter(null, from, to);
                TimeLogEntry tle;
                time = new double[32];
                int day;
                while (e.hasNext()) {
                    tle = (TimeLogEntry) e.next();
                    cal.setTime(tle.getStartTime());
                    day = cal.get(Calendar.DAY_OF_MONTH);

                    root.addTime(tle.getPath(), day, tle.getElapsedTime());
                    time[0] += tle.getElapsedTime();
                    time[day] += tle.getElapsedTime();
                }
            } catch (IONoSuchElementException ionsee) {
                showError(ionsee);
            } catch (IOException ioe) {
                showError(ioe);
            }
            root.prune();
            if (this.root == null)
                this.root = root;
            else {
                ((TimeCardNode) this.root).copyFrom(root);
                fireTreeNodesChanged(this, root.getPath(), null, null);
            }
        }

        private void showError(Exception e) {
            String[] message = resources
                    .getStrings("Time_Card.Recalc_Error.Message");
            String title = resources.getString("Time_Card.Recalc_Error.Title");
            JOptionPane.showMessageDialog(frame, message, title,
                    JOptionPane.ERROR_MESSAGE);
        }

        public int getDaysInMonth() { return daysInMonth; }
        public int getMonth() { return month; }
        public int getYear() { return year; }
        public boolean columnEmpty(int day) {
            return ((TimeCardNode) root).getTime(day) == 0;
        }

        //
        // The TreeModel interface
        //

        /** Returns the number of children of <code>node</code>. */
        public int getChildCount(Object node) {
            return ((TimeCardNode) node).getNumChildren();
        }

        /** Returns the child of <code>node</code> at index <code>i</code>. */
        public Object getChild(Object node, int i) {
            return ((TimeCardNode) node).getChild(i);
        }

        /** Returns true if the passed in object represents a leaf, false
         *  otherwise. */
        public boolean isLeaf(Object node) {
            return ((TimeCardNode) node).isLeaf();
        }

        /** Returns true if the value in column <code>column</code> of object
         *  <code>node</code> is editable. */
        public boolean isCellEditable(Object node, int column) {
            return (column == 0);
        }


        //
        //  The TreeTableNode interface.
        //

        /** Returns the number of columns. */
        public int getColumnCount() { return 32; }

        private String totalColumnName =
            resources.getString("Time_Card.Total_Column_Name");

        /** Returns the name for a particular column. */
        public String getColumnName(int column) {
            return (column == 31 ? totalColumnName :
                    Integer.toString(column+1));
        }

        /** Returns the class for the particular column. */
        public Class getColumnClass(int column) { return String.class; }

        /** Returns the value of the particular column. */
        public Object getValueAt(Object node, int column) {
            TimeCardNode n = (TimeCardNode) node;
            column++;
            if (column == 32) column = 0;
            return format(n.getTime(column));
        }

        /** Set the value at a particular row/column */
        public void setValueAt(Object aValue, Object node, int column) {}

        public boolean isWeekend(int col) {
            if (col > 30)
                return false;
            int dayOfWeek = (firstDayOfWeek + col) % 7;
            return dayOfWeek == (Calendar.SUNDAY % 7)
                    || dayOfWeek == (Calendar.SATURDAY % 7);
        }
    }

    protected String format(double time) {
        return format(time, format);
    }
    protected String format(double time, int format) {
        if (time == 0) return "";
        if (format == HOURS_MINUTES)
            return FormatUtil.formatTime(time);
        else if (format == MINUTES)
            return Integer.toString((int) time);
        else
            return formatter.format(time / 60.0);
    }

    private static NumberFormat formatter = NumberFormat.getNumberInstance();
    private SimpleDateFormat dayFormat = new SimpleDateFormat
        (resources.getString("Time_Card.Column_Date_Tooltip_Format"));
    static {
        formatter.setMaximumFractionDigits(2);
    }

    private Color mixColors(Color a, Color b, float r) {
        float s = 1.0f - r;
        return new Color((a.getRed()   * r + b.getRed()   * s) / 255f,
                         (a.getGreen() * r + b.getGreen() * s) / 255f,
                         (a.getBlue()  * r + b.getBlue()  * s) / 255f);
    }

    class TimeCardRenderer extends DefaultTableCellRenderer {

        /** Various foreground colors to use. */
        Color[] foregrounds;
        private static final int REGULAR = 0, EXPANDED = 1, SELECTED = 2;

        /** The alternate background color to use for weekend columns. */
        Color weekendBackground;

        public TimeCardRenderer() {
            Color background = treeTable.getBackground();
            this.foregrounds = new Color[4];
            this.foregrounds[REGULAR] = treeTable.getForeground();
            this.foregrounds[EXPANDED] = mixColors(treeTable.getForeground(),
                background, 0.5f);
            this.foregrounds[SELECTED] = treeTable.getSelectionForeground();
            this.foregrounds[EXPANDED + SELECTED] = mixColors(
                treeTable.getSelectionForeground(), background, 0.5f);
            this.weekendBackground = new Color(0xE3ECF5);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            setBackground(isWeekendColumn(column) ? weekendBackground
                    : table.getBackground());
            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, column);
            int fgPos = (isExpandedRow(row) ? EXPANDED : REGULAR)
                    + (isSelected ? SELECTED : REGULAR);
            result.setForeground(foregrounds[fgPos]);

            return result;
        }

        private boolean isWeekendColumn(int col) {
            return model.isWeekend(col);
        }

        private boolean isExpandedRow(int row) {
            return treeTable.getTree().isExpanded(row);
        }
    }

}
