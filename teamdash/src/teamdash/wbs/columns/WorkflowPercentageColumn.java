// Copyright (C) 2002-2021 Tuma Solutions, LLC
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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellPercentRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TableFontHandler;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.excel.ExcelValueExporter;

public class WorkflowPercentageColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn {

    private WBSModel wbsModel;

    public WorkflowPercentageColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = resources.getString("Workflow.Percent.Name");
        this.columnID = COLUMN_ID;
        this.preferredWidth = 60;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node)
                && node.getIndentLevel() > 1;
    }

    private static boolean isWorkflowNode(WBSNode node) {
        return node != null && node.getIndentLevel() == 1;
    }

    public Object getValueAt(WBSNode node) {
        if (isWorkflowNode(node))
            return new NumericDataValue(getRollupValueForNode(node), false);
        else if (isCellEditable(node))
            return super.getValueAt(node);
        else
            return null;
    }

    protected double getValueForNode(WBSNode node) {
        return getExplicitValueForNode(node);
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

    public static double getRollupValueForNode(WBSNode node) {
        return node.getNumericAttribute(ROLLUP_ATTR);
    }

    public static double getExplicitValueForNode(WBSNode node) {
        double d = node.getNumericAttribute(ATTR_NAME);
        return (Double.isNaN(d) ? 0 : d);
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean recalculate() {
        recalculate(wbsModel.getRoot());
        return true;
    }

    private double recalculate(WBSNode node) {
        double sum = 0;
        if (isCellEditable(node)) {
            sum = getExplicitValueForNode(node);
        } else {
            for (WBSNode child : wbsModel.getChildren(node))
                sum += recalculate(child);
        }
        node.setNumericAttribute(ROLLUP_ATTR, sum);
        return sum;
    }

    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    private static class CellRenderer extends DataTableCellPercentRenderer
            implements ExcelValueExporter {

        public CellRenderer() {
            super(1);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            // tweak numeric values to work as percentages
            boolean isWorkflowRollup = false;
            if (value instanceof NumericDataValue) {
                NumericDataValue ndv = (NumericDataValue) value;
                isWorkflowRollup = !ndv.isEditable;
                if (equal(ndv.value, 0, 0.05) || equal(ndv.value, 100, 0.05))
                    ndv.isInvisible = true;
                ndv.value /= 100;
            }

            // call superclass logic to format
            super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);

            // provide special formatting for workflow rollup cells
            if (isWorkflowRollup) {
                setFont(TableFontHandler.getItalic(table));
                setForeground(Color.gray);
                setHorizontalAlignment(JLabel.CENTER);
            } else {
                setFont(table.getFont());
                setForeground(Color.black);
                setHorizontalAlignment(JLabel.RIGHT);
            }

            return this;
        }

        public Object getValueForExcelExport(Object value) {
            if (value instanceof NumericDataValue) {
                NumericDataValue ndv = (NumericDataValue) value;
                if (ndv.value == 100)
                    return null;
                else
                    ndv.value /= 100;
            }
            return value;
        }

    }

    private static final String ATTR_NAME = "Workflow Percentage";
    static final String COLUMN_ID = ATTR_NAME;
    private static final String ROLLUP_ATTR = TopDownBottomUpColumn
            .getBottomUpAttrName(ATTR_NAME);

}
