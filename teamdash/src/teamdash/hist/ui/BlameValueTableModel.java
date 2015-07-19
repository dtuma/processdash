// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import static teamdash.hist.ui.BlameHistoryDialog.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.table.DefaultTableModel;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.hist.BlameNodeData;
import teamdash.hist.BlamePoint;
import teamdash.hist.BlameValueList;
import teamdash.wbs.WBSNode;

public class BlameValueTableModel extends DefaultTableModel {

    public BlameValueTableModel() {
        super(0, 4);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void clearRows() {
        setRowCount(0);
    }

    public void showMessage(String message) {
        clearRows();
        addBlameRow(null, message, null);
    }


    public void setBlameNodeStructure(BlameNodeData nodeData) {
        clearRows();

        // display a message about the addition of this node
        if (nodeData.getAddedBy() != null)
            addBlameRow(nodeData.getAddedBy(),
                resources.getString("Node.Added"), null);

        // display historical changes to the name of this node
        if (nodeData.getNodeNameChanges() != null) {
            addBlameRow(null, RENAME_HEADER, null);
            addBlameValues(nodeData.getNodeNameChanges(), INITIAL_NAME);
        }

        // display historical changes to the parent of this node
        if (nodeData.getParentPathChanges() != null) {
            addBlameRow(null, MOVE_HEADER, null);
            addBlameValues(nodeData.getParentPathChanges(), INITIAL_PARENT);
        }

        // display children that have been deleted from this node
        if (nodeData.getDeletedChildren() != null) {
            addBlameRow(null, DELETE_HEADER, null);
            for (Entry<WBSNode, BlamePoint> e : nodeData.getDeletedChildren()
                    .entrySet()) {
                BlamePoint blame = e.getValue();
                String deletedChildName = e.getKey().getName();
                addBlameRow(blame, deletedChildName, null);
            }
        }
    }

    public void setBlameValueList(BlameValueList values) {
        clearRows();
        addBlameValues(values, INITIAL_VALUE);
    }

    protected void addBlameValues(BlameValueList values, String initialText) {
        List<Entry<BlamePoint, String>> list = new ArrayList(values.entrySet());
        for (int i = list.size(); i-- > 0;) {
            Entry<BlamePoint, String> e = list.get(i);
            BlamePoint blame = e.getKey();
            String value = e.getValue();
            if (!StringUtils.hasValue(value))
                value = NOTHING;
            addBlameRow(blame, value, initialText);
        }
    }

    private void addBlameRow(BlamePoint blame, String display,
            String initialText) {
        String author = null, time = null;
        if (blame == BlamePoint.INITIAL) {
            author = initialText;
        } else if (blame != null) {
            author = blame.getAuthor();
            time = BlameTimelineRenderer.DATE_FMT.format(blame.getTimestamp());
        }
        addRow(new Object[] { blame, display, author, time });
    }


    private static final String NOTHING = msg("Nothing", "i");

    private static final String INITIAL_VALUE = msg("Initial_Value", "i");

    private static final String INITIAL_PARENT = msg("Initial_Parent", "i");

    private static final String INITIAL_NAME = msg("Initial_Name", "i");

    private static final String RENAME_HEADER = msg("Node.Name_Changes", "b");

    private static final String MOVE_HEADER = msg("Node.Parent_Changes", "b");

    private static final String DELETE_HEADER = msg("Node.Deleted_Children",
        "b");

    private static String msg(String key, String tag) {
        return "<html><" + tag + ">" + resources.getString(key) //
                + "</" + tag + "></html>";
    }

}
