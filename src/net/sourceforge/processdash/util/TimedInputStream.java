// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A filter that ensures some other input stream is closed properly.
 * 
 * When an input stream is opened, it is sometimes very important that the
 * stream be closed properly.  (For example, if a FileInputStream is not
 * closed properly, it may prevent other logic from writing to that file
 * until the process terminates.)
 * 
 * However, sometimes an object creates an input stream, then hands it to
 * another object for processing.  The stream creator cannot be certain that
 * the client will close it properly.  This class acts as a janitor: if the
 * input stream is not closed within a certain period of time, this class
 * will close the stream on behalf of the misbehaving client.
 * 
 * If the client using a TimedInputStream does not finish its work within the
 * allotted time, subsequent reads may throw an IOException.  The implication
 * is that the client needs to perform its work in a timely manner.  The
 * constructor for this class allows a time limit to be set.
 * 
 */
public class TimedInputStream extends FilterInputStream {

    private TimerTask cleanupTask;


    public TimedInputStream(InputStream in) {
        this(in, 30000);
    }

    public TimedInputStream(InputStream in, long timeDelay) {
        super(in);
        cleanupTask = new CleanupTask();
        CLOSE_TIMER.schedule(cleanupTask, timeDelay);
    }


    @Override
    public void close() throws IOException {
        super.close();
        cleanupTask.cancel();
    }


    private class CleanupTask extends TimerTask {

        @Override
        public void run() {
            try {
                close();
            } catch (Throwable t) {
            }
        }

    }

    private static final Timer CLOSE_TIMER = new Timer(
            "TimedInputStream-Janitor", true);

}
