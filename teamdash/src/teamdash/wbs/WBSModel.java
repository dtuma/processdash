
package teamdash.wbs;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** This class maintains a tree-like work breakdown structure, and
 * exposes it via a table model.
 *
 * Trees are normally represented in Swing with a TreeModel (and can
 * still be displayed in tables using a JTreeTable), but I chose not
 * to use a TreeModel for an important reason: above all else, I
 * wanted the work breakdown structure to be <b>very</b> easy to edit.
 * (This was my most important user requirement.)  I wanted the user
 * to be able to simply edit the hierarchy with simple actions similar
 * to those found when editing an outline in a Microsoft Word document:
 * promote/demote, insert/delete, and cut/paste.
 *
 * Facilitating simple editing comes with a price: you must allow the
 * user to create "illegal" trees. For example:

 * <PRE>PARENT
 +----CHILD1
 |     +-GRANDCHILD1
 +-CHILD2
    +-GRANDCHILD2
    +-GRANDCHILD3</PRE>

 * in this tree, child1 and child2 are siblings, even though there
 * appears to be a missing tree node in between parent and child1.
 * The TreeModel cannot support this type of behavior.  Further,
 * actions like promote/demote become incredibly difficult to
 * implement in a TreeModel - and even more difficult to implement
 * symmetrically (e.g. demoting something and then promoting it should
 * result in a no-op).
 *
 * After careful analysis, I decided I needed a custom data structure
 * to perform these operations.  This WBSModel maintains a simple
 * (flat) list of nodes, represented by {@link WBSNode}.  In addition
 * to the regular node characteristics like name and type, these nodes
 * contain an "indentationLevel" property.  Various methods in this
 * class examine the indentation levels of nodes to calculate tree-like
 * operations (e.g. getParent, getChildCount, getChildren).  The first
 * item in the list is the root, and always has indentation level 0. No
 * other node in the tree is allowed to have indentation level 0.
 *
 * Nodes also keep track of an "expanded" property, which indicates
 * whether the node should be expanded or collapsed when displaying a
 * tree view.  Swing's TreeModel does not encompass this concept,
 * opting instead to store the expanded/collapsed flags in a JTree
 * object.  This is a good design decision, which allows two different
 * JTrees to display the same TreeModel and have different
 * expanded/collapsed states.  I found that the behavior of many of
 * the editing operations I needed to support was tied very closely to
 * the expanded/collapsed state of the nodes in question.  Separating
 * expanded/collapsed data into a separate model would significantly
 * increase the complexity of the logic I needed to write, and did not
 * provide much value, since I don't need to display multiple
 * instances of the work breakdown structure.  (I decided that even if
 * multiple instances were required in the future, requiring their
 * expanded/collapsed states to stay in synch would not be a
 * liability.)
 * 
 * In addition, it should be noted that the nodes of the work breakdown
 * structure can store/retrieve an unlimited amount of related data via the
 * WBSNode.setAttribute methods.  This can be used to store data like the
 * planned size/time for a node, the name of a resource assigned to the task,
 * etc.
 */
public class WBSModel extends AbstractTableModel implements SnapshotSource {

    public static final String WBS_MODEL_TAG = "wbsModel";

    /** The flat list of nodes in this work breakdown structure */
    private ArrayList wbsNodes;

    /** An object which can check the WBS for errors. */
    private WBSModelValidator validator;



    public WBSModel() { this("Team Project"); }

    /** Create a work breakdown structure model.
     * 
     * This will add a few nodes to the model to give the user an idea on how
     * to start.
     * @param rootNodeName the name to give the root node of the hierarchy.
     */
    public WBSModel(String rootNodeName) {
        wbsNodes = new ArrayList();
        if (rootNodeName == null || rootNodeName.trim().length() == 0)
            rootNodeName = "Team Project";
        add(new WBSNode(this, rootNodeName, "Project", 0, true));
        getRoot().setUniqueID((new Random()).nextInt() & 0xffffff);
        add(new WBSNode(this, "Software Component",
                TeamProcess.SOFTWARE_COMPONENT_TYPE, 1, true));
        validator = new WBSModelValidator(this);
    }

