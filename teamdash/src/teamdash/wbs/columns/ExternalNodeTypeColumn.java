// Copyright (C) 2018-2020 Tuma Solutions, LLC
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
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.ExternalSystemManager.ExtNodeType;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;


public class ExternalNodeTypeColumn extends AbstractDataColumn implements
        ExternalSystemPrimaryColumn, CustomEditedColumn, CalculatedDataColumn {

    private String systemID;

    private List<ExtNodeType> nodeTypes;

    public ExternalNodeTypeColumn(String systemID, String systemName,
            List<ExtNodeType> nodeTypes) {
        this.systemID = systemID;
        this.columnID = systemID + " Type";
        this.columnName = resources.format("External_Type.Name_FMT",
            systemName);
        this.preferredWidth = 80;
        this.nodeTypes = nodeTypes.isEmpty() ? null : nodeTypes;
    }

    private boolean systemMatches(WBSNode node) {
        Object nodeSystemID = node.getAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR);
        return systemID.equals(nodeSystemID);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return nodeTypes != null //
                && TeamProcess.COMPONENT_TYPE.equals(node.getType()) //
                && !node.isReadOnly();
    }

    @Override
    public Object getValueAt(WBSNode node) {
        if (!systemMatches(node))
            return null;
        Object value = node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR);
        if (value != null && !isCellEditable(node))
            value = new ReadOnlyValue(value);
        return value;
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        if (!isCellEditable(node))
            return;

        if (aValue == null || aValue instanceof ExtNodeType) {
            storeType(node, (ExtNodeType) aValue);
        } else if (aValue instanceof String) {
            String name = ((String) aValue).trim();
            if (name.length() == 0) {
                storeType(node, null);
            } else {
                for (ExtNodeType type : nodeTypes)
                    if (type.getName().equals(name))
                        storeType(node, type);
            }
        }
    }

    public static void storeType(WBSNode node, ExtNodeType type) {
        if (type == null) {
            node.removeAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR);
            node.removeAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR);
            node.removeAttribute(ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR);
        } else {
            ExtSyncUtil.removeExtIDAttributes(node);
            node.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR,
                type.getExtSystem().getID());
            node.setAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR, type.getName());
            node.setAttribute(ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR, type.getId());
        }
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    @Override
    public TableCellEditor getCellEditor() {
        return new ExtNodeTypeCellEditor();
    }

    private class ExtNodeTypeCellEditor
            extends AutocompletingDataTableCellEditor {

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            // load the combo with node type names
            getComboBox().removeAllItems();
            if (nodeTypes != null) {
                for (ExtNodeType type : nodeTypes)
                    getComboBox().addItem(type.getName());
            }

            return super.getTableCellEditorComponent(table,
                WrappedValue.unwrap(value), isSelected, row, column);
        }

    }

}
