// Copyright (C) 2010-2013 Tuma Solutions, LLC
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

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowModel;

public class WorkflowScriptColumn extends AbstractDataColumn implements
        CustomRenderedColumn, WorkflowOptionalColumn {

    private static final String COLUMN_ID = "Workflow_URL";

    public static final String COLUMN_NAME = "Script URLs";

    public static final String VALUE_ATTR = "Workflow URL";

    public WorkflowScriptColumn() {
        this.columnID = COLUMN_ID;
        this.columnName = COLUMN_NAME;
        this.preferredWidth = 100;
        setConflictAttributeName(VALUE_ATTR);
    }

    public Object getValueAt(WBSNode node) {
        return node.getAttribute(VALUE_ATTR);
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0;
    }

    public void setValueAt(Object value, WBSNode node) {
        String s = (value == null ? null : value.toString().trim());
        node.setAttribute(VALUE_ATTR, s);
    }

    public TableCellRenderer getCellRenderer() {
        return WorkflowTableCellRenderer.INSTANCE;
    }

    public boolean shouldHideColumn(WorkflowModel model) {
        return !model.getWBSModel().containsAttr(VALUE_ATTR);
    }

}
