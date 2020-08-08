// Copyright (C) 2002-2016 Tuma Solutions, LLC
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

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowDataModel;

public class WorkflowRateColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn, WorkflowOptionalColumn {

    private DataTableModel dataModel;
    private WBSModel wbsModel;

    public WorkflowRateColumn(DataTableModel dataModel) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = resources.getString("Workflow.Rate.Name");
        this.columnID = COLUMN_ID;
        this.preferredWidth = 50;
        this.dependentColumns = new String[] {
            WorkflowNumPeopleColumn.COLUMN_ID,
            WorkflowPercentageColumn.COLUMN_ID };
        setConflictAttributeName(ATTR_NAME);
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

        // when the rate appears or disappears, it can affect the appearance
        // of the "%" and "Units" columns.  Fire an event on the table so it
        // can repaint those cells.
        int row = dataModel.getWBSModel().getRowForNode(node);
        if (row > 0)
            dataModel.fireTableRowsUpdated(row, row);
    }


    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return false;
    }
    private void recalc(WBSNode node) {
        if (TeamTimeColumn.isLeafTask(wbsModel, node)) {
            // use the workflow percentage to adjust the rate
            double baseRate = getValueForNode(node);   // units per hour
            double baseTimePerUnit = 1.0 / baseRate;   // hours per unit
            double percentage = WorkflowPercentageColumn
                    .getImplicitValueForNode(node) / 100.0;
            double effectiveTimePerUnit = percentage * baseTimePerUnit;
            double effectiveRate = 1.0 / effectiveTimePerUnit;

            // rate/percentage-based planning can take two very different modes:
            //  1) The percentage describes a percentage of the overall
            //     workflow.  When multiple people are working, their aggregate
            //     team time should add up to this percentage.
            //  2) This task is occurring at a specific rate, like a code
            //     review that proceeds at 200 LOC/Hr.  Each person
            //     should be redundantly spending the same amount of time.
            // To discern the difference between the two, we look at the
            // percentage.  If it is "100%", we assume the second mode.
            // Otherwise, if it is less than 100%, we want to adjust the
            // per-person rate by the number of people, so the final team
            // total will add back up to the requested percentage.
            double numPeople = WorkflowNumPeopleColumn.getNumPeopleAt(node);
            if (percentage > 0 && percentage < 1 && numPeople > 1)
                effectiveRate = effectiveRate * numPeople;

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
    }

    public boolean shouldHideColumn(WorkflowDataModel model) {
        WBSModel wbsModel = model.getWBSModel();
        for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot())) {
            if (node.getIndentLevel() > 1
                    && node.getNumericAttribute(ATTR_NAME) > 0)
                return false;
        }
        return true;
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


    private static final String ATTR_NAME = "Workflow Rate";
    public static final String COLUMN_ID = ATTR_NAME;

}
