// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowPercentageColumn extends AbstractNumericColumn implements
        CustomRenderedColumn {

    private WBSModel wbsModel;

    public WorkflowPercentageColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = resources.getString("Workflow.Percent.Name");
        this.columnID = COLUMN_ID;
        this.preferredWidth = 60;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : null);
    }

    protected double getValueForNode(WBSNode node) {
        return getImplicitValueForNode(node);
    }

    protected void setValueForNode(double value, WBSNode node) {
        // if the user deletes the value in this cell, the superclass logic
        // will interpret that as zero.  So zero passed in really means
        // "empty cell." In addition, 100% really means "no percentage is
        // active for this task," so we interpret that as null too.
        if (value == 0 || value == 100)
            node.setAttribute(ATTR_NAME, null);
        else if (value > 0 && value < 100)
            node.setNumericAttribute(ATTR_NAME, value);
    }

    protected static double getImplicitValueForNode(WBSNode node) {
        return getValueForNode(node, 100);
    }

    public static double getExplicitValueForNode(WBSNode node) {
        return getValueForNode(node, 0);
    }

    private static double getValueForNode(WBSNode node, double defaultVal) {
        double d = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(d))
            d = defaultVal;
        return d;
    }

    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    private static class CellRenderer extends WorkflowTableCellRenderer {

        public CellRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        protected Object tweakNumericValue(NumericDataValue ndv, JTable table,
                int row) {
            if (ndv.value == 100) {
                return null;
            } else {
                return resources.format("Workflow.Percent.Num_FMT",
                    String.valueOf(ndv));
            }
        }

    }

    private static final String ATTR_NAME = "Workflow Percentage";
    static final String COLUMN_ID = ATTR_NAME;

}
