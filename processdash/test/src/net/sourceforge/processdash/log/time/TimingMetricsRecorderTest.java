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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.ChangeFlagged;

public class TimingMetricsRecorderTest extends AbstractTimeLogTest {

    private DataContext data;

    private MockBaseTimeLog baseTimeLog;

    private ModifiableTimeLog timeLog;

    private MockApprover approver;

    private DashHierarchy hierarchy;

    private TimingMetricsRecorder recorder;

    private Map expectedTimes;

    protected void setUp() throws Exception {
        super.setUp();
        data = new MockDataContext();
        baseTimeLog = new MockBaseTimeLog();
        timeLog = new TimeLogModifications(baseTimeLog);
        approver = new MockApprover();
        hierarchy = new DashHierarchy(null);
        hierarchy.loadXML(openFile("hier3.xml"), new DashHierarchy(null));
        recorder = new TimingMetricsRecorder(timeLog, data, hierarchy, approver);
        recorder.refreshMetrics();

        expectedTimes = new HashMap();
        for (int i = 0; i < TIMELOG3_CONTENTS.length; i++) {
            String path = (String) TIMELOG3_CONTENTS[i][0];
            Integer elapsed = (Integer) TIMELOG3_CONTENTS[i][2];
            expectedTimes.put(path, elapsed);
        }
    }

    protected void tearDown() throws Exception {
        recorder.dispose();
        super.tearDown();
    }

    public void testNormalOperation() throws Exception {
        // the mere act of creating the recorder should have initialized
        // all times to their correct values
        assertTimes(expectedTimes);

        Iterator i = new TimeLogReader(openFile(TIMELOG3_MOD_XML));

        // add first modification (decreases elapsed time on entry #1, and
        // changes its path)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put("/Project/Dy4-181 BSP Build/Test Task/Test",
                new Integer(0));
        expectedTimes.put("/Non Project/Dy4-181 BSP Build/Test Task/Test",
                new Integer(800));
        assertTimes(expectedTimes);

