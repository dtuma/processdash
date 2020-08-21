// Copyright (C) 2010-2020 Tuma Solutions, LLC
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

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class WorkflowSizeUnitsColumn extends TaskSizeUnitsColumn implements
        CustomRenderedColumn {

    public WorkflowSizeUnitsColumn(DataTableModel dataModel,
            TeamProcess teamProcess) {
        super(dataModel, teamProcess);
        this.preferredWidth = 130;
    }

    @Override
    public void storeDependentColumn(String ID, int columnNumber) {}

    @Override
    public boolean isCellEditable(WBSNode node) {
        return isProbeTask(node);
    }

    private static boolean isProbeTask(JTable table, int row) {
        DataTableModel dataModel = (DataTableModel) table.getModel();
        WBSNode node = dataModel.getWBSModel().getNodeForRow(row);
        return isProbeTask(node);
    }

    private static boolean isProbeTask(WBSNode node) {
        return node != null && node.getIndentLevel() > 1
                && TeamProcess.isProbeTask(node.getType());
    }

    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    private static class CellRenderer extends WorkflowTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            if (value != null) {
                if (isProbeTask(table, row))
                    value = String.valueOf(value);
                else
                    value = null;
            }

            return super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);
        }

    }

    public static final String ATTR_NAME = COLUMN_ID;

}
