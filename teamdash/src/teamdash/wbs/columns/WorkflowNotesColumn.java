// Copyright (C) 2013-2018 Tuma Solutions, LLC
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

import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowDataModel;

public class WorkflowNotesColumn extends NotesColumn implements
        WorkflowOptionalColumn {

    /** The attribute this column uses to store notes for a workflow node */
    public static final String VALUE_ATTR = "Workflow Notes";


    public WorkflowNotesColumn() {
        super(VALUE_ATTR, null);
        this.preferredWidth = 100;
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0 && super.isCellEditable(node);
    }

    public boolean shouldHideColumn(WorkflowDataModel model) {
        return !model.getWBSModel().containsAttr(VALUE_ATTR);
    }

}