    /** Load a work breakdown structure from the data in the given XML element.
     */
    public WBSModel(Element e) {
        wbsNodes = new ArrayList();
        loadXML(e);
        validator = new WBSModelValidator(this);
        validator.recalc();
    }

    /** Add a node to the end of this work breakdown structure.
     */
    public synchronized void add(WBSNode node) {
        wbsNodes.add(makeNodeIDUnique(node));
        recalcRows();
    }

    /** Insert a node into this work breakdown structure.
     * 
     * @param beforeRow the row where the node should be inserted.
     * @param newNode the new node to insert.
     * @return int the actual row number of the inserted node (which may differ
     * from the <code>beforeRow</code> parameter if it contained an invalid
     * value)
     */
    public synchronized int add(int beforeRow, WBSNode newNode) {
        // don't insert anything before the root node
        if (beforeRow < 1) beforeRow = 1;

        // if beforeRow points to a position past the end of the table, just
        // append the row.
        if (beforeRow >= rows.length) {
            add(newNode);
            return rows.length - 1;

        } else {
            int beforePos = rows[beforeRow];
            wbsNodes.add(beforePos, makeNodeIDUnique(newNode));
            recalcRows();
            return beforeRow;
        }
    }

    /** Return the number of nodes in the wbs. */
    public int size() { return wbsNodes.size(); }

    /** Returns the node which is the root of the wbs hierarchy. */
    public WBSNode getRoot() {
        return (WBSNode) wbsNodes.get(0);
    }

