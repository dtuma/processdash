// Copyright (C) 2002-2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs.columns;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.MilestonesWBSModel;
import teamdash.wbs.WBSNode;

public abstract class MilestoneBooleanColumn extends AbstractDataColumn
        implements CustomRenderedColumn {

    private String attr;

    public MilestoneBooleanColumn(String attr) {
        this.attr = attr;
    }

    @Override
    public Class getColumnClass() {
        return Boolean.class;
    }

    public Object getValueAt(WBSNode node) {
        return (node.getAttribute(attr) != null);
    }

    public boolean isCellEditable(WBSNode node) {
        return MilestonesWBSModel.MILESTONE_TYPE.equals(node.getType());
    }

    public void setValueAt(Object value, WBSNode node) {
        if (value == Boolean.TRUE)
            node.setAttribute(attr, "true");
        else
            node.setAttribute(attr, null);
    }

    public abstract TableCellRenderer getCellRenderer();

    protected static class CellRenderer extends DefaultTableCellRenderer
            implements TableCellRenderer {

        private String tooltipHtml;

        public CellRenderer(String tooltipHtml) {
            this.tooltipHtml = tooltipHtml;
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            if (row == 0)
                return super.getTableCellRendererComponent(table, "",
                    isSelected, hasFocus, row, column);

            TableCellRenderer delegate = table.getDefaultRenderer(Boolean.class);
            Component result = delegate.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
            if (result instanceof JComponent) {
                JComponent jc = (JComponent) result;
                jc.setToolTipText("<html><div style='width:200px'>"
                        + tooltipHtml + "</div></html>");
            }
            return result;
        }

    }

}
