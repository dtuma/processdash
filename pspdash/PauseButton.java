// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.sampled.*;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import pspdash.data.SaveableData;
import pspdash.data.DateData;
import pspdash.data.DoubleData;


public class PauseButton extends DropDownButton implements ActionListener {
    PSPDashboard parent = null;
    ImageIcon pause_icon = null;
    ImageIcon continue_icon = null;
    boolean showCurrent = false;
    boolean paused = true;
    Timer stopwatch = null;
    PropertyKey currentPhase = null;
    String timeElementName = null;
    Clip timingSound = null;
    int maxNumHistoryItems = 10;
    private static final String pause_string = "Stop";
    private static final String continue_string = " Go ";

    private javax.swing.Timer activeRefreshTimer = null;
    int refreshIntervalMillis = MILLIS_PER_MINUTE; // default: one minute
    private static final int FAST_REFRESH_INTERVAL = 5 * 1000;

    PauseButton(PSPDashboard dash) {
        super();
        showCurrent = "true".equalsIgnoreCase(Settings.getVal
                                              ("pauseButton.showCurrent"));
        try {
            pause_icon = new ImageIcon(getClass().getResource("pause.gif"));
            continue_icon = new ImageIcon(getClass().getResource("continue.gif"));
        } catch (Exception e) {
            pause_icon = continue_icon = null;
        }
        updateAppearance();
        getButton().setMargin(new Insets(1,2,1,2));
        getButton().setFocusPainted(false);
        getButton().addActionListener(this);
        setRunFirstMenuOption(false);
        parent = dash;

        String refreshInterval = Settings.getVal("timelog.updateInterval");
        if (refreshInterval != null) try {
            refreshIntervalMillis = (int)
                (Double.parseDouble(refreshInterval) * MILLIS_PER_MINUTE);
        } catch (NumberFormatException nfe) {}

        activeRefreshTimer =
            new javax.swing.Timer(refreshIntervalMillis, this);
        activeRefreshTimer.setInitialDelay(MILLIS_PER_MINUTE + 1000);
        activeRefreshTimer.start();

        // Load the audio clip
        if (!"true".equalsIgnoreCase(Settings.getVal("pauseButton.quiet"))) {
            timingSound = loadAudioClip("timing.wav");
        }

        // Load the user setting for history size
        String userSetting = Settings.getVal("pauseButton.historySize");
        if (userSetting != null) try {
            maxNumHistoryItems = Integer.parseInt(userSetting);
            if (maxNumHistoryItems < 1)
                maxNumHistoryItems = 10;
        } catch (NumberFormatException nfe) {}

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

        getButton().setToolTipText(paused ? "Paused. Press to continue."
                                   : "Timing. Press to pause.");
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
            if (!setPath(item.getText()))
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

        playClip(timingSound);
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
            timeElementName = currentPhase.path() + "/Time";
        }

        if (!paused) cont();
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
            currentTimeLogEntry.minutesElapsed  = roundedElapsedMinutes;
            currentTimeLogEntry.minutesInterrupt= stopwatch.minutesInterrupt();
        }
    }

    /** Write the current time log entry out to the file.
     *  Create a current time log entry if it doesn't exist.
     */
    private synchronized void saveCurrentTimeLogEntry() {
        updateCurrentTimeLogEntry();

        if (currentTimeLogEntry == null) return;  // nothing to save.

        String timeLogFilename = parent.getTimeLog();
        if (timeLogFilename != null || timeLogFilename.length() != 0) try {
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
        SaveableData d = parent.data.getValue(timeElementName);
                                // Calculate the amount of time in this phase,
                                // NOT COUNTING the current time log entry.
        double previousTime = (d == null ? 0.0 : ((DoubleData) d).getDouble())
            - savedElapsedMinutes;
                                // Calculate the amount of time in this phase,
                                // INCLUDING the current time log entry.
        long currentMinutes =
            (long) (previousTime + currentTimeLogEntry.minutesElapsed);
        savedElapsedMinutes = currentMinutes - previousTime;
        parent.data.putValue(timeElementName,
                             new DoubleData(currentMinutes, false));
        //System.out.println("updating time to " + currentMinutes);

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

    private Clip loadAudioClip(String filename) {
        Clip result = null;
        try {
            AudioInputStream soundFile = AudioSystem.getAudioInputStream
                (getClass().getResource(filename));
            AudioFormat soundFormat = soundFile.getFormat();
            int bufferSize = (int) (soundFile.getFrameLength() *
                                    soundFormat.getFrameSize());
            DataLine.Info info = new DataLine.Info
                (Clip.class, soundFile.getFormat(), bufferSize);
            result = (Clip) AudioSystem.getLine(info);
            result.open(soundFile);
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private void playClip(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
}