    public WBSNode getNodeForRow(int row) {
        if (row < 0 || row >= rows.length) return null;
        return (WBSNode) wbsNodes.get(rows[row]);
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    public int getChildCount(Object n) {
        IntList children = getChildIndexes(n);
        return (children == null ? 0 : children.size());
    }

    public WBSNode[] getReorderableChildren(Object n) {
        if (!(n instanceof WBSNode)) return EMPTY_NODE_LIST;

        WBSNode node = (WBSNode) n;
        WBSNode[] result = (WBSNode[]) node
                .getAttribute(CACHED_REORDERABLE_CHILDREN);
        if (result != null) return result;

        result = getChildren(n);
        if (result.length > 1)
            result = (WBSNode[]) result.clone();
        if (CACHE) node.setAttribute(CACHED_REORDERABLE_CHILDREN, result);
        return result;
    }

    public WBSNode[] getChildren(Object n) {
        if (!(n instanceof WBSNode)) return EMPTY_NODE_LIST;

        WBSNode node = (WBSNode) n;
        WBSNode[] result = (WBSNode[]) node.getAttribute(CACHED_CHILDREN);
        if (result != null) return result;

        IntList childIndexes = getChildIndexes(n);
        if (childIndexes == null || childIndexes.size() == 0)
            result = EMPTY_NODE_LIST;
        else {
            result = new WBSNode[childIndexes.size()];
            for (int i = 0;   i < childIndexes.size();   i++)
                result[i] = (WBSNode) wbsNodes.get(childIndexes.get(i));
        }

        if (CACHE) node.setAttribute(CACHED_CHILDREN, result);
        return result;
    }
    private static final WBSNode[] EMPTY_NODE_LIST = new WBSNode[0];

    public WBSNode getParent(WBSNode n) {
        if (n == null) return null;
        WBSNode result = (WBSNode) n.getAttribute(CACHED_PARENT);
        if (result != null) return result;

        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return null;

        int nodeIndentLevel = n.getIndentLevel();
        WBSNode possibleParent;
        while (pos-- > 0) {
            possibleParent = (WBSNode) wbsNodes.get(pos);
            if (possibleParent.getIndentLevel() < nodeIndentLevel) {
                if (CACHE) n.setAttribute(CACHED_PARENT, possibleParent);
                return possibleParent;
            }
        }

        return null;
    }

    public String getFullName(WBSNode n) {
        WBSNode parent = getParent(n);
        if (parent != null)
            return getFullName(parent) + "/" + n.getName();
        else if (n == getRoot())
            return "/" + n.getName();
        else
            // the given node doesn't exist in our node list.
            return null;
    }

    public IntList getChildIndexes(Object n) {
        if (!(n instanceof WBSNode))
            return null;

        WBSNode node = (WBSNode) n;
        IntList result = (IntList) node.getAttribute(CACHED_CHILD_INDEXES);
        if (result != null) return result;

        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return null;
        return getChildIndexes((WBSNode) n, pos);
    }

    private IntList getChildIndexes(WBSNode node, int pos) {
        if (node == null)
            node = (WBSNode) wbsNodes.get(pos);
        int parentIndentLevel = node.getIndentLevel();
        int minChildIndentLevel = Integer.MAX_VALUE;

        IntList result = new IntList(wbsNodes.size() - pos);

        while (++pos < wbsNodes.size()) {
            WBSNode possibleChildNode = (WBSNode) wbsNodes.get(pos);
            int nodeIndentLevel = possibleChildNode.getIndentLevel();

            // if this node is at the same indentation level as the
            // parent, or if it is further left than the parent, then
            // it is impossible for the parent to have any more children
            if (nodeIndentLevel <= parentIndentLevel)
                break;

            // if this node is indented more deeply than the current
            // child indentation level, then it is not a direct child
            // of the parent.
            if (nodeIndentLevel > minChildIndentLevel)
                continue;

            // this node is a direct child of the parent.  Store it
            // in our result list.
            result.add(pos);
            minChildIndentLevel = nodeIndentLevel;
        }

        if (CACHE) node.setAttribute(CACHED_CHILD_INDEXES, result);
        return result;
    }

    private IntList getDescendantIndexes(WBSNode node, int pos) {
        if (node == null)
            node = (WBSNode) wbsNodes.get(pos);
        int parentIndentLevel = node.getIndentLevel();

        WBSNode possibleDescendantNode;
        int nodeIndentLevel;

        IntList result = new IntList(wbsNodes.size() - pos);

        while (++pos < wbsNodes.size()) {
            possibleDescendantNode = (WBSNode) wbsNodes.get(pos);
            nodeIndentLevel = possibleDescendantNode.getIndentLevel();

            // if this node is at the same indentation level as the
            // parent, or if it is further left than the parent, then
            // it is impossible for the parent to have any more descendants
            if (nodeIndentLevel <= parentIndentLevel)
                break;

            // this node is a descendant of the parent.  Store it in
            // our result list.
            result.add(pos);
        }
        return result;
    }

    public WBSNode[] getDescendants(WBSNode node) {
        int nodePos = wbsNodes.indexOf(node);
        IntList descendantIndexes = getDescendantIndexes(node, nodePos);
        WBSNode[] result = new WBSNode[descendantIndexes.size()];
        for (int i = 0;   i < descendantIndexes.size();   i++)
            result[i] = (WBSNode) wbsNodes.get(descendantIndexes.get(i));
        return result;
    }

    private int maxID = 0;

    private synchronized WBSNode makeNodeIDUnique(WBSNode node) {

        int currentID = node.getUniqueID();
        boolean isUnique = (currentID > 0);
        int oneID;

        // look through the node list to see if any existing nodes have
        // the same ID as this node.  While we're at it, find the largest
        // node ID in use. (This will generally be the ID of the root node,
        // but we won't take any chances.)
        Iterator i = wbsNodes.iterator();
        while (i.hasNext()) {
            oneID = ((WBSNode) i.next()).getUniqueID();
            maxID = Math.max(maxID, oneID);
            if (oneID == currentID)
                isUnique = false;
        }

        if (!isUnique) {
            if (wbsNodes.isEmpty())
                node.setUniqueID(maxID+1);
            else {
                node.setUniqueID(getRoot().getUniqueID());
                getRoot().setUniqueID(maxID+1);
            }
        }

        return node;
    }
    private synchronized List makeNodesIDUnique(List nodes) {
        Iterator i = nodes.iterator();
        while (i.hasNext())
            makeNodeIDUnique((WBSNode) i.next());
        return nodes;
    }

    private int[] rows;


    public int getRowCount() { return rows.length; }
    public int getColumnCount() { return 1; }
    public String getColumnName(int column) { return "WBS Node"; }
    public Class getColumnClass(int columnIndex) { return WBSNode.class; }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return rowIndex != 0;
    }
    public boolean isNodeTypeEditable(WBSNode node) {
        return !node.isReadOnly() && node.getIndentLevel() > 0;
    }
    public Object getValueAt(int row, int column) {
        return getNodeForRow(row);
    }



