
package teamdash.wbs;

import java.awt.Event;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;


/** Displays the nodes of a WBSModel in a JTable, and installs custom
 * editing functionality.
 */
public class WBSJTable extends JTable {

    /** The wbs model to display */
    WBSModel wbsModel;
    /** Our custom cell renderer */
    WBSNodeRenderer renderer;
    /** Our custom cell editor */
    WBSNodeEditor editor;
    /** The map translating node types to corresponding icons */
    Map iconMap;

    /** a list of keystroke/action mappings to install */
    private Set customActions = new HashSet();
    /** a scrollable JComponent that we can delegate our scrolling events to */
    private JComponent scrollableDelegate = null;
    /** The list of nodes that has been cut */
    private List cutList = null;
    /** The list of nodes that has been copied */
    private List copyList = null;


    /** Create a JTable to display a WBS. Construct a default icon menu. */
    public WBSJTable(WBSModel model, Map iconMap) {
        this(model, iconMap, null);
    }
    /** Create a JTable to display a WBS */
    public WBSJTable(WBSModel model, Map iconMap, JMenu iconMenu) {
        super(model);
        wbsModel = model;

        setRowHeight(19);
        buildCustomActionMaps();

        renderer = new WBSNodeRenderer(model, iconMap);
        setDefaultRenderer(WBSNode.class, renderer);

        editor = new WBSNodeEditor(this, model, iconMap, iconMenu);
        setDefaultEditor(WBSNode.class, editor);
        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        installCustomActions(this);
        installTableActions();
    }


    /** Return true if the given object is in the list of currently
     * cut nodes */
    public boolean isCutNode(Object node) {
        return (cutList != null && cutList.contains(node));
    }


    /** Cancel the current cut operation, if there is one. */
    public void cancelCut() {
        if (cutList == null || cutList.size() == 0) return;

        int firstRow = wbsModel.getRowForNode((WBSNode)cutList.get(0));
        int lastRow =  wbsModel.getRowForNode
            ((WBSNode)cutList.get(cutList.size()-1));
        wbsModel.fireTableRowsUpdated(firstRow, lastRow);
        cutList = null;
    }


    /** Override method to forward "scroll" events to our delegate, if one
     * is set. */
    public void scrollRectToVisible(Rectangle aRect) {
        if (scrollableDelegate == null)
            super.scrollRectToVisible(aRect);
        else
            scrollableDelegate.scrollRectToVisible(aRect);
    }

    /** Set the component which should handle scroll events on our behalf. */
    public void setScrollableDelegate(JComponent c) {
        scrollableDelegate = c;
    }



    /** Change the current selection so it contains the given list of rows */
    public void selectRows(int[] rowsToSelect) {
        if (rowsToSelect != null) {
            clearSelection();
            for (int i=rowsToSelect.length;   i-- > 0; )
                addRowSelectionInterval(rowsToSelect[i],
                                        rowsToSelect[i]);
        }
    }


    /** Populate the <tt>customActions</tt> list with actions for editing
     * the work breakdown structure. */
    private void buildCustomActionMaps() {

        // Map "Tab" and "Shift-Tab" to the demote/promote actions
        new ActionMapping(KeyEvent.VK_TAB, 0, "Demote", DEMOTE_ACTION);
        new ActionMapping(KeyEvent.VK_TAB, SHIFT, "Promote", PROMOTE_ACTION);
        new ActionMapping(KeyEvent.VK_INSERT, 0, "Insert", INSERT_ACTION);
        new ActionMapping(KeyEvent.VK_ENTER, 0, "Enter", ENTER_ACTION);

        // Java 1.3 doesn't handle the "auto restart editing" actions very
        // well, so if we're in a 1.3 JRE stop here.
        if (System.getProperty("java.version").startsWith("1.3")) return;

        // Find the default row navigation actions for the table, and replace
        // them with new actions which (1) perform the original action, then
        // (2) restart editing on the newly selected cell.
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        List filter = Arrays.asList(new String[]
            { "selectPreviousRow", "selectNextRow",
                "selectFirstRow", "selectLastRow",
                "scrollUpChangeSelection", "scrollDownChangeSelection" });

        KeyStroke[] keys = inputMap.allKeys();
        for (int i = 0;   i < keys.length;   i++) {
            Object actionKey = inputMap.get(keys[i]);
            if (! filter.contains(actionKey)) continue;
            Action action = actionMap.get(actionKey);
            if (action == null) continue;
            new ActionMapping
                (keys[i], actionKey + "-WBS",
                 new DelegateActionRestartEditing(action,actionKey));
        }
    }


    /** Install our list of custom actions into the input and action maps for a
     * given component */
    void installCustomActions(JComponent component) {
        Iterator i = customActions.iterator();
        while (i.hasNext())
            ((ActionMapping) i.next()).install(component);
    }


