// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

import java.util.Map;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class EditableSizeColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    public static final String COLUMN_ID = "Size";

    static final String ATTR_NAME = "Misc Size";

    DataTableModel dataModel;
    Map sizeMetricsMap;
    int newChangedColumn;

    public EditableSizeColumn(DataTableModel m, TeamProcess process) {
        this.dataModel = m;
        this.sizeMetricsMap = process.getWorkProductSizeMap();
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Size.Name");
        this.dependentColumns = new String[] { "N&C" };
    }

    public boolean recalculate() { return true; }
    public boolean isCellEditable(WBSNode node) {
        NumericDataValue value = (NumericDataValue) getValueAt(node);
        return (value == null || value.isEditable);
    }


    public void storeDependentColumn(String ID, int columnNumber) {
        if ("N&C".equals(ID))
            newChangedColumn = columnNumber;
    }


    public Object getValueAt(WBSNode node) {
        NumericDataValue value =
            (NumericDataValue) dataModel.getValueAt(node, newChangedColumn);
        if (value != null && value.isEditable)
            return value;
        if (SizeTypeColumn.getWorkProductSizeMetric(node, sizeMetricsMap) != null)
            return value;

        double number = node.getNumericAttribute(ATTR_NAME);
        if (!Double.isNaN(number))
            return new NumericDataValue(number);
        else
            return new NumericDataValue(0,true,true,null);
    }


    public void setValueAt(Object aValue, WBSNode node) {
        NumericDataValue value =
            (NumericDataValue) dataModel.getValueAt(node, newChangedColumn);
        if (value != null && value.isEditable)
            dataModel.setValueAt(aValue, node, newChangedColumn);
        else
            node.setAttribute(ATTR_NAME, aValue);
    }

    public void resetDependentColumns() {
        newChangedColumn = -1;
    }

}
