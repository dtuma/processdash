// Copyright (C) 2005-2017 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;

public class WrappedTextTableCellRenderer extends JTextArea implements
        TableCellRenderer {

    JTable table;

    public WrappedTextTableCellRenderer(JTable table) {
        super();
        this.table = table;
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object text,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (text == null)
            setText(null);
        else
            setText(text.toString());
        setFont(table.getFont());

        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        setSize(new Dimension(columnWidth, 1000));
        int requiredHeight = getPreferredSize().height + table.getRowMargin();
        int currentHeight = table.getRowHeight(row);
        if (currentHeight != requiredHeight)
            table.setRowHeight(row, requiredHeight);

        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }

        return this;
    }
}
