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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.ActionCategoryComparator;


/** Observes changes made to a {@link SnapshotSource}, and supports undo/redo
 * operations on that object.
 */
public class UndoList {

    /** How many levels of undo should ideally be maintained? */
    public static final int MAX_LEVELS = 20;

    /** What is the minimum number of levels of undo to maintain? */
    public static final int MIN_LEVELS = 4;

    /** Discard undo states if they would cause us to use more than this
     * percentage of the max available memory */
    public static final double USED_MEM_RATIO = 0.7;

    /** The Undo/Redo Actions category */
    public static final String UNDO_ACTION_CATEGORY =
        ActionCategoryComparator.ACTION_CATEGORY;
    public static final String UNDO_ACTION_CATEGORY_UNDO_REDO = "undo/redo";

    /** Support the ability to undo/redo changes made to this object */
    private SnapshotSource snapshotSource;

    /** Should this undo list be sensitive to memory usage? */
    private boolean memorySensitive;

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

    /** A list of the actions we support */
    private List customActions;

    /** Flag indicating whether we are in the middle of canceling
     * table cell editing sessions. */
    private boolean currentlyCancelingEditors = false;

    /** A list of ChangeListeners that should be notified when an undo or a
     *  redo operation is made */
    private EventListenerList changeListeners;


    /** Create a new UndoList for the given {@link SnapshotSource} */
    public UndoList(SnapshotSource snapshotSource) {
        this.snapshotSource = snapshotSource;
        this.undoList = new Stack();
        this.redoList = null;
        this.cellEditors = new HashSet();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        customActions = new LinkedList();
        customActions.add(new ActionMapping("Undo", undoAction));
        customActions.add(new ActionMapping("Redo", redoAction));
        this.currentState = snapshotSource.getSnapshot();
        this.changeListeners = new EventListenerList();
    }

    public boolean isMemorySensitive() {
        return memorySensitive;
    }

    public void setMemorySensitive(boolean memorySensitive) {
        this.memorySensitive = memorySensitive;
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
        notifyAllChangeListeners();
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
        while (shouldDiscardOldestState())
            undoList.remove(0);
        currentState = snapshotSource.getSnapshot();
        redoList = null;
        refreshActions();
    }

    private boolean shouldDiscardOldestState() {
        if (undoList.size() > MAX_LEVELS)
            return true;
        if (!memorySensitive)
            return false;
        if (undoList.size()-1 < MIN_LEVELS)
            return false;

        Runtime.getRuntime().gc();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memRatio = ((double) usedMemory) / maxMemory;
        return memRatio > USED_MEM_RATIO;
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

    public void addChangeListener(ChangeListener l) {
        changeListeners.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(ChangeListener.class, l);
    }

    protected void notifyAllChangeListeners() {
        ChangeEvent e = new ChangeEvent(this);

        for (ChangeListener l : changeListeners.getListeners(ChangeListener.class)) {
            l.stateChanged(e);
        }
    }

    /** An action that can be used to trigger an undo operation */
    private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", IconFactory.getUndoIcon());
            putValue(UNDO_ACTION_CATEGORY, UNDO_ACTION_CATEGORY_UNDO_REDO);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, //
                MacGUIUtils.getCtrlModifier()));
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
            putValue(UNDO_ACTION_CATEGORY, UNDO_ACTION_CATEGORY_UNDO_REDO);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, //
                MacGUIUtils.getCtrlModifier()));
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
        Iterator i = customActions.iterator();
        while (i.hasNext())
            ((ActionMapping) i.next()).install(component,
                    JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
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
