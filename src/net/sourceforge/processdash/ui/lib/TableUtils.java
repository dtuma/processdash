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

import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class TableUtils {

    public static int configureTable(JTable table, int[] columnWidths,
            String[] tooltips) {

        // setup column widths
        int totalWidth = 0;
        if (columnWidths != null) {
            if (columnWidths.length != table.getColumnCount())
                throw new IllegalArgumentException(
                        "Mismatch in number of columns between table model and column width array");
            for (int i = 0; i < columnWidths.length; i++) {
                TableColumn column = table.getColumnModel().getColumn(i);
                column.setPreferredWidth(columnWidths[i]);
                totalWidth += columnWidths[i];
            }
        }

        // setup column tooltips
        if (tooltips != null)
            ToolTipTableCellRendererProxy
                    .installHeaderToolTips(table, tooltips);

        return totalWidth;
    }

    public static TableColumnModel cloneTableColumnModel(TableColumnModel m) {
        DefaultTableColumnModel result = new DefaultTableColumnModel();
        for (int i = 0;  i < m.getColumnCount();  i++)
            result.addColumn(cloneTableColumn(m.getColumn(i)));
        return result;
    }

    public static TableColumn cloneTableColumn(TableColumn c) {
        TableColumn result = new TableColumn(c.getModelIndex(),
                c.getPreferredWidth(), c.getCellRenderer(),
                c.getCellEditor());
        result.setMaxWidth(c.getMaxWidth());
        result.setMinWidth(c.getMinWidth());
        result.setResizable(c.getResizable());
        result.setHeaderValue(c.getHeaderValue());
        result.setHeaderRenderer(c.getHeaderRenderer());
        result.setIdentifier(c.getIdentifier());
        return result;
    }

    public static int convertColumnIndexToView(TableColumnModel model,
            int modelCol) {
        if (modelCol < 0)
            return modelCol;
        for (int viewCol = 0; viewCol < model.getColumnCount(); viewCol++) {
            if (model.getColumn(viewCol).getModelIndex() == modelCol) {
                return viewCol;
            }
        }
        return -1;
    }

}
