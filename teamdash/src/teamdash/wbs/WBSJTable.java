
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


/** Displays the nodes of a WBSModel in a JTable, and installs custom
 * editing functionality.
 */
public class WBSJTable extends JTable {

    WBSModel wbsModel;
    WBSNodeRenderer renderer;
    WBSNodeEditor editor;
    Map iconMap;

    private Map customInputMap;
    private Map customActionMap;
    private JComponent scrollableDelegate = null;

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
                for (int i=0;   i < newSelectedRows.length;   i++)
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

        public DelegateActionRestartEditing(Action realAction) {
            super();
            putValue(NAME, realAction.getValue(NAME));
            this.realAction = realAction;
        }

        public void doAction(ActionEvent e) {
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
                       new DelegateActionRestartEditing(action));
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
}
