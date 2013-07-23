// Copyright (C) 2002-2013 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;


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
    private ArrayList<WBSNode> wbsNodes;

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
        this(rootNodeName, true);
    }

    public WBSModel(String rootNodeName, boolean createDefaultNode) {
        wbsNodes = new ArrayList();
        if (rootNodeName == null || rootNodeName.trim().length() == 0)
            rootNodeName = "Team Project";
        add(new WBSNode(this, rootNodeName, "Project", 0, true));
        getRoot().setUniqueID((new Random()).nextInt() & 0xffffff);
        if (createDefaultNode)
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

    public WBSModelValidator getValidator() {
        return validator;
    }

    /** Add a node to the end of this work breakdown structure.
     */
    public synchronized void add(WBSNode node) {
        wbsNodes.add(prepareNodeForInsertion(node));
        recalcRows();
    }

    /**
     * Add a node to the end of this WBS; but do not alter its unique ID and do
     * not recalculate dependent values (like the list of visible rows).
     * 
     * This method is appropriate for situations where the WBS node structure
     * has been determined in advance (like a merging scenario).  The caller
     * assumes all responsibility for ensuring that node IDs are unique, and
     * that the dependent values (like the row list) are eventually updated.
     */
    synchronized void addImpl(WBSNode node) {
        wbsNodes.add(node);
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
            wbsNodes.add(beforePos, prepareNodeForInsertion(newNode));
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

    protected int getIndexOfNode(Object node) {
        return wbsNodes.indexOf(node);
    }

    /** Return a collection of the nodes in this model, indexed by unique ID.
     * The root node is included in the result with the <tt>null</tt> key. */
    public Map<Integer, WBSNode> getNodeMap() {
        Map<Integer, WBSNode> result = new HashMap<Integer, WBSNode>();
        for (int i = 1;  i < wbsNodes.size();  i++) {
            WBSNode n = (WBSNode) wbsNodes.get(i);
            result.put(n.getUniqueID(), n);
        }
        result.put(null, getRoot());
        result.put(-1000, getRoot());
        return result;
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

    private WBSNode prepareNodeForInsertion(WBSNode node) {
        prepareNodesForInsertion(Collections.singletonList(node));
        return node;
    }

    protected void tweakNodeForInsertion(WBSNode node) {}

    private synchronized List prepareNodesForInsertion(List<WBSNode> nodes) {
        // tweak each of the incoming nodes, and gather up a Set of their IDs.
        // if any of the IDs are duplicates, add them to a collision Set too.
        IntList incomingIDs = new IntList();
        IntList collidingIDs = new IntList();
        for (WBSNode node : nodes) {
            tweakNodeForInsertion(node);

            int oneID = node.getUniqueID();
            if (incomingIDs.contains(oneID))
                collidingIDs.add(oneID);
            else if (oneID > 0)
                incomingIDs.add(oneID);
        }

        // look through the node list to see if any existing nodes have the
        // same ID as an incoming node. While we're at it, find the largest
        // node ID in use. (This will generally be the ID of the root node,
        // but we won't take any chances.)
        for (WBSNode node : wbsNodes) {
            int oneID = node.getUniqueID();
            maxID = Math.max(maxID, oneID);
            if (incomingIDs.contains(oneID))
                collidingIDs.add(oneID);
        }

        // assign unique IDs to the incoming nodes as needed
        for (WBSNode node : nodes) {
            int oneID = node.getUniqueID();
            if (oneID <= 0 || collidingIDs.contains(oneID)) {
                if (wbsNodes.isEmpty())
                    node.setUniqueID(++maxID);
                else {
                    node.setUniqueID(getRoot().getUniqueID());
                    getRoot().setUniqueID(++maxID);
                }
                node.discardTransientAttributes(true);
            }
        }

        return nodes;
    }

    /**
     * A legacy bug allowed nodes to receive duplicate IDs in a small number of
     * situations. This method searches through the list of WBS nodes looking
     * for that problem, and assigns new IDs to resolve any duplicates.
     */
    private synchronized void ensureAllIDsAreUnique() {
        Set<Integer> ids = new HashSet<Integer>(wbsNodes.size());
        List<WBSNode> collisions = new ArrayList<WBSNode>();
        for (WBSNode node : wbsNodes) {
            if (ids.add(node.getUniqueID()) == false)
                collisions.add(node);
        }
        if (!collisions.isEmpty())
            prepareNodesForInsertion(collisions);
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
    void recalcRowsForExpansionEvent() { recalcRows(true, true); }
    void recalcRows(boolean notify) { recalcRows(notify, false); }
    void recalcRows(boolean notify, boolean isExpansionOnly) {
        IntList resultList = new IntList(wbsNodes.size());
        if (isExpansionOnly)
            recalcRows(resultList, 0);
        else
            recalcStructureAndRows(resultList);
        int[] oldRows = rows;
        rows = resultList.getAsArray();
        if (notify)
            if (oldRows != null
                    && !fireSimpleRowChangeEvent(oldRows, rows, isExpansionOnly)) {
                //System.out.println("firing table data changed");
                fireTableChanged(new WBSModelEvent(this, isExpansionOnly));
            }
    }

    private void recalcRows(IntList resultList, int nodePos) {
        WBSNode node = (WBSNode) wbsNodes.get(nodePos);

        // check to see if this node was hidden by a filtering operation.  If
        // so, this node and its children should not appear in the row list.
        if (nodePos > 0 && isHiddenByFilter(node))
            return;

        resultList.add(nodePos);

        if (nodePos == 0 || node.isExpanded()) {
            IntList children = getChildIndexes(node);

            if (children != null)
                for (int i=0;   i < children.size();   i++)
                    recalcRows(resultList, children.get(i));
        }
    }

    // In a single pass through the list of WBS nodes, calculate the parents,
    // the child indexes, and the nodes which are visible on each row.
    private void recalcStructureAndRows(IntList rows) {
        // add an entry for the root node
        rows.add(0);
        StructureData sd = new StructureData(0);

        // iterate over other nodes, calculate their structure, and add them
        // to the row list if applicable
        for (int i = 1;  i < wbsNodes.size();  i++) {
            sd = sd.appendNextRow(i);
            if (sd.visible)
                rows.add(i);
        }
    }

    private class StructureData {
        StructureData parent;
        int nodePos;
        WBSNode node;
        boolean visible;
        boolean expanded;
        IntList childIndexes;

        public StructureData(int nodePos) {
            this.nodePos = nodePos;
            this.node = wbsNodes.get(nodePos);
            this.expanded = (nodePos == 0 || node.isExpanded());
            this.visible = !isHiddenByFilter(node);
            this.childIndexes = new IntList();
            node.setAttribute(CACHED_PARENT, null);
            node.setAttribute(CACHED_CHILDREN, null);
            node.setAttribute(CACHED_REORDERABLE_CHILDREN, null);
            node.setAttribute(CACHED_CHILD_INDEXES, childIndexes);
        }

        public StructureData appendNextRow(int nodePos) {
            StructureData result = new StructureData(nodePos);

            StructureData parent = this;
            while (parent.node.getIndentLevel() >= result.node.getIndentLevel())
                parent = parent.parent;
            result.setParent(parent);

            return result;
        }

        private void setParent(StructureData parent) {
            this.parent = parent;
            this.node.setAttribute(CACHED_PARENT, parent.node);
            this.visible = this.visible && parent.visible && parent.expanded;
            parent.childIndexes.add(this.nodePos);
        }
    }

    private boolean fireSimpleRowChangeEvent(int[] oldRows, int[] newRows,
            boolean isExpansionOnly) {
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

        // in response to a typical insertion or deletion of rows, it is common
        // for the row immediately preceding the change to alter its appearance.
        // for example, with an expansion/collapse event, the node will need to
        // repaint its expansion icon. Send an UPDATE event for this purpose.
        fireTableChanged(new WBSModelEvent(this, initialLen, initialLen,
                WBSModelEvent.ALL_COLUMNS, WBSModelEvent.UPDATE,
                isExpansionOnly));

        // Now, send a change event for any inserted/deleted rows.
        if (diff > 0) {
            int changeType = (oldRows == shortRows
                    ? WBSModelEvent.INSERT : WBSModelEvent.DELETE);
            fireTableChanged(new WBSModelEvent(this, initialLen+1,
                    initialLen+diff, WBSModelEvent.ALL_COLUMNS,
                    changeType, isExpansionOnly));
        }
        return true;
    }



    /** A collection of IDs for nodes that should be hidden */
    private Set<Integer> filteredNodeIDs;

    public void filterRows(WBSFilter... filters) {
        if (filters == null || filters.length == 0
                || (filters.length == 1 && filters[0] == null)) {
            this.filteredNodeIDs = null;
        } else {
            Set<Integer> newHiddenNodes = new HashSet();
            calcfilteredNodes(filters, 0, getRoot(), newHiddenNodes);
            newHiddenNodes.remove(getRoot().getUniqueID());
            this.filteredNodeIDs = newHiddenNodes;
        }
        recalcRows(true, true);
    }

    /**
     * Iterate over the nodes hierarchically, determining which ones should be
     * hidden and which should be visible.
     * 
     * @param filters
     *            a list of filters which nodes must match. Note that a single
     *            node does not need to match all of the filters; rather, the
     *            list of filters must be satified by a node and its ancestors.
     *            If this condition is met, the node, its ancestors, and its
     *            descendants will all match, and none will be filtered.
     * @param alreadyMatchedLen
     *            the number of initial entires in the "filters" array that have
     *            been satisfied by ancestors of this node.
     * @param node
     *            a node to match
     * @param filteredIDs
     *            a collection where we should write the IDs of nodes that do
     *            not match the filter.
     * 
     * @return true if this node matches, false if it should be filtered.
     */
    private boolean calcfilteredNodes(WBSFilter[] filters,
            int alreadyMatchedLen, WBSNode node, Set<Integer> filteredIDs) {
        // check to see if this node matches all of the remaining filters.
        int matchLen = filterMatchLen(filters, alreadyMatchedLen, node);
        boolean shouldShow = (matchLen == filters.length);

        // if this node matches, all of its decendants match too; so we don't
        // need to recurse, we can return immediately.
        if (shouldShow)
            return true;

        // recurse over each of our children.
        for (WBSNode child : getChildren(node)) {
            // if one of our children matches the remaining filters, we match.
            if (calcfilteredNodes(filters, matchLen, child, filteredIDs))
                shouldShow = true;
        }

        // if we don't match all the filters, and our decendants don't either,
        // this node should be hidden.
        if (shouldShow == false)
            filteredIDs.add(node.getUniqueID());
        return shouldShow;
    }

    /**
     * Determine whether a node matches any of the filters in an array. If so,
     * reorder the array to place the matching filters first, and return the
     * number of filters that were matched.
     * 
     * @param filters
     *            a list of filters to match against. Items in this array in
     *            positions greater than or equal to alreadyMatchedLen may be
     *            reordered to place matching filters first.
     * @param alreadyMatchedLen
     *            the number of filters at the beginning of the array that have
     *            already been satisfied, and which do not need to be evaluated
     *            for this node. Those initial filters will not be reordered by
     *            this method.
     * @param node
     *            the node that we should evaluate the filters for.
     * 
     * @return the total number of filters that were matched by this node, plus
     *         the alreadyMatchedLen
     */
    private int filterMatchLen(WBSFilter[] filters, int alreadyMatchedLen,
            WBSNode node) {
        int len = alreadyMatchedLen;
        for (int pos = len;  pos < filters.length;  pos++) {
            WBSFilter f = filters[pos];
            if (f.match(node)) {
                if (pos != len) {
                    // swap array entries to put the matching filter first.
                    filters[pos] = filters[len];
                    filters[len] = f;
                }
                len++;
            }
        }
        return len;
    }

    private boolean isHiddenByFilter(WBSNode node) {
        if (filteredNodeIDs == null || node == null)
            return false;
        else
            return filteredNodeIDs.contains(node.getUniqueID());
    }

    public void remapFilteredNodeIDs(Map<Integer, Integer> idMap) {
        if (filteredNodeIDs != null && idMap != null && !idMap.isEmpty()) {
            Set<Integer> newFilter = new HashSet<Integer>();
            for (Integer oldID : filteredNodeIDs) {
                Integer newID = idMap.get(oldID);
                if (newID != null)
                    newFilter.add(newID);
                else
                    newFilter.add(oldID);
            }
            filteredNodeIDs = newFilter;
        }
    }

    /**
     * Find a WBSNode that matches the given filter
     * 
     * @param f the filter for searching
     * @param after search after a particular node
     * @param wrap if true, search should wrap around if no match is found
     * @return the next matching node found, or null if there were no matches
     */
    public WBSNode findNextNodeMatching(WBSFilter f, WBSNode after, boolean wrap) {
        int pos = -1;
        if (after != null)
            pos = wbsNodes.indexOf(after);

        for (pos++; pos < wbsNodes.size();  pos++) {
            WBSNode node = wbsNodes.get(pos);
            if (f.match(node))
                return node;
        }

        if (wrap)
            return findNextNodeMatching(f, null, false);
        else
            return null;
    }

    /**
     * Find a WBSNode that matches the given filter
     * 
     * @param f the filter for searching
     * @param before search before a particular node
     * @param wrap if true, search should wrap around if no match is found
     * @return the next matching node found, or null if there were no matches
     */
    public WBSNode findPreviousNodeMatching(WBSFilter f, WBSNode before,
            boolean wrap) {
        int pos = -1;
        if (before != null)
            pos = wbsNodes.indexOf(before);
        if (pos == -1)
            pos = wbsNodes.size();

        for (pos--; pos >= 0;  pos--) {
            WBSNode node = wbsNodes.get(pos);
            if (f.match(node))
                return node;
        }

        if (wrap)
            return findPreviousNodeMatching(f, null, false);
        else
            return null;
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

    public List<WBSNode> getNodesForRows(int[] rowNumbers, boolean excludeRoot) {
        ArrayList<WBSNode> result = new ArrayList();
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
    public int makeVisible(WBSNode node) {
        int pos = wbsNodes.indexOf(node);
        if (pos == -1)
            return -1;
        makeVisible(pos, true);
        return nodePosToRow(pos);
    }
    private void makeVisible(int nodePos) {
        makeVisible(nodePos, false);
    }
    private void makeVisible(int nodePos, boolean notify) {
        if (nodePos < 0 || nodePos > wbsNodes.size()-1) return;
        WBSNode n = (WBSNode) wbsNodes.get(nodePos);

        // if the node in question is currently filtered, we will be making
        // it visible again.  This means that all of its descendants should
        // also be made visible.
        if (isHiddenByFilter(n)) {
            for (WBSNode desc : getDescendants(n))
                filteredNodeIDs.remove(desc.getUniqueID());
        }

        do {
            if (filteredNodeIDs != null)
                filteredNodeIDs.remove(n.getUniqueID());
            n = getParent(n);
            if (n == null) break;
            n.setExpanded(true);
        } while (true);
        recalcRows(notify);
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

    public boolean deleteNodes(List nodesToDelete) {
        return deleteNodes(nodesToDelete, true);
    }
    public boolean deleteNodes(List nodesToDelete, boolean notify) {
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

        return deletionOccurred;
    }

    public int[] insertNodes(List nodesToInsert, int beforeRow) {
        return insertNodes(nodesToInsert, beforeRow, true);
    }

    protected int[] insertNodes(List nodesToInsert, int beforeRow, boolean notify) {
        if (nodesToInsert == null || nodesToInsert.size() == 0) return null;

        int beforePos;

        // it's illegal to insert anything before the root node!
        if (beforeRow == 0) beforePos = 1;

        if (beforeRow < 0 || beforeRow >= rows.length)
            // if the insertion point is illegal, just append nodes to
            // the end of the list.
            beforePos = Integer.MAX_VALUE;

        else
            beforePos = rows[beforeRow];

        insertNodesAt(nodesToInsert, beforePos, notify);
        return getRowsForNodes(nodesToInsert);
    }

    protected void insertNodesAt(List nodesToInsert, int beforePos,
            boolean notify) {
        if (nodesToInsert == null || nodesToInsert.size() == 0)
            return;

        List currentVisibleNodes = new ArrayList();
        for (int i = 0;   i < rows.length;   i++)
            currentVisibleNodes.add(wbsNodes.get(rows[i]));
        currentVisibleNodes.add(nodesToInsert.get(0));

        if (beforePos >= wbsNodes.size())
            wbsNodes.addAll(prepareNodesForInsertion(nodesToInsert));
        else
            wbsNodes.addAll(beforePos, prepareNodesForInsertion(nodesToInsert));

        recalcRows(false);
        Iterator i = currentVisibleNodes.iterator();
        while (i.hasNext())
            makeVisible(wbsNodes.indexOf(i.next()));

        if (notify)
            fireTableDataChanged();
    }

    /**
     * Move a specified node "up" - for example, to make it swap places with its
     * prior sibling.
     * 
     * @param node
     *            the node to move. All descendants of the specified node will
     *            be moved as well.
     * @return the list of rows for the nodes that have moved. If no move was
     *         performed, returns null.
     */
    public int[] moveNodeUp(WBSNode node) {
        int nodePos = getIndexOfNode(node);
        int destPos = getMoveNodeUpInsertionPos(node, nodePos);
        if (destPos < 1)
            return null;

        List nodesToMove = new ArrayList();
        nodesToMove.add(node);
        nodesToMove.addAll(Arrays.asList(getDescendants(node)));
        deleteNodes(nodesToMove, false);
        insertNodesAt(nodesToMove, destPos, true);
        return getRowsForNodes(nodesToMove);
    }

    private int getMoveNodeUpInsertionPos(WBSNode nodeToMove, int nodePos) {
        if (nodePos < 2)
            return -1;

        int indent = nodeToMove.getIndentLevel();

        // Start by examining the node that immediately precedes the nodeToMove.
        // If it is a sibling or a parent of the nodeToMove, then we want
        // to move the node immediately before it. If not, start walking up
        // the tree until we find a node that is a sibling or parent.
        WBSNode node = wbsNodes.get(nodePos - 1);
        while (node != null) {
            if (node.getIndentLevel() <= indent)
                return getIndexOfNode(node);
            else
                node = getParent(node);
        }

        return -1;
    }

    /**
     * Move a specified node "down" - for example, to make it swap places with
     * its subsequent sibling.
     * 
     * @param node
     *            the node to move. All descendants of the specified node will
     *            be moved as well.
     * @return the list of rows for the nodes that have moved. If no move was
     *         performed, returns null.
     */
    public int[] moveNodeDown(WBSNode node) {
        int nodePos = getIndexOfNode(node);
        if (nodePos == -1)
            return null;

        List nodesToMove = new ArrayList();
        nodesToMove.add(node);
        nodesToMove.addAll(Arrays.asList(getDescendants(node)));

        int destPos = getMoveNodeDownInsertionPos(nodesToMove);
        if (destPos == -1)
            return null;

        deleteNodes(nodesToMove, false);
        insertNodesAt(nodesToMove, destPos - nodesToMove.size(), true);
        return getRowsForNodes(nodesToMove);
    }

    private int getMoveNodeDownInsertionPos(List nodesToMove) {
        WBSNode firstMovedNode = (WBSNode) nodesToMove.get(0);
        Object lastMovedNode = nodesToMove.get(nodesToMove.size() - 1);
        int lastMovedPos = getIndexOfNode(lastMovedNode);
        int nextPos = lastMovedPos + 1;

        if (nextPos >= wbsNodes.size())
            return -1;

        // look at the node that follows the nodes we are moving. If it has
        // a shallower indent level than the nodes we are moving, then we
        // want to insert our node immediately after it. (This helps the
        // moveUp and moveDown operations to be opposites of each other.)
        WBSNode nextNode = wbsNodes.get(nextPos);
        if (nextNode.getIndentLevel() < firstMovedNode.getIndentLevel())
            return nextPos + 1;

        IntList nextDescendants = getDescendantIndexes(null, nextPos);
        if (nextDescendants.size() == 0)
            return nextPos + 1;
        else
            return nextDescendants.get(nextDescendants.size() - 1) + 1;
    }

    /** Make this WBS be a copy of the given WBS.
     */
    public void copyFrom(WBSModel w) {
        if (w != this) {
            wbsNodes = (ArrayList) WBSNode.cloneNodeList(w.wbsNodes, this);
            recalcRows(false);
            fireTableDataChanged();
        }
    }

    /**
     * @return true if this object contains the exact same list of nodes as
     * another WBSModel.
     */
    public boolean isEqualTo(WBSModel that) {
        // first, check to see if we have the same number of nodes.  If not,
        // these two WBS models are not equal.
        if (this.wbsNodes.size() != that.wbsNodes.size())
            return false;

        // next, walk the list of nodes and make certain they are all equal.
        // if we find a mismatch, these two WBS models are not equal.
        for (int i = 0;  i < this.wbsNodes.size();  i++)
            if (!this.wbsNodes.get(i).isEqualTo(that.wbsNodes.get(i)))
                return false;

        // no differences were found.  The WBS models are equal.
        return true;
    }

    protected void loadXML(Element e) {
        NodeList wbsElements = e.getChildNodes();
        int len = wbsElements.getLength();
        for (int i=0;   i < len;   i++) {
            Node n = wbsElements.item(i);
            if (n instanceof Element &&
                WBSNode.ELEMENT_NAME.equals(((Element) n).getTagName()))
                addImpl(new WBSNode(this, (Element) n));
        }
        ensureAllIDsAreUnique();
        recalcRows(false);
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


    public int[] insertWorkflow(int destRow, String workflowName,
            WBSModel workflows, PatternList attrsToKeep, Map extraDefaultAttrs) {
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

        // update the workflow source ID of the destination node.
        addWorkflowSourceID(destNode, srcNode.getUniqueID());

        // calculate the list of nodes to insert.
        List<WBSNode> nodesToInsert =
            calcInsertWorkflow(srcNode, destNode, workflows);
        if (nodesToInsert == null || nodesToInsert.isEmpty()) return null;

        // possibly clear extraneous attributes that are undesirable to keep.
        if (attrsToKeep != null) {
            for (WBSNode node : nodesToInsert)
                node.discardAttributesExcept(attrsToKeep,
                    WORKFLOW_SOURCE_IDS_ATTR);
        }

        // insert the nodes after the last descendant of the dest node.
        IntList destDescendants = getDescendantIndexes(destNode, destPos);
        int insertAfter = destPos;
        if (destDescendants != null && destDescendants.size() > 0)
            insertAfter = destDescendants.get(destDescendants.size() - 1);
        wbsNodes.addAll(insertAfter + 1, prepareNodesForInsertion(nodesToInsert));

        // possibly set extra default attrs that were requested
        if (extraDefaultAttrs != null && !extraDefaultAttrs.isEmpty()) {
            for (Iterator i = extraDefaultAttrs.entrySet().iterator(); i
                    .hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String attr = (String) e.getKey();
                Object value = e.getValue();
                for (WBSNode node : nodesToInsert) {
                    if (node.getAttribute(attr) == null)
                        node.setAttribute(attr, value);
                }
            }
        }

        // make certain some of the inserted nodes are visible.
        destNode.setExpanded(true);
        recalcRows();
        return getRowsForNodes(nodesToInsert);
    }

    private void addWorkflowSourceID(WBSNode node, int sourceID) {
        String oldIDs = (String) node.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
        String newIDs = Integer.toString(sourceID);
        if (oldIDs != null && oldIDs.length() > 0) {
            List<String> oldIdList = Arrays.asList(oldIDs.split(","));
            if (oldIdList.contains(newIDs))
                newIDs = oldIDs;
            else
                newIDs = oldIDs + "," + newIDs;
        }
        node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, newIDs);
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
        Set destChildNames = new HashSet();
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
        node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR,
            Integer.toString(node.getUniqueID()));
        dest.add(node);
    }

    public void remapWorkflowSourceIDs(Map<Integer, Integer> idMap) {
        if (idMap == null || idMap.isEmpty())
            return;

        for (WBSNode node : this.wbsNodes) {
            String ids = (String) node.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
            if (ids != null && ids.length() > 0) {
                boolean madeChange = false;
                String[] list = ids.split(",");
                for (int i = 0; i < list.length; i++) {
                    Integer oneID = Integer.parseInt(list[i]);
                    Integer newID = idMap.get(oneID);
                    if (newID != null) {
                        list[i] = Integer.toString(newID);
                        madeChange = true;
                    }
                }
                if (madeChange) {
                    String newVal = StringUtils.join(Arrays.asList(list), ",");
                    node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, newVal);
                }
            }
        }
    }

    public String filterNodeType(WBSNode node) {
        return node.getType();
    }

    public int[] mergeWBSModel(WBSModel srcModel, WBSNodeMerger merger,
            WBSNodeComparator comp, boolean reorderNodes) {
        List insertedNodes = new ArrayList();
        mergeWBSModel(srcModel, srcModel.getRoot(), this, this.getRoot(),
                merger, comp, reorderNodes, insertedNodes);

        recalcRows(false);
        fireTableDataChanged();
        return getRowsForNodes(insertedNodes);
    }

    private static void mergeWBSModel(WBSModel srcModel, WBSNode srcNode,
            WBSModel destModel, WBSNode destNode,
            WBSNodeMerger merger, WBSNodeComparator comp,
            boolean reorderNodes, List insertedNodes) {

        WBSNode[] srcChildren = srcModel.getChildren(srcNode);
        WBSNode[] destChildren = destModel.getChildren(destNode);

        boolean[] destMatched = new boolean[destChildren.length];
        Arrays.fill(destMatched, false);
        WBSNode lastMergedDestChild = null;

        for (int i = srcChildren.length; i-- > 0; ) {
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
                            merger, comp, reorderNodes, insertedNodes);

                    // if the newly merged child is out of order, reorder it.
                    if (reorderNodes && lastMergedDestChild != null) {
                        int destChildPos = destModel.wbsNodes
                                .indexOf(destChild);
                        int lastMergedChildPos = destModel.wbsNodes
                                .indexOf(lastMergedDestChild);
                        if (destChildPos > lastMergedChildPos) {
                            int destChildEnd = getLastDescendantPos(destModel,
                                destChild);
                            List destChildNodes = destModel.wbsNodes.subList(
                                destChildPos, destChildEnd + 1);
                            List nodesToMove = new ArrayList(destChildNodes);
                            destChildNodes.clear();
                            destModel.wbsNodes.addAll(lastMergedChildPos,
                                nodesToMove);
                        }
                    }

                    lastMergedDestChild = destChild;
                    break;
                }
            }

            if (!foundMatch) {
                WBSNode[] srcDescendants = srcModel.getDescendants(srcChild);
                List nodesToInsert = WBSNode.cloneNodeList(Arrays
                        .asList(srcDescendants));
                nodesToInsert.add(0, srcChild.clone());

                int insertBefore;
                if (lastMergedDestChild == null) {
                    // if this is the last child to merge, insert it at the end
                    // of the list, after all other children of the dest parent.
                    insertBefore = getLastDescendantPos(destModel, destNode) + 1;
                } else {
                    // if we've already merged a subsequent sibling, insert this
                    // child immediately before it
                    insertBefore = destModel.wbsNodes.indexOf(lastMergedDestChild);
                }

                destModel.wbsNodes.addAll(insertBefore,
                        destModel.prepareNodesForInsertion(nodesToInsert));
                insertedNodes.addAll(nodesToInsert);

                lastMergedDestChild = (WBSNode) nodesToInsert.get(0);
            }
        }

        if (merger != null) {
            for (int j = 0; j < destMatched.length; j++)
                if (destMatched[j] == false)
                    mergeUnmatchedNodes(destModel, destChildren[j], merger);
        }
    }

    private static int getLastDescendantPos(WBSModel model, WBSNode node) {
        int pos = model.wbsNodes.indexOf(node);
        if (pos == -1)
            return -1;
        IntList descendants = model.getDescendantIndexes(node, pos);
        if (descendants == null || descendants.size() == 0)
            return pos;
        else
            return descendants.get(descendants.size() - 1);
    }

    private static void mergeUnmatchedNodes(WBSModel destModel,
            WBSNode destNode, WBSNodeMerger merger) {
        merger.mergeNodes(null, destNode);
        WBSNode[] descendants = destModel.getDescendants(destNode);
        for (int i = 0; i < descendants.length; i++)
            merger.mergeNodes(null, descendants[i]);
    }

    /** WARNING! Not for general use!! This operation is destructive to most
     * types of WBSModels. */
    void sortAllNonRootNodes(Comparator<WBSNode> c) {
        Collections.sort(wbsNodes.subList(1, wbsNodes.size()), c);
        fireTableDataChanged();
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
        recalcRowsForExpansionEvent();
    }

    public static final String CREATED_WITH_ATTR = "createdWithVersion";
    public static final String WORKFLOW_SOURCE_IDS_ATTR = "workflowSourceIDs";

    private static final String CACHED_CHILDREN = "_cached_child_list_";
    private static final String CACHED_REORDERABLE_CHILDREN =
        "_cached_reorderable_child_list_";
    private static final String CACHED_CHILD_INDEXES = "_cached_child_ind_";
    private static final String CACHED_PARENT = "_cached_parent_node_";
    private static final boolean CACHE = true;
}