    void recalcRows() { recalcRows(true); }
    void recalcRows(boolean notify) {
        clearCachedNodeData();

        IntList resultList = new IntList(wbsNodes.size());
        recalcRows(resultList, 0);
        int[] oldRows = rows;
        rows = resultList.getAsArray();
        if (notify)
            if (oldRows != null && !fireSimpleRowChangeEvent(oldRows, rows)) {
                //System.out.println("firing table data changed");
                fireTableDataChanged();
            }
    }

    private void clearCachedNodeData() {
        for (Iterator i = wbsNodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            for (int j = 0; j < CACHING_ATTRS.length; j++) {
                node.setAttribute(CACHING_ATTRS[j], null);
            }
        }
    }

    private void recalcRows(IntList resultList, int nodePos) {
        resultList.add(nodePos);

        WBSNode node = (WBSNode) wbsNodes.get(nodePos);
        if (nodePos == 0 || node.isExpanded()) {
            IntList children = getChildIndexes(node, nodePos);

            if (children != null)
                for (int i=0;   i < children.size();   i++)
                    recalcRows(resultList, children.get(i));
        }
    }

    private boolean fireSimpleRowChangeEvent(int[] oldRows, int[] newRows) {
        int[] shortRows, longRows;
        if (oldRows.length < newRows.length) {
            shortRows = oldRows;   longRows = newRows;
        } else {
            shortRows = newRows;   longRows = oldRows;
        }

        // the two arrays will begin with a sequence of matching
        // initial elements; determine the length of this matching
        // sequence (could be zero)
        int initialLen = 0;
        for (initialLen = 0;   initialLen < shortRows.length;   initialLen++)
            if (shortRows[initialLen] != longRows[initialLen])
                break;

        if (initialLen == longRows.length)
            // the two arrays are identical; no events need to be sent.
            return true;

        // make certain that the remaining elements in the short array
        // match the final elements of the long array.
        int finalLen = shortRows.length - initialLen;
        int diff = longRows.length - shortRows.length;

        for (int i = shortRows.length - finalLen;  i < shortRows.length;  i++)
            if (shortRows[i] != longRows[i+diff])
                // the final elements of the arrays don't match - we
                // can't send a simple table change event.
                return false;

        // Send the resulting simple table change events.
        initialLen--;
        //System.out.println("firing table row updated "+initialLen);
        fireTableRowsUpdated(initialLen, initialLen);
        if (diff > 0) {
            if (oldRows == shortRows) {
                //System.out.println("firing table rows inserted "+
                //                   (initialLen+1)+","+(initialLen+diff));
                fireTableRowsInserted(initialLen+1, initialLen+diff);
            } else {
                //System.out.println("firing table rows deleted "+(initialLen+1)+
                //                   ","+ (initialLen+diff));
                fireTableRowsDeleted(initialLen+1, initialLen+diff);
            }
        }
        return true;
    }



    protected IntList getIndexesForRows(int[] rowNumbers,
                                        boolean excludeRoot)
    {
        IntList result = new IntList();
        WBSNode node;
        for (int i = 0;   i < rowNumbers.length;   i++) {
            if (rowNumbers[i] == 0 && excludeRoot) continue;
            node = getNodeForRow(rowNumbers[i]);
            if (node == null) continue;
            result.add(rows[rowNumbers[i]]);
            if (node.isExpanded() == false)
                result.addAll(getDescendantIndexes(node, rows[rowNumbers[i]]));
        }
        return result;
    }

