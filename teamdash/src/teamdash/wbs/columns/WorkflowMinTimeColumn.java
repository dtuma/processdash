// Copyright (C) 2016 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowModel;

public class WorkflowMinTimeColumn extends AbstractNumericColumn implements
        CustomRenderedColumn, WorkflowOptionalColumn {

    public static final String ATTR_NAME = "Workflow Min Time";

    private static final String COLUMN_ID = ATTR_NAME;

    private WBSModel wbsModel;

    public WorkflowMinTimeColumn(DataTableModel dataModel) {
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = "Min Time";
        this.columnID = COLUMN_ID;
        this.preferredWidth = 100;
        setConflictAttributeName(ATTR_NAME);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    @Override
    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : null);
    }

    @Override
    public double getValueForNode(WBSNode node) {
        return getMinTimeAt(node);
    }

    public static double getMinTimeAt(WBSNode node) {
        return node.getNumericAttribute(ATTR_NAME);
    }

    @Override
    protected void setValueForNode(double value, WBSNode node) {
        if (value == 0 || Double.isNaN(value))
            node.removeAttribute(ATTR_NAME);
        else
            node.setNumericAttribute(ATTR_NAME, value);
    }

    public boolean shouldHideColumn(WorkflowModel model) {
        return !model.getWBSModel().containsAttr(ATTR_NAME);
    }

    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    private static class CellRenderer extends WorkflowTableCellRenderer {

        public CellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        protected Object tweakNumericValue(NumericDataValue ndv, JTable table,
                int row) {
            // the number 0 should be made invisible (i.e., replaced with null)
            return (ndv.value == 0 ? null : ndv);
        }

    }

}
