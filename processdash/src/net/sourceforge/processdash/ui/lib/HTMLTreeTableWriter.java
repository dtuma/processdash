// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.io.IOException;
import java.io.Writer;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class HTMLTreeTableWriter extends HTMLTableWriter {

    String treeName = "t";

    int nodeColumn = 0;

    JTree tree;

    TreeNodeCellRenderer nodeRenderer;


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
        out.write(" id='");
        out.write(getIdForRow(r));
        out.write("'");
        super.printRowAttrs(out, r);
    }

    private String getIdForRow(int r) {
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
        String idStr = id.toString();
        return idStr;
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
            int indent = path.getPathCount() - 1;
            int iconWidth = (isLeaf ? 15 : 30);

            StringBuffer result = new StringBuffer();
            result.append("<div style='margin-left: ") //
                    .append(30 * indent + iconWidth) //
                    .append("px; position: relative'>");
            if (isLeaf)
                result.append("<img class='treeTableDoc' width='12'"
                        +" height='14' src='/Images/document.gif'>");
            else
                result.append("<a href='#' class='treeTableFolder' "
                        + "onclick='toggleRows(this); return false;'>"
                        + "<img border='0' src='/Images/folder-closed.gif'"
                        + " width='26' height='14'></a>");

            if (innerHtml != null)
                result.append(innerHtml);

            result.append("</div>");
            return result.toString();
        }

    }



    public static final String TREE_ICON_HEADER =
        "<span style='display:none'>" +
        "<img alt='tree-table-folder-open' src='/Images/folder-open.gif'>" +
        "<img alt='tree-table-folder-closed' src='/Images/folder-closed.gif'>" +
        "</span>\n";

    public static final String TREE_HEADER_ITEMS =
        "<link rel=stylesheet type='text/css' href='/lib/treetable.css'>\n" +
        "<script type='text/javascript' src='/lib/treetable.js'></script>\n";

}
