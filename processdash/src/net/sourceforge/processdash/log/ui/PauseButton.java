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


package net.sourceforge.processdash.log.ui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.Icon;

import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.*;
import net.sourceforge.processdash.log.*;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.SoundClip;
import net.sourceforge.processdash.ui.help.*;
import net.sourceforge.processdash.ui.lib.*;
import pspdash.InternalSettings;
import pspdash.PSPDashboard;
import pspdash.Settings;
import pspdash.Timer;
import pspdash.data.DataRepository;
import pspdash.data.SaveableData;
import pspdash.data.DateData;
import pspdash.data.DoubleData;


public class PauseButton extends DropDownButton implements ActionListener {
    PSPDashboard parent = null;
    Icon pause_icon = null;
    Icon continue_icon = null;
    boolean showCurrent = false;
    boolean paused = true;
    Timer stopwatch = null;
    PropertyKey currentPhase = null;
    String timeElementName = null;
    SoundClip timingSound = null;
    int maxNumHistoryItems = 10;
    private String pause_string, continue_string, pause_tip, continue_tip;

    private javax.swing.Timer activeRefreshTimer = null;
    int refreshIntervalMillis = MILLIS_PER_MINUTE; // default: one minute
    private static final int FAST_REFRESH_INTERVAL = 5 * 1000;

    public PauseButton(PSPDashboard dash) {
        super();
        PCSH.enableHelp(this, "PlayPause");
        PCSH.enableHelpKey(getMenu(), "PlayPause");
        loadUserSettings();

        Resources res = Resources.getDashBundle("pspdash.PSPDashboard");
        pause_string = res.getString("Pause_String");
        pause_tip = res.getString("Pause_Tip");
        continue_string = res.getString("Continue_String");
        continue_tip = res.getString("Continue_Tip");

        try {
            pause_icon = DashboardIconFactory.getPauseIcon();
            continue_icon = DashboardIconFactory.getContinueIcon();
        } catch (Exception e) {
            pause_icon = continue_icon = null;
        }
        updateAppearance();
        getButton().setMargin(new Insets(1,2,1,2));
        getButton().setFocusPainted(false);
        getButton().addActionListener(this);
        setRunFirstMenuOption(false);
        parent = dash;


        activeRefreshTimer =
            new javax.swing.Timer(refreshIntervalMillis, this);
        activeRefreshTimer.setInitialDelay
            (Math.min(MILLIS_PER_MINUTE, refreshIntervalMillis) + 1000);
        activeRefreshTimer.start();

        // Load the audio clip
        if (!"true".equalsIgnoreCase(Settings.getVal("pauseButton.quiet"))) {
            timingSound = new SoundClip(getClass().getResource("timing.wav"));
        } else
            timingSound = new SoundClip(null);

        dash.getContentPane().add(this);
    }

    private static final int MILLIS_PER_MINUTE = 60 * 1000;