    /** Install actions that are appropriate only for the table (not for any
     * other component). */
    void installTableActions() {
        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, CTRL), "Cut");
        actionMap.put("Cut", CUT_ACTION);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, CTRL), "Copy");
        actionMap.put("Copy", COPY_ACTION);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, CTRL), "Paste");
        actionMap.put("Paste", PASTE_ACTION);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
        actionMap.put("Delete", DELETE_ACTION);
    }





    /** Class for managing a custom keystroke/action mapping */
    private class ActionMapping {
        KeyStroke keyStroke;
        Object actionKey;
        Action action;

        /** Create a new action mapping */
        public ActionMapping(int keyCode, int modifiers,
                             Object actionKey, Action action) {
            this(KeyStroke.getKeyStroke(keyCode, modifiers),
                 actionKey, action);
        }

        /** Create a new action mapping */
        public ActionMapping(KeyStroke key, Object actionKey, Action action) {
            this.keyStroke = key;
            this.actionKey = actionKey;
            this.action = action;
            customActions.add(this);
        }

        /** Install this action mapping into a component */
        public void install(JComponent component) {
            component.getInputMap().put(keyStroke, actionKey);
            component.getActionMap().put(actionKey, action);
        }

        /** override hashcode and equals to defer to our KeyStroke field */
        public int hashCode() { return this.keyStroke.hashCode(); }
        public boolean equals(Object obj) {
            return (obj instanceof ActionMapping &&
                    ((ActionMapping) obj).keyStroke.equals(this.keyStroke));
        }
    }


    /////////////////////////////////////////////////////////////
    //
    // Internal classes for various custom Actions
    //
    /////////////////////////////////////////////////////////////


    /** Abstract class for an action that (1) stops editing, (2) does
     * something, then (3) restarts editing */
    private class RestartEditingAction extends AbstractAction {
        /** Policy for editing restart.  If you want to resume editing
         * the node that was being edited when our action was triggered,
         * set this to false.  If you want to begin editing whatever cell is
         * highlighted after our action completes, set this to true. */
        protected boolean editingFollowsSelection = false;

        protected int editingRow, editingColumn;

        public RestartEditingAction() { }
        public RestartEditingAction(String name) { super(name); }
        public void actionPerformed(ActionEvent e) {
            // Get a "restart editing" event from the cell editor.
            EventObject restartEvent = editor.getRestartEditingEvent();

            // ask what column we're editing. (for our purposes, it's always
            // going to be column 0, but lets be more robust.)
            editingColumn = WBSJTable.this.editingColumn;
            editingRow    = WBSJTable.this.editingRow;

            // find out what node is currently being edited.
            WBSNode editingNode = (WBSNode) editor.getCellEditorValue();

            // stop the current editing session.
            if (editingNode != null) editor.stopCellEditing();

            // do something.
            doAction(e);

            // restart editing.
            restartEditing(editingColumn, editingNode, restartEvent);
        }
        public void doAction(ActionEvent e) {}

        /** Start up an editing session. */
        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {

            if (editingFollowsSelection) {
                // Begin editing the newly selected node.
                WBSJTable.this.grabFocus();
                int rowToEdit = getSelectedRow();
                int columnToEdit = getSelectedColumn();
                editCellAt(rowToEdit, columnToEdit, restartEvent);

            } else if (editingNode != null) {
                // Resume editing the cell we used to be editing.
                int editingRow = wbsModel.getRowForNode(editingNode);
                if (editingRow > 0)
                    // start editing the node
                    editCellAt(editingRow, editingColumn, restartEvent);
            }
        }

        /** Convenience method to get the row selection. some actions we
         * take may clear the selection;  to solve this, we generate a list
         * containing the editing row. */
        protected int[] getSelectedRows() {
            int[] result = WBSJTable.this.getSelectedRows();

            if ((result == null || result.length == 0) && editingRow != -1) {
                result = new int[1];
                result[0] = editingRow;
            }

            return result;
        }
    }



    /** An action to demote the selected rows. */
    private class DemoteAction extends RestartEditingAction {
        public DemoteAction() { super("Demote"); }
        public void doAction(ActionEvent e) {
            System.out.println("Demote");
            selectRows(wbsModel.indentNodes(getSelectedRows(), 1));
        }
    }
    final DemoteAction DEMOTE_ACTION = new DemoteAction();


    /** An action to promote the selected rows. */
    private class PromoteAction extends RestartEditingAction {
        public PromoteAction() { super("Promote"); }
        public void doAction(ActionEvent e) {
            System.out.println("Promote");
            selectRows(wbsModel.indentNodes(getSelectedRows(), -1));
        }
    }
    final PromoteAction PROMOTE_ACTION = new PromoteAction();


    /** An action which (1) stops the current editing session, (2) triggers
     * some other action, then (3) restarts editing. */
    private class DelegateActionRestartEditing extends RestartEditingAction {
        Action realAction;

        public DelegateActionRestartEditing(Action realAction, Object key) {
            super();
            putValue(NAME, key);
            this.realAction = realAction;
            this.editingFollowsSelection = true;
        }

        public void doAction(ActionEvent e) {
            System.out.println(getValue(NAME));
            ActionEvent event = new ActionEvent
                (WBSJTable.this, e.getID(), e.getActionCommand(),
                 e.getModifiers());
            realAction.actionPerformed(event);
        }
    }


    /** An action to perform a "cut" operation */
    private class CutAction extends AbstractAction {
        public CutAction() { super("Cut"); }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // record the cut nodes.
            copyList = null;
            cutList = wbsModel.getNodesForRows(rows, true);

            // update the appearance of newly cut cells (they will be
            // displaying phantom icons).
            wbsModel.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
            if (isEditing()) editor.updateIconAppearance();
        }
    }
    final CutAction CUT_ACTION = new CutAction();


    /** An action to perform a "copy" operation */
    private class CopyAction extends AbstractAction {
        public CopyAction() { super("Copy"); }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // make a list of the copied nodes
            copyList = WBSNode.cloneNodeList
                (wbsModel.getNodesForRows(rows, true));
        }
    }
    final CopyAction COPY_ACTION = new CopyAction();


    /** An action to perform a "paste" operation */
    private class PasteAction extends AbstractAction {
        public PasteAction() { super("Paste"); }
        public void actionPerformed(ActionEvent e) {
            WBSNode beforeNode = getLocation();
            if (beforeNode == null) return;
            finalizeCut();
            insertCopiedNodes(beforeNode);
        }

        WBSNode getLocation() {
            int rowNum = getSelectedRow();
            if (rowNum == -1) return null;
            WBSNode result = wbsModel.getNodeForRow(rowNum);
            if (cutList != null && cutList.contains(result))
                return null;
            return result;
        }

        void finalizeCut() {
            if (cutList == null || cutList.size() == 0) return;

            wbsModel.deleteNodes(cutList, false);
            copyList = cutList;
            cutList = null;
        }

        void insertCopiedNodes(WBSNode beforeNode) {
            if (copyList == null || copyList.size() == 0) return;
            int pos = wbsModel.getRowForNode(beforeNode);
            List nodesToInsert = copyList;
            copyList = cutList = null;
            int[] rowsInserted = wbsModel.insertNodes(nodesToInsert, pos);
            if (rowsInserted != null) {
                clearSelection();
                for (int i=rowsInserted.length;   i-- > 0; )
                    addRowSelectionInterval(rowsInserted[i], rowsInserted[i]);
            }
        }
    }
    final PasteAction PASTE_ACTION = new PasteAction();


    /** An action to perform an "insert node before" operation */
    private class InsertAction extends RestartEditingAction {
        // we inherit from DelegateActionRestartEditing because we want
        // the editing to resume with the cell we designate, not the cell
        // that was last edited.  We don't use the delegation mechanism
        public InsertAction() { this("Insert"); }
        public InsertAction(String name) {
            super(name);
            editingFollowsSelection = true;
        }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            insertRowBefore(currentRow, currentRow);
        }
        protected void insertRowBefore(int row, int rowToCopy) {
            if (row == -1) return;
            if (row == 0) row = 1;
            if (rowToCopy == 0) rowToCopy = 1;

            WBSNode nodeToCopy = wbsModel.getNodeForRow(rowToCopy);
            if (nodeToCopy == null) return;
            if (cutList != null && cutList.contains(nodeToCopy)) cancelCut();
            WBSNode newNode = new WBSNode
                (wbsModel, "", nodeToCopy.getType(),
                 nodeToCopy.getIndentLevel(), nodeToCopy.isExpanded());

            row = wbsModel.add(row, newNode);
            setRowSelectionInterval(row, row);
            scrollRectToVisible(getCellRect(row, 0, true));
        }
    }
    final InsertAction INSERT_ACTION = new InsertAction();


    /** An action to perform an "insert node after" operation */
    private class InsertAfterAction extends InsertAction {
        public InsertAfterAction() { super("Insert After"); }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            insertRowBefore(currentRow+1, currentRow);
        }
    }
    final InsertAfterAction ENTER_ACTION = new InsertAfterAction();

    /** An action to perform a "delete" operation */
    private class DeleteAction extends AbstractAction {
        public DeleteAction() { super("Delete"); }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // verify that the user wants to cut the nodes.
            List nodesToDelete = wbsModel.getNodesForRows(rows, true);
            if (nodesToDelete == null || nodesToDelete.size() == 0) return;
            int size = nodesToDelete.size();
            String message = "Delete "+size+(size==1 ? " item":" items")+
                " from the work breakdown structure?";
            if (JOptionPane.showConfirmDialog
                (WBSJTable.this, message, "Confirm Deletion",
                 JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                 return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // delete the nodes.
            wbsModel.deleteNodes(nodesToDelete);

            int rowToSelect = Math.min(rows[0], wbsModel.getRowCount()-1);
            setRowSelectionInterval(rowToSelect, rowToSelect);
            scrollRectToVisible(getCellRect(rowToSelect, 0, true));
        }
    }
    final DeleteAction DELETE_ACTION = new DeleteAction();


    // convenience declarations
    private static final int SHIFT = Event.SHIFT_MASK;
    private static final int CTRL  = Event.CTRL_MASK;

}
