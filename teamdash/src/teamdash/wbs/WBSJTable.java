
package teamdash.wbs;

import java.util.*;
import javax.swing.*;


import java.awt.Event;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.KeyboardFocusManager;
import java.awt.Component;
import java.util.ArrayList;


/** Displays the nodes of a WBSModel in a JTable, and installs custom
 * editing functionality.
 */
public class WBSJTable extends JTable { //implements ListSelectionListener {

    private static final boolean ENABLE_AUTO_EDITING = true;

    WBSModel wbsModel;
    WBSNodeRenderer renderer;
    WBSNodeEditor editor;
    Map iconMap;

    private Map customInputMap;
    private Map customActionMap;
    private JComponent scrollableDelegate = null;
    private List cutList = null;
    private List copyList = null;

    public WBSJTable(WBSModel model, Map iconMap) {
        super(model);
        wbsModel = model;

        setRowHeight(19);
        getColumnModel().getColumn(0).setPreferredWidth(200);
        buildInputActionMaps();

        renderer = new WBSNodeRenderer(model, iconMap);
        setDefaultRenderer(WBSNode.class, renderer);

        editor = new WBSNodeEditor(this, model, iconMap);
        setDefaultEditor(WBSNode.class, editor);

        installActions(this);
        installTableActions();
        System.out.println("I am still here!");

        //cutList = new ArrayList();
        //cutList.add(model.getNodeForRow(2));
    }

    public boolean isCutNode(Object node) {
        return (cutList != null && cutList.contains(node));
    }

    private class NodeChangeAction extends AbstractAction {
        public NodeChangeAction() { }
        public NodeChangeAction(String name) { super(name); }
        public void actionPerformed(ActionEvent e) {
            EventObject restartEvent = editor.getRestartEditingEvent();

            int editingColumn = WBSJTable.this.editingColumn;
            WBSNode editingNode = editor.getEditingNode();
            if (editingNode != null)
                editor.stopCellEditing();

            doAction(e);

            restartEditing(editingColumn, editingNode, restartEvent);
        }
        public void doAction(ActionEvent e) {}
        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {
            if (editingNode != null) {
                int editingRow = wbsModel.getRowForNode(editingNode);
                if (editingRow > 0)
                    // start editing the node
                    editCellAt(editingRow, editingColumn, restartEvent);
            }
        }
    }

    private class DemoteAction extends NodeChangeAction {
        public DemoteAction() { super("Demote"); }
        public void doAction(ActionEvent e) {
            System.out.println("Demote!");

            int[] newSelectedRows = wbsModel.indentNodes
                (getSelectedRows(), 1);
            if (newSelectedRows != null) {
                clearSelection();
                for (int i=newSelectedRows.length;   i-- > 0; )
                    addRowSelectionInterval(newSelectedRows[i],
                                            newSelectedRows[i]);
            }
        }
    }

    private class PromoteAction extends NodeChangeAction {
        public PromoteAction() { super("Promote"); }
        public void doAction(ActionEvent e) {
            System.out.println("Promote!");

            int[] newSelectedRows = wbsModel.indentNodes
                (getSelectedRows(), -1);
            if (newSelectedRows != null) {
                clearSelection();
                for (int i=0;   i < newSelectedRows.length;   i++)
                    addRowSelectionInterval(newSelectedRows[i],
                                            newSelectedRows[i]);
            }
        }
    }

    private class DelegateActionRestartEditing extends NodeChangeAction {
        Action realAction;

        public DelegateActionRestartEditing(Action realAction, Object key) {
            super();
            putValue(NAME, key); //realAction.getValue(NAME));
            this.realAction = realAction;
        }

        public void doAction(ActionEvent e) {
            System.out.println(getValue(NAME));
            ActionEvent event = new ActionEvent
                (WBSJTable.this, e.getID(), e.getActionCommand(),
                 e.getModifiers());
            realAction.actionPerformed(event);
        }

        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {
            WBSJTable.this.grabFocus();
            int rowToEdit = getSelectedRow();
            int columnToEdit = getSelectedColumn();
            editCellAt(rowToEdit, columnToEdit, restartEvent);
        }
    }

    private class RowChangeAction extends NodeChangeAction {
        Object originalActionKey;
        public RowChangeAction(String name, KeyStroke keyStroke) {
            super(name);
            originalActionKey = WBSJTable.this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(keyStroke);
            System.out.println("rca."+name+": action="+originalActionKey);
        }
        public void doAction(ActionEvent e) {
            System.out.println(getValue(NAME));
            Action a = WBSJTable.this.getActionMap().get(originalActionKey);
            if (a != null) {
                ActionEvent ae = new ActionEvent
                    (WBSJTable.this, e.getID(), e.getActionCommand(),
                     e.getModifiers());
                a.actionPerformed(ae);
            }
        }
        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {
            int rowToEdit = getSelectedRow();
            int columnToEdit = getSelectedColumn();
            editCellAt(rowToEdit, columnToEdit, restartEvent);
        }
    }

