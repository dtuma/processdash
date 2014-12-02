// Copyright (C) 2014 Tuma Solutions, LLC
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
import java.util.Map;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

import teamdash.wbs.AutocompletingDataTableCellEditor;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class ProxyEstTypeColumn extends AbstractDataColumn implements
        CustomEditedColumn, CalculatedDataColumn {

    private static final String ATTR_NAME = "Estimated Proxy Type";

    public static final String COLUMN_ID = ATTR_NAME;


    private DataTableModel dataModel;

    private ProxyWBSModel proxyModel;

    public ProxyEstTypeColumn(DataTableModel dataModel, ProxyWBSModel proxies) {
        this.dataModel = dataModel;
        this.proxyModel = proxies;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Proxy_Est_Type.Name");
        this.preferredWidth = 200;
        setConflictAttributeName(ATTR_NAME);

        proxies.addTableModelListener(new ProxyNameListener());
    }

    public Object getValueAt(WBSNode node) {
        Integer nodeValue = node.getIntegerAttribute(ATTR_NAME);
        if (nodeValue != null)
            return proxyModel.getNameForProxy(nodeValue);
        else
            return null;
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        String name = (String) aValue;

        if (name == null || name.trim().length() == 0) {
            // the user has deleted the value in the cell.
            node.removeAttribute(ATTR_NAME);

        } else {
            // look up the milestone with the given name.
            Integer id = proxyModel.getIdForProxy(name);
            if (id != null)
                node.setAttribute(ATTR_NAME, id);
        }
    }

    @Override
    public void storeConflictResolutionValue(Object value, WBSNode node) {
        node.setAttribute(ATTR_NAME, value);
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public TableCellEditor getCellEditor() {
        return new ProxyTypeCellEditor();
    }


    private class ProxyTypeCellEditor extends AutocompletingDataTableCellEditor {


        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known proxies.
            getComboBox().removeAllItems();
            for (String name : proxyModel.getProxyIdMap().keySet())
                getComboBox().addItem(name);

            // now defer to the parent for the rest of the work.
            return super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);
        }

    }


    private class ProxyNameListener implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            dataModel.columnChanged(ProxyEstTypeColumn.this);
        }

    }


    public static void remapNodeIDs(WBSModel model, Map<Integer, Integer> idMap) {
        if (idMap != null && !idMap.isEmpty()) {
            for (WBSNode node : model.getDescendants(model.getRoot()))
                remapNodeIDs(node, idMap);
        }
    }

    private static void remapNodeIDs(WBSNode node, Map<Integer, Integer> idMap) {
        Integer nodeValue = node.getIntegerAttribute(ATTR_NAME);
        if (nodeValue != null) {
            Integer newValue = idMap.get(nodeValue);
            if (newValue != null)
                node.setAttribute(ATTR_NAME, newValue);
        }
    }

}
