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

package teamdash.wbs.columns;

import static teamdash.wbs.columns.CustomColumnEditor.CUST_ID_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;

import teamdash.merge.ContentMerger;
import teamdash.merge.MergeWarning;
import teamdash.merge.ModelType;
import teamdash.merge.MergeWarning.Severity;
import teamdash.merge.TreeMerger;
import teamdash.merge.TreeNode;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.wbs.TeamProject;

public class CustomColumnsMerger {

    private CustomColumnSpecs base;

    private CustomColumnSpecs main;

    private CustomColumnSpecs incoming;

    private CustomColumnSpecs merged;

    private Set<MergeWarning<String>> mergeWarnings;

    public CustomColumnsMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this(base.getColumns(), main.getColumns(), incoming.getColumns());
    }

    public CustomColumnsMerger(CustomColumnSpecs base, CustomColumnSpecs main,
            CustomColumnSpecs incoming) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;

        performMerge();
    }

    public CustomColumnSpecs getMerged() {
        return merged;
    }

    private void performMerge() {
        TreeMerger<String, Element> worker = new TreeMerger<String, Element>(
                toTree(base), toTree(main), toTree(incoming), new Merger());
        worker.run();
        merged = getMergedColumns(worker);
        mergeWarnings = worker.getMergeWarnings();
    }

    private TreeNode<String, Element> toTree(CustomColumnSpecs columns) {
        TreeNode<String, Element> root = new TreeNode<String, Element>("root",
                null);
        for (Entry<String, Element> e : columns.entrySet()) {
            String columnID = e.getKey();
            Element columnSpec = e.getValue();
            root.addChild(new TreeNode<String, Element>(columnID, columnSpec));
        }
        return root;
    }

    private CustomColumnSpecs getMergedColumns(
            TreeMerger<String, Element> columnMerger) {
        CustomColumnSpecs result = new CustomColumnSpecs();
        List<TreeNode<String, Element>> mergedColumns = columnMerger
                .getMergedTree().getChildren();
        for (TreeNode<String, Element> column : mergedColumns) {
            result.put(column.getID(), column.getContent());
        }
        return result;
    }

    public Collection<? extends MergeConflictNotification> getConflictNotifications() {
        List<MergeConflictNotification> result = new ArrayList();
        for (MergeWarning<String> warning : mergeWarnings) {
            MergeConflictNotification notification = create(warning);
            if (notification != null)
                result.add(notification);
        }
        return result;
    }

    private MergeConflictNotification create(MergeWarning<String> mw) {
        MergeConflictNotification result = new MergeConflictNotification(
                ModelType.Columns, mw);
        String columnID = mw.getMainNodeID();
        result.putNodeAttributes(getColumnName(main.get(columnID)),
            getColumnName(incoming.get(columnID)));
        result.putAttribute("columnID", getColumnDisplayID(columnID));
        result.addUserOption(MergeConflictNotification.DISMISS, null);
        try {
            result.formatDescription();
            return result;
        } catch (MissingResourceException mre) {
            System.err.println("Unexpected merge conflict key for "
                    + "custom column specs: " + mre.getKey());
            return null;
        }
    }

    private String getColumnName(Element columnSpec) {
        return (columnSpec == null ? null : columnSpec
                .getAttribute(CustomColumnManager.COLUMN_NAME_ATTR));
    }

    private Object getColumnDisplayID(String columnID) {
        if (columnID.startsWith(CUST_ID_PREFIX))
            return columnID.substring(CUST_ID_PREFIX.length());
        else
            return columnID;
    }

    private class Merger implements ContentMerger<String, Element> {

        @Override
        public boolean isEqual(Element a, Element b) {
            if (a == b)
                return true;
            else if (a == null || b == null)
                return false;
            else
                return a.isEqualNode(b);
        }

        @Override
        public Element mergeContent(TreeNode<String, Element> destNode,
                Element base, Element main, Element incoming,
                ErrorReporter<String> err) {

            // If the spec agrees in both branches, return it.
            if (isEqual(main, incoming))
                return main;

            // if the incoming branch didn't change the spec, keep main
            if (isEqual(base, incoming))
                return main;

            // if the main branch didn't change the spec, keep incoming
            if (isEqual(base, main))
                return incoming;

            // the column has been added/edited in both places. Keep main and
            // flag a conflict
            String key = (base == null ? "Add_Conflict" : "Edit_Conflict");
            String columnID = destNode.getID();
            err.addMergeWarning(new MergeWarning<String>(Severity.CONFLICT,
                    key, columnID, columnID));
            return main;
        }
    }

}