    public List getNodesForRows(int[] rowNumbers, boolean excludeRoot) {
        ArrayList result = new ArrayList();
        IntList nodeIndexes = getIndexesForRows(rowNumbers, excludeRoot);
        for (int i = 0;   i < nodeIndexes.size();   i++)
            result.add(wbsNodes.get(nodeIndexes.get(i)));

        return result;
    }

    public int[] getRowsForNodes(List nodes) {
        IntList result = new IntList(nodes.size());
        Iterator i = nodes.iterator();
        while (i.hasNext()) {
            int pos = getRowForNode((WBSNode) i.next());
            if (pos != -1) result.add(pos);
        }
        return result.getAsArray();
    }



    public synchronized int[] indentNodes(int[] rowNumbers, int delta) {
        if (delta == 0) return null;
        WBSNode n;

        IntList allNodesToIndent = getIndexesForRows(rowNumbers, true);

        // check to ensure that the indent is legal
        if (delta < 0) {
            for (int i = 0;   i < allNodesToIndent.size();   i++) {
                int nodePos = allNodesToIndent.get(i);
                int newIndentLevel =
                    ((WBSNode) wbsNodes.get(nodePos)).getIndentLevel() + delta;
                if (newIndentLevel < 1) {
                    //System.out.println("indent is illegal");
                    return null;
                }
            }
        }

        int[] oldVisibleRows = rows;

        // now perform the indent
        for (int i = 0;   i < allNodesToIndent.size();   i++) {
            int nodePos = allNodesToIndent.get(i);
            n = (WBSNode) wbsNodes.get(nodePos);
            int newIndentLevel = n.getIndentLevel() + delta;
            n.setIndentLevel(newIndentLevel);
        }
        recalcRows(false);

        // the indentation operation may have caused some previously
        // visible nodes to disappear. See if this has happened, and
        // correct.
        makeVisible(oldVisibleRows);

        // fire "table data changed" to alert the table about the changes.
        fireTableDataChanged();

        // Calculate and return the new effective selection list.
        IntList newSelectedRows = new IntList();
        for (int i = 0;   i < allNodesToIndent.size();   i++) {
            int visibleOnRow = nodePosToRow(allNodesToIndent.get(i));
            if (visibleOnRow != -1)
                newSelectedRows.add(visibleOnRow);
        }
        return newSelectedRows.getAsArray();
    }


    public boolean isVisible(WBSNode n) {
        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return false;
        return isVisible(pos);
    }
    private boolean isVisible(int nodePos) {
        return nodePosToRow(nodePos) != -1;
    }
    public int getRowForNode(WBSNode n) {
        if (n == null) return -1;
        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return -1;
        return nodePosToRow(pos);
    }

    public int nodePosToRow(int nodePos) {
        for (int i = 0;   i < rows.length;   i++)
            if (rows[i] == nodePos) return i;
        return -1;
    }
    private void makeVisible(int nodePos) {
        if (nodePos < 0 || nodePos > wbsNodes.size()-1) return;
        WBSNode n = (WBSNode) wbsNodes.get(nodePos);
        do {
            n = getParent(n);
            if (n == null) break;
            n.setExpanded(true);
        } while (true);
        recalcRows(false);
    }
    private boolean makeVisible(int[] nodePosList) {
        boolean expandedNodes = false;
        for (int i = 0;   i < nodePosList.length;   i++) {
            int nodePos = nodePosList[i];
            if (!isVisible(nodePos)) {
                makeVisible(nodePos);
                expandedNodes = true;
            }
        }
        return expandedNodes;
    }

