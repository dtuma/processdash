// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.util.*;

public class Timer {
    public Date createTime = null;
    private Date startTime = null;
    private Date stopTime = null;
    private long elapsedTime = 0;        // represented in seconds
    private long interruptTime = 0;      // represented in seconds
    private double multiplier = 1.0;

    Timer(boolean running) {
        createTime = new Date();
        if (running) { startTime = createTime; }
        initMultiplier();
    }

    Timer() {
        createTime = new Date();
        startTime = createTime;
        initMultiplier();
    }

    private void initMultiplier() {
        multiplier = 1.0;
        String mult = Settings.getVal("timer.multiplier");
        if (mult != null) try {
            multiplier = Double.parseDouble(mult);
        } catch (NumberFormatException nfe) {}
    }

    public void start() {
        if (startTime == null) {
            startTime = new Date();
            if (stopTime != null) {
                interruptTime = interruptTime +
                    ((startTime.getTime() - stopTime.getTime()) / 1000);
            }
            stopTime = null;
        }
    }

    public void stop() {
        if (startTime != null) {
            stopTime = new Date();
            elapsedTime = elapsedTime +
                ((stopTime.getTime() - startTime.getTime()) / 1000);
            startTime = null;
        }
    }

    public boolean toggle() {
        if (startTime == null)
            start();
        else
            stop();

        return (startTime != null);
    }

    public boolean isRunning() {
        return (startTime != null);
    }

    public void reset() {
        startTime = null;
        stopTime = null;
        elapsedTime = 0;
        interruptTime = 0;
    }

    public void setElapsed(long seconds) {
        if (isRunning()) {
            stop();
            elapsedTime = (long) (seconds / multiplier);
            start();
        } else
            elapsedTime = (long) (seconds / multiplier);
    }

    public double minutesElapsedDouble() {
        if (startTime == null) {
            return (double)elapsedTime * multiplier / 60.0;
        } else {
            Date now = new Date();
            return (double)(((now.getTime() - startTime.getTime()) / 1000) +
                           elapsedTime) * multiplier / 60.0;
        }
    }

    public long minutesElapsed() {
        return (long)minutesElapsedDouble();
    }

    public double runningMinutesInterrupt() {
        if (startTime != null) {
            return (double)interruptTime * multiplier / 60.0;
        } else {
            Date now = new Date();
            return (double)(((now.getTime() - stopTime.getTime()) / 1000) +
                            interruptTime) * multiplier / 60.0;
        }
    }

    public double minutesInterruptDouble() {
        return (double)interruptTime * multiplier / 60.0;
    }

    public long minutesInterrupt() {
        return (long)minutesInterruptDouble();
    }

}
