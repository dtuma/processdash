// Copyright (C) 2020 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;

import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.SizeMetric;
import teamdash.wbs.SizeMetricsWBSModel;


public class SizeMetricCellEditor extends AutocompletingDataTableCellEditor
        implements TableModelListener {

    private SizeMetricsWBSModel sizeMetrics;

    private DataTableModel editedModel;

    private DataColumn editedColumn;

    public SizeMetricCellEditor(SizeMetricsWBSModel sizeMetrics,
            DataTableModel editedModel, DataColumn editedColumn) {
        this.sizeMetrics = sizeMetrics;
        this.editedModel = editedModel;
        this.editedColumn = editedColumn;
        if (editedModel != null && editedColumn != null)
            sizeMetrics.addTableModelListener(this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        // get the textual display of the starting value
        String valueText = (value == null ? null : value.toString().trim());
        int selectedItemPos = -1;

        // update the combo box with the current list of size metrics
        JComboBox cb = getComboBox();
        cb.removeAllItems();
        for (SizeMetric sm : sizeMetrics.getIdToMetricMap().values()) {
            if (sm.getName().equalsIgnoreCase(valueText))
                selectedItemPos = cb.getItemCount();
            cb.addItem(sm.getName());
        }

        // call parent logic to set up the editor
        Component result = super.getTableCellEditorComponent(table, valueText,
            isSelected, row, column);

        // set the selected item for the combo box
        if (selectedItemPos != -1)
            cb.setSelectedIndex(selectedItemPos);

        return result;
    }

    public SizeMetric parseValue(Object aValue, boolean createIfMissing) {
        if (aValue instanceof SizeMetric) {
            return (SizeMetric) aValue;

        } else if (aValue instanceof String) {
            String text = SizeMetricsWBSModel.scrubMetricName((String) aValue)
                    .trim();
            if ("".equals(text) || text.startsWith("?"))
                return null;
            else
                return sizeMetrics.getMetric(text, createIfMissing);

        } else {
            return null;
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        editedModel.columnChanged(editedColumn);
    }

}
