// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

import teamdash.wbs.DataTableModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

/**
 * This class echoes the value of the SizeTypeColumn, but only if:
 * <ul>
 * <li>a top-down size estimate has been entered for a particular node, or</li>
 * <li>the node is a PROBE task.</li>
 * </ul>
 * If those conditions are not met, it returns null.
 */
public class DirectSizeTypeColumn extends SizeTypeColumn {

    public static final String COLUMN_ID = "Direct Size Units";

    public DirectSizeTypeColumn(DataTableModel m, Map sizeMetrics) {
        super(m, sizeMetrics);
        this.columnID = this.columnName = COLUMN_ID;
    }

    public Object getValueAt(WBSNode node) {
        Object result = super.getValueAt(node);
        if (result == null) return null;
        String units = String.valueOf(result);
        String addedAttr = TopDownBottomUpColumn.getTopDownAttrName
            (SizeAccountingColumnSet.getAddedID(units));
        String modifiedAttr = TopDownBottomUpColumn.getTopDownAttrName
            (SizeAccountingColumnSet.getModifiedID(units));

        if (node.getAttribute(addedAttr) != null ||
            node.getAttribute(modifiedAttr) != null ||
            TeamProcess.isProbeTask(node.getType()))
            return units;
        else
            return null;
    }

    public static class Simple extends AbstractDataColumn {

        public Simple() {
            this.columnID = this.columnName = COLUMN_ID;
        }

        public boolean isCellEditable(WBSNode node) { return false; }
        public void setValueAt(Object aValue, WBSNode node) {}

        public Object getValueAt(WBSNode node) {
            if (TeamProcess.isProbeTask(node.getType())) {
                return TaskSizeUnitsColumn.getSizeUnitsForProbeTask(node);

            } else if (TeamProcess.isPSPTask(node.getType())) {
                return "LOC";

            } else {
                return null;
            }
        }

    }

}
