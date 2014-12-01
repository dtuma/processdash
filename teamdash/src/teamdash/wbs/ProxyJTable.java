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

package teamdash.wbs;

import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.table.TableColumn;

public class ProxyJTable extends WBSJTable {

    public ProxyJTable(ProxyDataModel model) {
        super(model.getWBSModel(), new HashMap(), new JMenu());
        setModel(model);

        buildIconMap();
        customizeColumns();
        tweakBehaviors();
    }

    private void buildIconMap() {
        iconMap.put(ProxyWBSModel.PROXY_LIST_TYPE,
            IconFactory.getProxyListIcon());
        iconMap.put(ProxyWBSModel.PROXY_TYPE, IconFactory.getProxyTableIcon());
        iconMap.put(ProxyWBSModel.BUCKET_TYPE,
            IconFactory.getProxyBucketIcon(getRowHeight()));
        iconMap.put(null, IconFactory.getModifiedIcon(
            IconFactory.getComponentIcon(), IconFactory.ERROR_ICON));
    }

    private void customizeColumns() {

        TableColumn col;

        // customize the display of the "Name" column.
        col = getColumn("Name");
        col.setPreferredWidth(300);
    }

    private void tweakBehaviors() {
        getTableHeader().setReorderingAllowed(false);
        // do not allow indentation; users should not be able to turn a
        // proxy into a bucket, or vice-versa
        setIndentationDisabled(true);
        // add a new enablement calculator. This is added to the end of the
        // list, so it will run after the others are done and have the
        // opportunity to override earlier decisions.
        addEnablementCalculation(new EnablementRecalcTweaker());
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

}
