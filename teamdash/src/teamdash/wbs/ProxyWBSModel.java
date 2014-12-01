// Copyright (C) 2014 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

public class ProxyWBSModel extends WBSModel {

    public static final String PROXY_LIST_TYPE = "Estimation Tables";

    public static final String PROXY_TYPE = "Estimation Table";

    public static final String BUCKET_TYPE = "Relative Size";

    public ProxyWBSModel() {
        this(PROXY_LIST_TYPE);
    }

    public ProxyWBSModel(Element e) {
        super(e);
    }

    public ProxyWBSModel(String name) {
        super(name);

        WBSNode proxy = getNodeForRow(1);
        proxy.setName("");
        createDefaultBucketsForNode(proxy, 1);
    }

    @Override
    protected void tweakNodeForInsertion(WBSNode node) {
        if (node.getIndentLevel() > 2)
            node.setIndentLevel(2);
        switch (node.getIndentLevel()) {
        case 0: node.setType(PROXY_LIST_TYPE); break;
        case 1: node.setType(PROXY_TYPE); break;
        case 2: node.setType(BUCKET_TYPE); break;
        }
    }

    public boolean isNodeTypeEditable(WBSNode node) {
        return false;
    }

    @Override
    public int getInsertAfterPos(int row) {
        // retrieve the node we are about to insert after. We will implicitly
        // be creating another row of the same type.
        WBSNode node = getNodeForRow(row);

        // if this node is a bucket, we can insert a new bucket to the next row.
        if (isBucket(node))
            return row + 1;

        // if this proxy node is currently collapsed, we can insert a new proxy
        // on the next row.
        else if (node.isExpanded() == false)
            return row + 1;

        // otherwise, insert the new proxy before the *next* proxy category row.
        else
            return getNextProxyRowAfter(row);
    }

    @Override
    public int getPasteBeforePos(int targetRow, List<WBSNode> nodesToInsert) {
        if (targetRow >= getRowCount())
            return getRowCount();

        boolean targetRowIsProxy = containsProxyRow(targetRow);
        boolean pastedNodesHaveProxy = containsProxyRow(nodesToInsert);
        if (targetRowIsProxy == pastedNodesHaveProxy)
            // if we are pasting buckets onto a bucket row, or pasting proxies
            // onto a proxy row, we can allow the paste to proceed normally.
            return targetRow;
        else
            // if we are pasting buckets onto a proxy row, or if we are pasting
            // new proxies onto a bucket row, skip past all of the buckets and
            // paste to the row immediately following.
            return getNextProxyRowAfter(targetRow);
    }

    private int getNextProxyRowAfter(int row) {
        // find the next proxy category, and return its row number.
        while (++row < getRowCount()) {
            WBSNode node = getNodeForRow(row);
            if (isProxy(node))
                return row;
        }

        // No next proxy category was found. return the end of the table.
        return getRowCount();
    }

    @Override
    public int[] getAtomicRowList(int[] rows) {
        // no rows in the list? return unchanged
        if (rows == null || rows.length == 0)
            return rows;

        // if the rows do not include a proxy category, return unchanged
        if (containsProxyRow(rows) == false)
            return rows;

        // create a new list of rows that includes complete categories (proxy
        // category row and all buckets) for the rows listed.
        IntList result = new IntList();
        for (int i = 0; i < rows.length; i++) {
            // look at each row/node in turn.
            int row = rows[i];
            WBSNode node = getNodeForRow(row);

            // if the very first listed row is a bucket, fetch its parent
            // instead, so we get the whole proxy category it belongs to.
            if (i == 0 && isBucket(node)) {
                node = getParent(node);
                row = getRowForNode(node);
            }

            // if this is a proxy category node, add the rows for the category
            // and all of its children.
            if (isProxy(node)) {
                result.add(row);
                if (node.isExpanded())
                    for (WBSNode child : getDescendants(node))
                        result.add(getRowForNode(child));
            }
        }
        return result.getAsArray();
    }

    @Override
    public synchronized int add(int beforeRow, WBSNode newNode) {
        int result = super.add(beforeRow, newNode);
        createDefaultBucketsForNode(newNode, result);
        return result;
    }

    private boolean createDefaultBucketsForNode(WBSNode node, int row) {
        if (node.getWbsModel() != this)
            return false;

        if (!isProxy(node) || !isLeaf(node))
            return false;

        List<WBSNode> buckets = new ArrayList<WBSNode>();
        for (String bucketName : DEFAULT_BUCKET_NAMES) {
            WBSNode oneBucket = new WBSNode(this, bucketName, BUCKET_TYPE, 2,
                    false);
            buckets.add(oneBucket);
        }
        node.setExpanded(true);
        insertNodes(buckets, row + 1);
        return true;
    }

    private static final String[] DEFAULT_BUCKET_NAMES = { "VS", "S", "M", "L",
            "VL" };

    public boolean containsProxyRow(int... rows) {
        if (rows != null)
            for (int row : rows)
                if (isProxy(getNodeForRow(row)))
                    return true;
        return false;
    }

    public static boolean containsProxyRow(List<WBSNode> nodes) {
        if (nodes != null)
            for (WBSNode node : nodes)
                if (isProxy(node))
                    return true;
        return false;
    }

    public static boolean isProxy(WBSNode node) {
        return node != null && node.getIndentLevel() == 1;
    }

    public static boolean isBucket(WBSNode node) {
        return node != null && node.getIndentLevel() == 2;
    }

}