    private void updateAppearance() {
        if (pause_icon == null)
            getButton().setText
                (showCurrent == paused ? pause_string : continue_string);
        else
            getButton().setIcon
                (showCurrent == paused ? pause_icon : continue_icon);

        getButton().setToolTipText(paused ? continue_tip : pause_tip);
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activeRefreshTimer) {
            saveCurrentTimeLogEntry();

            // Possibly commit the current row.  If a user clicks
            // pause, then goes home for the evening, and comes back
            // the next day and starts working on the same activity,
            // it really isn't meaningful to log 15 hours of interrupt
            // time.  So if the interrupt time passes some "critical
            // point", just commit the current row.
            if (paused && stopwatch != null) {
                double interruptMinutes = stopwatch.runningMinutesInterrupt();
                if (interruptMinutes > 5.0 &&
                    interruptMinutes > (0.25 * elapsedMinutes))
                    releaseCurrentTimeLogEntry();
            }

        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) e.getSource();
            if (setPath(item.getText()))
                cont();
            else
                getMenu().remove(item);
        } else {
            if (paused) cont(); else pause();
        }
    }

    public void pause() {
        paused = true;
        if (stopwatch != null) {
            stopwatch.stop();
            saveCurrentTimeLogEntry();
        }
        updateAppearance();
    }

    public void cont() {
        paused = false;
        if (stopwatch == null) {
            stopwatch = newTimer();
        } else {
            stopwatch.start();
        }
        updateAppearance();

        timingSound.play();
    }

    public void setCurrentPhase(PropertyKey newCurrentPhase) {
        if (currentTimeLogEntry != null)
            addToMenu(currentPhase.path());

        boolean needCleanup = (entryHasBeenSaved > 1);
        releaseCurrentTimeLogEntry();
        if (needCleanup)
            cleanupTimeLog();

        if (newCurrentPhase != null) {
            currentPhase = newCurrentPhase;
            timeElementName = DataRepository.createDataName
                (currentPhase.path(), "Time");
        }

        if (TimeLog.timeLoggingAllowed
            (currentPhase, parent.getHierarchy(), parent.getData())) {

            getButton().setEnabled(true);
            if (!paused) cont();

        } else {
            pause();
            getButton().setEnabled(false);
        }
    }

    public boolean setPath(String path) {
        if (parent.hierarchy.setPath(path))
            return true;
        else {
            // They've gone and edited their hierarchy, and the
            // requested node no longer exists! Beep to let them
            // know there was a problem, then remove this item
            // from the history list so they can't select it again
            // in the future.
            pause();
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    public boolean setPhase(String phase) {
        if (parent.hierarchy.setPhase(phase))
            return true;
        else {
            // They have navigated to a new portion of the hierarchy,
            // where the current phase is not present.  Beep to let them
            // know there was a problem.
            pause();
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    public void addToMenu(String path) {
        JMenu menu = getMenu();
        JMenuItem itemToAdd = null, oneItem;

        // if the menu already contains a menu item for this path, remove
        // that menu item.
        for (int i = menu.getItemCount();  i-- > 0; ) {
            oneItem = menu.getItem(i);
            if (oneItem != null && path.equals(oneItem.getText())) {
                itemToAdd = oneItem;
                menu.remove(i);
                break;
            }
        }

        if (itemToAdd == null) {
            itemToAdd = new JMenuItem(path);
            itemToAdd.addActionListener(this);
        }

        menu.insert(itemToAdd, 0);

        while (menu.getItemCount() > maxNumHistoryItems)
            menu.remove(maxNumHistoryItems);
    }

    public void quit() {
        saveUserSettings();
    }

    private static boolean WRITE_ZERO =
        "true".equalsIgnoreCase(Settings.getVal("timeLog.writeZero"));

    /** The time log entry for the current activity. */
    TimeLogEntry currentTimeLogEntry = null;

    /** How many times has the current time log entry been saved to the time
     *  log file? */
    int entryHasBeenSaved = 0;

    /** How much elapsed stopwatch time has been saved to the data
     *  repository?  Note: since we round items off when adding them
     *  to the repository, this value will rarely ever equal
     *  elapsedMinutes.
     */
    double savedElapsedMinutes = 0.0;

    /** How many minutes are currently on the stopwatch? (We need to
     *  keep track of this separately, because the TimeLogEntry only
     *  keeps information accurate to a minute.
     */
    double elapsedMinutes = 0.0;

    /** Update the current time log entry with information from the
     *  stopwatch. Creates the current time log entry if it doesn't
     *  already exist.
     */
    private synchronized void updateCurrentTimeLogEntry() {
        if (stopwatch == null) return;

        double previousElapsedMinutes = elapsedMinutes;
        elapsedMinutes = stopwatch.minutesElapsedDouble();
        long roundedElapsedMinutes = (long) (elapsedMinutes + 0.5);

        if (currentTimeLogEntry == null) {
            if (elapsedMinutes < 1.0 && !WRITE_ZERO) return;
            if (previousElapsedMinutes == elapsedMinutes) return;

            currentTimeLogEntry = new TimeLogEntry
                (currentPhase,
                 stopwatch.createTime,
                 roundedElapsedMinutes,
                 stopwatch.minutesInterrupt());
            entryHasBeenSaved = 0;

            // When we began timing this phase, we set the timer for a
            // fast, 5 second interval (so we could catch the top of
            // the minute as it went by).  This allows us to create
            // the new time log entry within 5 seconds of the one
            // minute point.  Once we've created the new time log
            // entry, slow the timer back down to the user-requested
            // refresh interval.
            activeRefreshTimer.setDelay(refreshIntervalMillis);

        } else {
            currentTimeLogEntry.setElapsedTime(roundedElapsedMinutes);
            currentTimeLogEntry.setInterruptTime(stopwatch.minutesInterrupt());
        }
    }

    /** Write the current time log entry out to the file.
     *  Create a current time log entry if it doesn't exist.
     */
    private synchronized void saveCurrentTimeLogEntry() {
        updateCurrentTimeLogEntry();

        if (currentTimeLogEntry == null) return;  // nothing to save.

        String timeLogFilename = parent.getTimeLog();
        if (timeLogFilename != null && timeLogFilename.length() != 0) try {
                                // write an entry to the time log.
            String log_msg = currentTimeLogEntry.toString();
            FileOutputStream timeLogFile =
                new FileOutputStream(timeLogFilename, true);

            if (entryHasBeenSaved > 0)
                log_msg = TimeLog.CONTINUATION_FLAG + log_msg;

            parent.addToTimeLogEditor (currentTimeLogEntry);
            timeLogFile.write(log_msg.getBytes());
            timeLogFile.close();
            entryHasBeenSaved++;

            if (entryHasBeenSaved > 30)
                cleanupTimeLog();
        } catch (IOException ioe) {
            System.err.println("Couldn't update time log " + timeLogFilename);
            ioe.printStackTrace();
        }

        // Need to make changes to the data elements.
        SaveableData d = parent.getData().getValue(timeElementName);
                                // Calculate the amount of time in this phase,
                                // NOT COUNTING the current time log entry.
        double previousTime = (d == null ? 0.0 : ((DoubleData) d).getDouble())
            - savedElapsedMinutes;
                                // Calculate the amount of time in this phase,
                                // INCLUDING the current time log entry.
        long currentMinutes =
            (long) (previousTime + currentTimeLogEntry.getElapsedTime());
        savedElapsedMinutes = currentMinutes - previousTime;
        parent.getData().putValue(timeElementName,
                             new DoubleData(currentMinutes, false));
        //System.out.println("updating time to " + currentMinutes);

        if (stopwatch != null)
            parent.hierarchy.workPerformed
                (new DateData(stopwatch.createTime, true));
    }

    private void releaseCurrentTimeLogEntry() {
        saveCurrentTimeLogEntry();
        activeRefreshTimer.setDelay(refreshIntervalMillis);

        stopwatch = (paused ? null : newTimer());
        currentTimeLogEntry = null;
        entryHasBeenSaved = 0;
        savedElapsedMinutes = elapsedMinutes = 0.0;
    }

    private Timer newTimer() {
        // The instructions below will cause the timer to wait for 61
        // seconds (starting right now), then fire once every 5
        // seconds.  This quick firing interval allows it to catch the
        // one minute mark fairly closely.
        activeRefreshTimer.setDelay(FAST_REFRESH_INTERVAL);
        activeRefreshTimer.restart();

        return new Timer();
    }

    public void maybeReleaseEntry(TimeLogEntry tle) {
        if (tle == null ||
            (currentTimeLogEntry != null &&
             currentTimeLogEntry.isSimilarTo(tle)))
            setCurrentPhase(null);
    }

    private void cleanupTimeLog() {
        String timeLogFilename = parent.getTimeLog();
        if (timeLogFilename != null || timeLogFilename.length() != 0) try {
            TimeLog log = new TimeLog();
            log.read(timeLogFilename);
            log.save(timeLogFilename);
            if (entryHasBeenSaved > 0)
                entryHasBeenSaved = 1;
            //System.err.println("Cleaned up time log.");
        } catch (IOException ioe) {}
    }

    private void loadUserSettings() {
        // Load the user setting for button appearance
        showCurrent = "true".equalsIgnoreCase(Settings.getVal
                                              ("pauseButton.showCurrent"));

        // Load the user setting for refresh interval
        String refreshInterval = Settings.getVal("timelog.updateInterval");
        if (refreshInterval != null) try {
            refreshIntervalMillis = (int)
                (Double.parseDouble(refreshInterval) * MILLIS_PER_MINUTE);
        } catch (NumberFormatException nfe) {}

        // Load time multiplier setting
        double multiplier = 1.0;
        String mult = Settings.getVal("timer.multiplier");
        if (mult != null) try {
            multiplier = Double.parseDouble(mult);
        } catch (NumberFormatException nfe) {}
        refreshIntervalMillis = (int) (refreshIntervalMillis / multiplier);

        // Load the user setting for history size
        String historySize = Settings.getVal("pauseButton.historySize");
        if (historySize != null) try {
            maxNumHistoryItems = Integer.parseInt(historySize);
            if (maxNumHistoryItems < 1)
                maxNumHistoryItems = 10;
        } catch (NumberFormatException nfe) {}

        // Load the saved history list, if it is available.
        String history = Settings.getVal("pauseButton.historyList");
        if (history != null) {
            StringTokenizer tok = new StringTokenizer(history, "\t");
            while (tok.hasMoreTokens())
                addToMenu(tok.nextToken());
        }
    }

    private void saveUserSettings() {
        // the only item that could have changed is the history list.
        JMenu menu = getMenu();
        JMenuItem oneItem;
        StringBuffer setting = new StringBuffer();

        // Walk through the history items in reverse order, and build
        // up a list.
        for (int i = menu.getItemCount();  i-- > 0; ) {
            oneItem = menu.getItem(i);
            setting.append(oneItem.getText());
            if (i > 0) setting.append("\t");
        }

        // save the setting.
        InternalSettings.set("pauseButton.historyList", setting.toString());
    }

}
