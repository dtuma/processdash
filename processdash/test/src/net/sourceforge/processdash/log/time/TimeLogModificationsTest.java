// Copyright (C) 2005-2007 Tuma Solutions, LLC
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.FileUtils;

public class TimeLogModificationsTest extends AbstractTimeLogTest implements TimeLogListener, PropertyChangeListener {

    private File tempDir;
    private List eventsReceived;

    protected void setUp() throws Exception {
        super.setUp();
        eventsReceived = new LinkedList();
        tempDir = null;
    }

     protected void tearDown() throws Exception {
         if (tempDir != null)
             FileUtils.deleteDirectory(tempDir);

         super.tearDown();
    }

    public void testTimeLogModifications() throws Exception {
        TimeLog mockBase = new MockBaseTimeLog();
        Iterator modEntries = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        TimeLogModifications timeLogMods = new TimeLogModifications(mockBase, modEntries);
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLogMods);
    }

    public void testFiltering() throws Exception {
        TimeLog mockBase = new MockBaseTimeLog();
        Iterator modEntries = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        TimeLogModifications timeLogMods = new TimeLogModifications(mockBase, modEntries);

        Iterator entries = timeLogMods.filter("/Project/Dy4-181 BSP Build", null, null);
        assertFalse(entries.hasNext());
        entries = timeLogMods.filter("/Project/Gen2 MPA Build for VxWorks", null, null);
        assertFalse(entries.hasNext());
        entries = timeLogMods.filter(null, new Date(1094238594000L), new Date(1094244165000L));
        assertTimeLogEntryContents(TIMELOG3_MOD_CONTENTS[1], (TimeLogEntry) entries.next());
        assertTimeLogEntryContents(TIMELOG3_MOD_CONTENTS[2], (TimeLogEntry) entries.next());
        assertTimeLogEntryContents(TIMELOG3_MOD_CONTENTS[4], (TimeLogEntry) entries.next());
        assertFalse(entries.hasNext());
    }

    public void testEventDelivery() throws Exception {
        TimeLog mockBase = new MockBaseTimeLog();
        Iterator modEntries = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        TimeLogModifications timeLogMods = new TimeLogModifications(mockBase, modEntries);
        timeLogMods.addTimeLogListener(this);
        assertTrue(eventsReceived.isEmpty());

        Date now = new Date();
        ChangeFlaggedTimeLogEntry tle = new TimeLogEntryVO(50, "/foo", now, 123, 456, "comment", ChangeFlagged.ADDED);
        assertEventDelivered(timeLogMods, tle);
        assertEventDelivered(timeLogMods, tle);

        tle = new TimeLogEntryVO(50, "/foo", now, 123, 456, "comment", ChangeFlagged.MODIFIED);
        assertEventDelivered(timeLogMods, tle);

        tle = new TimeLogEntryVO(50, "/foo", now, 123, 456, "comment", ChangeFlagged.MODIFIED);
        assertEventDelivered(timeLogMods, tle);

        tle = new TimeLogEntryVO(50, null, null, 0, 0, null, ChangeFlagged.DELETED);
        assertEventDelivered(timeLogMods, tle);
        assertEventDelivered(timeLogMods, tle);

        tle = new TimeLogEntryVO(50, "/foo", now, 123, 456, "comment");
        try {
            timeLogMods.addModification(tle);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }

        timeLogMods.clear();
        assertTimeLogWideEvent();

        modEntries = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        timeLogMods.addModifications(modEntries);
        assertTimeLogWideEvent();
        timeLogMods.addModifications(Collections.EMPTY_LIST.iterator());
        assertTrue(eventsReceived.isEmpty());
    }

    private void assertEventDelivered(TimeLogModifications timeLogMods, TimeLogEntry tle) {
        assertEventDelivered(timeLogMods, timeLogMods, tle);
    }

    private void assertEventDelivered(TimeLogModifications timeLogMods, TimeLogModifications source, TimeLogEntry tle) {
        timeLogMods.addModification((ChangeFlaggedTimeLogEntry) tle);
        assertEquals(1, eventsReceived.size());
        TimeLogEvent evt = (TimeLogEvent) eventsReceived.get(0);
        assertSame(source, evt.getSource());
        assertSame(tle, evt.getTimeLogEntry());
        eventsReceived.clear();
    }

    private void assertTimeLogWideEvent() {
        assertEquals(1, eventsReceived.size());
        TimeLogEvent evt = (TimeLogEvent) eventsReceived.get(0);
        assertNull(evt.getTimeLogEntry());
        eventsReceived.clear();
    }

    public void testAdditionSemantics() throws Exception {
        // create an empty time log
        EmptyBaseTimeLog base = new EmptyBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        assertFalse(timeLogMods.filter(null, null, null).hasNext());
        timeLogMods.addTimeLogListener(this);

        // add entries to it, one at a time.
        Iterator newEntries = new TimeLogReader(openFile(TIMELOG3_XML));
        for (int i = 0; i < TIMELOG3_CONTENTS.length; i++) {
            TimeLogEntry tle = (TimeLogEntry) newEntries.next();
            assertNotNull(tle);
            TimeLogEntry added = new TimeLogEntryVO(tle, ChangeFlagged.ADDED);
            assertEventDelivered(timeLogMods, added);

            // after each entry, compare the resulting contents of the time
            // log with our expectations
            Iterator contents = timeLogMods.filter(null, null, null);
            for (int j = 0; j <= i; j++) {
                TimeLogEntry contentTle = (TimeLogEntry) contents.next();
                assertTimeLogEntryContents(TIMELOG3_CONTENTS[j], contentTle);
            }
            assertFalse(contents.hasNext());
        }

        // now re-add all the entries to it a second time.
        newEntries = new TimeLogReader(openFile(TIMELOG3_XML));
        while (newEntries.hasNext()) {
            TimeLogEntry tle = (TimeLogEntry) newEntries.next();
            TimeLogEntry added = new TimeLogEntryVO(tle, ChangeFlagged.ADDED);
            assertEventDelivered(timeLogMods, added);

            // since these entries are already in the modification list,
            // they should show up as "no change"
            assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);
        }
    }

    public void testDeletionSemantics() throws Exception {
        // create a starting point time log and assert its contents
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);
        timeLogMods.addTimeLogListener(this);

        // now, delete time log entries, one at a time.
        for (int i = 0; i < TIMELOG3_CONTENTS.length; i++) {
            TimeLogEntry deleted = new TimeLogEntryVO(i+1, null, null, 0, 0, null, ChangeFlagged.DELETED);
            assertEventDelivered(timeLogMods, deleted);

            // test to make certain that the entry got deleted.
            Iterator contents = timeLogMods.filter(null, null, null);
            for (int j = i+1; j < TIMELOG3_CONTENTS.length; j++) {
                TimeLogEntry contentTle = (TimeLogEntry) contents.next();
                assertTimeLogEntryContents(TIMELOG3_CONTENTS[j], contentTle);
            }
        }
    }

    public void testModificationSemantics() throws Exception {
        // create a starting point time log and assert its contents
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);
        timeLogMods.addTimeLogListener(this);

        // send a "no-op" modification event, and make certain that the
        // time log contents are not changed
        TimeLogEntry mod = new TimeLogEntryVO(1, null, null, 0, 0, null, ChangeFlagged.MODIFIED);
        assertEventDelivered(timeLogMods, mod);
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);

        // now start making modifications to a particular time log entry, and
        // see if they behave as expected
        MutableTimeLogEntry expectedEntry = new MutableTimeLogEntryVO(getEntry(timeLogMods, 1));

        // first, change the path
        mod = new TimeLogEntryVO(1, "/new path", null, 0, 0, null, ChangeFlagged.MODIFIED);
        expectedEntry.setPath("/new path");
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // now, change the date
        Date now = new Date();
        mod = new TimeLogEntryVO(1, null, now, 0, 0, null, ChangeFlagged.MODIFIED);
        expectedEntry.setStartTime(now);
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // decrement the elapsed time by 42 minutes
        mod = new TimeLogEntryVO(1, null, null, -42, 0, null, ChangeFlagged.MODIFIED);
        expectedEntry.setElapsedTime(expectedEntry.getElapsedTime() - 42);
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // increment the elapsed time by 4 minutes
        mod = new TimeLogEntryVO(1, null, null, 4, 0, null, ChangeFlagged.MODIFIED);
        expectedEntry.setElapsedTime(expectedEntry.getElapsedTime() + 4);
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // increment the interrupt time by 10 minutes
        mod = new TimeLogEntryVO(1, null, null, 0, 10, null, ChangeFlagged.MODIFIED);
        expectedEntry.setInterruptTime(expectedEntry.getInterruptTime() + 10);
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // change the comment
        mod = new TimeLogEntryVO(1, null, null, 0, 0, "new comment", ChangeFlagged.MODIFIED);
        expectedEntry.setComment("new comment");
        assertModificationOccurs(timeLogMods, mod, expectedEntry);

        // null out the comment (special semantics required)
        mod = new TimeLogEntryVO(1, null, null, 0, 0, "", ChangeFlagged.MODIFIED);
        expectedEntry.setComment(null);
        assertModificationOccurs(timeLogMods, mod, expectedEntry);
    }

    private void assertModificationOccurs(TimeLogModifications timeLogMods, TimeLogEntry mod, MutableTimeLogEntry expectedEntry) throws Exception {
        assertEventDelivered(timeLogMods, mod);
        TimeLogEntry tle = getEntry(timeLogMods, expectedEntry.getID());
        assertTimeLogEntryContents(expectedEntry, tle);
    }

    private TimeLogEntry getEntry(TimeLog timeLog, long id) throws Exception {
        TimeLogEntry result = null;
        Iterator entries = timeLog.filter(null, null, null);
        while (entries.hasNext()) {
            TimeLogEntry tle = (TimeLogEntry) entries.next();
            if (tle.getID() == id)
                result = tle;
        }
        return result;
    }

    public void testInPlaceModification() throws Exception {
        // create a starting point time log
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        timeLogMods.addTimeLogListener(this);

        // add an entry and make sure it was added
        Date now = new Date();
        MutableTimeLogEntry addition = new MutableTimeLogEntryVO(6, "/path", now, 1, 0, null, ChangeFlagged.ADDED);
        addition.addPropertyChangeListener(this);
        assertEventDelivered(timeLogMods, addition);
        assertTimeLogContents(TIMELOG3_CONTENTS,
                timeLogMods.filter(null, null, null), addition);

        // make a change to the entry and add it again.
        addition.setElapsedTime(2);
        assertPropertyChangeEvent(addition, "elapsedTime", new Integer(1), new Integer(2));
        assertEventDelivered(timeLogMods, addition);
        assertTimeLogContents(TIMELOG3_CONTENTS,
                timeLogMods.filter(null, null, null), addition);

        // now apply an external change to the entry.
        Date then = new Date(now.getTime() - 1000);
        ChangeFlaggedTimeLogEntry mod = new TimeLogEntryVO(6, "/new path", then, 10, 1, "new comment", ChangeFlagged.MODIFIED);
        timeLogMods.addModification(mod);
        // Make certain the mutable time log entry fired appropriate property change events.
        assertEquals(6, eventsReceived.size());
        assertPropertyChangeEvent(addition, "path", "/path", "/new path");
        assertPropertyChangeEvent(addition, "startTime", now, then);
        assertPropertyChangeEvent(addition, "elapsedTime", new Integer(2), new Integer(12));
        assertPropertyChangeEvent(addition, "interruptTime", new Integer(0), new Integer(1));
        assertPropertyChangeEvent(addition, "comment", null, "new comment");
        // Make certain that the mutable time log entry itself was changed appropriately.
        assertEquals(6, addition.getID());
        assertEquals("/new path", addition.getPath());
        assertEquals(then, addition.getStartTime());
        assertEquals(12, addition.getElapsedTime());
        assertEquals(1, addition.getInterruptTime());
        assertEquals("new comment", addition.getComment());
        assertEquals(ChangeFlagged.ADDED, ((ChangeFlagged)addition).getChangeFlag());
        // also check to make certain we received the correct event from the time log.
        assertEquals(1, eventsReceived.size());
        TimeLogEvent evt = (TimeLogEvent) eventsReceived.remove(0);
        assertSame(mod, evt.getTimeLogEntry());

        // now externally delete the entry.
        ChangeFlaggedTimeLogEntry del = new TimeLogEntryVO(6, null, null, 0, 0, null, ChangeFlagged.DELETED);
        timeLogMods.addModification(del);
        assertPropertyChangeEvent(addition, "changeFlag", new Integer(ChangeFlagged.ADDED), new Integer(ChangeFlagged.DELETED));
        // also check to make certain we received the correct event from the time log.
        assertEquals(1, eventsReceived.size());
        evt = (TimeLogEvent) eventsReceived.remove(0);
        assertSame(del, evt.getTimeLogEntry());
    }

    private void assertPropertyChangeEvent(Object source, String propName, Object oldVal, Object newVal) {
        Object evt = eventsReceived.remove(0);
        assertTrue("event should be a PropertyChangeEvent", evt instanceof PropertyChangeEvent);
        PropertyChangeEvent e = (PropertyChangeEvent) evt;
        assertSame(source, e.getSource());
        assertEquals(propName, e.getPropertyName());
        assertEquals(oldVal, e.getOldValue());
        assertEquals(newVal, e.getNewValue());
    }

    public void testLoadAndSave() throws Exception {
        // copy a starting point modifications file into a temp directory
        tempDir = createTempDir();
        copyFile(tempDir, TIMELOG3_MOD_XML, "foo.xml");
        File modificationsFile = new File(tempDir, "foo.xml");

        // create a time log modifications object with that destination file
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, modificationsFile, null);
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLogMods);

        timeLogMods.clear();
        assertFalse(timeLogMods.isDirty());
        assertTrue(modificationsFile.exists());
        assertIsEmptyTimeLogFile(modificationsFile);
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);

        // create a directory where the file should appear.  This will cause
        // subsequent save operations to fail.
        assertTrue(modificationsFile.delete() && modificationsFile.mkdir());
        PrintStream origErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        // now add back all the changes that were originally in the modifications
        // list.  The modifications should take effect, even though the file
        // cannot be saved.  The modification list should show as "dirty".
        timeLogMods.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        assertTrue(timeLogMods.isDirty());
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLogMods);

        System.setErr(origErr);
        assertTrue(err.toString().startsWith(
                "Unable to save time log modifications to file"));

        // now delete the bogus directory we created to cause problems.
        // we should then be able to save the modifications list successfully.
        assertTrue(modificationsFile.delete());
        assertTrue(timeLogMods.save());
        assertFalse(timeLogMods.isDirty());
        assertTrue(modificationsFile.isFile());
        assertTrue(compareFile(openFile(TIMELOG3_MOD_XML), modificationsFile));
    }

    public void testCommitFunctionality() throws Exception {
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        TimeLogModifications deferredMods = new TimeLogModifications(timeLogMods, null, null);

        deferredMods.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        assertTimeLogContents(TIMELOG3_CONTENTS, base);
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLogMods);
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, deferredMods);
        assertFalse(timeLogMods.hasUncommittedData());
        assertTrue(deferredMods.hasUncommittedData());
        deferredMods.clearUncommittedData();
        assertFalse(deferredMods.hasUncommittedData());
        assertTimeLogContents(TIMELOG3_CONTENTS, deferredMods);
        deferredMods.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, deferredMods);
        assertTrue(deferredMods.hasUncommittedData());
        deferredMods.commitData();
        assertFalse(deferredMods.hasUncommittedData());
        assertTrue(timeLogMods.hasUncommittedData());
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLogMods);
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, deferredMods);

        try {
            timeLogMods.commitData();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            assertEquals(
                    "Cannot commit modifications. Parent time log is not modifiable",
                    ise.getMessage());
        }
    }

    public void testEventRepetition() throws Exception {
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications parent = new TimeLogModifications(base, null, null);
        TimeLogModifications child = new TimeLogModifications(parent,
                new TimeLogReader(openFile(TIMELOG3_MOD_XML)));

        child.addTimeLogListener(this);

        // add several time log changes to the parent that should have no effect
        // on the child.
        parent.addModification(new TimeLogEntryVO(1, null, null, 0, 0, null,
                ChangeFlagged.MODIFIED));
        parent.addModification(new TimeLogEntryVO(1, "/new path", null, 0, 0,
                null, ChangeFlagged.MODIFIED));
        Date now = new Date();
        parent.addModification(new TimeLogEntryVO(3, null, now, 0, 0,
                "new comment", ChangeFlagged.MODIFIED));
        parent.addModification(new TimeLogEntryVO(6, "foo", now, 123,
                456, "new comment", ChangeFlagged.MODIFIED));
        assertTrue(eventsReceived.isEmpty());

        // delete something in the parent that we modified. We should get their
        // event, modified with the child as the source.
        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(3, null, null, 0, 0,
                null, ChangeFlagged.DELETED);
        assertEventDelivered(parent, child, diff);

        // change an entry that we didn't touch. We should get the event back
        diff = new TimeLogEntryVO(5, "new path", null, 0, 0, null,
                ChangeFlagged.MODIFIED);
        assertEventDelivered(parent, child, diff);

        // make a modification in the parent to something also modified in the
        // child, and observe the event we receive.
        diff = new TimeLogEntryVO(1, "/new path2", now, 10, 20, "new comment",
                ChangeFlagged.MODIFIED);
        TimeLogEntry expectedDiff = new TimeLogEntryVO(1, null, now, 10, 20,
                "new comment", ChangeFlagged.MODIFIED);
        parent.addModification(diff);
        assertEquals(1, eventsReceived.size());
        TimeLogEvent evt = (TimeLogEvent) eventsReceived.remove(0);
        assertSame(child, evt.getSource());
        assertEquals(expectedDiff, evt.getTimeLogEntry());

        // issue an additino in the parent to an entry that has been modified in
        // the child, and observe the event we receive.
        ChangeFlaggedTimeLogEntry readdition = new TimeLogEntryVO(1,
                "/Project/Dy4-181 BSP Build/Test Task/Test", now, 2000, 10,
                "comment number one", ChangeFlagged.ADDED);
        TimeLogEntry expectedTle = new TimeLogEntryVO(1,
                "/Non Project/Dy4-181 BSP Build/Test Task/Test", now, 1900, 10,
                "comment number one", ChangeFlagged.ADDED);
        parent.addModification(readdition);
        assertEquals(1, eventsReceived.size());
        evt = (TimeLogEvent) eventsReceived.remove(0);
        assertSame(child, evt.getSource());
        assertEquals(expectedTle, evt.getTimeLogEntry());
    }

    public void testBoundaryCases() throws Exception {
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null, null);
        try {
            timeLogMods.getNextID();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {}

        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(50, "/foo", null, 0, 0, null, ChangeFlagged.MODIFIED);
        TimeLogEntry diff2 = new MockExternalTimeLogEntry(diff);
        try {
            timeLogMods.addModifications(Collections.singletonList(diff2).iterator());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("Time log modifications must be ChangeFlagged", iae.getMessage());
        }

        diff = new TimeLogEntryVO(50, "/foo", null, 0, 0, null, ChangeFlagged.NO_CHANGE);
        try {
            timeLogMods.addModification(diff);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals("Time log modifications must describe a change", iae.getMessage());
        }
    }

    public void timeLogChanged(TimeLogEvent e) {
        eventsReceived.add(e);
    }

    public void propertyChange(PropertyChangeEvent e) {
        eventsReceived.add(e);
    }

}
