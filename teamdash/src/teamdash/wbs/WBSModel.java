
package teamdash.wbs;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class WBSModel extends AbstractTableModel {

    private ArrayList wbsNodes;
    private WBSModelValidator validator;
    protected EventListenerList listenerList = new EventListenerList();


    public WBSModel() { this("Root"); }

    public WBSModel(String name) {
        wbsNodes = new ArrayList();
        add(new WBSNode(this, name, "Project", 0, true));
        validator = new WBSModelValidator(this);
    }

    public WBSModel(Element e) {
        wbsNodes = new ArrayList();
        loadXML(e);
        validator = new WBSModelValidator(this);
        validator.recalc();
    }

    public void add(WBSNode node) {
        wbsNodes.add(node);
        recalcRows();
    }

    public int size() { return wbsNodes.size(); }

    public WBSNode getRoot() {
        return (WBSNode) wbsNodes.get(0);
    }

    public WBSNode getNodeForRow(int row) {
        if (row < 0 || row > rows.length) return null;
        return (WBSNode) wbsNodes.get(rows[row]);
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    public int getChildCount(Object n) {
        IntList children = getChildIndexes(n);
        return (children == null ? 0 : children.size());
    }

    public WBSNode[] getChildren(Object n) {
        IntList childIndexes = getChildIndexes(n);
        if (childIndexes == null || childIndexes.size() == 0)
            return EMPTY_NODE_LIST;

        WBSNode[] result = new WBSNode[childIndexes.size()];
        for (int i = 0;   i < childIndexes.size();   i++)
            result[i] = (WBSNode) wbsNodes.get(childIndexes.get(i));

        return result;
    }
    private static final WBSNode[] EMPTY_NODE_LIST = new WBSNode[0];

    public WBSNode getParent(WBSNode n) {
        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return null;

        int nodeIndentLevel = n.getIndentLevel();
        WBSNode possibleParent;
        while (pos-- > 0) {
            possibleParent = (WBSNode) wbsNodes.get(pos);
            if (possibleParent.getIndentLevel() < nodeIndentLevel)
                return possibleParent;
        }

        return null;
    }

    public IntList getChildIndexes(Object n) {
        int pos = wbsNodes.indexOf(n);
        if (pos == -1) return null;
        return getChildIndexes((WBSNode) n, pos);
    }

    private IntList getChildIndexes(WBSNode node, int pos) {
        if (node == null)
            node = (WBSNode) wbsNodes.get(pos);
        int parentIndentLevel = node.getIndentLevel();
        int minChildIndentLevel = Integer.MAX_VALUE;

        WBSNode possibleChildNode;
        int nodeIndentLevel;

        IntList result = new IntList(wbsNodes.size() - pos);

        while (++pos < wbsNodes.size()) {
            possibleChildNode = (WBSNode) wbsNodes.get(pos);
            nodeIndentLevel = possibleChildNode.getIndentLevel();

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

    private int[] rows;


    public int getRowCount() { return rows.length; }
    public int getColumnCount() { return 1; }
    public String getColumnName(int column) { return "WBS Node"; }
    public Class getColumnClass(int columnIndex) { return WBSNode.class; }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return rowIndex != 0;
    }
    public Object getValueAt(int row, int column) {
        return getNodeForRow(row);
    }



    void recalcRows() { recalcRows(true); }
    void recalcRows(boolean notify) {
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
    public IntList getRowsForNodes(List nodes) {
        IntList result = new IntList(nodes.size());
        Iterator i = nodes.iterator();
        while (i.hasNext())
            result.add(wbsNodes.indexOf(i.next()));
        return result;
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
            wbsNodes.addAll(nodesToInsert);

        else {
            int beforePos = rows[beforeRow];
            wbsNodes.addAll(beforePos, nodesToInsert);
        }

        recalcRows(false);
        Iterator i = currentVisibleNodes.iterator();
        while (i.hasNext())
            makeVisible(wbsNodes.indexOf(i.next()));
        fireTableDataChanged();

        IntList result = new IntList(nodesToInsert.size());
        i = nodesToInsert.iterator();
        while (i.hasNext()) {
            int pos = getRowForNode((WBSNode) i.next());
            if (pos != -1) result.add(pos);
        }
        return result.getAsArray();
    }


    public static final String PROJECT_TYPE = "Project";
    public static final String SOFTWARE_COMPONENT_TYPE = "Software Component";
    public static final String REQTS_DOCUMENT = "Requirements Document";
    public static final String GENERAL_DOCUMENT = "General Document";
    public static final String HLD_DOCUMENT = "High Level Design Document";
    public static final String DLD_DOCUMENT = "Detailed Design Document";
    public static final String PSP_TASK = "PSP Task";
    public static final Set DOCUMENT_TYPES = new HashSet();
    static {
        DOCUMENT_TYPES.add(REQTS_DOCUMENT);
        DOCUMENT_TYPES.add(GENERAL_DOCUMENT);
        DOCUMENT_TYPES.add(HLD_DOCUMENT);
        DOCUMENT_TYPES.add(DLD_DOCUMENT);
    }

    public boolean isSoftwareComponent(String type) {
        return "Software Component".equalsIgnoreCase(type) ||
            "Project".equalsIgnoreCase(type) ||
            "PSP Task".equalsIgnoreCase(type);
    }

    public boolean isPSPTask(String type) {
        return "PSP Task".equalsIgnoreCase(type);
    }

    public boolean isDocument(String type) {
        return DOCUMENT_TYPES.contains(type);
    }


    public void addWBSNodeListener(WBSNodeListener l) {
        listenerList.add(WBSNodeListener.class, l);
    }

    public void removeWBSNodeListener(WBSNodeListener l) {
        listenerList.remove(WBSNodeListener.class, l);
    }

    public void fireNodeChanged(WBSNodeEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==WBSNodeListener.class) {
                ((WBSNodeListener)listeners[i+1]).nodeChanged(e);
            }
        }
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
        out.write("<wbsModel>");
        for (int i = 0;   i < wbsNodes.size();   i++)
            ((WBSNode) wbsNodes.get(i)).getAsXML(out);
        out.write("</wbsModel>");
    }
}
