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


package net.sourceforge.processdash.ui.lib;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/** Wraps a TableCellRenderer, and adds column-based tool tips.
 */
public class ToolTipTableCellRendererProxy implements TableCellRenderer {

    TableCellRenderer renderer;
    String[] toolTips;

    public static void installHeaderToolTips(JTable table, String[] toolTips) {
        // get the current default renderer for the table header.
        TableCellRenderer headerCellRenderer =
            table.getTableHeader().getDefaultRenderer();

        // construct a renderer proxy that adds tool tips.
        headerCellRenderer = new ToolTipTableCellRendererProxy
            (headerCellRenderer, toolTips);

        // install the new renderer proxy as the default renderer for
        // the table header.
        table.getTableHeader().setDefaultRenderer(headerCellRenderer);
    }

    public ToolTipTableCellRendererProxy(TableCellRenderer renderer,
                                         String [] toolTips) {
        this.renderer = renderer;
        this.toolTips = toolTips;
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Component result = renderer.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        if (table != null && result instanceof JComponent) {
            String tip = toolTips[table.convertColumnIndexToModel(column)];
            ((JComponent) result).setToolTipText(tip);
        }
        return result;
    }
}
