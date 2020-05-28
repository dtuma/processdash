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

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

public class TaskSizeColumn extends SizeAliasColumn implements
        CustomRenderedColumn {

    public static final String COLUMN_ID = "Task Size";

    private int unitsColumn = -1;

    public TaskSizeColumn(DataTableModel dataModel, TeamProcess teamProcess) {
        super(dataModel, COLUMN_ID, "Task_Size.Name", "N&C-",
                teamProcess.getSizeMetrics(), null);

        int len = this.dependentColumns.length;
        String [] dependentCols = new String[len+1];
        System.arraycopy(this.dependentColumns, 0, dependentCols, 0, len);
        dependentCols[len] = TaskSizeUnitsColumn.COLUMN_ID;
        this.dependentColumns = dependentCols;
        this.preferredWidth = 65;
        setConflictAttributeName(ATTR_NAME);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (TaskSizeUnitsColumn.COLUMN_ID.equals(ID))
            unitsColumn = columnNumber;
        else
            super.storeDependentColumn(ID, columnNumber);
    }

    public void resetDependentColumns() {
        unitsColumn = -1;
        super.resetDependentColumns();
    }

    @Override
    protected String getSizeUnit(WBSNode node) {
        Object result = dataModel.getValueAt(node, unitsColumn);
        return (result == null ? null : String.valueOf(result));
    }

    private boolean isCustomTaskSize(WBSNode node) {
        return getSizeColumn(node) == -1;
    }

    private boolean canEditTaskSizeUnits(WBSNode node) {
        return unitsColumn != -1
            && dataModel.isCellEditable(node, unitsColumn);
    }

    public boolean isCellEditable(WBSNode node) {
        // if this is a task with a custom unit of size measure, the size
        // is editable.
        if (isCustomTaskSize(node))
            return true;

        // if the task size units column is editable, then we can auto-
        // alter it to have a custom unit (which would, in turn, make this
        // value editable).  If so, this cell is effectively editable too.
        if (canEditTaskSizeUnits(node))
            return true;

        // otherwise, ask our superclass if the value is editable.
        return super.isCellEditable(node);
    }

    public Object getValueAt(WBSNode node) {
        if (isCustomTaskSize(node)) {
            double miscSize = node.getNumericAttribute(ATTR_NAME);
            if (Double.isNaN(miscSize))
                return null;
            else
                return new NumericDataValue(miscSize);
        }

        Object result = super.getValueAt(node);
        if (result instanceof NumericDataValue && result != BLANK) {
            NumericDataValue ndv = (NumericDataValue) result;
            boolean editable = ndv.isEditable || canEditTaskSizeUnits(node);
            String error = ndv.errorMessage;
            if (error == null && editable && !ndv.isEditable)
                error = INHERITED_SIZE_MESSAGE;
            result = new NumericDataValue(ndv.value, editable, false, error,
                    ndv.expectedValue);
        }
        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (isCustomTaskSize(node)) {
            // if this node already has a custom task size in effect, just
            // update the existing custom size value.
            node.setNumericAttribute(ATTR_NAME, NumericDataValue.parse(aValue));

        } else if (super.isCellEditable(node)) {
            // if this node does not have a custom size, but if the underlying
            // aliased size column is editable, write the data through.
            try {
                SizeDataColumn.setTopDownEditMode(true);
                super.setValueAt(aValue, node);
            } finally {
                SizeDataColumn.setTopDownEditMode(false);
            }

        } else if (canEditTaskSizeUnits(node)) {
            // this node is NOT using a custom size unit, and the currently
            // chosen size metric is not editable.  However, the units column
            // is editable, so we can alter it to become custom.  That will
            // allow us to store a new custom size value which will become
            // the new size.
            node.setNumericAttribute(ATTR_NAME, NumericDataValue.parse(aValue));
            Object units = WrappedValue.unwrap(dataModel.getValueAt(node,
                unitsColumn));
            String newUnits = (units == null ? "-" : units + " ");
            dataModel.setValueAt(newUnits, node, unitsColumn);
        }
    }

    public TableCellRenderer getCellRenderer() {
        return TASK_SIZE_RENDERER;
    }

    private static final String ATTR_NAME = EditableSizeColumn.ATTR_NAME;

    private static final String INHERITED_SIZE_MESSAGE =
        resources.getString("Task_Size.Inherited_Tooltip");
    private static final TableCellRenderer TASK_SIZE_RENDERER =
        new ItalicNumericCellRenderer(INHERITED_SIZE_MESSAGE);

    public boolean recalculate() { return true; }

}
