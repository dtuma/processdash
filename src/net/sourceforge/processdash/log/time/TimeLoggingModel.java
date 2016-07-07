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

import java.beans.PropertyChangeListener;
import java.util.List;

import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.log.SaveableDataSource;

public interface TimeLoggingModel extends SaveableDataSource {

    public static final String RECENT_PATHS_PROPERTY = "recentPaths";

    public static final String PAUSED_PROPERTY = "paused";

    public static final String ACTIVE_TASK_PROPERTY = "activeTaskModel.path";

    /**
     * Add a listener for changes to properties reflecting the current logging
     * state.
     * 
     * @param listener
     *            the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remove a listener for changes to properties reflecting the current
     * logging state.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * @return true if the timer is currently paused (not running).
     */
    public boolean isPaused();

    /**
     * Pause or restart the timer
     */
    public void setPaused(boolean paused);

    /**
     * Stop the timer if it is running.
     */
    public void stopTiming();

    /**
     * Start the timer if it is not running, and time logging is allowed for the
     * currently active task
     */
    public void startTiming();

    /**
     * @return true if time logging is allowed for the currently active task.
     */
    public boolean isLoggingAllowed();

    /**
     * Returns a list of paths where time has been logged recently.
     * 
     * The most recent entries will be at the beginning of the list.
     */
    public List getRecentPaths();

    /**
     * Initialize the list of paths where time has been logged recently.
     */
    public void setRecentPaths(List c);

    /**
     * @return the maximum number of entries which should be retained in the
     *         recent paths list. (Older entries will be removed automatically.)
     */
    public int getMaxRecentPathsRetained();

    /**
     * Set the maximum number of entries which should be retained in the recent
     * paths list.
     */
    public void setMaxRecentPathsRetained(int maxRecentPathsRetained);

    /**
     * @return the model describing the active task
     */
    public ActiveTaskModel getActiveTaskModel();

    /**
     * Set the model describing the active task
     */
    public void setActiveTaskModel(ActiveTaskModel model);

    /**
     * @return the current time multiplier in effect
     */
    public double getMultiplier();

    /**
     * Multiply clock time by some amount when creating time log entries
     */
    public void setMultiplier(double multiplier);

    /**
     * Update state based on some other time log entry, which has been added
     * by another client.
     */
    public void handleExternalAddition(TimeLogEntry externallyAddedEntry);

}
