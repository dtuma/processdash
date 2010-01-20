// Copyright (C) 2006-2010 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.Writer;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class HTMLTreeTableWriter extends HTMLTableWriter {

    String treeName = "t";

    int nodeColumn = 0;

    int showDepth = 2;

    JTree tree;

    TreeNodeCellRenderer nodeRenderer;

    private String expandAllTooltip;

    public int getNodeColumn() {
        return nodeColumn;
    }

    public void setNodeColumn(int nodeColumn) {
        this.nodeColumn = nodeColumn;
    }

    public String getTreeName() {
        return treeName;
    }

    public void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    public void setExpandAllTooltip(String expandAllTooltip) {
        this.expandAllTooltip = expandAllTooltip;
    }

    /** Set the number of levels beneath the root node which should be
     * displayed initially. */
    public void setShowDepth(int d) {
        this.showDepth = Math.max(1, d);
    }

    public void writeTree(Writer out, TreeTableModel t) throws IOException {
        // create a JTree for this tree table
        tree = new JTree(t);
        // expand all the rows in the tree
        int row = 0;
        while (row < tree.getRowCount())
            tree.expandRow(row++);

        // create an adapting table model
        TreeTableModelAdapter tableModel = new TreeTableModelAdapter(t, tree);

        // create the renderer for the tree column
        nodeRenderer = new TreeNodeCellRenderer(super
                .getCellRenderer(nodeColumn));

        super.writeTable(out, tableModel);

        tree = null;
        nodeRenderer = null;
    }

    public CellRenderer getCellRenderer(int col) {
        if (col == nodeColumn)
            return nodeRenderer;
        else
            return super.getCellRenderer(col);
    }

    protected void printRowAttrs(Writer out, int r) throws IOException {
        RowID rowId = getIdForRow(r);
        out.write(" id='");
        out.write(rowId.id);
        out.write("'");
        if (rowId.indent > showDepth)
            out.write(" style='display:none'");
        super.printRowAttrs(out, r);
    }

    private class RowID {
        public String id;
        public int indent;
        public RowID(String id, int indent) {
            this.id = id;
            this.indent = indent;
        }
    }

    private RowID getIdForRow(int r) {
        TreeModel treeModel = tree.getModel();
        TreePath path = tree.getPathForRow(r);
        StringBuffer id = new StringBuffer();
        id.append(treeName);
        for (int i = 1; i < path.getPathCount(); i++) {
            Object parent = path.getPathComponent(i - 1);
            Object node = path.getPathComponent(i);
            int pos = treeModel.getIndexOfChild(parent, node);
            id.append("-").append(Integer.toString(pos));
        }
        return new RowID(id.toString(), path.getPathCount() - 1);
    }

    private class TreeNodeCellRenderer implements CellRenderer {

        private CellRenderer r;

        public TreeNodeCellRenderer(CellRenderer r) {
            this.r = r;
        }

        public String getAttributes(Object value, int row, int column) {
            return r.getAttributes(value, row, column);
        }

        public String getInnerHtml(Object value, int row, int column) {
            String innerHtml = r.getInnerHtml(value, row, column);

            if (column != nodeColumn)
                return innerHtml;

            TreeModel treeModel = tree.getModel();
            TreePath path = tree.getPathForRow(row);
            Object node = path.getLastPathComponent();
            boolean isLeaf = treeModel.isLeaf(node);
            boolean isRoot = (row == 0);

            int indent = path.getPathCount() - 1;

            StringBuffer result = new StringBuffer();
            result.append("<div style='margin-left: ") //
                    .append(24 + indent * 20) //
                    .append("px; position: relative'>");

            if (isRoot) {
                // If the node is the root node, show the "Expand all" icon
                result.append("<a href='#' class='treeTableExpandAll' "
                            + "onclick='toggleRows(this); return false;'>"
                            + "<img border='0' src='/Images/expand-all.gif'");

                if (expandAllTooltip != null)
                    result.append(" title='").append(expandAllTooltip).append("'");

                result.append(" width='16' height='16'></a>");

            } else if (!isLeaf) {
                // If the node is not a leaf, display an arrow for opening and
                // closing it. Check the indent level to determine if the node
                // should be initially opened.
                result.append("<a href='#' class='treeTableFolder' "
                        + "onclick='toggleRows(this); return false;'>"
                        + "<img border='0' src='/Images/tree-");
                result.append(indent < showDepth ? "open" : "closed");
                result.append(".gif' width='11' height='14'></a>");
            }

            if (innerHtml != null)
                result.append(innerHtml);

            result.append("</div>");
            return result.toString();
        }

    }



    public static final String TREE_ICON_HEADER =
        "<span style='display:none'>" +
        "<img alt='tree-table-folder-open' src='/Images/tree-open.gif'>" +
        "<img alt='tree-table-folder-closed' src='/Images/tree-closed.gif'>" +
        "</span>\n";

    public static final String TREE_HEADER_ITEMS =
        "<link rel=stylesheet type='text/css' href='/lib/treetable.css'>\n" +
        "<script type='text/javascript' src='/lib/treetable.js'></script>\n";

}
