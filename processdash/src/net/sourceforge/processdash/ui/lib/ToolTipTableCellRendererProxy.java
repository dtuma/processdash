// Copyright (C) 2001-2003 Tuma Solutions, LLC
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
