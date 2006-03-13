
package teamdash.wbs;

import java.awt.Cursor;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
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
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


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

    /** Should editing of WBS nodes be disabled? */
    private boolean disableEditing = false;

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
        getSelectionModel().addListSelectionListener(SELECTION_LISTENER);
        addMouseMotionListener(MOTION_LISTENER);
    }


    public void setEditingEnabled(boolean enable) {
        disableEditing = !enable;
        editor.setEditingEnabled(enable);
        recalculateEnablement();
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
            if (rowsToSelect.length == 0)
                clearSelection();
            else {
                getSelectionModel().setValueIsAdjusting(true);
                clearSelection();
                for (int i=rowsToSelect.length;   i-- > 0; )
                    addRowSelectionInterval(rowsToSelect[i],
                                            rowsToSelect[i]);
                getSelectionModel().setValueIsAdjusting(false);
            }
        }
    }


    /** Return a list of actions useful for editing the wbs */
    public Action[] getEditingActions() {
        return new Action[] { CUT_ACTION, COPY_ACTION, PASTE_ACTION,
            DEMOTE_ACTION, PROMOTE_ACTION, INSERT_ACTION, ENTER_ACTION,
            DELETE_ACTION };
    }

    /** Return an action capable of inserting a workflow */
    public Action getInsertWorkflowAction(WBSModel workflows) {
        if (INSERT_WORKFLOW_ACTION == null)
            INSERT_WORKFLOW_ACTION = new InsertWorkflowAction(workflows);
        return INSERT_WORKFLOW_ACTION;
    }

    public Action[] getMasterActions(File dir) {
        return new Action[] { new MergeMasterWBSAction(dir) };
    }



    /** */
    public void setSelectionModel(ListSelectionModel newModel) {
        ListSelectionModel oldModel = getSelectionModel();
        if (oldModel != null)
            oldModel.removeListSelectionListener(SELECTION_LISTENER);
        newModel.addListSelectionListener(SELECTION_LISTENER);
        super.setSelectionModel(newModel);
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


    /** Look at each of our actions to determine if it should be enabled. */
    private void recalculateEnablement() {
        int[] selectedRows = getSelectedRows();
        CUT_ACTION.recalculateEnablement(selectedRows);
        COPY_ACTION.recalculateEnablement(selectedRows);
        PASTE_ACTION.recalculateEnablement(selectedRows);
        DELETE_ACTION.recalculateEnablement(selectedRows);
        PROMOTE_ACTION.recalculateEnablement(selectedRows);
        DEMOTE_ACTION.recalculateEnablement(selectedRows);
        INSERT_ACTION.recalculateEnablement(selectedRows);
        ENTER_ACTION.recalculateEnablement(selectedRows);
        if (INSERT_WORKFLOW_ACTION != null)
            INSERT_WORKFLOW_ACTION.recalculateEnablement(selectedRows);
    }


    /** Return true if the list of rows contains at least one row. */
    private boolean notEmpty(int[] selectedRows) {
        return (selectedRows != null && selectedRows.length > 0);
    }

    /** Return true if the list of rows contains at least one row other than
     * row 0. */
    private boolean notJustRoot(int[] selectedRows) {
        if (selectedRows == null || selectedRows.length == 0) return false;
        if (selectedRows.length == 1 && selectedRows[0] == 0) return false;
        return true;
    }

    private boolean containsReadOnlyNode(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            WBSNode node = wbsModel.getNodeForRow(rows[i]);
            if (node == null)
                continue;
            if (node.isReadOnly())
                return true;
            if (node.isExpanded() == false) {
                WBSNode[] descendants = wbsModel.getDescendants(node);
                if (containsReadOnlyNode(Arrays.asList(descendants)))
                    return true;
            }
        }
        return false;
    }

    private boolean containsReadOnlyNode(List nodes) {
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            if (node.isReadOnly())
                return true;
        }
        return false;
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
        protected EventObject restartEvent;
        protected WBSNode editingNode;

        public RestartEditingAction() { }
        public RestartEditingAction(String name) { super(name); }
        public RestartEditingAction(String name, Icon icon) {
            super(name, icon);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // Get a "restart editing" event from the cell editor.
            restartEvent = editor.getRestartEditingEvent();

            // ask what column we're editing. (for our purposes, it's always
            // going to be column 0, but lets be more robust.)
            editingColumn = WBSJTable.this.editingColumn;
            editingRow    = WBSJTable.this.editingRow;

            // find out what node is currently being edited.
            editingNode = (WBSNode) editor.getCellEditorValue();

            // stop the current editing session.
            if (editingNode != null) editor.stopCellEditing();
            UndoList.stopCellEditing(WBSJTable.this);

            // do something.
            doAction(e);

            // restart editing.
            restartEditing(editingColumn, editingNode, restartEvent);
        }

        public void doAction(ActionEvent e) {}

        /** Start up an editing session. */
        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {
            if (disableEditing)
                return;

            if (editingFollowsSelection) {
                // Begin editing the newly selected node.
                WBSJTable.this.grabFocus();
                int rowToEdit = getSelectedRow();
                int columnToEdit = editingColumn; //getSelectedColumn();
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
        public DemoteAction() {
            super("Demote", IconFactory.getDemoteIcon());
        }
        public void doAction(ActionEvent e) {
            System.out.println("Demote");
            if (containsReadOnlyNode(getSelectedRows()))
                return;
            selectRows(wbsModel.indentNodes(getSelectedRows(), 1));
            UndoList.madeChange(WBSJTable.this, "Demote");
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && notJustRoot(selectedRows)
                    && !containsReadOnlyNode(selectedRows));
        }
    }
    final DemoteAction DEMOTE_ACTION = new DemoteAction();


    /** An action to promote the selected rows. */
    private class PromoteAction extends RestartEditingAction {
        public PromoteAction() {
            super("Promote", IconFactory.getPromoteIcon());
        }
        public void doAction(ActionEvent e) {
            System.out.println("Promote");
            if (containsReadOnlyNode(getSelectedRows()))
                return;
            selectRows(wbsModel.indentNodes(getSelectedRows(), -1));
            UndoList.madeChange(WBSJTable.this, "Promote");
        }
        public void recalculateEnablement(int[] selectedRows) {
            if (disableEditing
                    || notJustRoot(selectedRows) == false
                    || containsReadOnlyNode(selectedRows))
                setEnabled(false);
            else {
                for (int i = selectedRows.length;   i-- > 0; ) {
                    WBSNode n = wbsModel.getNodeForRow(selectedRows[i]);
                    if (n != null && n.getIndentLevel() < 2) {
                        setEnabled(false);
                        return;
                    }
                }
                setEnabled(true);
            }
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

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                doAction(e);
            else
                super.actionPerformed(e);
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
    private class CutAction extends AbstractAction implements ClipboardOwner {
        public CutAction() {
            super("Cut", IconFactory.getCutIcon());
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // record the cut nodes.
            cutList = wbsModel.getNodesForRows(rows, true);
            WBSClipSelection.putNodeListOnClipboard(cutList, this);

            // update the appearance of newly cut cells (they will be
            // displaying phantom icons).
            wbsModel.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
            if (isEditing()) editor.updateIconAppearance();
            WBSJTable.this.recalculateEnablement();
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing && notJustRoot(selectedRows));
        }
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            cancelCut();
        }
    }
    final CutAction CUT_ACTION = new CutAction();


    /** An action to perform a "copy" operation */
    private class CopyAction extends AbstractAction {
        public CopyAction() {
            super("Copy", IconFactory.getCopyIcon());
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // make a list of the copied nodes
            List copyList = WBSNode.cloneNodeList
                (wbsModel.getNodesForRows(rows, true));
            WBSClipSelection.putNodeListOnClipboard(copyList, null);

            WBSJTable.this.recalculateEnablement();
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(notJustRoot(selectedRows));
        }
    }
    final CopyAction COPY_ACTION = new CopyAction();


    /** An action to perform a "paste" operation */
    private class PasteAction extends AbstractAction {
        public PasteAction() {
            super("Paste", IconFactory.getPasteIcon());
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            WBSNode beforeNode = getLocation();
            if (beforeNode == null) return;
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

        void insertCopiedNodes(WBSNode beforeNode) {
            List nodesToInsert = WBSClipSelection
                    .getNodeListFromClipboard(beforeNode);
            if (nodesToInsert == null || nodesToInsert.size() == 0) return;

            if (nodesToInsert == cutList)
                wbsModel.deleteNodes(cutList, false);
            cutList = null;

            int pos = wbsModel.getRowForNode(beforeNode);
            int[] rowsInserted = wbsModel.insertNodes(nodesToInsert, pos);
            selectRows(rowsInserted);

            UndoList.madeChange(WBSJTable.this, "Paste WBS elements");
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing &&
                       notEmpty(selectedRows) &&
                       getLocation() != null);
        }
    }
    final PasteAction PASTE_ACTION = new PasteAction();


    /** An action to perform an "insert node before" operation */
    private class InsertAction extends RestartEditingAction {
        public InsertAction() { this("Insert"); }
        public InsertAction(String name) {
            super(name);
            editingFollowsSelection = true;
        }
        public void actionPerformed(ActionEvent e) {
            int currentCol = getColumnModel().getSelectionModel()
                    .getAnchorSelectionIndex();
            if (currentCol == -1)
                currentCol = getEditingColumn();
            if (currentCol != -1 && getColumnClass(currentCol) == WBSNode.class)
                super.actionPerformed(e);
            else
                performAlternateAction(e);
        }
        protected void performAlternateAction(ActionEvent e) {
        }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            insertRowBefore(currentRow, currentRow);
        }
        protected void insertRowBefore(int row, int rowToCopy) {
            if (row == -1) return;
            if (row == 0) row = 1;
            if (rowToCopy == 0) rowToCopy = 1;

            String type = "Software Component";
            int indentLevel = 1;
            boolean expanded = false;

            WBSNode nodeToCopy = wbsModel.getNodeForRow(rowToCopy);
            if (nodeToCopy != null) {
                type = nodeToCopy.getType();
                indentLevel = nodeToCopy.getIndentLevel();
                expanded = nodeToCopy.isExpanded();
            }
            if (cutList != null && cutList.contains(nodeToCopy)) cancelCut();
            WBSNode newNode = new WBSNode(wbsModel, "", type, indentLevel,
                    expanded);

            row = wbsModel.add(row, newNode);
            setRowSelectionInterval(row, row);
            scrollRectToVisible(getCellRect(row, 0, true));

            UndoList.madeChange(WBSJTable.this, "Insert WBS element");
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing && notEmpty(selectedRows));
        }
    }
    final InsertAction INSERT_ACTION = new InsertAction();


    /** An action to perform an "insert node after" operation */
    private class InsertAfterAction extends InsertAction {
        private Action alternateAction;
        public InsertAfterAction() {
            super("Insert After");

            try {
                Object actionKey = getInputMap(
                        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
                alternateAction = getActionMap().get(actionKey);
            } catch (Exception e) {
            }
        }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            insertRowBefore(currentRow+1, currentRow);
        }
        protected void performAlternateAction(ActionEvent e) {
            if (alternateAction != null)
                alternateAction.actionPerformed(e);
        }
    }
    final InsertAfterAction ENTER_ACTION = new InsertAfterAction();


    /** An action to perform a "delete" operation */
    private class DeleteAction extends AbstractAction {
        public DeleteAction() {
            super("Delete", IconFactory.getDeleteIcon());
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // verify that the user wants to cut the nodes.
            List nodesToDelete = wbsModel.getNodesForRows(rows, true);
            if (nodesToDelete == null || nodesToDelete.size() == 0
                    || containsReadOnlyNode(nodesToDelete)) return;
            int size = nodesToDelete.size();
            String message = "Delete "+size+(size==1 ? " item":" items")+
                " from the "+selfName+"?";
            if (JOptionPane.showConfirmDialog
                (WBSJTable.this, message, "Confirm Deletion",
                 JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                 return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            if (isEditing()) editor.stopCellEditing();
            UndoList.stopCellEditing(WBSJTable.this);

            // delete the nodes.
            wbsModel.deleteNodes(nodesToDelete);

            int rowToSelect = Math.min(rows[0], wbsModel.getRowCount()-1);
            setRowSelectionInterval(rowToSelect, rowToSelect);
            scrollRectToVisible(getCellRect(rowToSelect, 0, true));

            UndoList.madeChange(WBSJTable.this, "Delete WBS elements");
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && notJustRoot(selectedRows)
                    && !containsReadOnlyNode(selectedRows));
        }
    }
    final DeleteAction DELETE_ACTION = new DeleteAction();
    /** This string is used by the delete action to compose a message
     * which will be displayed to the user. */
    String selfName = "work breakdown structure";


    /** An action to insert information from a workflow */
    private class InsertWorkflowAction extends AbstractAction {
        private WBSModel workflows;

        public InsertWorkflowAction(WBSModel workflows) {
            this.workflows = workflows;
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            UndoList.stopCellEditing(WBSJTable.this);

            // get the name of the workflow to insert.
            String workflowName = e.getActionCommand();
            if (workflowName == null || workflowName.length() == 0) return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;
            int destRow = rows[0];

            // make the change
            int[] newRowsToSelect =
                wbsModel.insertWorkflow(destRow, workflowName, workflows);
            if (newRowsToSelect != null) {
                editor.stopCellEditing();
                selectRows(newRowsToSelect);
                UndoList.madeChange(WBSJTable.this, "Insert workflow");
            }
        }

        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing && notEmpty(selectedRows));
        }
    }
    private InsertWorkflowAction INSERT_WORKFLOW_ACTION = null;


    private class MergeMasterWBSAction extends AbstractAction {

        private File dir;

        public MergeMasterWBSAction(File dir) {
            super("Copy Core Work Items from Master Project");
            this.dir = dir;
        }

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            UndoList.stopCellEditing(WBSJTable.this);

            TeamProject masterProject = new TeamProject(dir, "");
            String masterProjectID = masterProject.getProjectID();
            if (masterProjectID == null)
                return;

            // make the change
            int[] newRowsToSelect = MasterWBSUtil.mergeFromMaster(
                    masterProject.getWBS(), masterProjectID, wbsModel);
            if (newRowsToSelect != null) {
                editor.stopCellEditing();
                selectRows(newRowsToSelect);
                UndoList.madeChange(WBSJTable.this, "Copy Work from Master");
            }
        }
    }


    /** Recalculate enablement following changes in selection */
    private final class SelectionListener implements ListSelectionListener {
        public SelectionListener() {
            recalculateEnablement();
        }
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting())
                recalculateEnablement();
        }
    }
    final SelectionListener SELECTION_LISTENER = new SelectionListener();



    /** Display a hand cursor when the mouse is over a node icon */
    private final class MotionListener implements MouseMotionListener {
        private Cursor handCursor =
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private Cursor textCursor =
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        public void mouseDragged(MouseEvent e) {}
        public void mouseMoved(MouseEvent e) {
            setCursor(getCursorToDisplay(e.getPoint()));
        }
        private Cursor getCursorToDisplay(Point p) {
            if (disableEditing)
                return null;

            int column = columnAtPoint(p);
            if (column != 0) return null;
            int row = rowAtPoint(p);
            if (!wbsModel.isCellEditable(row, column)) return null;

            // find the node that the cursor is over.
            WBSNode overNode = wbsModel.getNodeForRow(row);
            if (overNode == null) return null;

            // calculate x relative to the table cell origin.
            Rectangle r = getCellRect(row, column, true);
            int ourXPos = p.x - r.x;

            // translate the x position according to the indentation level of
            // clicked node, and determine which part of the node was clicked.
            int indentLevel = overNode.getIndentLevel();
            int xDelta = ourXPos - indentLevel * WBSNodeRenderer.ICON_HORIZ_SPACING;
            if (xDelta <= 0)
                return null;
            else if (xDelta < WBSNodeRenderer.ICON_HORIZ_SPACING)
                return (wbsModel.isNodeTypeEditable(overNode)
                        ? handCursor : null);
            else if (xDelta > WBSNodeRenderer.ICON_HORIZ_SPACING + 4)
                return textCursor;
            else
                return null;
        }
    }
    final MotionListener MOTION_LISTENER = new MotionListener();



    // convenience declarations
    private static final int SHIFT = Event.SHIFT_MASK;
    private static final int CTRL  = Event.CTRL_MASK;

}
