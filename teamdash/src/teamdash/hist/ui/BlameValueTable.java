// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class BlameValueTable extends JTable {

    public BlameValueTable(BlameValueTableModel model) {
        super(model);

        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);
        setRowHeight(new JLabel("X").getPreferredSize().height + 4);

        getTableHeader().setPreferredSize(new Dimension(0, 0));

        TableColumn col = getColumnModel().getColumn(0);
        col.setCellRenderer(new BlameTimelineRenderer());
        col.setMinWidth(getRowHeight());
        col.setMaxWidth(getRowHeight());
    }

    public void autoResizeColumns() {
        int cellPad = 0;
        for (int column = getColumnCount(); column-- > 1;) {
            int minWidth = 0;
            for (int row = getRowCount(); row-- > 0;) {
                int cellWidth = getPreferredCellWidth(row, column);
                minWidth = Math.max(minWidth, cellWidth);
            }
            TableColumn tableCol = getColumnModel().getColumn(column);
            tableCol.setMinWidth(minWidth + cellPad);
            tableCol.setPreferredWidth(minWidth + cellPad);
            cellPad = 8;
        }
    }

    private int getPreferredCellWidth(int row, int column) {
        TableCellRenderer cellRenderer = getCellRenderer(row, column);
        Component c = prepareRenderer(cellRenderer, row, column);
        int width = c.getPreferredSize().width + getIntercellSpacing().width;
        return width;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component result = super.prepareRenderer(renderer, row, column);
        if (getValueAt(row, 0) == null)
            result.setBackground(Color.LIGHT_GRAY);
        else
            result.setBackground((row & 1) == 0 ? getBackground()
                    : ROW_HIGHLIGHT);
        return result;
    }

    private static final Color ROW_HIGHLIGHT = new Color(240, 240, 255);

}