    public void deleteNodes(List nodesToDelete) {
        deleteNodes(nodesToDelete, true);
    }
    public void deleteNodes(List nodesToDelete, boolean notify) {
        boolean deletionOccurred = false;

        List currentVisibleNodes = new ArrayList();
        for (int i = 0;   i < rows.length;   i++)
            currentVisibleNodes.add(wbsNodes.get(rows[i]));

        Iterator i = nodesToDelete.iterator();
        while (i.hasNext())
            if (wbsNodes.remove(i.next()))
                deletionOccurred = true;

        if (deletionOccurred) {
            recalcRows(false);
            i = currentVisibleNodes.iterator();
            while (i.hasNext())
                makeVisible(wbsNodes.indexOf(i.next()));

            if (notify)
                fireTableDataChanged();
        }
    }

    public int[] insertNodes(List nodesToInsert, int beforeRow) {
        return insertNodes(nodesToInsert, beforeRow, true);
    }

    protected int[] insertNodes(List nodesToInsert, int beforeRow, boolean notify) {
        if (nodesToInsert == null || nodesToInsert.size() == 0) return null;

        List currentVisibleNodes = new ArrayList();
        for (int i = 0;   i < rows.length;   i++)
            currentVisibleNodes.add(wbsNodes.get(rows[i]));
        currentVisibleNodes.add(nodesToInsert.get(0));

        // it's illegal to insert anything before the root node!
        if (beforeRow == 0) beforeRow = 1;

        if (beforeRow < 0 || beforeRow >= rows.length)
            // if the insertion point is illegal, just append nodes to
            // the end of the list.
            wbsNodes.addAll(makeNodesIDUnique(nodesToInsert));

        else {
            int beforePos = rows[beforeRow];
            wbsNodes.addAll(beforePos, makeNodesIDUnique(nodesToInsert));
        }

        recalcRows(false);
        Iterator i = currentVisibleNodes.iterator();
        while (i.hasNext())
            makeVisible(wbsNodes.indexOf(i.next()));

        if (notify)
            fireTableDataChanged();

        return getRowsForNodes(nodesToInsert);
    }

    /** Make this WBS be a copy of the given WBS.
     */
    public void copyFrom(WBSModel w) {
        wbsNodes = (ArrayList) WBSNode.cloneNodeList(w.wbsNodes);
        recalcRows(false);
        fireTableDataChanged();
    }

    protected void loadXML(Element e) {
        NodeList wbsElements = e.getChildNodes();
        int len = wbsElements.getLength();
        for (int i=0;   i < len;   i++) {
            Node n = wbsElements.item(i);
            if (n instanceof Element &&
                WBSNode.ELEMENT_NAME.equals(((Element) n).getTagName()))
                add(new WBSNode(this, (Element) n));
        }
    }

    public void getAsXML(Writer out) throws IOException {
        out.write("<" + WBS_MODEL_TAG + ">\n");
        for (int i = 0;   i < wbsNodes.size();   i++)
            ((WBSNode) wbsNodes.get(i)).getAsXML(out);
        out.write("</" + WBS_MODEL_TAG + ">\n");
    }

    /** This class takes a snapshot of the state of this wbs model, which
     * can be restored later.
     *
     * By wrapping the data in a private class with private access, we
     * ensure that no one else has an opportunity to tamper with the ArrayList
     * between the time the snapshot is created and the time it is restored.
     */
    private class WBSModelSnapshot {
        private ArrayList wbsNodeList;
        public WBSModelSnapshot() {
            synchronized (WBSModel.this) {
                wbsNodeList = (ArrayList) WBSNode.cloneNodeList(wbsNodes);
            }
        }
        void restore() {
            synchronized (WBSModel.this) {
                wbsNodes = (ArrayList) WBSNode.cloneNodeList(wbsNodeList);
                recalcRows(false);
                fireTableDataChanged();
            }
        }
    }

    public Object getSnapshot() {
        return new WBSModelSnapshot();
    }

    public void restoreSnapshot(Object snapshot) {
        if (snapshot instanceof WBSModelSnapshot)
            ((WBSModelSnapshot) snapshot).restore();
    }


