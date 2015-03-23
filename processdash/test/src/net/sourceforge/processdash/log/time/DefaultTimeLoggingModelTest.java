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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.log.IDSource;

public class DefaultTimeLoggingModelTest extends AbstractTimeLogTest implements
        PropertyChangeListener, TimeLogListener {

    protected static final String PATH1 = "/Project/Requirements/Utility bus/RRW/Reqts";

    protected static final String PATH2 = "/Project/Dy4-181 BSP Build/Test Task/Test";

    protected static final String PATH3 = "/Project/Gen2 MPA Build for VxWorks/Test Task/Test";

    protected static final String PATH4 = "/Non Project/Dy4-181 BSP Build/Test Task/Test";

    protected List eventsReceived;

    protected DataContext data;

    protected DashHierarchy hier;

    protected TimeLoggingApprover approver;

    protected MockActiveTaskModel activeTaskModel;

    protected ModifiableTimeLog timeLog;

    protected DefaultTimeLoggingModel timeLoggingModel;

    protected Map expectedTimes;

    protected void setUp() throws Exception {
        super.setUp();
        eventsReceived = new LinkedList();
        expectedTimes = new HashMap();
        data = new MockDataContext();
        hier = new DashHierarchy(null);
        hier.loadXML(openFile("hier3.xml"), new DashHierarchy(null));

        createModels();

        activeTaskModel = new MockActiveTaskModel(hier);
        timeLoggingModel.setActiveTaskModel(activeTaskModel);

        timeLog.addTimeLogListener(this);
        timeLoggingModel.addPropertyChangeListener(this);
    }

    protected void tearDown() throws Exception {
        timeLog.removeTimeLogListener(this);
        super.tearDown();
    }

    protected  void createModels() throws Exception {
        approver = new TimeLoggingApprover() {
            public boolean isTimeLoggingAllowed(String path) {
                return (path != null && path.startsWith("/Project/"));
            }
        };

        File timeLogModsFile = File.createTempFile("timeModsTestFile", ".xml");
        timeLog = new TimeLogModifications(new EmptyBaseTimeLog(),
                timeLogModsFile, new DummyIdSource());

        timeLoggingModel = new DefaultTimeLoggingModel(timeLog, approver);

        TimingMetricsRecorder recorder = new TimingMetricsRecorder(timeLog,
                data, hier, approver);
    }

    public void testPlayPause() throws Exception {
        setPath(PATH1, null, null);

        startTiming();
        setStopwatch(6, 0);
        expectTime(PATH1, 6);

        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(1, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 6, 0, null,
                ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);
        stopTiming();
        assertTimeLogEvent(tle);
        startTiming();
        setStopwatch(9, 1);
        expectTime(PATH1, 9);
        tle.setElapsedTime(9);
        tle.setInterruptTime(1);
        assertTimeLogEvent(tle);

        assertTrue(timeLoggingModel.isDirty());
        timeLoggingModel.saveData();
        assertFalse(timeLoggingModel.isDirty());
    }

    public void testChangePathsWithoutStopping() throws Exception {
        List recentPaths = new LinkedList();

        setPath(PATH1, null, null);
        recentPaths.add(PATH1);
        startTiming();
        setStopwatch(6, 0);
        expectTime(PATH1, 6);
        assertTimes(expectedTimes);
        eventsReceived.clear();

        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(1, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 6, 0, null,
                ChangeFlagged.ADDED);

        // with timer still running, change path
         setPath(PATH2, recentPaths, tle);
         expectTime(PATH2, 0);
         // don't allow any time to elapse, change path again
         setPath(PATH3, null, null);
         assertTrue(eventsReceived.isEmpty());
         // simulate time passing and watch for the right event
         setStopwatch(2, 0);
         expectTime(PATH3, 2);
         tle = new MutableTimeLogEntryVO(2, PATH3,
                 timeLoggingModel.stopwatch.getCreateTime(), 2, 0, null,
                 ChangeFlagged.ADDED);
         assertTimeLogEvent(tle);

         recentPaths.add(0, PATH3);
         setPath(PATH2, recentPaths, tle);
    }

    public void testExternalChangesToCurrentEntry() {
        // start logging time to PATH1
        setPath(PATH1, null, null);
        startTiming();
        Date timeOne = timeLoggingModel.stopwatch.getCreateTime();
        setStopwatch(6, 0);
        expectTime(PATH1, 6);
        ChangeFlaggedTimeLogEntry tle = new TimeLogEntryVO(1, PATH1,
                timeOne, 6, 0, null, ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);

        // make a modification to the current time log entry - add 10 minutes
        ChangeFlaggedTimeLogEntry mod = new TimeLogEntryVO(1, null, null, 10,
                1, null, ChangeFlagged.MODIFIED);
        timeLog.addModification(mod);
        expectTime(PATH1, 16);
        assertTimeLogEvent(mod);
        // on the next timer tick, we should see the additional time present
        // in the time log entry.
        TimeLogEntry expectedTle = new TimeLogEntryVO(1, PATH1,
                timeOne, 16, 1, null, ChangeFlagged.ADDED);
        timeLoggingModel.handleTimerEvent();
        assertTimeLogEvent(expectedTle);

        // let some REAL time pass before we delete the time log entry.  This
        // allows us to test that the timer was restarted at a different point
        // in time.
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { }

        // now, externally delete the current time log entry
        ChangeFlaggedTimeLogEntry del = new TimeLogEntryVO(1, null, null, 0, 0,
                null, ChangeFlagged.DELETED);
        timeLog.addModification(del);
        expectTime(PATH1, 0);
        assertTimeLogEvent(del);
        assertTrue(eventsReceived.isEmpty());

        // the timer should still be running
        assertFalse(timeLoggingModel.isPaused());
        // although the stopwatch should have been restarted
        assertEquals(0.0, timeLoggingModel.stopwatch.minutesElapsedDouble(), 0);
        assertEquals(0.0, timeLoggingModel.stopwatch.minutesInterruptDouble(), 0);
        // the restarted stopwatch should have a new create time
        Date timeTwo = timeLoggingModel.stopwatch.getCreateTime();
        assertFalse(timeOne.equals(timeTwo));
        // let some time elapse.  Check to make certain a *new* entry gets logged.
        setStopwatch(2, 0);
        expectTime(PATH1, 2);
        tle = new TimeLogEntryVO(2, PATH1, timeTwo, 2, 0, null,
                ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);
        // stop the timer.
        stopTiming();
        assertTimeLogEvent(tle);

        // delete the second time log entry, too (while the timer isn't running)
        del = new TimeLogEntryVO(2, null, null, 0, 0, null,
                ChangeFlagged.DELETED);
        timeLog.addModification(del);
        expectTime(PATH1, 0);
        assertTimeLogEvent(del);
        assertTrue(eventsReceived.isEmpty());

        // the timer should still be stopped, and the stopwatch should have
        // been discarded.
        assertTrue(timeLoggingModel.isPaused());
        assertNull(timeLoggingModel.stopwatch);
    }

    public void testStopTimingOnChangeToInvalidPath() {
        // log some time to PATH1
        setPath(PATH1, null, null);
        startTiming();
        setStopwatch(6, 0);
        expectTime(PATH1, 6);
        eventsReceived.clear();

        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(1, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 6, 0, null,
                ChangeFlagged.ADDED);

        // with timer still running, change path
         assertTrue(activeTaskModel.setPath(PATH4));
         // timing isn't allowed here
         assertFalse(timeLoggingModel.isLoggingAllowed());
         // the recent paths list has changed
         assertPropertyChangeEvent(timeLoggingModel,
                 TimeLoggingModel.RECENT_PATHS_PROPERTY, null, null);
         assertEquals(Collections.singletonList(PATH1), timeLoggingModel
                .getRecentPaths());
         // a time log event for the previous path has been posted
         assertTimeLogEvent(tle);
         // since timing isn't allowed here, the "paused" property is
         // automatically changed to true
         assertPropertyChangeEvent(timeLoggingModel, "paused", Boolean.FALSE,
                 Boolean.TRUE);
         assertTrue(timeLoggingModel.isPaused());
         // finally, we receive the change event for the active task
         assertPropertyChangeEvent(timeLoggingModel,
                TimeLoggingModel.ACTIVE_TASK_PROPERTY, PATH1, PATH4);

         assertTrue(eventsReceived.isEmpty());
    }

    public void testRecentPaths() {
        timeLoggingModel.setMaxRecentPathsRetained(2);
        assertEquals(2, timeLoggingModel.getMaxRecentPathsRetained());

        List recentPaths = new LinkedList();

        activeTaskModel.setPath(PATH1);
        timeLoggingModel.startTiming();
        timeLoggingModel.stopwatch.setElapsed(120);
        timeLoggingModel.handleTimerEvent();
        activeTaskModel.setPath(PATH2);
        recentPaths.add(0, PATH1);
        timeLoggingModel.stopwatch.setElapsed(120);
        timeLoggingModel.handleTimerEvent();
        activeTaskModel.setPath(PATH3);
        recentPaths.add(0, PATH2);
        assertEquals(recentPaths, timeLoggingModel.getRecentPaths());
        timeLoggingModel.stopwatch.setElapsed(120);
        timeLoggingModel.handleTimerEvent();
        activeTaskModel.setPath(PATH4);
        recentPaths.add(0, PATH3);
        recentPaths.remove(2);
        assertEquals(recentPaths, timeLoggingModel.getRecentPaths());

        eventsReceived.clear();
        recentPaths.clear();
        recentPaths.add("foo");
        recentPaths.add("bar");
        recentPaths.add("baz");
        timeLoggingModel.setRecentPaths(recentPaths);
        assertPropertyChangeEvent(timeLoggingModel,
                TimeLoggingModel.RECENT_PATHS_PROPERTY, null, null);
        assertEquals(recentPaths, timeLoggingModel.getRecentPaths());
    }

    public void testReleaseEntryAfterLongTerminalInterrupt() throws Exception {
        // accelerate the timer
        timeLoggingModel.setMultiplier(600);
        assertEquals(600, timeLoggingModel.getMultiplier(), 0);

        // log some time against PATH1
        setPath(PATH1, null, null);
        startTiming();
        setStopwatch(10, 0);
        expectTime(PATH1, 10);

        // stop the timer and ensure that time gets logged.
        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(1, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 10, 0, null,
                ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);
        stopTiming();
        assertTimeLogEvent(tle);

        // pause to simulate a long delay after the timer was stopped
        Thread.sleep(1000);
        timeLoggingModel.handleTimerEvent();
        do {
            // we may have received many events due to "timer ticks" that
            // were accelerated at 600x the normal rate - clear them all
            assertTimeLogEvent(tle);
        } while (eventsReceived.isEmpty() == false);

        // the timer should still be paused, and the stopwatch should have
        // been reset
        assertTrue(timeLoggingModel.isPaused());
        assertNull(timeLoggingModel.stopwatch);

        // when we restart timing, it should be creating a new time log entry
        // instead of adding to the old one.
        startTiming();
        setStopwatch(20, 0);
        expectTime(PATH1, 30);
        tle = new MutableTimeLogEntryVO(2, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 20, 0, null,
                ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);
    }

    public void testActiveTaskModelChanges() {
        // FIXME_TIMELOG:  need lots of tests here

        /*
         * when the hierarchy is edited and changed, we will be assigned a
         * new active task model.  Any of the following could be true:
         *   - business as usual; no changes to active task or its attributes
         *   - the path of the active task has been renamed while the timer
         *     is running. (Keep timing?  Start a new entry?)
         *   - the active task has not changed, but the approver's permission
         *     has.
         *   - the active task has changed; timing is still allowed here
         *   - the active task has changed; timing no longer allowed
         */
    }

    public void testBoundaryConditions() {
        // stop the timer when it is already stopped.  Should have no effect.
        timeLoggingModel.setPaused(true);
        assertTrue(eventsReceived.isEmpty());

        // log some time to get the model in a common state for normal
        // dashboard operation
        setPath(PATH1, null, null);
        startTiming();
        setStopwatch(6, 0);
        expectTime(PATH1, 6);
        MutableTimeLogEntry tle = new MutableTimeLogEntryVO(1, PATH1,
                timeLoggingModel.stopwatch.getCreateTime(), 6, 0, null,
                ChangeFlagged.ADDED);
        assertTimeLogEvent(tle);
        stopTiming();
        eventsReceived.clear();

        // "change" the active task model to exercise listener registration
        timeLoggingModel.setActiveTaskModel(activeTaskModel);
        // since the active task is still the same, no changes should have been
        // received
        assertTrue(eventsReceived.isEmpty());

        // disconnect the active task model, and make certain we no longer
        // respond to its changes
        timeLoggingModel.setActiveTaskModel(null);
        assertNull(timeLoggingModel.getActiveTaskModel());
        assertTrue(activeTaskModel.setPath(PATH2));
        assertTrue(eventsReceived.isEmpty());

        // check that disconnecting our listener to the timeLoggingModel
        // causes us no longer to receive events
        timeLoggingModel.setRecentPaths(new LinkedList());
        assertPropertyChangeEvent(timeLoggingModel,
                TimeLoggingModel.RECENT_PATHS_PROPERTY, null, null);
        timeLoggingModel.removePropertyChangeListener(this);
        timeLoggingModel.setRecentPaths(Collections.singletonList("foo"));
        assertTrue(eventsReceived.isEmpty());

        // send a nonsense, unrelated property change event and make certain
        // it gets ignored
        PropertyChangeEvent pce = new PropertyChangeEvent(this, "foo", "bar",
                "baz");
        timeLoggingModel.externalChangeListener.propertyChange(pce);
        assertTrue(eventsReceived.isEmpty());
    }

    private void setPath(String path, List recentPaths, TimeLogEntry expectedTle) {
        String oldPath = activeTaskModel.getPath();
        assertTrue(activeTaskModel.setPath(path));
        assertEquals(approver.isTimeLoggingAllowed(path), timeLoggingModel
                .isLoggingAllowed());

        if (recentPaths != null) {
            assertPropertyChangeEvent(timeLoggingModel,
                    TimeLoggingModel.RECENT_PATHS_PROPERTY, null, null);
            assertEquals(recentPaths, timeLoggingModel.getRecentPaths());
        }
        if (expectedTle != null) {
            assertTimeLogEvent(expectedTle);
        }
        if (oldPath == null || !oldPath.equals(path))
            assertPropertyChangeEvent(timeLoggingModel,
                    TimeLoggingModel.ACTIVE_TASK_PROPERTY, oldPath, path);
    }

    private void startTiming() {
        timeLoggingModel.setPaused(false);
        assertPropertyChangeEvent(timeLoggingModel, "paused", Boolean.TRUE,
                Boolean.FALSE);
        assertFalse(timeLoggingModel.isPaused());
    }

    private void stopTiming() {
        timeLoggingModel.setPaused(true);
        assertPropertyChangeEvent(timeLoggingModel, "paused", Boolean.FALSE,
                Boolean.TRUE);
        assertTrue(timeLoggingModel.isPaused());
    }

    public void propertyChange(PropertyChangeEvent evt) {
        eventsReceived.add(evt);
    }

    public void timeLogChanged(TimeLogEvent evt) {
        eventsReceived.add(evt);
    }

    protected void expectTime(String path, int totalTime) {
        expectedTimes.put(path, new Integer(totalTime));
    }

    protected void assertTimes(Map times) {
        for (Iterator iter = times.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String path = (String) e.getKey();
            Integer elapsed = (Integer) e.getValue();
            assertTime(path, elapsed.intValue());
        }
    }

    protected void assertTime(String path, int i) {
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

    private void assertTimeLogEvent(Object expectedEntry) {
        assertFalse("expected to find an event", eventsReceived.isEmpty());
        Object evt = eventsReceived.remove(0);
        assertTrue("event should be a TimeLogEvent",
                evt instanceof TimeLogEvent);
        TimeLogEvent e = (TimeLogEvent) evt;
        assertSame(timeLog, e.getSource());
        assertEquals(expectedEntry, e.getTimeLogEntry());
        assertTimes(expectedTimes);
    }

    private void assertPropertyChangeEvent(Object source, String propName,
            Object oldVal, Object newVal) {
        Object evt = eventsReceived.remove(0);
        assertTrue("event should be a PropertyChangeEvent",
                evt instanceof PropertyChangeEvent);
        PropertyChangeEvent e = (PropertyChangeEvent) evt;
        assertSame(source, e.getSource());
        assertEquals(propName, e.getPropertyName());
        assertEquals(oldVal, e.getOldValue());
        assertEquals(newVal, e.getNewValue());
    }

    private void setStopwatch(int elapsedMinutes, int interruptMinutes) {
        timeLoggingModel.stopwatch.setElapsed(elapsedMinutes * 60);
        timeLoggingModel.stopwatch.setInterrupt(interruptMinutes * 60);
        timeLoggingModel.handleTimerEvent();
    }

    private static final class DummyIdSource implements IDSource {
        private int id = 0;

        public long getNextID() {
            return ++id;
        }
    }
}
