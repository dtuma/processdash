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

import java.util.Map;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

public class SizeAliasColumn extends AbstractNumericColumn
implements CalculatedDataColumn {

    protected static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);

    protected DataTableModel dataModel;
    private Map sizeTypes;
    private String[] sizeUnits;
    private int [] columns;

    public SizeAliasColumn(DataTableModel m, String columnID, String nameKey,
                           String accountingID, String [] sizeUnits,
                           Map sizeTypes)
    {
        this.dataModel = m;
        this.columnID = columnID;
        this.columnName = resources.getString(nameKey);

        this.sizeUnits = sizeUnits;
        columns = new int[sizeUnits.length];
        dependentColumns = new String[sizeUnits.length];
        for (int i = 0;   i < sizeUnits.length;   i++) {
            columns[i] = -1;
            dependentColumns[i] = accountingID + sizeUnits[i];
        }
        this.sizeTypes = sizeTypes;
    }

    public boolean recalculate() { return true; }


    public void storeDependentColumn(String ID, int columnNumber) {
        for (int i = sizeUnits.length;   i-- > 0; )
            if (ID.equals(dependentColumns[i])) {
                columns[i] = columnNumber;
                return;
            }
    }

    protected String getSizeUnit(WBSNode node) {
        return SizeTypeColumn.getWorkProductSizeMetric(node, sizeTypes);
    }

    protected int getSizeColumn(WBSNode node) {
        String unit = getSizeUnit(node);
        if (unit == null) return -1;
        for (int i = sizeUnits.length;   i-- > 0; )
            if (unit.equals(sizeUnits[i]))
                return columns[i];
        return -1;
    }

    public boolean isCellEditable(WBSNode node) {
        int column = getSizeColumn(node);
        return (column == -1 ? false : dataModel.isCellEditable(node, column));
    }

    public Object getValueAt(WBSNode node) {
        int column = getSizeColumn(node);
        if (column == -1) return BLANK;
        return dataModel.getValueAt(node, column);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        int column = getSizeColumn(node);
        if (column != -1)
            dataModel.setValueAt(aValue, node, column);
    }
    public void resetDependentColumns() {
        for (int i = 0;   i < columns.length;   i++)
            columns[i] = -1;
    }

}
