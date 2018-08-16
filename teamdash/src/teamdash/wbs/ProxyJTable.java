// Copyright (C) 2014-2018 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;

import teamdash.wbs.columns.ProxySizeColumn;

public class ProxyJTable extends WBSJTable {

    public ProxyJTable(ProxyDataModel model) {
        super(model, makeIconMap(), new JMenu());

        customizeColumns();
        tweakBehaviors();
    }

    private static Map makeIconMap() {
        Map result = new HashMap();
        result.put(ProxyWBSModel.PROXY_LIST_TYPE,
            WBSZoom.icon(IconFactory.getProxyListIcon()));
        result.put(ProxyWBSModel.PROXY_TYPE,
            WBSZoom.icon(IconFactory.getProxyTableIcon()));
        result.put(null, WBSZoom.icon(IconFactory.getModifiedIcon(
            HierarchyIcons.getComponentIcon(), IconFactory.ERROR_ICON)));
        return result;
    }

    @Override
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (iconMap != null) {
            iconMap.put(ProxyWBSModel.BUCKET_TYPE,
                IconFactory.getProxyBucketIcon(rowHeight));
        }
    }

    private void customizeColumns() {
        // install the default editor for table data.
        setDefaultEditor(Object.class, new ProxyCellEditor());

        // customize the behavior and appearance of the columns.
        DataTableModel.installColumnCustomizations(this);
    }

    private void tweakBehaviors() {
        // do not allow indentation; users should not be able to turn a
        // proxy into a bucket, or vice-versa
        setIndentationDisabled(true);
        // add a new enablement calculator. This is added to the end of the
        // list, so it will run after the others are done and have the
        // opportunity to override earlier decisions.
        addEnablementCalculation(new EnablementRecalcTweaker());
    }


    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component result = super.prepareRenderer(renderer, row, column);
        if (column > 0 && !isCellEditable(row, column))
            result.setBackground(UNEDITABLE);
        return result;
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        DataColumn dc = ((ProxyDataModel) getModel()).getColumn(column);
        if (dc instanceof ProxySizeColumn) {
            ProxySizeColumn psc = (ProxySizeColumn) dc;
            TableCellEditor result = psc.getCellEditorForRow(row);
            if (result != null)
                return result;
        }
        return super.getCellEditor(row, column);
    }


    private class EnablementRecalcTweaker implements EnablementCalculation {

        public void recalculateEnablement(int[] selectedRows) {
            // don't allow someone to move a relative size bucket into a
            // different category.
            maybeDisableMoveAction(MOVEUP_ACTION, selectedRows, -1);
            maybeDisableMoveAction(MOVEDOWN_ACTION, selectedRows, +1);
            // don't allow deletion if the selection is ragged; the result
            // might be surprising to the user
            maybeDisableDeleteAction(selectedRows);
        }

        private void maybeDisableMoveAction(AbstractAction a,
                int[] selectedRows, int direction) {
            // if this action is already disabled, no more checks are needed
            if (!a.isEnabled())
                return;

            // check to see if the user is moving a relative size bucket.
            int row = selectedRows[0];
            WBSNode node = wbsModel.getNodeForRow(row);
            if (node.getIndentLevel() != 2)
                return;

            // find the adjacent node that will trade places with this bucket.
            // If it's a proxy category, disable the operation.
            WBSNode prevNode = wbsModel.getNodeForRow(row + direction);
            if (prevNode.getIndentLevel() == 1)
                a.setEnabled(false);
        }

        private void maybeDisableDeleteAction(int[] rows) {
            if (((ProxyWBSModel) wbsModel).containsProxyRow(rows)
                    && wbsModel.getNodeForRow(rows[0]).getIndentLevel() > 1)
                ((AbstractAction) DELETE_ACTION).setEnabled(false);
        }

    }

    private static class ProxyCellEditor extends DefaultCellEditor {

        public ProxyCellEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            Component result = super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);

            if (result instanceof JTextField)
                ((JTextField) result).selectAll();

            return result;
        }

    }

    private static final Color UNEDITABLE = new Color(220, 220, 220);

}
