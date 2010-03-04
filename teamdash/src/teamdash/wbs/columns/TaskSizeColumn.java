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

import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class TaskSizeColumn extends SizeAliasColumn {

    private int unitsColumn = -1;

    public TaskSizeColumn(DataTableModel dataModel, TeamProcess teamProcess) {
        super(dataModel, "Task Size", "N&C-", teamProcess.getSizeMetrics(),
                teamProcess.getWorkProductSizeMap());

        int len = this.dependentColumns.length;
        String [] dependentCols = new String[len+1];
        System.arraycopy(this.dependentColumns, 0, dependentCols, 0, len);
        dependentCols[len] = TaskSizeUnitsColumn.COLUMN_ID;
        this.dependentColumns = dependentCols;
        this.preferredWidth = 65;
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

    protected String getSizeUnit(WBSNode node) {
        Object result = dataModel.getValueAt(node, unitsColumn);
        return (result == null ? null : String.valueOf(result));
    }

    private boolean isCustomTaskSize(WBSNode node) {
        return getSizeColumn(node) == -1;
    }

    public boolean isCellEditable(WBSNode node) {
        // if the user has entered a custom size metric for this node, then
        // the size value is editable.
        if (isCustomTaskSize(node))
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
            result = new NumericDataValue(ndv.value, ndv.isEditable, false,
                    ndv.errorMessage, ndv.expectedValue);
        }
        return result;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (isCustomTaskSize(node)) {
            node.setNumericAttribute(ATTR_NAME, NumericDataValue.parse(aValue));
        } else {
            super.setValueAt(aValue, node);
        }
    }

    private static final String ATTR_NAME = EditableSizeColumn.ATTR_NAME;

    public boolean recalculate() { return true; }

}
