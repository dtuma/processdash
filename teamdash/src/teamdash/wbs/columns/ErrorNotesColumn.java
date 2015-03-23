// Copyright (C) 2012 Tuma Solutions, LLC
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

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSNodeTest;

public class ErrorNotesColumn extends AbstractNotesColumn {

    /** The ID we use for this column in the data model */
    public static final String COLUMN_ID = "Error Notes";

    /** The attribute this column uses to store task notes for a WBS node */
    public static final String VALUE_ATTR = "Error Notes";


    public ErrorNotesColumn(String authorName) {
        super(VALUE_ATTR, authorName);
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Error_Notes.Name");
    }

    @Override
    protected String getEditDialogTitle() {
        return columnName;
    }

    @Override
    protected Object getEditDialogHeader(WBSNode node) {
        return new Object[] {
                resources.getStrings("Error_Notes.Edit_Dialog_Header"), " " };
    }

    public static String getTextAt(WBSNode node) {
        return getTextAt(node, VALUE_ATTR);
    }

    public static String getTooltipAt(WBSNode node, boolean includeByline) {
        return getTooltipAt(node, includeByline, VALUE_ATTR);
    }

    /**
     * Find nodes that have errors attached, and expand their ancestors as
     * needed to ensure that they are visible.
     * 
     * @param wbs
     *            the WBSModel
     * @param belowNode
     *            an optional starting point for the search, to limit expansion
     *            to a particular branch of the tree; can be null to search from
     *            the root
     * @param condition
     *            an optional condition to test; only nodes matching the
     *            condition will be made visible. Can be null to show all nodes
     *            with errors
     */
    public static void showNodesWithErrors(WBSModel wbs, WBSNode belowNode,
            WBSNodeTest condition) {
        if (belowNode == null)
            belowNode = wbs.getRoot();

        for (WBSNode node : wbs.getDescendants(belowNode)) {
            String errText = getTextAt(node, VALUE_ATTR);
            if (errText != null && errText.trim().length() > 0) {
                if (condition == null || condition.test(node))
                    wbs.makeVisible(node);
            }
        }
    }

}
