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
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellPercentRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TableFontHandler;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowJTable;
import teamdash.wbs.excel.ExcelValueExporter;

public class WorkflowPercentageColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn, CustomEditedColumn {

    private WBSModel wbsModel;

    public WorkflowPercentageColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = resources.getString("Workflow.Percent.Name");
        this.columnID = COLUMN_ID;
        this.preferredWidth = 60;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return isWorkflowTask(node) || isWorkflowNode(node);
    }

    private boolean isWorkflowTask(WBSNode node) {
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
        if (isWorkflowNode(node)) {
            if (value == NORMALIZE_WORKFLOW_VALUE)
                normalizeWorkflow(node);
        } else if (value == 0 || value == 100)
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
        if (isWorkflowTask(node)) {
            sum = getExplicitValueForNode(node);
        } else {
            for (WBSNode child : wbsModel.getChildren(node))
                sum += recalculate(child);
        }
        node.setNumericAttribute(ROLLUP_ATTR, sum);
        return sum;
    }

    public void normalizeWorkflow(WBSNode node) {
        // find the workflow to which this node belongs.
        WBSNode workflow = node;
        while (workflow != null && workflow.getIndentLevel() > 1)
            workflow = wbsModel.getParent(workflow);
        if (!isWorkflowNode(workflow))
            return;

        // retrieve the total percentage calculated for this workflow
        double total = workflow.getNumericAttribute(ROLLUP_ATTR);
        double mult = 100 / total;
        if (Double.isNaN(mult) || Double.isInfinite(mult))
            return;

        // multiply all children by the desired factor to normalize to 100%
        for (WBSNode child : wbsModel.getDescendants(workflow)) {
            if (TeamTimeColumn.isLeafTask(wbsModel, child)) {
                double onePct = getExplicitValueForNode(child);
                child.setNumericAttribute(ATTR_NAME, onePct * mult);
            }
        }
    }

    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    public TableCellEditor getCellEditor() {
        return new CellEditor();
    }

    private static class CellRenderer extends DataTableCellPercentRenderer
            implements ExcelValueExporter {

        private String normalizeTip;

        public CellRenderer() {
            super(1);
            this.normalizeTip = resources
                    .getString("Workflow.Percent.Normalize_Tooltip");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            // tweak numeric values to work as percentages
            boolean isWorkflowRollup = false;
            boolean isNormalized = false;
            if (value instanceof NumericDataValue) {
                NumericDataValue ndv = (NumericDataValue) value;
                isWorkflowRollup = !ndv.isEditable;
                if (equal(ndv.value, 0, 0.05) || equal(ndv.value, 100, 0.05))
                    ndv.isInvisible = isNormalized = true;
                ndv.value /= 100;
            }

            // call superclass logic to format
            super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);

            // provide special formatting for workflow rollup cells
            if (isWorkflowRollup) {
                setFont(TableFontHandler.getItalic(table));
                setForeground(Color.gray);
                setBackground(WorkflowJTable.UNEDITABLE);
                setToolTipText(isNormalized ? null : normalizeTip);
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

    private class CellEditor extends DefaultCellEditor implements Runnable {

        private boolean armed;

        public CellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            this.armed = isDoubleClick(e);
            return super.isCellEditable(e);
        }

        private boolean isDoubleClick(EventObject e) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                return me.getClickCount() == 2 && !me.isShiftDown()
                        && !me.isControlDown() && !me.isMetaDown();
            } else {
                return false;
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            // if this is the top-level node for a workflow, set a trigger for
            // normalizing the percentages; then arrange for the edit to be
            // terminated
            if (isWorkflowNode(value)) {
                if (armed)
                    value = NORMALIZE_WORKFLOW_VALUE;
                SwingUtilities.invokeLater(this);
            }

            Component result = super.getTableCellEditorComponent(table, value,
                isSelected, row, column);
            if (result instanceof JTextField)
                ((JTextField) result).selectAll();
            return result;
        }

        private boolean isWorkflowNode(Object value) {
            if (!(value instanceof NumericDataValue))
                return false;

            NumericDataValue ndv = (NumericDataValue) value;
            if (ndv.isEditable)
                return false;

            if (equal(ndv.value, 100, 0.01))
                armed = false;
            return true;
        }

        public void run() {
            if (armed)
                stopCellEditing();
            else
                cancelCellEditing();
        }
    }


    private static final String ATTR_NAME = "Workflow Percentage";
    public static final String COLUMN_ID = ATTR_NAME;
    private static final String ROLLUP_ATTR = TopDownBottomUpColumn
            .getBottomUpAttrName(ATTR_NAME);
    private static final int NORMALIZE_WORKFLOW_VALUE = -1;

}
