// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class DisplayThreadDump extends TinyCGIBase {

    @Override
    protected void writeHeader() {
        out.print("Content-type: text/plain; charset=" + charset + "\r\n\r\n");
        out.flush();
    }

    @Override
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        out.write("Process Dashboard JVM Thread Dump:\n\n");

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean
                .getThreadInfo(threadMXBean.getAllThreadIds(), 100);

        long[] deadlocks = threadMXBean.findDeadlockedThreads();
        if (deadlocks != null && deadlocks.length > 0) {
            out.write("--------------------------------------\n");
            out.write("DEADLOCKED THREADS DETECTED:");
            for (long threadID : deadlocks) {
                out.write("\n    #" + threadID + ": ");
                for (ThreadInfo threadInfo : threadInfos) {
                    if (threadInfo.getThreadId() == threadID) {
                        out.write(" \"" + threadInfo.getThreadName() + '"');
                        break;
                    }
                }
            }
            out.write("\n--------------------------------------\n\n");
        }

        for (ThreadInfo threadInfo : threadInfos) {
            out.write(threadInfo.toString());
        }
    }

    /*

    static {
        createDeadlock();
    }

    private static void createDeadlock() {
        Object a = new Object(), b = new Object();
        Deadlocker aa = new Deadlocker(a, b);
        Deadlocker bb = new Deadlocker(b, a);
        aa.start();
        bb.start();
    }

    private static class Deadlocker extends Thread {
        Object a, b;

        public Deadlocker(Object a, Object b) {
            super("Deadlocker");
            this.a = a;
            this.b = b;
        }

        @Override
        public void run() {
            try {
                synchronized (a) {
                    Thread.sleep(100);
                    synchronized (b) {
                        System.out.println("Got both monitors");
                        b.wait();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

     */

}
