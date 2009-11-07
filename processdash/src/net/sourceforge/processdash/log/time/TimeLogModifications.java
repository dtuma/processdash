// Copyright (C) 2005-2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.log.time;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.PathRenamingInstruction;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.log.IDSource;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.IteratorConcatenator;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.RobustFileWriter;

public class TimeLogModifications implements CommittableModifiableTimeLog {

    private TimeLog parent;

    private IDSource idSource;

    private File saveFile;

    private long saveFileTimestamp;

    private Map modifications;

    private List batchRenames;

    private boolean dirty;

    private List listeners;

    public TimeLogModifications(TimeLog parent, File file, IDSource idSource)
            throws IOException {
        this(parent, idSource);
        this.saveFile = file;
        load();
    }

    public TimeLogModifications(TimeLog parent, IDSource idSource) {
        this(parent);
        this.idSource = idSource;
    }

    /** Package private constructor for unit testing purposes only */
    TimeLogModifications(TimeLog parent, Iterator mods) throws IOException {
        this(parent);
        addModifications(mods);
    }

    TimeLogModifications(TimeLog parent) {
        this.parent = parent;
        if (parent instanceof TimeLogEventSource)
            ((TimeLogEventSource) parent)
                    .addTimeLogListener(new TimeLogEventRepeater(this));
        this.modifications = new LinkedHashMap();
        this.batchRenames = new LinkedList();
        this.listeners = new LinkedList();
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized boolean isEmpty() {
        return modifications.isEmpty() && batchRenames.isEmpty();
    }

    public boolean hasUncommittedData() {
        return !isEmpty();
    }

    public void clearUncommittedData() {
        clear();
    }

    public void clear() {
        synchronized (this) {
            modifications.clear();
            batchRenames.clear();
            if (saveFile == null)
                dirty = false;
            else
                save();
        }

        fireTimeLogChanged();
    }

    public void commitData() {
        if (parent instanceof ModifiableTimeLog) {
            List mods = Collections.list(new RenamingOperationsIterator());
            mods.addAll(modifications.values());

            synchronized (this) {
                modifications.clear();
                batchRenames.clear();
                if (saveFile == null)
                    dirty = false;
                else
                    save();
            }

            ((ModifiableTimeLog) parent).addModifications(mods.iterator());
        } else {
            throw new IllegalStateException("Cannot commit modifications. "
                    + "Parent time log is not modifiable");
        }
    }

    public long getNextID() {
        if (idSource != null)
            return idSource.getNextID();
        else
            throw new IllegalStateException("ID source not provided");
    }

    public void addModifications(Iterator iter) {
        if (iter == null || iter.hasNext() == false)
            return;

        synchronized (this) {
            while (iter.hasNext()) {
                TimeLogEntry tle = (TimeLogEntry) iter.next();
                addModificationImpl(tle);
            }
        }

        fireTimeLogChanged();
        save();
    }

    public void addModification(ChangeFlaggedTimeLogEntry mod) {
        if (addModificationImpl(mod))
            fireEntryAdded(mod);
        else
            fireTimeLogChanged();
        save();
    }

    protected synchronized boolean addModificationImpl(TimeLogEntry mod) {
        if (!(mod instanceof ChangeFlagged))
            throw new IllegalArgumentException(
                    "Time log modifications must be ChangeFlagged");

        boolean result = true;
        Long id = new Long(mod.getID());
        TimeLogEntry oldEntry = (TimeLogEntry) modifications.get(id);

        switch (((ChangeFlagged) mod).getChangeFlag()) {
        case ChangeFlagged.ADDED:
            modifications.put(id, mod);
            break;

        case ChangeFlagged.MODIFIED:
            if (oldEntry != null)
                // merge modifications
                mod = TimeLogEntryVO.applyChanges(oldEntry, mod, true);
            modifications.put(id, mod);
            break;

        case ChangeFlagged.DELETED:
            if (oldEntry instanceof MutableTimeLogEntry)
                // let the previous time log entry know that it is being deleted
                ((MutableTimeLogEntry) oldEntry)
                        .setChangeFlag(ChangeFlagged.DELETED);
            modifications.put(id, mod);
            break;

        case ChangeFlagged.BATCH_MODIFICATION:
            processBatchChange((ChangeFlaggedTimeLogEntry) mod, true);
            result = false;
            break;

        default:
            throw new IllegalArgumentException(
                    "Time log modifications must describe a change");
        }

        dirty = true;
        return result;
    }

    protected void processBatchChange(ChangeFlaggedTimeLogEntry mod, boolean addToList) {
        PathRenamingInstruction instr = PathRenamer.toInstruction(mod);
        synchronized (this) {
            if (addToList)
                batchRenames.add(instr);
            processBatchRename(instr);
        }
        fireEntryAdded(mod);
    }

    protected synchronized void processBatchRename(PathRenamingInstruction instr) {
        List instrList = Collections.singletonList(instr);

        for (Iterator i = modifications.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            ChangeFlaggedTimeLogEntry oldChange =
                (ChangeFlaggedTimeLogEntry) e.getValue();
            ChangeFlaggedTimeLogEntry newChange =
                PathRenamer.getRenameModification(oldChange, instrList);
            if (newChange != null) {
                newChange = (ChangeFlaggedTimeLogEntry) TimeLogEntryVO
                        .applyChanges(oldChange, newChange, true);
                e.setValue(newChange);
            }
        }
    }

    public void addTimeLogListener(TimeLogListener l) {
        listeners.add(l);
    }

    public void removeTimeLogListener(TimeLogListener l) {
        listeners.remove(l);
    }

    protected void fireTimeLogChanged() {
        if (!listeners.isEmpty())
            fireTimeLogEvent(new TimeLogEvent(this));
    }

    protected void fireEntryAdded(ChangeFlaggedTimeLogEntry e) {
        if (!listeners.isEmpty())
            fireTimeLogEvent(new TimeLogEvent(this, e));
    }

    private void fireTimeLogEvent(TimeLogEvent e) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            TimeLogListener l = (TimeLogListener) iter.next();
            l.timeLogChanged(e);
        }
    }

    protected void repeatTimeLogEvent(TimeLogEvent evt) {
        ChangeFlaggedTimeLogEntry e = evt.getTimeLogEntry();
        if (e != null) {
            if (PathRenamer.isRenamingOperation(e))
                processBatchChange(e, false);
            else {
                ChangeFlaggedTimeLogEntry mod = getModification(e.getID());
                if (mod != null) {
                    e = buildRepeatedParentChange(e, mod);
                    if (e == null)
                        return;
                }
            }
        }

        fireTimeLogEvent(new TimeLogEvent(this, e));
    }

    private ChangeFlaggedTimeLogEntry buildRepeatedParentChange(ChangeFlaggedTimeLogEntry parentMod, ChangeFlaggedTimeLogEntry ourMod) {
        if (ourMod.getChangeFlag() == ChangeFlagged.ADDED
                || ourMod.getChangeFlag() == ChangeFlagged.DELETED)
            // we have locally deleted or modified the entry. Changes
            // made by our parent will have no effect as far as
            // we are concerned.
            return null;

        // our change is a modification. Check to see how this affect the parent
        // change.

        if (parentMod.getChangeFlag() == ChangeFlagged.DELETED)
            // the parent is deleting an entry that we changed.  They win.
            return parentMod;

        if (parentMod.getChangeFlag() == ChangeFlagged.MODIFIED) {
            // we're both trying to change the entry. Now things get
            // complicated. We have to figure out which of the parent's changes
            // will actually take effect once we have our way.
            String pathChange = (String) mergeChange(parentMod.getPath(),
                    ourMod.getPath());
            Date dateChange = (Date) mergeChange(parentMod.getStartTime(),
                    ourMod.getStartTime());
            long elapsedChange = parentMod.getElapsedTime();
            long interruptChange = parentMod.getInterruptTime();
            String commentChange = (String) mergeChange(parentMod.getComment(),
                    ourMod.getComment());

            if (pathChange == null && dateChange == null && elapsedChange == 0
                    && interruptChange == 0 && commentChange == null)
                // none of the parents changes will have any effect. Their
                // event can be ignored.
                return null;
            else
                // construct a new time log entry indicating the real effect
                // of the parent's change.
                return new TimeLogEntryVO(parentMod.getID(), pathChange,
                        dateChange, elapsedChange, interruptChange,
                        commentChange, ChangeFlagged.MODIFIED);
        }

        // the parent is adding an entry (or making a tweak to a previously
        // added entry). We just need to apply our changes to it before
        // passing it along.
        return (ChangeFlaggedTimeLogEntry) TimeLogEntryVO.applyChanges(
                parentMod, ourMod, false);
    }

    private Object mergeChange(Object baseChange, Object ourChange) {
        if (ourChange == null)
            return baseChange;
        else
            return null;
    }

    public synchronized ChangeFlaggedTimeLogEntry getModification(long id) {
        return (ChangeFlaggedTimeLogEntry) modifications.get(new Long(id));
    }

    protected void load() throws IOException {
        synchronized (this) {
            modifications = new LinkedHashMap();

            if (saveFile != null && saveFile.isFile()) {
                for (Iterator iter = new TimeLogReader(saveFile); iter.hasNext();) {
                    TimeLogEntry tle = (TimeLogEntry) iter.next();
                    if (PathRenamer.isRenamingOperation(tle))
                        batchRenames.add(PathRenamer.toInstruction(tle));
                    else
                        modifications.put(new Long(tle.getID()), tle);
                }
                saveFileTimestamp = saveFile.lastModified();
            }

            this.dirty = false;
        }
        fireTimeLogChanged();
    }

    public void maybeReloadData() throws IOException {
        if (saveFile != null && saveFile.isFile()) {
            long timestamp = saveFile.lastModified();
            if (timestamp != 0 && timestamp != saveFileTimestamp)
                load();
        }
    }

    public synchronized boolean save() {
        if (saveFile == null || Settings.isReadOnly())
            return false;

        try {
            RobustFileWriter out = new RobustFileWriter(saveFile, "UTF-8");
            Iterator entries = new IteratorConcatenator(
                    new RenamingOperationsIterator(), //
                    modifications.values().iterator());
            TimeLogWriter.write(out, entries);
            saveFileTimestamp = saveFile.lastModified();
            dirty = false;
            return true;
        } catch (IOException ioe) {
            System.err
                    .println("Unable to save time log modifications to file '"
                            + saveFile + "'");
            ioe.printStackTrace();
            return false;
        }
    }

    public EnumerIterator filter(String path, Date from, Date to)
            throws IOException {
        if (isEmpty())
            // optimization if there are no modifications to perform
            return parent.filter(path, from, to);

        Iterator baseEntries = parent.filter(null, null, null);
        Iterator modifiedEntries = new ModifiedEntriesFilter(baseEntries);
        Iterator addedEntries = new AddedEntriesFilter();
        EnumerIterator allEntries = new IteratorConcatenator(modifiedEntries,
                addedEntries);
        if (path == null && from == null && to == null)
            return allEntries;
        else
            return new TimeLogIteratorFilter(allEntries, path, from, to);
    }

    protected class AddedEntriesFilter extends IteratorFilter {
        public AddedEntriesFilter() {
            super(modifications.values().iterator());
            init();
        }

        public Object next() {
            TimeLogEntry result = (TimeLogEntry) super.next();
            return new TimeLogEntryVO(result, ChangeFlagged.NO_CHANGE);
        }

        protected boolean includeInResults(Object o) {
            ChangeFlagged tle = (ChangeFlagged) o;
            return (tle.getChangeFlag() == ChangeFlagged.ADDED);
        }
    }

    protected class ModifiedEntriesFilter extends IteratorFilter {

        public ModifiedEntriesFilter(Iterator baseEntries) {
            super(baseEntries);
            init();
        }

        public Object next() {
            TimeLogEntry result = (TimeLogEntry) super.next();

            TimeLogEntry renameMod = PathRenamer.getRenameModification(result,
                    batchRenames);
            result = TimeLogEntryVO.applyChanges(result, renameMod, false);

            TimeLogEntry diff = getModification(result.getID());
            result = TimeLogEntryVO.applyChanges(result, diff, false);

            return result;
        }

        protected boolean includeInResults(Object o) {
            TimeLogEntry tle = (TimeLogEntry) o;
            ChangeFlagged mod = (ChangeFlagged) getModification(tle.getID());
            // filter out deleted entries in the parent map.
            return (mod == null || mod.getChangeFlag() != ChangeFlagged.DELETED);
        }

    }

    protected class RenamingOperationsIterator extends IteratorFilter {

        public RenamingOperationsIterator() {
            super(batchRenames.iterator());
            init();
        }

        public Object next() {
            PathRenamingInstruction instr = (PathRenamingInstruction) super.next();
            return PathRenamer.toTimeLogEntry(instr);
        }

        protected boolean includeInResults(Object o) {
            return true;
        }

    }

    protected static class TimeLogEventRepeater implements TimeLogListener {

        private Reference target;

        public TimeLogEventRepeater(TimeLogModifications target) {
            this.target = new WeakReference(target);
        }

        public void timeLogChanged(TimeLogEvent e) {
            TimeLogModifications tlm = (TimeLogModifications) target.get();
            if (tlm != null)
                tlm.repeatTimeLogEvent(e);
        }

    }

}
