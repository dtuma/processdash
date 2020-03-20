// Copyright (C) 2003-2020 Tuma Solutions, LLC
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

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.Stopwatch;

public class DefaultTimeLoggingModel implements TimeLoggingModel {

    ModifiableTimeLog timeLog;

    TimeLoggingApprover approver;

    DataContext data;

    ActiveTaskModel activeTaskModel;

    boolean paused = true;

    Stopwatch stopwatch = null;

    PropertyKey currentPhase = null;

    private Date activeTaskDate = null;

    private boolean pauseInProgress;

    private double multiplier = 1.0;

    PropertyChangeListener externalChangeListener;

    private PropertyChangeSupport propertyChangeSupport;

    private List recentPaths = new LinkedList();

    private int maxRecentPathsRetained = 10;

    private javax.swing.Timer activeRefreshTimer = null;

    int refreshIntervalMillis = MILLIS_PER_MINUTE; // default: one minute

    private static final int FAST_REFRESH_INTERVAL = 5 * 1000;

    private static Logger logger = Logger.getLogger
        (DefaultTimeLoggingModel.class.getName());

    public DefaultTimeLoggingModel(ModifiableTimeLog timeLog,
            TimeLoggingApprover approver, DataContext data) {
        this.timeLog = timeLog;
        this.approver = approver;
        this.data = data;

        activeRefreshTimer = new javax.swing.Timer(refreshIntervalMillis,
                (ActionListener) EventHandler.create(ActionListener.class,
                        this, "handleTimerEvent"));
        activeRefreshTimer.setInitialDelay(Math.min(MILLIS_PER_MINUTE,
                refreshIntervalMillis) + 50);

        externalChangeListener = new ExternalChangeListener();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public void setActiveTaskModel(ActiveTaskModel model) {
        if (activeTaskModel != model) {
            logger.fine("setting active task model");
            if (activeTaskModel != null)
                activeTaskModel.removePropertyChangeListener(externalChangeListener);
            activeTaskModel = model;
            if (activeTaskModel != null) {
                activeTaskModel.addPropertyChangeListener(externalChangeListener);
                setCurrentPhase(activeTaskModel.getNode());
            }
        }
    }

    public ActiveTaskModel getActiveTaskModel() {
        return activeTaskModel;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        if (paused)
            stopTiming();
        else
            startTiming();
    }

    public boolean isLoggingAllowed() {
        return currentPhase != null
                && (approver == null
                        || approver.isTimeLoggingAllowed(currentPhase.path()));
    }

    /** Returns a list of paths where time has been logged recently.
     * 
     * The most recent entries will be at the beginning of the list.
     */
    public List getRecentPaths() {
        return recentPaths;
    }

    public void setRecentPaths(List c) {
        logger.fine("setting recent paths");
        recentPaths.clear();
        recentPaths.addAll(c);
        propertyChangeSupport.firePropertyChange(RECENT_PATHS_PROPERTY, null,
                null);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        logger.config("setting timing multiplier to " + multiplier);
        this.multiplier = multiplier;
        this.refreshIntervalMillis = (int) (MILLIS_PER_MINUTE / multiplier);
        this.activeRefreshTimer.setInitialDelay(refreshIntervalMillis);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public boolean isDirty() {
        return currentTimeLogEntry != null;
    }

    public void saveData() {
        setCurrentPhase(null);
    }


    private static final int MILLIS_PER_MINUTE = 60 * 1000;

    public void handleTimerEvent() {
        logger.finer("handleTimerEvent");
        saveCurrentTimeLogEntry(false);
        maybeReleaseEntryWithLongTerminalInterrupt();
    }

    private void maybeReleaseEntryWithLongTerminalInterrupt() {
        // Possibly commit the current row. If a user clicks
        // pause, then goes home for the evening, and comes back
        // the next day and starts working on the same activity,
        // it really isn't meaningful to log 15 hours of interrupt
        // time. So if the interrupt time passes some "critical
        // point", just commit the current row.
        if (paused && stopwatch != null && currentTimeLogEntry != null) {
            double interruptMinutes = stopwatch.runningMinutesInterrupt();
            double elapsedMinutes = currentTimeLogEntry.getElapsedTime();
            if (interruptMinutes > 5.0
                    && interruptMinutes > (0.25 * elapsedMinutes)) {
                logger.finer("interrupt time threshhold reached; " +
                        "releasing current time log entry");
                saveAndReleaseCurrentTimeLogEntry();
            }
        }
    }

    public void stopTiming() {
        try {
            logger.fine("stopTiming");
            if (paused == false) {
                paused = pauseInProgress = true;
                propertyChangeSupport.firePropertyChange(PAUSED_PROPERTY,
                    false, true);
            }
            if (stopwatch != null) {
                stopwatch.stop();
                saveCurrentTimeLogEntry(false);
            }
        } finally {
            pauseInProgress = false;
        }
    }

    public void startTiming() {
        logger.fine("startTiming");
        if (approver != null
                && approver.isTimeLoggingAllowed(currentPhase.path()) == false) {
            logger.log(Level.FINER, "timing not allowed for path {0}",
                    currentPhase.path());
            stopTiming();
            return;
        }

        if (paused == true) {
            maybeReleaseEntryWithLongTerminalInterrupt();
            paused = false;
            propertyChangeSupport.firePropertyChange(PAUSED_PROPERTY, true, false);
        }
        if (stopwatch == null) {
            stopwatch = newTimer();
        } else {
            stopwatch.start();
        }
    }

    protected void setCurrentPhase(PropertyKey newCurrentPhase) {
        PropertyKey oldPhase = currentPhase;
        if (newCurrentPhase != null && newCurrentPhase.equals(oldPhase))
            return;

        logger.log(Level.FINE, "setting current phase to {0}",
                (newCurrentPhase == null ? "null" : newCurrentPhase.path()));

        if (currentTimeLogEntry != null)
            addToRecentPaths(currentPhase.path());

        saveAndReleaseCurrentTimeLogEntry();

        if (newCurrentPhase != null)
            currentPhase = newCurrentPhase;
        activeTaskDate = new Date();

        if (!paused)
            startTiming();

        propertyChangeSupport.firePropertyChange(ACTIVE_TASK_PROPERTY,
                getPhasePath(oldPhase), getPhasePath(currentPhase));
    }

    private String getPhasePath(PropertyKey phase) {
        return phase == null ? null : phase.path();
    }

    protected void addToRecentPaths(String path) {
        recentPaths.remove(path);
        recentPaths.add(0, path);
        if (recentPaths.size() > maxRecentPathsRetained)
            recentPaths = new LinkedList(recentPaths.subList(0,
                    maxRecentPathsRetained));
        propertyChangeSupport.firePropertyChange(RECENT_PATHS_PROPERTY, null, null);
    }

    public int getMaxRecentPathsRetained() {
        return maxRecentPathsRetained;
    }

    public void setMaxRecentPathsRetained(int maxRecentPathsRetained) {
        this.maxRecentPathsRetained = maxRecentPathsRetained;
    }


    /** The time log entry ID for the current activity. */
    MutableTimeLogEntry currentTimeLogEntry = null;

    private boolean updatingCurrentTimeLogEntry = false;

    /**
     * Update the current time log entry with information from the stopwatch.
     * Creates the current time log entry if it doesn't already exist.
     */
    private synchronized void updateCurrentTimeLogEntry() {
        if (stopwatch == null)
            return;

        logger.fine("updating current time log entry");
        double elapsedMinutes = stopwatch.minutesElapsedDouble();
        if (shouldForceTimeLogEntry(elapsedMinutes))
            elapsedMinutes = 1.0;
        long roundedElapsedMinutes = (long) (elapsedMinutes + 0.5);

        if (currentTimeLogEntry == null) {
            if (elapsedMinutes < 1.0) {
                logger.fine("less than one minute elapsed; no entry created");
                return;
            }

            long id = timeLog.getNextID();
            currentTimeLogEntry = new MutableTimeLogEntryVO(id, currentPhase
                    .path(), stopwatch.getCreateTime(), roundedElapsedMinutes,
                    stopwatch.minutesInterrupt(), null, ChangeFlagged.ADDED);
            currentTimeLogEntry
                    .addPropertyChangeListener(externalChangeListener);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Created time log entry, id=" + id + ", elapsed="
                        + roundedElapsedMinutes + ", path="
                        + currentPhase.path());
            }

            // When we began timing this phase, we set the timer for a
            // fast, 5 second interval (so we could catch the top of
            // the minute as it went by). This allows us to create
            // the new time log entry within 5 seconds of the one
            // minute point. Once we've created the new time log
            // entry, slow the timer back down to the user-requested
            // refresh interval.
            activeRefreshTimer.setDelay(refreshIntervalMillis);

        } else {
            try {
                updatingCurrentTimeLogEntry = true;
                currentTimeLogEntry.setElapsedTime(roundedElapsedMinutes);
                currentTimeLogEntry.setInterruptTime(stopwatch
                        .minutesInterrupt());
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Updating time log entry, id="
                            + currentTimeLogEntry.getID() + ", elapsed="
                            + roundedElapsedMinutes + ", interrupt="
                            + stopwatch.minutesInterrupt());
                }
            } finally {
                updatingCurrentTimeLogEntry = false;
            }
        }
    }

    /**
     * In some usage scenarios, an individual needs to mark several tasks
     * complete in a row, but still reflect the fact that they "worked" on them.
     * If the timer is running when the user marks a task complete, and no time
     * has ever been logged against that task, round the current time log entry
     * up to 1 minute if it isn't there already.
     */
    private boolean shouldForceTimeLogEntry(double elapsedMinutes) {
        // If some time has already been logged, there is no need to force a
        // time log entry.
        if (elapsedMinutes > 1.0)
            return false;

        // If the timer is paused (and not because it is being programmatically
        // paused at this very moment), don't force a time log entry.
        if (paused && !pauseInProgress)
            return false;

        // Check a user setting to see if this functionality is disabled.
        if (!Settings.getBool("pauseButton.forceCompletedTaskTime", true))
            return false;

        // Check the data repository to see if the current task has been
        // flagged as one that should never receive "forced" time log entries.
        String path = currentPhase.path();
        SaveableData sd = DataRepository.getInheritableValue(data,
            new StringBuffer(path), "Force_Completed_Task_Time");
        if (sd != null && sd.getSimpleValue().test() == false)
            return false;

        // If time has been logged against this task in the past, don't force
        // a new time log entry.
        String dataName = DataRepository.createDataName(path, "Time");
        if (sd != null && sd.getSimpleValue().test())
            return false;

        // Check to see if the current task was *just* marked complete. If so,
        // force a time log entry.
        dataName = DataRepository.createDataName(path, "Completed");
        sd = data.getSimpleValue(dataName);
        if (sd instanceof DateData) {
            Date completed = ((DateData) sd).getValue();
            long age = System.currentTimeMillis() - completed.getTime();
            if (age < 1000) {
                logger.fine("Forcing 1 minute time log entry for "
                        + "completed task " + path);
                return true;
            }
        }

        return false;
    }

    /**
     * Write the current time log entry out to the file. Create a current time
     * log entry if it doesn't exist.
     */
    private synchronized void saveCurrentTimeLogEntry(boolean release) {
        updateCurrentTimeLogEntry();

        if (currentTimeLogEntry == null)
            return; // nothing to save.

        logger.fine("saving current time log entry");

        if (release)
            timeLog.addModification(new TimeLogEntryVO(currentTimeLogEntry));
        else
            timeLog.addModification((ChangeFlaggedTimeLogEntry) currentTimeLogEntry);
    }

    private void saveAndReleaseCurrentTimeLogEntry() {
        saveCurrentTimeLogEntry(true);
        activeRefreshTimer.setDelay(refreshIntervalMillis);
        releaseCurrentTimeLogEntry();
    }

    private void releaseCurrentTimeLogEntry() {
        stopwatch = (paused ? null : newTimer());
        if (currentTimeLogEntry != null) {
            logger.fine("releasing current time log entry");
            currentTimeLogEntry
                    .removePropertyChangeListener(externalChangeListener);
            currentTimeLogEntry = null;
        }
    }

    protected Stopwatch newTimer() {
        logger.finer("creating new timer");
        // The instructions below will cause the timer to wait for 61
        // seconds (starting right now), then fire once every 5
        // seconds. This quick firing interval allows it to catch the
        // one minute mark fairly closely.
        activeRefreshTimer.setDelay(FAST_REFRESH_INTERVAL);
        activeRefreshTimer.restart();

        Stopwatch result = new Stopwatch();
        result.setMultiplier(multiplier);
        return result;
    }

    public void handleExternalAddition(TimeLogEntry tle) {
        // Check to see if this event represents a time log entry that started
        // after our stopwatch was created
        Date otherStart = tle.getStartTime();
        if (stopwatch != null && otherStart.after(stopwatch.getCreateTime())) {
            // another client started a time log entry after us. Update our
            // state to ensure that our time log entry doesn't overlap.
            stopwatch.cancelTimingAsOf(otherStart);
            stopTiming();
            saveAndReleaseCurrentTimeLogEntry();
        }

        // possibly change the active task, to reflect the activity the external
        // client was logging time against
        if (activeTaskDate == null || activeTaskDate.before(otherStart)) {
            if (getActiveTaskModel().setPath(tle.getPath()))
                activeTaskDate = otherStart;
        }
    }


    private class ExternalChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (currentTimeLogEntry != null
                    && evt.getSource() == currentTimeLogEntry) {

                // Some external agent has made a change to our current time
                // log entry.

                if (((ChangeFlagged) currentTimeLogEntry).getChangeFlag() == ChangeFlagged.DELETED) {
                    logger.finer("current time log entry was deleted");
                    releaseCurrentTimeLogEntry();

                } else if ("path".equals(evt.getPropertyName())) {
                    logger.finer("path of current time log entry was changed");
                    releaseCurrentTimeLogEntry();

                } else if (updatingCurrentTimeLogEntry == false
                        && stopwatch != null) {

                    if ("elapsedTime".equals(evt.getPropertyName())) {
                        logger.finer("Updating elapsed time based on external "
                                + "changes to current time log entry");
                        stopwatch.setElapsed(currentTimeLogEntry
                                .getElapsedTime() * 60);
                        // we just reset the stopwatch to an even number of
                        // minutes.  resync the refresh timer with this new
                        // top-of-the-minute mark.
                        activeRefreshTimer.restart();

                    } else if ("interruptTime".equals(evt.getPropertyName())) {
                        logger.finer("updating interrupt time based on "
                                + "external changes to current time log entry");
                        stopwatch.setInterrupt(currentTimeLogEntry
                                .getInterruptTime() * 60);
                    }
                }

            } else if (evt.getSource() == activeTaskModel) {
                // Some external agent has changed the currently selected path.
                logger.finer("activeTaskModel changed currently selected task");
                setCurrentPhase(activeTaskModel.getNode());
            }
        }
    }

}
