// Copyright (C) 2007 Tuma Solutions, LLC
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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * This simple executor wraps another executor, and provides a {@link #drain()}
 * method which will wait until all queued commands have completed execution.
 * 
 * Note that the wrapped executor can be shared with other clients.  The
 * {@link #drain()} method in this object will wait until all tasks registered
 * through the {@link #execute(Runnable)} method of this object have finished.
 * 
 * @author Tuma
 */
public class DrainableExecutor implements Executor {

    private Executor delegate;

    private TaskCounter taskCounter;

    public DrainableExecutor(Executor delegate) {
        this.delegate = delegate;
        this.taskCounter = new TaskCounter();
    }

    public void execute(Runnable command) throws RejectedExecutionException {
        synchronized (taskCounter) {
            delegate.execute(new CountedRunnable(command));
            taskCounter.increment();
        }
    }

    public int getRemainingCount() {
        return taskCounter.getCount();
    }

    public void drain() {
        taskCounter.waitForZero();
    }

    private class TaskCounter {

        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized void decrement() {
            count--;
            if (count == 0)
                notifyAll();
        }

        public synchronized int getCount() {
            return count;
        }

        public synchronized void waitForZero() {
            while (count > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private class CountedRunnable implements Runnable {
        Runnable command;

        public CountedRunnable(Runnable command) {
            this.command = command;
        }

        public void run() {
            try {
                command.run();
            } finally {
                taskCounter.decrement();
            }
        }

        public String toString() {
            return command.toString();
        }

    }

}
