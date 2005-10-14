// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.time;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.Stopwatch;

public class DefaultTimeLoggingModel implements TimeLoggingModel {

    ModifiableTimeLog timeLog;

    TimeLoggingApprover approver;

    ActiveTaskModel activeTaskModel;

    boolean paused = true;

    Stopwatch stopwatch = null;

    PropertyKey currentPhase = null;

    private double multiplier = 1.0;

    PropertyChangeListener externalChangeListener;

    private PropertyChangeSupport propertyChangeSupport;

    private List recentPaths = new LinkedList();

    private int maxRecentPathsRetained = 10;

    private javax.swing.Timer activeRefreshTimer = null;

    int refreshIntervalMillis = MILLIS_PER_MINUTE; // default: one minute

    private static final int FAST_REFRESH_INTERVAL = 5 * 1000;

    public DefaultTimeLoggingModel(ModifiableTimeLog timeLog,
            TimeLoggingApprover approver) {
        this.timeLog = timeLog;
        this.approver = approver;

        activeRefreshTimer = new javax.swing.Timer(refreshIntervalMillis,
                (ActionListener) EventHandler.create(ActionListener.class,
                        this, "handleTimerEvent"));
        activeRefreshTimer.setInitialDelay(Math.min(MILLIS_PER_MINUTE,
                refreshIntervalMillis) + 1000);

        externalChangeListener = new ExternalChangeListener();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public void setActiveTaskModel(ActiveTaskModel model) {
        if (activeTaskModel != null)
            activeTaskModel.removePropertyChangeListener(externalChangeListener);
        activeTaskModel = model;
        if (activeTaskModel != null) {
            activeTaskModel.addPropertyChangeListener(externalChangeListener);
            setCurrentPhase(activeTaskModel.getNode());
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
        recentPaths.clear();
        recentPaths.addAll(c);
        propertyChangeSupport.firePropertyChange(RECENT_PATHS_PROPERTY, null,
                null);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
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
        saveCurrentTimeLogEntry(false);

        // Possibly commit the current row. If a user clicks
        // pause, then goes home for the evening, and comes back
        // the next day and starts working on the same activity,
        // it really isn't meaningful to log 15 hours of interrupt
        // time. So if the interrupt time passes some "critical
        // point", just commit the current row.
        if (paused && currentTimeLogEntry != null) {
            double interruptMinutes = stopwatch.runningMinutesInterrupt();
            double elapsedMinutes = currentTimeLogEntry.getElapsedTime();
            if (interruptMinutes > 5.0
                    && interruptMinutes > (0.25 * elapsedMinutes))
                releaseCurrentTimeLogEntry();
        }
    }

    public void stopTiming() {
        if (paused == false) {
            paused = true;
            propertyChangeSupport.firePropertyChange(PAUSED_PROPERTY, false, true);
        }
        if (stopwatch != null) {
            stopwatch.stop();
            saveCurrentTimeLogEntry(false);
        }
    }

    public void startTiming() {
        if (approver != null
                && approver.isTimeLoggingAllowed(currentPhase.path()) == false) {
            stopTiming();
            return;
        }

        if (paused == true) {
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

        if (currentTimeLogEntry != null)
            addToRecentPaths(currentPhase.path());

        releaseCurrentTimeLogEntry();

        if (newCurrentPhase != null)
            currentPhase = newCurrentPhase;

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

        double elapsedMinutes = stopwatch.minutesElapsedDouble();
        long roundedElapsedMinutes = (long) (elapsedMinutes + 0.5);

        if (currentTimeLogEntry == null) {
            if (elapsedMinutes < 1.0)
                return;

            long id = timeLog.getNextID();
            currentTimeLogEntry = new MutableTimeLogEntryVO(id, currentPhase
                    .path(), stopwatch.getCreateTime(), roundedElapsedMinutes,
                    stopwatch.minutesInterrupt(), null, ChangeFlagged.ADDED);
            currentTimeLogEntry
                    .addPropertyChangeListener(externalChangeListener);

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
            } finally {
                updatingCurrentTimeLogEntry = false;
            }
        }
    }

    /**
     * Write the current time log entry out to the file. Create a current time
     * log entry if it doesn't exist.
     */
    private synchronized void saveCurrentTimeLogEntry(boolean release) {
        updateCurrentTimeLogEntry();

        if (currentTimeLogEntry == null)
            return; // nothing to save.

        if (release)
            timeLog.addModification(new TimeLogEntryVO(currentTimeLogEntry));
        else
            timeLog.addModification((ChangeFlaggedTimeLogEntry) currentTimeLogEntry);
    }

    private void releaseCurrentTimeLogEntry() {
        saveCurrentTimeLogEntry(true);
        activeRefreshTimer.setDelay(refreshIntervalMillis);

        stopwatch = (paused ? null : newTimer());
        if (currentTimeLogEntry != null) {
            currentTimeLogEntry
                    .removePropertyChangeListener(externalChangeListener);
            currentTimeLogEntry = null;
        }
    }

    protected Stopwatch newTimer() {
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

    private class ExternalChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (currentTimeLogEntry != null
                    && evt.getSource() == currentTimeLogEntry) {

                // Some external agent has made a change to our current time
                // log entry.

                if (((ChangeFlagged) currentTimeLogEntry).getChangeFlag() == ChangeFlagged.DELETED
                        || "path".equals(evt.getPropertyName())) {
                    stopwatch = (paused ? null : newTimer());
                    currentTimeLogEntry.removePropertyChangeListener(this);
                    currentTimeLogEntry = null;

                } else if (updatingCurrentTimeLogEntry == false
                        && stopwatch != null) {
                    stopwatch
                            .setElapsed(currentTimeLogEntry.getElapsedTime() * 60);
                    stopwatch.setInterrupt(currentTimeLogEntry
                            .getInterruptTime() * 60);
                }

            } else if (evt.getSource() == activeTaskModel) {
                // Some external agent has changed the currently selected path.
                setCurrentPhase(activeTaskModel.getNode());
            }
        }
    }

}