    static void installAction(JComponent component, KeyStroke keyStroke,
                              Object actionKey, Action action) {
        component.getInputMap().put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, action);
    }
    void installActions(JComponent component) {
        InputMap inputMap = component.getInputMap();
        ActionMap actionMap = component.getActionMap();

        Iterator i = customInputMap.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            KeyStroke keyStroke = (KeyStroke) e.getKey();
            Object actionKey = e.getValue();
            Action action = (Action) customActionMap.get(actionKey);
            installAction(component, keyStroke, actionKey, action);
        }
    }
    void installTableActions() {
        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        installAction
            (this, KeyStroke.getKeyStroke(KeyEvent.VK_X,Event.CTRL_MASK),
             "Cut", new CutAction());
        installAction
            (this, KeyStroke.getKeyStroke(KeyEvent.VK_C,Event.CTRL_MASK),
             "Copy", new CopyAction());
        installAction
            (this, KeyStroke.getKeyStroke(KeyEvent.VK_V,Event.CTRL_MASK),
             "Paste", new PasteAction());
    }


    private void addMapping(Object key, Object tie, Object action) {
        customInputMap.put(key, tie);
        customActionMap.put(tie, action);
    }
    private void buildInputActionMaps() {
        customInputMap = new HashMap();
        customActionMap = new HashMap();

        addMapping(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
                   "Demote", new DemoteAction());
        addMapping(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,Event.SHIFT_MASK),
                   "Promote", new PromoteAction());

        if (System.getProperty("java.version").startsWith("1.3")) return;

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
            addMapping(keys[i], actionKey + "-WBS",
                       new DelegateActionRestartEditing(action,actionKey));
        }
    }

    public void scrollRectToVisible(Rectangle aRect) {
        if (scrollableDelegate == null)
            super.scrollRectToVisible(aRect);
        else
            scrollableDelegate.scrollRectToVisible(aRect);
    }
    public void setScrollableDelegate(JComponent c) {
        scrollableDelegate = c;
    }

    private void maybeCancelCut() {
        if (cutList == null || cutList.size() == 0) return;

        int firstRow = wbsModel.getRowForNode((WBSNode)cutList.get(0));
        int lastRow =  wbsModel.getRowForNode
            ((WBSNode)cutList.get(cutList.size()-1));
        wbsModel.fireTableRowsUpdated(firstRow, lastRow);
        cutList = null;
    }

    private class CutAction extends AbstractAction {
        public CutAction() { super("Cut"); }
        public void actionPerformed(ActionEvent e) {
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            maybeCancelCut();

            copyList = null;
            cutList = wbsModel.getNodesForRows(rows, true);
            wbsModel.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
            if (isEditing())
                editor.updateEditorAppearance();
        }
    }

    private class CopyAction extends AbstractAction {
        public CopyAction() { super("Copy"); }
        public void actionPerformed(ActionEvent e) {
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            maybeCancelCut();

            List nodesToCopy = wbsModel.getNodesForRows(rows, true);
            List result = new ArrayList();
            Iterator i = nodesToCopy.iterator();
            while (i.hasNext())
                result.add(((WBSNode) i.next()).clone());

            copyList = result;
            wbsModel.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
            if (isEditing())
                editor.updateEditorAppearance();
        }
    }

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

    // implementation of javax.swing.event.ListSelectionListener interface

    /*
    public void valueChanged(ListSelectionEvent e) {
        System.out.println("value changed");

        if (e.getValueIsAdjusting()) return;
        System.out.println("value isn't adjusting");

        SwingUtilities.invokeLater(new maybeRestartEditing());
    }


    private class maybeRestartEditing implements Runnable {
        public void run() {

        Component focusOwner = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != WBSJTable.this) {
            System.out.println("focus owner is" + focusOwner);
            // try {Thread.currentThread().sleep(5000); } catch (Throwable t) {}
            return;
        }
        System.out.println("we're focus owner");

        if (getSelectedRowCount() != 1) return;
        System.out.println("select row count");

        int rowToEdit = getSelectedRow();
        int columnToEdit = getSelectedColumn();
        editCellAt(rowToEdit, columnToEdit, editor.getRestartEditingEvent());
        System.out.println("call edit cell at");

        }
    }
    */

}
