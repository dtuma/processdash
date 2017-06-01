// Copyright (C) 2013 Tuma Solutions, LLC
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

import teamdash.wbs.DataTableModel;
import teamdash.wbs.ItalicCellRenderer;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowModel;

public class WorkflowLabelColumn extends TaskLabelColumn implements
        WorkflowOptionalColumn {

    public static final String VALUE_ATTR = "Workflow Label";


    public WorkflowLabelColumn(DataTableModel dataModel) {
        super(dataModel, VALUE_ATTR);
        this.preferredWidth = 100;
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0 && super.isCellEditable(node);
    }

    @Override
    public TableCellRenderer getCellRenderer() {
        return new WorkflowTableCellRenderer(new ItalicCellRenderer(
                resources.getString("Inherited_Tooltip")));
    }

    public boolean shouldHideColumn(WorkflowModel model) {
        return !model.getWBSModel().containsAttr(VALUE_ATTR);
    }

}
