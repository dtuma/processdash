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


/** Observes changes made to a {@link SnapshotSource}, and supports undo/redo
 * operations on that object.
 */
public class UndoList {

    /** How many levels of undo should be maintained? */
    public static final int MAX_LEVELS = 20;

    /** Support the ability to undo/redo changes made to this object */
    private SnapshotSource snapshotSource;

    /** A list of historical snapshots of the object, for undo
     * purposes. The item on the top of the stack represents the state
     * of the {@link SnapshotSource} before the most recent change.  */
    private Stack undoList;

    /** A list of historical snapshots of the object, for redo
     * purposes. The item on the top of the stack represents the state of
     * the {@link SnapshotSource} before the most recent undo operation.  */
    private Stack redoList;

    /** The current state of the object. */
    private Object currentState;

    /** A collection of table cell editors that edit the
     * {@link SnapshotSource} */
    private HashSet cellEditors;

    /** An action that can be used to trigger an undo operation */
    private Action undoAction;

    /** An action that can be used to trigger a redo operation */
    private Action redoAction;

    /** Flag indicating whether we are in the middle of canceling
     * table cell editing sessions. */
    private boolean currentlyCancelingEditors = false;


    /** Create a new UndoList for the given {@link SnapshotSource} */
    public UndoList(SnapshotSource snapshotSource) {
        this.snapshotSource = snapshotSource;
        this.undoList = new Stack();
        this.redoList = null;
        this.cellEditors = new HashSet();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        this.currentState = snapshotSource.getSnapshot();
    }

    /** Retrieve an action that triggers an undo operation */
    public Action getUndoAction() { return undoAction; }

    /** Retrieve an action that triggers a redo operation */
    public Action getRedoAction() { return redoAction; }

    /** Return true if the undo stack currently contains any entries */
    public boolean isUndoAvailable() {
        return (undoList != null && undoList.size() > 0);
    }

    /** Return true if the redo stack currently contains any entries */
    public boolean isRedoAvailable() {
        return (redoList != null && redoList.size() > 0);
    }

    /** Enable/disable the undo/redo actions to indicate whether they are
     * currently available */
    private void refreshActions() {
        undoAction.setEnabled(isUndoAvailable());
        redoAction.setEnabled(isRedoAvailable());
    }

    /** Cancel any editing sessions in progress for the
     * {@link SnapshotSource} */
    private synchronized void cancelEditors() {
        currentlyCancelingEditors = true;
        Iterator i = cellEditors.iterator();
        while (i.hasNext())
            ((CellEditor) i.next()).cancelCellEditing();
        cellEditors.clear();
        currentlyCancelingEditors = false;
    }

    /** Stop any editing sessions in progress for the
     * {@link SnapshotSource} */
    private synchronized void stopEditors() {
        Iterator i = cellEditors.iterator();
        while (i.hasNext()) try {
            ((CellEditor) i.next()).stopCellEditing();
        } catch (Exception e) {}
        cancelEditors();
    }

    /** A user interface component should call this method (or the
     * <code>static</code> equivalent of this method) after making a
     * user-directed change to the {@link SnapshotSource}.
     */
    public synchronized void madeChange(String description) {
        // if this change appears to be in response to our cancellation of
        // an editing session, ignore it.
        if (currentlyCancelingEditors) return;

        //System.out.println("UndoList.madeChange("+description+")");
        undoList.push(currentState);
        if (undoList.size() > MAX_LEVELS)
            undoList.remove(0);
        currentState = snapshotSource.getSnapshot();
        redoList = null;
        refreshActions();
    }

    /** Perform an undo operation. */
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

    /** Perform a redo operation. */
    public synchronized void redo() {
        if (isRedoAvailable()) {
            cancelEditors();
            undoList.push(currentState);
            currentState = redoList.pop();
            snapshotSource.restoreSnapshot(currentState);
            refreshActions();
        }
    }

    /** Discard all undo and redo states */
    public void clear() {
        undoList = new Stack();
        redoList = null;
        currentState = snapshotSource.getSnapshot();
        refreshActions();
    }

    /** An action that can be used to trigger an undo operation */
    private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", IconFactory.getUndoIcon());
            setEnabled(isUndoAvailable());
        }
        public void actionPerformed(ActionEvent e) {
            undo();
        }
    }

    /** An action that can be used to trigger a redo operation */
    private class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo", IconFactory.getRedoIcon());
            setEnabled(isRedoAvailable());
        }
        public void actionPerformed(ActionEvent e) {
            redo();
        }
    }

    /** Register this undo list as the designated undo list for the given
     * component. */
    public void setForComponent(JComponent component) {
        component.putClientProperty(CLIENT_PROP_NAME, this);
    }


    // The following methods provide a convenient, decoupled interface for
    // interacting with an undo list.


    private static final String CLIENT_PROP_NAME = "teamdash.wbs.undoList";

    /** Find the designated undo list for a given component, or null if
     * there does not appear to be a designated undo list.
     *
     * Walks up the component hierarchy until it finds a JComponent which
     * has its undoList property set */
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

    /** A user interface component should call this method (or the
     * non-<code>static</code> equivalent of this method) after making a
     * user-directed change to a {@link SnapshotSource}.
     */
    public static void madeChange(Component source, String description) {
        UndoList l = findUndoList(source);
        if (l != null) l.madeChange(description);
    }

    public static void stopCellEditing(Component source) {
        UndoList l = findUndoList(source);
        if (l != null) l.stopEditors();
    }
}
