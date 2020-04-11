// Copyright (C) 2010-2020 Tuma Solutions, LLC
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

import java.awt.Component;

import javax.swing.JTable;

import teamdash.wbs.DataTableCellRenderer;
import teamdash.wbs.NumericDataValue;

public class WorkflowTableCellRenderer extends DataTableCellRenderer {

    public WorkflowTableCellRenderer() {}

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof NumericDataValue)
            value = tweakNumericValue((NumericDataValue) value, table, row);

        return super.getTableCellRendererComponent(table, value, isSelected,
            hasFocus, row, column);
    }

    protected Object tweakNumericValue(NumericDataValue ndv, JTable table,
            int row) {
        return ndv;
    }

    protected boolean isRatePresent(JTable table, int row) {
        Object value = table.getModel().getValueAt(row, RATE_COLUMN);
        if (value instanceof NumericDataValue) {
            NumericDataValue ndv = (NumericDataValue) value;
            return ndv.value > 0;
        }
        return false;
    }

    private static final int RATE_COLUMN = 2;

}
