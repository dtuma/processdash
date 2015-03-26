// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

public class JFilterableTreeTable extends JTreeTable {

    public interface Filter {
        public boolean test(TreePath pathToTreeNode);
    }

    private TreeTableSorter sorter;

    public JFilterableTreeTable(TreeTableModel treeTableModel) {
        super(treeTableModel);
        // expand all the rows in the tree so they can be filtered
        expandAllRows();
        // don't display lines on the tree; they display incorrectly when
        // a filter is in effect.
        getTree().putClientProperty("JTree.lineStyle", "None");
        // make the tree selection mirror the table selection.
        new FilteredSelectionAdapter();
        // install a RowSorter which performs the filtering
        setRowSorter(sorter = new TreeTableSorter());
    }

    public boolean isRootVisible() {
        return getTree().isRootVisible();
    }

    public void setRootVisible(boolean rootVisible) {
        if (rootVisible != isRootVisible()) {
            clearSelection();
            getTree().setRootVisible(rootVisible);
            sorter.sort();
        }
    }

    /** Change the object that filters which tree nodes should be displayed.
     * 
     * @param f a {@link Filter} object which can decide which tree nodes
     *     should be displayed.  <code>null</code> implies "display everything".
     *     Note that this filter is treated as if it is immutable; that is, as
     *     if its test method will always return the same value for the same
     *     treeNode argument.  If the filtering logic changes, this method
     *     should be called again (with the same or a different {@link Filter}
     *     object) to let this {@link JFilterableTreeTable} object know that
     *     the filtering should be reevaluated.
     */
    public void setFilter(Filter f) {
        sorter.setFilter(f);
    }

    public TreePath getPathForRow(int row) {
        return getTree().getPathForRow(convertRowIndexToModel(row));
    }

    @Override
    protected int convertTableRowToTreeRow(int tableRow) {
        return convertRowIndexToModel(tableRow);
    }

    protected void expandAllRows() {
        for (int i = 0;  i < getRowCount();  i++)
            getTree().expandRow(i);
    }

    protected void expandAllRowsUnder(TreePath path) {
        for (int row = 0;  row < getRowCount();  row++) {
            TreePath rowPath = getPathForRow(row);
            if (path.isDescendant(rowPath))
                getTree().expandPath(rowPath);
        }
    }


    private class TreeTableSorter extends TableRowSorter {

        public TreeTableSorter() {
            super(JFilterableTreeTable.this.getModel());
            setRowFilter(new TreeTableFilter());
            setSortsOnUpdates(true);
        }

        public void setFilter(Filter f) {
            getTreeFilter().setFilter(f);
            sort();
        }

        private TreeTableFilter getTreeFilter() {
            return ((TreeTableFilter) getRowFilter());
        }

        @Override
        public void sort() {
            getTreeFilter().recalcRows();
            super.sort();
        }

        @Override
        public boolean isSortable(int column) {
            return false;
        }

    }

    private class TreeTableFilter extends RowFilter<TreeTableModel, Integer> {

        private Filter filter = null;
        private Set<Integer> cachedIncludedRows = null;

        public void setFilter(Filter f) {
            this.filter = f;
        }

        public void recalcRows() {
            Set<Integer> newRows = null;
            if (filter != null) {
                newRows = new HashSet<Integer>();
                Object root = getTree().getModel().getRoot();
                calcIncludedRows(newRows, new TreePath(root), filter, false);
            }
            this.cachedIncludedRows = newRows;
        }

        private boolean calcIncludedRows(Set<Integer> newRows, TreePath path,
                Filter filter, boolean parentMatches) {
            if (!tree.isVisible(path))
                return false;

            Object node = path.getLastPathComponent();
            boolean nodeIsMatch = parentMatches || filter.test(path);

            boolean childIsMatch = false;
            for (int i = tree.getModel().getChildCount(node);  i-- > 0; ) {
                Object child = tree.getModel().getChild(node, i);
                TreePath childPath = path.pathByAddingChild(child);
                if (calcIncludedRows(newRows, childPath, filter, nodeIsMatch))
                    childIsMatch = true;
            }

            nodeIsMatch |= childIsMatch;
            if (nodeIsMatch)
                newRows.add(tree.getRowForPath(path));
            return nodeIsMatch;
        }


        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends TreeTableModel, ? extends Integer> entry) {
            if (cachedIncludedRows == null)
                return true;
            else
                return cachedIncludedRows.contains(entry.getIdentifier());
        }

    }

    /**
     * An adapter that forces the tree selection to mirror the table selection.
     * 
     * JTreeTable provides an adapter for this same purpose, but it cannot
     * handle the filtered rows present in this table)
     */
    private class FilteredSelectionAdapter extends DefaultTreeSelectionModel
            implements ListSelectionListener {

        public FilteredSelectionAdapter() {
            ListSelectionModel lsm = new DefaultListSelectionModel();
            lsm.addListSelectionListener(this);
            JFilterableTreeTable.this.setSelectionModel(lsm);
            getTree().setSelectionModel(this);
        }

        public void valueChanged(ListSelectionEvent e) {
            clearSelection();
            int[] selectedRows = JFilterableTreeTable.this.getSelectedRows();
            for (int i = 0; i < selectedRows.length; i++) {
                int row = convertRowIndexToModel(selectedRows[i]);
                TreePath path = getTree().getPathForRow(row);
                this.addSelectionPath(path);
            }
        }

    }

}