    public int[] insertWorkflow(int destRow,
                                String workflowName, WBSModel workflows) {
        // locate the destination node for insertion.
        WBSNode destNode = getNodeForRow(destRow);
        if (destNode == null) return null;
        int destPos = wbsNodes.indexOf(destNode);
        if (destPos == -1) return null;

        // locate the workflow to be inserted.
        WBSNode[] workflowItems = workflows.getChildren(workflows.getRoot());
        WBSNode srcNode = null;
        for (int i = 0;   i < workflowItems.length;   i++)
            if (workflowName.equals(workflowItems[i].getName())) {
                srcNode = workflowItems[i];
                break;
            }
        if (srcNode == null) return null;

        // calculate the list of nodes to insert.
        ArrayList nodesToInsert =
            calcInsertWorkflow(srcNode, destNode, workflows);
        if (nodesToInsert == null || nodesToInsert.isEmpty()) return null;

        // insert the nodes after the last descendant of the dest node.
        IntList destDescendants = getDescendantIndexes(destNode, destPos);
        int insertAfter = destPos;
        if (destDescendants != null && destDescendants.size() > 0)
            insertAfter = destDescendants.get(destDescendants.size() - 1);
        wbsNodes.addAll(insertAfter + 1, makeNodesIDUnique(nodesToInsert));

        // make certain some of the inserted nodes are visible.
        destNode.setExpanded(true);
        recalcRows();
        return getRowsForNodes(nodesToInsert);
    }

    private ArrayList calcInsertWorkflow(WBSNode srcNode, WBSNode destNode,
                                         WBSModel workflows) {
        ArrayList nodesToInsert = new ArrayList();

        // calculate the difference in indentation level out
        int srcIndentation = srcNode.getIndentLevel();
        int destIndentation = destNode.getIndentLevel();
        int indentDelta = destIndentation - srcIndentation;

        // make a list of the names of the children of destNode.
        WBSNode[] destChildren = getChildren(destNode);
        ArrayList destChildNames = new ArrayList();
        for (int i = 0;   i < destChildren.length;   i++)
            destChildNames.add(destChildren[i].getName());

        // iterate over each child of srcNode.
        WBSNode[] srcChildren = workflows.getChildren(srcNode);
        for (int i = 0;   i < srcChildren.length;   i++) {
            WBSNode srcChild = srcChildren[i];
            // we don't want to clobber any nodes that already exist in
            // the destination, so we'll skip any children whose names
            // already appear underneath destNode
            if (destChildNames.contains(srcChild.getName())) continue;

            // add the child to our insertion list.
            appendWorkflowNode(nodesToInsert, srcChild, indentDelta);
            // add all the descendants of the child to our insertion list.
            WBSNode[] srcDescendants = workflows.getDescendants(srcChild);
            for (int j = 0;   j < srcDescendants.length;   j++)
                appendWorkflowNode(nodesToInsert, srcDescendants[j],
                                   indentDelta);
        }

        return nodesToInsert;
    }
    private void appendWorkflowNode(List dest, WBSNode node, int indentDelta) {
        node = (WBSNode) node.clone();
        node.setIndentLevel(node.getIndentLevel() + indentDelta);
        dest.add(node);
    }
    public String filterNodeType(WBSNode node) {
        return node.getType();
    }

    public int[] mergeWBSModel(WBSModel srcModel, WBSNodeMerger merger,
            WBSNodeComparator comp) {
        List insertedNodes = new ArrayList();
        mergeWBSModel(srcModel, srcModel.getRoot(), this, this.getRoot(),
                merger, comp, insertedNodes);

        recalcRows(false);
        fireTableDataChanged();
        return getRowsForNodes(insertedNodes);
    }

