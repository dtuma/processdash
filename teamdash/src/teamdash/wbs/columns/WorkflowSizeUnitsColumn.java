// Copyright (C) 2010-2012 Tuma Solutions, LLC
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
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class WorkflowSizeUnitsColumn extends TaskSizeUnitsColumn implements
        CustomRenderedColumn {

    private int rateColumn = -1;

    public WorkflowSizeUnitsColumn(DataTableModel dataModel,
            TeamProcess teamProcess) {
        super(dataModel, teamProcess);
        this.preferredWidth = 130;
        this.dependentColumns = new String[] { WorkflowRateColumn.COLUMN_ID };
    }

    @Override
    public void storeDependentColumn(String ID, int columnNumber) {
        if (WorkflowRateColumn.COLUMN_ID.equals(ID))
            rateColumn = columnNumber;
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        super.setValueAt(aValue, node);

        if (isProbeTask(node)) {
            // no special rate handling is required for PROBE tasks

        } else if (valueIsEmpty(aValue)) {
            // if the user just tried to delete the size units, we should
            // oblige by setting the rate to zero.  That will cause the
            // value in the units column to disappear.
            dataModel.setValueAt(0, node, rateColumn);

        } else {
            // if the user just selected something in this size units column,
            // but the rate is currently null or zero, then we need to put
            // some nonzero value (default "1") in the rate column to make
            // this column become visible.
            Object currentRate = dataModel.getValueAt(node, rateColumn);
            if (currentRate instanceof NumericDataValue) {
                NumericDataValue ndv = (NumericDataValue) currentRate;
                if (ndv.value == 0)
                    currentRate = null;
            }

            if (currentRate == null)
                dataModel.setValueAt(1, node, rateColumn);
        }
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
                if (isRatePresent(table, row))
                    value = resources.format("Workflow.Units.Per_Hour_FMT",
                        value);
                else if (isProbeTask(table, row))
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
