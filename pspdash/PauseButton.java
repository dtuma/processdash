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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.sampled.*;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import pspdash.data.SaveableData;
import pspdash.data.DateData;
import pspdash.data.DoubleData;


public class PauseButton extends JButton implements ActionListener {
    PSPDashboard parent = null;
    ImageIcon pause_icon = null;
    ImageIcon continue_icon = null;
    boolean showCurrent = false;
    boolean paused = true;
    Timer stopwatch = null;
    PropertyKey currentPhase = null;
    String timeElementName = null;
    Clip timingSound = null;
    private static final String pause_string = "Stop";
    private static final String continue_string = " Go ";

    private javax.swing.Timer activeRefreshTimer = null;

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
        setMargin (new Insets (1,2,1,2));
        parent = dash;
        addActionListener(this);

        int refreshIntervalMillis = MILLIS_PER_MINUTE; // default: one minute

        String refreshInterval = Settings.getVal("timelog.updateInterval");
        if (refreshInterval != null) try {
            refreshIntervalMillis = (int)
                (Double.parseDouble(refreshInterval) * MILLIS_PER_MINUTE);
        } catch (NumberFormatException nfe) {}

        activeRefreshTimer =
            new javax.swing.Timer(refreshIntervalMillis, this);
        activeRefreshTimer.start();

        // Load the audio clip
        if (!"true".equalsIgnoreCase(Settings.getVal("pauseButton.quiet"))) {
            timingSound = loadAudioClip("timing.wav");
        }

        dash.getContentPane().add(this);
    }

    private static final int MILLIS_PER_MINUTE = 60 * 1000;

    private void updateAppearance() {
        if (pause_icon == null)
            setText(showCurrent == paused ? pause_string : continue_string);
        else
            setIcon(showCurrent == paused ? pause_icon : continue_icon);

        setToolTipText(paused ? "Paused. Press to continue."
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
                long interruptMinutes = stopwatch.minutesInterrupt();
                if (interruptMinutes > 5 &&
                    interruptMinutes > (0.25 * elapsedMinutes))
                    releaseCurrentTimeLogEntry();
            }

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
            stopwatch = new Timer();
        } else {
            stopwatch.start();
        }
        updateAppearance();

        playClip(timingSound);
    }

    public void setCurrentPhase(PropertyKey newCurrentPhase) {
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
            if (roundedElapsedMinutes == 0 && !WRITE_ZERO) return;
            if (previousElapsedMinutes == elapsedMinutes) return;

            currentTimeLogEntry = new TimeLogEntry
                (currentPhase,
                 stopwatch.createTime,
                 roundedElapsedMinutes,
                 stopwatch.minutesInterrupt());
            entryHasBeenSaved = 0;

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

        stopwatch = (paused ? null : new Timer());
        currentTimeLogEntry = null;
        entryHasBeenSaved = 0;
        savedElapsedMinutes = elapsedMinutes = 0.0;
    }

    public void maybeReleaseEntry(TimeLogEntry tle) {
        if (tle != null &&
            currentTimeLogEntry != null &&
            currentTimeLogEntry.isSimilarTo(tle))
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
            System.err.println("Cleaned up time log.");
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