        // add second modification (deletes entry #2)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put("/Project/Gen2 MPA Build for VxWorks/Test Task/Test",
                new Integer(0));
        assertTimes(expectedTimes);

        // add third modification (changes irrelevant fields)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        assertTimes(expectedTimes);

        // add fourth modification (changes nothing)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        assertTimes(expectedTimes);

        // add fifth modification (adds a new entry)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put(
                "/Project/Requirements/FDDI bus/Inspect/Reqts Inspect",
                new Integer(28));
        assertTimes(expectedTimes);

        // add another modification (change the path of an entry)
        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(3,
                "/Project/Requirements/Utility bus/RRW/Reqts", null, 0, 0,
                null, ChangeFlagged.MODIFIED);
        timeLog.addModification(diff);
        expectedTimes.put("/Project/Requirements/Serial IO/RRW/Reqts",
                new Integer(0));
        expectedTimes.put("/Project/Requirements/Utility bus/RRW/Reqts",
                new Integer(195));
        assertTimes(expectedTimes);
    }

    public void testIOException() throws Exception {
        Map expectedTimes = new HashMap(this.expectedTimes);

        // the mere act of creating the recorder should have initialized
        // all times to their correct values
        assertTimes(expectedTimes);

        // reconfigure the base time log to throw IOExceptions midway through
        // the file
        baseTimeLog.numBytes = 300;

        Iterator i = new TimeLogReader(openFile(TIMELOG3_MOD_XML));

        // add first modification (decreases elapsed time on entry #1, and
        // changes its path)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put("/Project/Dy4-181 BSP Build/Test Task/Test",
                new Integer(0));
        expectedTimes.put("/Non Project/Dy4-181 BSP Build/Test Task/Test",
                new Integer(800));
        assertTimes(this.expectedTimes);

        // add second modification (deletes entry #2)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put("/Project/Gen2 MPA Build for VxWorks/Test Task/Test",
                new Integer(0));
        assertTimes(this.expectedTimes);

        // add third modification (changes irrelevant fields)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        assertTimes(this.expectedTimes);

        // now reconfigure the base time log to throw IOExceptions very early
        // in the file
        baseTimeLog.numBytes = 100;

        // add fourth modification (changes nothing)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        assertTimes(this.expectedTimes);

        // add fifth modification (adds a new entry)
        timeLog.addModification((ChangeFlaggedTimeLogEntry) i.next());
        expectedTimes.put(
                "/Project/Requirements/FDDI bus/Inspect/Reqts Inspect",
                new Integer(28));
        assertTimes(this.expectedTimes);

        // now reconfigure the base time log to behave
        baseTimeLog.numBytes = Integer.MAX_VALUE;

        // add another modification (change the path of an entry)
        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(3,
                "/Project/Requirements/Utility bus/RRW/Reqts", null, 0, 0,
                null, ChangeFlagged.MODIFIED);
        timeLog.addModification(diff);
        expectedTimes.put("/Project/Requirements/Serial IO/RRW/Reqts",
                new Integer(0));
        expectedTimes.put("/Project/Requirements/Utility bus/RRW/Reqts",
                new Integer(195));
        assertTimes(expectedTimes);
    }

    public void testCleanupBogusElements() throws Exception {
        // the mere act of creating the recorder should have initialized
        // all times to their correct values
        assertTimes(expectedTimes);

        // add a bogus value to the data repository
        StringData stringData = new StringData("\"foo");
        data.putValue("/Project/Time", new DoubleData(666, false));
        data.putValue("/Non Project/Time", stringData);
        // but we don't want it to show up.
        expectedTimes.put("/Project", new Integer(0));
        boolean timesMatch = false;
        try {
            assertTimes(expectedTimes);
            timesMatch = true;
        } catch (AssertionFailedError afe) {
        }
        assertFalse("expected times NOT to match", timesMatch);

        // this should cause things to get cleaned up
        recorder.timeLogChanged(new TimeLogEvent(timeLog));
        assertTimes(expectedTimes);
        assertSame(stringData, data.getValue("/Non Project/Time"));
    }

    public void testDisallowedTimeElement() throws Exception {
        // the mere act of creating the recorder should have initialized
        // all times to their correct values
        assertTimes(expectedTimes);

        Date now = new Date();
        Date then = new Date(now.getTime() - 1000);

        // reconfigure the approver to deny requests.
        approver.approve = false;

        // try logging somewhere
        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(6,
                "/Project/Dy4-181 BSP Build/Test Task/Test", then, 100, 0,
                null, ChangeFlagged.ADDED);
        timeLog.addModification(diff);
        assertTimes(expectedTimes); // no times should have changed.

        // reconfigure the approver to approve requests.
        approver.approve = true;

        // try logging somewhere
        diff = new TimeLogEntryVO(7,
                "/Project/Dy4-181 BSP Build/Test Task/Test", now, 100, 0, null,
                ChangeFlagged.ADDED);
        timeLog.addModification(diff);
        // now we should see both of the added entries in effect.
        expectedTimes.put("/Project/Dy4-181 BSP Build/Test Task/Test",
                new Integer(1100));
        assertTimes(expectedTimes);

    }

    public void testStartDates() throws Exception {
        Date now = new Date();
        Date then = new Date(now.getTime() - 1000);
        Date future = new Date(now.getTime() + 1000);

        ChangeFlaggedTimeLogEntry diff = new TimeLogEntryVO(6,
                "/Project/Requirements/Utility bus/RRW/Reqts", then, 100, 0,
                null, ChangeFlagged.ADDED);
        timeLog.addModification(diff);
        diff = new TimeLogEntryVO(7,
                "/Project/Requirements/Serial IO/RRW/Reqts", now, 100, 0, null,
                ChangeFlagged.ADDED);
        timeLog.addModification(diff);
        diff = new TimeLogEntryVO(8,
                "/Project/Requirements/Serial IO/RRW/Reqts", future, 100, 0,
                null, ChangeFlagged.ADDED);
        timeLog.addModification(diff);

        // check to see if start times were properly recorded.
        checkStartTime("/Project/Requirements/Utility bus/RRW/Reqts", then);
        checkStartTime("/Project/Requirements/Utility bus/RRW", then);
        checkStartTime("/Project/Requirements/Utility bus", then);
        checkStartTime("/Project/Requirements", then);
        checkStartTime("/Project", then);
        checkStartTime("/Project/Requirements/Serial IO/RRW/Reqts", now);
        checkStartTime("/Project/Requirements/Serial IO/RRW", now);
        checkStartTime("/Project/Requirements/Serial IO", now);
        checkStartTime("/Project/Requirements/Serial IO", now);
    }

    private void checkStartTime(String path, Date expected) {
        Object sd = data.getValue(path + "/Started");
        assertNotNull(sd);
        assertTrue(sd instanceof DateData);
        assertEquals(expected, ((DateData) sd).getValue());
    }

    private void assertTimes(Map times) {
        for (Iterator iter = times.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String path = (String) e.getKey();
            Integer elapsed = (Integer) e.getValue();
            assertTime(path, elapsed.intValue());
        }
    }

    private void assertTime(String path, int i) {
        String dataName = path + "/Time";
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null && i == 0)
            return;

        assertNotNull("Missing time data for " + path, d);
        assertTrue("Time data for " + path + " is not NumberData",
                d instanceof NumberData);
        NumberData n = (NumberData) d;
        assertEquals("Wrong time for path " + path, i, n.getDouble(), 0);
    }

    private void assertStartTime(String path, Date s) {
        String dataName = path + "/Started";
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null && s == null)
            return;

        assertTrue(d instanceof DateData);
        DateData w = (DateData) d;
        assertEquals(s, w.getValue());
    }

    private static class MockApprover implements TimeLoggingApprover {

        public boolean approve = true;

        public boolean isTimeLoggingAllowed(String path) {
            return approve;
        }

    }

}