    private static void mergeWBSModel(WBSModel srcModel, WBSNode srcNode,
            WBSModel destModel, WBSNode destNode,
            WBSNodeMerger merger, WBSNodeComparator comp,
            List insertedNodes) {

        WBSNode[] srcChildren = srcModel.getChildren(srcNode);
        WBSNode[] destChildren = destModel.getChildren(destNode);

        boolean[] destMatched = new boolean[destChildren.length];
        Arrays.fill(destMatched, false);

        for (int i = 0; i < srcChildren.length; i++) {
            WBSNode srcChild = srcChildren[i];
            String srcChildName = srcChild.getName();
            if (srcChildName == null || srcChildName.trim().length() == 0)
                continue;

            boolean foundMatch = false;
            for (int j = 0; j < destChildren.length; j++) {
                WBSNode destChild = destChildren[j];
                if (comp == null
                        ? srcChildName.equals(destChild.getName())
                        : comp.nodesMatch(srcChild, destChild)) {
                    foundMatch = destMatched[j] = true;
                    if (merger != null)
                        merger.mergeNodes(srcChild, destChild);
                    mergeWBSModel(srcModel, srcChild, destModel, destChild,
                            merger, comp, insertedNodes);
                    break;
                }
            }

            if (!foundMatch) {
                WBSNode[] srcDescendants = srcModel.getDescendants(srcChild);
                List nodesToInsert = WBSNode.cloneNodeList(Arrays
                        .asList(srcDescendants));
                nodesToInsert.add(0, srcChild.clone());

                int destPos = destModel.wbsNodes.indexOf(destNode);
                int insertAfter = destPos;

                IntList destDescendants = destModel.getDescendantIndexes(
                        destNode, destPos);
                if (destDescendants != null && destDescendants.size() > 0)
                    insertAfter = destDescendants.get(destDescendants.size() - 1);

                destModel.wbsNodes.addAll(insertAfter + 1,
                        destModel.makeNodesIDUnique(nodesToInsert));
                insertedNodes.addAll(nodesToInsert);
            }
        }

        if (merger != null) {
            for (int j = 0; j < destMatched.length; j++)
                if (destMatched[j] == false)
                    mergeUnmatchedNodes(destModel, destChildren[j], merger);
        }
    }

    private static void mergeUnmatchedNodes(WBSModel destModel,
            WBSNode destNode, WBSNodeMerger merger) {
        merger.mergeNodes(null, destNode);
        WBSNode[] descendants = destModel.getDescendants(destNode);
        for (int i = 0; i < descendants.length; i++)
            merger.mergeNodes(null, descendants[i]);
    }

    public void copyNodeExpansionStates(WBSModel src, WBSNodeComparator comp) {
        for (Iterator i = this.wbsNodes.iterator(); i.hasNext();) {
            WBSNode destNode = (WBSNode) i.next();
            for (Iterator j = src.wbsNodes.iterator(); j.hasNext();) {
                WBSNode srcNode = (WBSNode) j.next();
                if (comp.nodesMatch(srcNode, destNode)) {
                    destNode.setExpanded(srcNode.isExpanded());
                    break;
                }
            }
        }
    }

    public Set getExpandedNodeIDs() {
        Set expandedNodes = new HashSet();
        for (int i = 1;  i < wbsNodes.size(); i++) {
            WBSNode node = (WBSNode) wbsNodes.get(i);
            if (node.isExpanded()) {
                expandedNodes.add(Integer.toString(node.getUniqueID()));
            }
        }

        return expandedNodes;
    }

    public void setExpandedNodeIDs(Set expandedNodes) {
        for (int i = 1;  i < wbsNodes.size(); i++) {
            WBSNode node = (WBSNode) wbsNodes.get(i);
            String nodeID = Integer.toString(node.getUniqueID());
            node.setExpanded(expandedNodes.contains(nodeID));
        }
        recalcRows(true);
    }

    private static final String CACHED_CHILDREN = "_cached_child_list_";
    private static final String CACHED_REORDERABLE_CHILDREN =
        "_cached_reorderable_child_list_";
    private static final String CACHED_CHILD_INDEXES = "_cached_child_ind_";
    private static final String CACHED_PARENT = "_cached_parent_node_";
    private static final String[] CACHING_ATTRS = { CACHED_CHILDREN,
            CACHED_REORDERABLE_CHILDREN, CACHED_CHILD_INDEXES, CACHED_PARENT };
    private static final boolean CACHE = true;
}
