// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowRateColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    private DataTableModel dataModel;
    private WBSModel wbsModel;
    private int percentageColumn = -1;

    public WorkflowRateColumn(DataTableModel dataModel) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = "Rate";
        this.columnID = "Workflow Rate";
        this.dependentColumns = new String[] {
            WorkflowPercentageColumn.COLUMN_ID };
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : "");
    }

    protected double getValueForNode(WBSNode node) {
        double d = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(d)) d = 0;
        return d;
    }

    protected void setValueForNode(double value, WBSNode node) {
        node.setNumericAttribute(ATTR_NAME, value);
    }


    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return false;
    }
    private void recalc(WBSNode node) {
        if (TeamTimeColumn.isLeafTask(wbsModel, node)) {
            double baseRate = getValueForNode(node);
            double baseProductivity = 1.0 / baseRate;
            double percentage = NumericDataValue.parse
                (dataModel.getValueAt(node, percentageColumn)) / 100.0;
            double effectiveProductivity = percentage * baseProductivity;
            double effectiveRate = 1.0 / effectiveProductivity;
            if (Double.isNaN(effectiveRate) ||
                Double.isInfinite(effectiveRate))
                node.setAttribute(TeamTimeColumn.RATE_ATTR, null);
            else
                node.setNumericAttribute(TeamTimeColumn.RATE_ATTR, effectiveRate);
        }

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0;   i < children.length;   i++)
            recalc(children[i]);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (WorkflowPercentageColumn.COLUMN_ID.equals(ID))
            percentageColumn = columnNumber;
    }

    public void resetDependentColumns() {
        percentageColumn = -1;
    }


    private static final String ATTR_NAME = "Workflow Rate";


}
