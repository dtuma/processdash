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
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.Date;

public class TaskScheduleDialog
    implements EVTaskList.Listener, EVSchedule.Listener, ActionListener
{

    /** Model for the JTreeTable */
    protected EVTaskList model;
    /** TreeTable displaying the task list */
    protected JTreeTable treeTable;
    /** table displaying the schedule */
    protected JTable scheduleTable;
    /** Frame containing everything */
    protected JFrame frame;
    /** timer for triggering recalculations */
    protected Timer recalcTimer;


    public TaskScheduleDialog(PSPDashboard dash) {
        // Create the frame and set an appropriate icon
        frame = new JFrame("Task and Schedule");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));

        // Create the earned value model.
        model = new EVTaskList("My Task List", dash.data, dash.props, true);
        model.recalc();
        model.addEVTaskListListener(this);
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

        // Create a JTable to display the schedule list.
        scheduleTable = new ScheduleJTable(model.getSchedule());
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (scheduleTable, EVSchedule.toolTips);
                                // set default widths for the columns
        for (int i = 0;  i < EVSchedule.colWidths.length;  i++)
            scheduleTable.getColumnModel().getColumn(i)
                .setPreferredWidth(EVSchedule.colWidths[i]);


        JSplitPane jsp = new JSplitPane
            (JSplitPane.VERTICAL_SPLIT, true,
             new JScrollPane(treeTable,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
             new JScrollPane(scheduleTable));
        jsp.setResizeWeight(0.5);
        frame.getContentPane().add(jsp);

        frame.setSize(new Dimension(580, 600));
        frame.show();
        recalcTimer = new Timer(Integer.MAX_VALUE, this);
        recalcTimer.setInitialDelay(1000);
        recalcTimer.setRepeats(false);
    }

    public void show() { frame.show(); }


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
            if (isSelected)
                result.setBackground(selectedBackgroundColor);
            else
                result.setBackground(backgroundColor);
            result.setForeground(useAltForeground(row) ?
                                 altForeground : table.getForeground());
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


    public void evNodeChanged(EVTaskList.Event e) {
        TreePath tp = new TreePath(e.getNode().getPath());
        int row = treeTable.getTree().getRowForPath(tp);
        if (row != -1) {
            AbstractTableModel model =
                (AbstractTableModel) treeTable.getModel();
            model.fireTableChanged(new TableModelEvent(model, row));
        }
        recalcTimer.restart();
    }

    public void evScheduleChanged() { actionPerformed(null); }

    public void actionPerformed(ActionEvent e) {
        model.recalc();
        AbstractTableModel model =
            (AbstractTableModel) treeTable.getModel();
        model.fireTableChanged(new TableModelEvent(model, 0,
                                                   treeTable.getRowCount()-1));
    }
}
