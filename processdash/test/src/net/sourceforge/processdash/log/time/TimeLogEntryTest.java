// Copyright (C) 2005 Tuma Solutions, LLC
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.processdash.log.ChangeFlagged;

public class TimeLogEntryTest extends TestCase implements
        PropertyChangeListener {

    public void testContructors() {
        Date now = new Date();
        TimeLogEntryVO a = new TimeLogEntryVO(42, "/path", now, 120, 42,
                "comment");
        checkFields(now, a);

        MockExternalTimeLogEntry b = new MockExternalTimeLogEntry(a);
        checkFields(now, b);

        TimeLogEntryVO c = new TimeLogEntryVO(b);
        checkFields(now, c);
    }

    private void checkFields(Date now, TimeLogEntry a) {
        assertEquals(42, a.getID());
        assertEquals("/path", a.getPath());
        assertEquals(now, a.getStartTime());
        assertEquals(120, a.getElapsedTime());
        assertEquals(42, a.getInterruptTime());
        assertEquals("comment", a.getComment());
    }

    public void testApplyChanges() throws Exception {
        TimeLogEntryVO base = new TimeLogEntryVO(42, "/path", new Date(), 120,
                0, "comment");
        TimeLogEntry noop = TimeLogEntryVO.applyChanges(base, null, true);
        assertSame(base, noop);
        noop = TimeLogEntryVO.applyChanges(base, null, false);
        assertSame(base, noop);

        TimeLogEntry diff = new TimeLogEntryVO(42, "/changed path", null, 0, 0,
                null, ChangeFlagged.MODIFIED);
        TimeLogEntry changed = TimeLogEntryVO.applyChanges(base, diff, false);
        assertFalse(base == changed);
        assertEquals(base.getID(), changed.getID());
        assertEquals(diff.getPath(), changed.getPath());
        assertEquals(base.getStartTime(), changed.getStartTime());
        assertEquals(base.getElapsedTime(), changed.getElapsedTime());
        assertEquals(base.getInterruptTime(), changed.getInterruptTime());
        assertEquals(base.getComment(), changed.getComment());

        // make certain it can work even if the base type isn't TimeLogEntryVO
        MockExternalTimeLogEntry tle = new MockExternalTimeLogEntry(base);
        changed = TimeLogEntryVO.applyChanges(tle, diff, false);
        assertFalse(tle == changed);
        assertEquals(base.getID(), changed.getID());
        assertEquals(diff.getPath(), changed.getPath());
        assertEquals(base.getStartTime(), changed.getStartTime());
        assertEquals(base.getElapsedTime(), changed.getElapsedTime());
        assertEquals(base.getInterruptTime(), changed.getInterruptTime());
        assertEquals(base.getComment(), changed.getComment());

        diff = new TimeLogEntryVO(43, null, null, 0, 0, null,
                ChangeFlagged.MODIFIED);
        try {
            TimeLogEntryVO.applyChanges(base, diff, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
        }
    }

    public void testCompareTo() throws Exception {
        Date now = new Date();
        Date then = new Date(now.getTime() - 1000);

        TimeLogEntryVO a = new TimeLogEntryVO(42, "/path", now, 120, 0,
                "comment");
        TimeLogEntryVO b = new TimeLogEntryVO(42, new String("/path"), now,
                120, 0, "comment");
        assertEquals(0, a.compareTo(b));
        assertEquals(0, b.compareTo(a));
        assertEquals(0, a.compareTo(a));

        TimeLogEntryVO c = new TimeLogEntryVO(42, "/path", then, 0, 0, null);
        assertTrue(a.compareTo(c) > 0);
        assertTrue(c.compareTo(a) < 0);

        TimeLogEntryVO d = new TimeLogEntryVO(42, "/path", null, 0, 0, null);
        assertTrue(a.compareTo(d) > 0);
        assertTrue(d.compareTo(a) < 0);

        TimeLogEntryVO e = new TimeLogEntryVO(42, "/path/two", now, 120, 0,
                "comment");
        assertTrue(a.compareTo(e) < 0);
        assertTrue(e.compareTo(a) > 0);

        TimeLogEntryVO f = new TimeLogEntryVO(24, "/path", now, 120, 0,
                "comment");
        assertTrue(a.compareTo(f) > 0);
        assertTrue(f.compareTo(a) < 0);
    }

    public void testEquals() {
        Date now = new Date();
        MutableTimeLogEntry a = new MutableTimeLogEntryVO(1, "/path a", now, 3, 5, null, ChangeFlagged.ADDED);
        MutableTimeLogEntryVO b = new MutableTimeLogEntryVO(2, "/path b", null, 4, 6, "comment b", ChangeFlagged.MODIFIED);
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.ID = 1;
        b.setPath(a.getPath());
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.setStartTime(a.getStartTime());
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.setElapsedTime(a.getElapsedTime());
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.setInterruptTime(a.getInterruptTime());
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.setComment(a.getComment());
        assertFalse(a.equals(b));  assertFalse(b.equals(a));
        b.setChangeFlag(a.getChangeFlag());
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        assertTrue(a.equals(a));
        assertFalse(a.equals("foo"));

        TimeLogEntry ext = new MockExternalTimeLogEntry(a);
        assertFalse(a.equals(ext));
        a.setChangeFlag(ChangeFlagged.NO_CHANGE);
        assertTrue((a.equals(ext)));
    }

    public void testMutableEntry() {
        Date now = new Date();
        Date then = new Date(now.getTime() - 1000);

        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(42, "/path", now,
                120, 0, "orig comment", ChangeFlagged.NO_CHANGE);
        tle.removePropertyChangeListener(this);  // noop
        tle.addPropertyChangeListener(this);

        tle.setPath("/new path");
        assertEquals("/new path", tle.getPath());
        assertPropertyChange(tle, "path", "/path", "/new path");

        tle.setStartTime(then);
        assertEquals(then, tle.getStartTime());
        assertPropertyChange(tle, "startTime", now, then);

        tle.setElapsedTime(22);
        assertEquals(22, tle.getElapsedTime());
        assertPropertyChange(tle, "elapsedTime", new Integer(120), new Integer(22));

        tle.setInterruptTime(12);
        assertEquals(12, tle.getInterruptTime());
        assertPropertyChange(tle, "interruptTime", new Integer(0), new Integer(12));

        tle.setComment("new comment");
        assertEquals("new comment", tle.getComment());
        assertPropertyChange(tle, "comment", "orig comment", "new comment");

        tle.setChangeFlag(ChangeFlagged.MODIFIED);
        assertEquals(ChangeFlagged.MODIFIED, tle.getChangeFlag());
        assertPropertyChange(tle, "changeFlag", new Integer(
                ChangeFlagged.NO_CHANGE), new Integer(ChangeFlagged.MODIFIED));

        tle.setStartTime(new Date(tle.getStartTime().getTime()));
        assertTrue(eventsReceived.isEmpty());

        tle.removePropertyChangeListener(this);
        tle.setStartTime(now);
        assertTrue(eventsReceived.isEmpty());

        tle.addPropertyChangeListener(this);
        tle.setStartTime(then);
        assertPropertyChange(tle, "startTime", now, then);
    }

    private void assertPropertyChange(Object source, String propName,
            Object oldVal, Object newVal) {
        assertFalse(eventsReceived.isEmpty());
        Object obj = eventsReceived.remove(0);
        assertTrue(obj instanceof PropertyChangeEvent);
        PropertyChangeEvent evt = (PropertyChangeEvent) obj;
        assertEquals(source, evt.getSource());
        assertEquals(propName, evt.getPropertyName());
        assertEquals(oldVal, evt.getOldValue());
        assertEquals(newVal, evt.getNewValue());
    }

    private List eventsReceived = new LinkedList();

    public void propertyChange(PropertyChangeEvent evt) {
        eventsReceived.add(evt);
    }
}
