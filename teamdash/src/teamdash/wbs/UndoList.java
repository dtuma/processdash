package teamdash.wbs;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.JComponent;


public class UndoList {

    /** How many levels of undo should be maintained? */
    public static final int MAX_LEVELS = 20;

    private SnapshotSource snapshotSource;
    private Stack undoList;
    private Stack redoList;
    private Object currentState;
    private HashSet cellEditors;

    private Action undoAction, redoAction;
    private boolean currentlyCancelingEditors = false;

    public UndoList(SnapshotSource snapshotSource) {
        this.snapshotSource = snapshotSource;
        this.undoList = new Stack();
        this.redoList = null;
        this.cellEditors = new HashSet();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        this.currentState = snapshotSource.getSnapshot();
    }

    public Action getUndoAction() { return undoAction; }
    public Action getRedoAction() { return redoAction; }

    public boolean isUndoAvailable() {
        return (undoList != null && undoList.size() > 0);
    }

    public boolean isRedoAvailable() {
        return (redoList != null && redoList.size() > 0);
    }

    private void refreshActions() {
        undoAction.setEnabled(isUndoAvailable());
        redoAction.setEnabled(isRedoAvailable());
    }

    private synchronized void cancelEditors() {
        currentlyCancelingEditors = true;
        Iterator i = cellEditors.iterator();
        while (i.hasNext())
            ((CellEditor) i.next()).cancelCellEditing();
        cellEditors.clear();
        currentlyCancelingEditors = false;
    }

    public synchronized void madeChange(String description) {
        if (currentlyCancelingEditors) return;

        System.out.println("UndoList.madeChange("+description+")");
        undoList.push(currentState);
        if (undoList.size() > MAX_LEVELS)
            undoList.remove(0);
        currentState = snapshotSource.getSnapshot();
        redoList = null;
        refreshActions();
    }

    public synchronized void undo() {
        if (isUndoAvailable()) {
            cancelEditors();
            if (redoList == null) redoList = new Stack();
            redoList.push(currentState);
            currentState = undoList.pop();
            snapshotSource.restoreSnapshot(currentState);
            refreshActions();
        }
    }

    public synchronized void redo() {
        if (isRedoAvailable()) {
            cancelEditors();
            undoList.push(currentState);
            currentState = redoList.pop();
            snapshotSource.restoreSnapshot(currentState);
            refreshActions();
        }
    }

    private static final String CLIENT_PROP_NAME = "teamdash.wbs.undoList";

    public void setForComponent(JComponent component) {
        component.putClientProperty(CLIENT_PROP_NAME, this);
    }

    private static UndoList findUndoList(Component source) {
        while (source != null) {
            if (source instanceof JComponent) {
                Object o = ((JComponent) source).getClientProperty
                    (CLIENT_PROP_NAME);
                if (o instanceof UndoList)
                    return (UndoList) o;
            }

            source = source.getParent();
        }
        return null;
    }

    public static void addCellEditor(Component source, CellEditor editor) {
        UndoList l = findUndoList(source);
        if (l != null) l.cellEditors.add(editor);
    }

    public static void madeChange(Component source, String description) {
        UndoList l = findUndoList(source);
        if (l != null) l.madeChange(description);
    }

    private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", IconFactory.getUndoIcon());
            setEnabled(isUndoAvailable());
        }
        public void actionPerformed(ActionEvent e) {
            undo();
        }
    }

    private class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo", IconFactory.getRedoIcon());
            setEnabled(isRedoAvailable());
        }
        public void actionPerformed(ActionEvent e) {
            redo();
        }
    }
}
