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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;


/** Class to manage voluntary CPU throttling limits for a thread.
 * 
 * <p>When a non-time-sensitive (but computation intensive) task runs, it is
 * desirable for the task to limit its use of CPU resources, to avoid
 * irritating the user.  Such tasks can voluntarily limit their CPU use by:</p>
 * 
 * <ul>
 *   <li>Making an intitial call to {@link #beginThrottling(double)
 *       ThreadThrottler.beginThrottling() } before starting the task</li>
 *   <li>Calling {@link #tick() ThreadThrottler.tick() } as often as
 *       possible during the computation-intensive work</li>
 *   <li>Making a final call to {@link #endThrottling(double)
 *       ThreadThrottler.endThrottling() } after completing the task</li>
 * </ul>
 * 
 * <p>Calls to {@link #tick() } may sleep for a moment to reduce the CPU usage of
 * the current thread.  (As such, they should not be called while the thread
 * holds any sensitive locks.)</p>
 * 
 * <p>Throttling is only performed for the thread that called
 * {@link #beginThrottling(double) }.  Child threads that are started will not
 * be throttled.  When constructing a Thread object, note that you should call
 * {@link #beginThrottling(double) } in your run() method, not in your
 * constructor. (Making the call in your constructor will initiate throttling
 * in the thread that called the constructor!)</p>
 * 
 * <p>If no throttling percentage is in effect for the current thread,
 * {@link #tick() } will have no effect, and will return immediately.</p>
 * 
 * @author Tuma
 *
 */
public class ThreadThrottler {

    protected ThreadThrottler previous;

    protected long lastTickTime;

    protected double percentage;

    protected double multiplier;


    private ThreadThrottler(ThreadThrottler previous, double percentage) {
        this.previous = previous;
        this.lastTickTime = System.currentTimeMillis();
        setPercentage(percentage);
    }

    protected void setPercentage(double p) {
        p = Math.min(p, 1.0);
        p = Math.max(p, 0.05);
        this.percentage = p;

        p = getEffectivePercentage();
        this.multiplier = (1 - p) / p;
        this.lastTickTime = System.currentTimeMillis();
    }

    protected double getEffectivePercentage() {
        if (previous == null)
            return percentage;
        else
            return Math.min(percentage, previous.getEffectivePercentage());
    }

    protected void doTick() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;
        long timeToWait = (long) (elapsed * multiplier);
        timeToWait = Math.min(timeToWait, 2000);

        if (timeToWait > 0) {
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {}
            lastTickTime = System.currentTimeMillis();
        }
    }



    private static final boolean DISABLED = Boolean
            .getBoolean(ThreadThrottler.class.getName() + ".disabled");

    private static ThreadLocal THROTTLERS = new ThreadLocal();

    private static double DEFAULT_THROTTLING_PERCENTAGE = 0.2;


    /** Call this method as often as possible during the execution of a
     * throttled operation.
     * 
     * If no throttling operation is in effect, this method will do nothing.
     */
    public static void tick() {
        if (DISABLED)
            return;

        ThreadThrottler t = (ThreadThrottler) THROTTLERS.get();
        if (t != null)
            t.doTick();
    }




    public static double getDefaultThrottlingPercentage() {
        return DEFAULT_THROTTLING_PERCENTAGE;
    }

    public static void setDefaultThrottlingPercentage(double percentage) {
        DEFAULT_THROTTLING_PERCENTAGE = percentage;
    }

    /** Indicate the start of a throttled operation on the current thread.
     * 
     * <p>Throttling will continue until the {@link #endThrottling()} method is
     * called, or until the thread dies - whichever comes first.  In general,
     * clients of this class should use a finally clause to ensure that they
     * call {@link #endThrottling()}, unless they specifically created a thread
     * that should be throttled for its entire lifetime.</p>
     * 
     * <p>Throttling operations can nest;  the effective throttling percentage
     * will be the minimum percentage of all enclosing operations.</p>
     * 
     * @param percentage The maximum percentage of the CPU that this task
     *    should consume.  If a lower throttling percentage is already in
     *    effect for the current thread, this will not increase the effective
     *    percentage.  (It will still open a new bracket, though, which must
     *    be closed with {@link #endThrottling()}.)
     */
    public static void beginThrottling(double percentage) {
        if (DISABLED)
            return;

        ThreadThrottler currentThrottler = (ThreadThrottler) THROTTLERS.get();
        ThreadThrottler newThrottler = new ThreadThrottler(currentThrottler,
                percentage);
        THROTTLERS.set(newThrottler);
    }

    /** Begin throttling, using the default throttling percentage.
     * 
     * {@see #setDefaultThrottlingPercentage(double)}
     */
    public static void beginThrottling() {
        beginThrottling(DEFAULT_THROTTLING_PERCENTAGE);
    }


    /** Change the maximum percentage of the current throttling operation.
     * 
     * If no throttling operation is in effect, one will be started.
     * 
     * @param percentage the new maximum cpu percentage for the current thread.
     * 
     * @return true if a throttling operation was started, false if an existing
     *     operation was altered.
     */
    public static boolean setThrottling(double percentage) {
        if (DISABLED)
            return false;

        ThreadThrottler t = (ThreadThrottler) THROTTLERS.get();
        if (t == null) {
            t = new ThreadThrottler(null, percentage);
            THROTTLERS.set(t);
            return true;
        } else {
            t.setPercentage(percentage);
            return false;
        }
    }


    /** Indicate the end of a throttled operation that was previously started
     * via {@link #beginThrottling}.
     */
    public static void endThrottling() {
        if (DISABLED)
            return;

        ThreadThrottler currentThrottler = (ThreadThrottler) THROTTLERS.get();
        if (currentThrottler != null) {
            ThreadThrottler previousThrottler = currentThrottler.previous;
            THROTTLERS.set(previousThrottler);
        }
    }

}
