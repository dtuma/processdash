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
    private static final String pause_string = "Stop";
    private static final String continue_string = " Go ";

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
        // GridBagConstraints g = new GridBagConstraints();
        // g.gridx = 0;
        // g.gridy = 0;
        // g.fill = g.BOTH;
        // dash.layout.setConstraints(this, g);
        dash.getContentPane().add(this);
    }

    private void updateAppearance() {
        if (pause_icon == null)
            setText(pause_string);
        else
            setIcon(showCurrent == paused ? pause_icon : continue_icon);

        setToolTipText(paused ? "Paused. Press to continue."
                              : "Timing. Press to pause.");
    }


    public void actionPerformed(ActionEvent e) {
        if (paused) cont(); else pause();
    }

    public void pause() {
        paused = true;
        if (stopwatch != null)
            stopwatch.stop();
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
    }

    public void setCurrentPhase(PropertyKey newCurrentPhase) {
        if (stopwatch != null) {
            stopwatch.stop();
            String timeLogFilename = parent.getTimeLog();

                                // If there is a time log, and
            if (timeLogFilename != null && timeLogFilename.length() != 0 &&
                                // at least one minute has elapsed, or
                (stopwatch.minutesElapsed() > 0 ||
                                // the user has requested zero-duration time
                                // log entries to be saved,
                 "true".equalsIgnoreCase
                 (Settings.getVal("timeLog.writeZero")))) try {

                                // write an entry to the time log.
                FileOutputStream timeLogFile =
                    new FileOutputStream(timeLogFilename, true);
                TimeLogEntry tle = new TimeLogEntry
                    (currentPhase,
                     stopwatch.createTime,
                     stopwatch.minutesElapsed(),
                     stopwatch.minutesInterrupt());
                String log_msg = tle.toString();
                parent.addToTimeLogEditor (tle);
                                // write to the time log.
                timeLogFile.write(log_msg.getBytes());
                timeLogFile.close();

                String timeName = currentPhase.path() + "/Time";

                SaveableData d = parent.data.getValue(timeName);
                DoubleData time;
                if ((d != null) && (d instanceof DoubleData))
                    time = new DoubleData(((DoubleData)d).getDouble() +
                                          stopwatch.minutesElapsed());
                else
                    time = new DoubleData(stopwatch.minutesElapsed());
                time.setEditable(false);
                parent.data.putValue(timeName, time);

                parent.hierarchy.workPerformed
                    (new DateData(stopwatch.createTime, true));
            } catch (IOException e) {
                System.err.println("Couldn't update time log " +
                                   timeLogFilename);
                e.printStackTrace();
            }
            stopwatch = null;
        }

        currentPhase = newCurrentPhase;
        if (!paused) cont();
    }
}
