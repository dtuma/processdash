// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


package net.sourceforge.processdash.log.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;


import net.sourceforge.processdash.ev.*;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.*;
import net.sourceforge.processdash.log.*;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.*;
import net.sourceforge.processdash.ui.lib.*;

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

    public TimeCardDialog(DashHierarchy useProps, TimeLog timeLog) {
        this.useProps = useProps;
        this.timeLog = timeLog;

        frame = new JFrame(resources.getString("Time_Card.Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());

        model = new TimeCard(useProps, timeLog);
        treeTable = new JTreeTable(model);
        treeTable.setDefaultRenderer
            (String.class,
             new TimeCardRenderer(treeTable.getTree(),
                                  treeTable.getSelectionBackground(),
                                  treeTable.getBackground(),
                                  mixColors(treeTable.getBackground(),
                                            treeTable.getForeground(), 0.8f)));
        treeTable.setShowGrid(true);
        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        treeTable.getTableHeader().setReorderingAllowed(false);
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (treeTable, model.dayNames);

        JScrollPane sp = new JScrollPane
            (treeTable,
             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
             JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Box box = Box.createHorizontalBox();
        box.add(treeTable.getTree());
        box.add(Box.createHorizontalStrut(1));
        sp.setRowHeaderView(box);

        frame.getContentPane().add("Center", sp);
        frame.getContentPane().add("North", buildTopPanel());
        resizeColumns();
        PCSH.enableHelpKey(frame, "UsingTimeLogEditor.TimeCardView");

        frame.pack();
        frame.show();
    }
    private void setColWidth(TableColumn c, int width) {
        c.setMinWidth(width);
        c.setPreferredWidth(width);
    }
    private void resizeColumns() {
                                // set default widths for the columns
        boolean hideColumns = this.hideColumns.isSelected();
        int i = model.getColumnCount() - 1;
        setColWidth(treeTable.getColumnModel().getColumn(i), 64);
        int daysInMonth = model.getDaysInMonth();
        while (i-- > 0)
            setColWidth(treeTable.getColumnModel().getColumn(i),
                        (i<daysInMonth &&
                         (!hideColumns || !model.columnEmpty(i+1))) ? 32 : 0);
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
        resizeColumns();
    }
    public void redraw() {
        format = formatType.getSelectedIndex();
        ((AbstractTableModel) treeTable.getModel()).fireTableDataChanged();
    }




    private class TimeCard extends AbstractTreeTableModel {

        int month, year, daysInMonth;
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

            public String getName() { return name; }
            public String toString() { return name; }
            public double getTime(int day) { return time[day]; }
            public int getNumChildren() { return children.size(); }
            public boolean isLeaf() { return (getNumChildren() == 0); }
            public TimeCardNode getChild(int which) {
                return (TimeCardNode) children.get(which);
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
            cal.add(Calendar.MONTH, 1);
            Date to = cal.getTime();

            for (int i=32;  i-- > 0; )
                if (i >= daysInMonth)
                    dayNames[i] = null;
                else {
                    cal.set(year, month, i+1);
                    dayNames[i] = dayFormat.format(cal.getTime());
                }


            Enumeration e = timeLog.filter(null, from, to);
            TimeLogEntry tle;
            time = new double[32];
            int day;
            while (e.hasMoreElements()) {
                tle = (TimeLogEntry) e.nextElement();
                cal.setTime(tle.getCreateTime());
                day = cal.get(Calendar.DAY_OF_MONTH);

                root.addTime(tle.getPath(), day, tle.getElapsedTime());
                time[0] += tle.getElapsedTime();
                time[day] += tle.getElapsedTime();
            }
            root.prune();
            this.root = root;
            fireTreeStructureChanged(this, root.getPath(), null, null);
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
    }

    protected String format(double time) {
        return format(time, format);
    }
    protected String format(double time, int format) {
        if (time == 0) return "";
        if (format == HOURS_MINUTES)
            return EVTask.formatTime(time);
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

    class TimeCardRenderer extends ShadedTableCellRenderer {
        JTree tree;
        public TimeCardRenderer(JTree tree, Color sel, Color desel, Color fg) {
            super(sel, desel, fg);
            this.tree = tree;
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
        protected boolean useAltForeground(int row) {
            return tree.isExpanded(row);
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
    }

}
