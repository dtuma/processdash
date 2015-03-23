// Copyright (C) 2007-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.ThreadThrottler;

import org.w3c.dom.Element;


/** This class manages the registration and execution of background tasks.
 * 
 * Background tasks represent work that can run asynchronously, and is not
 * time-sensitive.  Background tasks will typically be throttled such that
 * they do not use excessive system resources (e.g. CPU).
 * 
 * Background tasks can be defined in template.xml files. They can be run at
 * startup and potentially once an hour thereafter.
 * 
 * In addition, java clients can request for tasks to be run on the background
 * thread.
 * 
 * @author Tuma
 *
 */
public class BackgroundTaskManager {

    private static final String BACKGROUND_TASK_TAG = "background-task";
    private static final String CLASS_ATTR = "class";
    private static final String RUN_AT_STARTUP_ATTR = "runAtStartup";
    private static final String SETTING_PREFIX = "settingPrefix";
    private static final String WHEN_DEFAULT_ATTR = "whenDefault";
    private static final String DISABLE_SETTING_NAME = "disabledWith";
    private static final String ENABLE_SETTING_NAME = "enabledWith";
    private static final String SORT_ORDINAL = "ordinal";
    private static final String DEFAULT_SORT_ORDINAL = "1000";
    private static final long STUCK_TASK_ELAPSED_TIME =
        15 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;
    public static final String CPU_SETTING = "backgroundTasks.cpuPercent";




    private static final BackgroundTaskManager INSTANCE = new BackgroundTaskManager();

    private static DashboardPermission INIT_PERMISSION = new DashboardPermission(
            "backgroundTaskManager.initialize");
    private static DashboardPermission SUSPEND_PERMISSION = new DashboardPermission(
            "backgroundTaskManager.suspend");

    public synchronized static void initialize(DashboardContext dashContext) {
        INIT_PERMISSION.checkPermission();
        INSTANCE.setContext(dashContext);
    }

    public static BackgroundTaskManager getInstance() {
        return INSTANCE;
    }

    public void addTask(Runnable r) {
        addTask(null, r);
    }

    public void addTask(String id, Runnable r) {
        doAddTask(new UserTask(id, r));
    }

    /**
     * Stop the processing of background tasks for some period of time.
     *
     * @param delayMillis
     *            the number of milliseconds to stop processing.
     * @param waitMillis
     *            if less than or equal to zero, any currently running
     *            background task will be allowed to asynchronously run to
     *            completion. Otherwise, an attempt will be made to stop the
     *            current task, and this call will wait up to the given number
     *            of milliseconds for the task to finish. (If no task is
     *            currently running, this parameter is unused.)
     * @since 1.14.1
     */
    public void suspend(long delayMillis, long waitMillis) {
        doSuspend(delayMillis, waitMillis);
    }

    public boolean isSuspended() {
        return System.currentTimeMillis() < suspendUntil;
    }



    private List definedTasks;

    private List tasksAwaitingExecution;

    private boolean initialized;

    private BackgroundTaskExecutor currentTaskExecutor;

    private volatile long suspendUntil;

    private static final Logger logger = Logger
    .getLogger(BackgroundTaskManager.class.getName());


    private BackgroundTaskManager() {
        tasksAwaitingExecution = Collections.synchronizedList(new ArrayList());
        initialized = false;
        suspendUntil = -1;
    }

    private synchronized void setContext(DashboardContext dashContext) {
        if (initialized)
            throw new IllegalStateException("Initialization already performed");

        initialized = true;
        createBackgroundTasks(dashContext);
        if (!definedTasks.isEmpty()) {
            queueScheduledTasks(true);
            startPeriodicExecutor();
        }
        startExecutingTasks();
    }

    private void createBackgroundTasks(DashboardContext dashContext) {
        logger.finest("Creating background tasks");

        definedTasks = Collections.synchronizedList(new ArrayList());
        List configElements = ExtensionManager
                .getXmlConfigurationElements(BACKGROUND_TASK_TAG);
        for (Iterator i = configElements.iterator(); i.hasNext();) {
            Element configElem = (Element) i.next();
            try {
                BackgroundTask bt = new BackgroundTask(configElem, dashContext);
                logger.log(Level.FINEST, "Created background task {0}", bt);
                definedTasks.add(bt);
            } catch (Throwable t) {
                String message = "Unable to create background task "
                        + configElem.getAttribute(CLASS_ATTR) + " "
                        + ExtensionManager.getDebugDescriptionOfSource(configElem);
                logger.log(Level.SEVERE, message, t);
            }
        }
        Collections.sort(definedTasks);

        logger.log(Level.FINER, "Background tasks ({0}) created", new Integer(
                definedTasks.size()));
    }


    private void startPeriodicExecutor() {
        int millisPerHour = 60 /* minutes */* 60 /* seconds */* 1000 /* millis */;

        ActionListener periodicTaskInitiator = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queueScheduledTasks(false);
            }};
        Timer t = new Timer(millisPerHour, periodicTaskInitiator);
        t.start();
    }

    private synchronized void queueScheduledTasks(boolean isStartup) {
        queueLogMessage(isStartup ? "Running startup tasks"
                : "Running periodic tasks");

        int currentHourOfDay = Calendar.getInstance().get(
                Calendar.HOUR_OF_DAY);

        for (Iterator i = definedTasks.iterator(); i.hasNext();) {
            BackgroundTask task = (BackgroundTask) i.next();
            if (task.shouldRun(isStartup, currentHourOfDay))
                doAddTask(task);
        }

        queueLogMessage(isStartup ? "Startup tasks completed"
                : "Periodic tasks completed");
    }

    private void queueLogMessage(final String message) {
        doAddTask(new Runnable() {
            public void run() {
                logger.fine(message);
            }});
    }

    private synchronized void doAddTask(Runnable r) {
        tasksAwaitingExecution.add(r);
        if (initialized)
            startExecutingTasks();
    }

    private synchronized void startExecutingTasks() {
        if (currentTaskExecutor == null || !currentTaskExecutor.running)
            currentTaskExecutor = new BackgroundTaskExecutor();

        else if (currentTaskExecutor.appearsStuck()) {
            BackgroundTaskExecutor stuckThread = currentTaskExecutor;
            currentTaskExecutor = new BackgroundTaskExecutor();
            stuckThread.abort();
        }
    }

    private synchronized Runnable getNextTask() {
        if (tasksAwaitingExecution.isEmpty() || isSuspended()) {
            currentTaskExecutor = null;
            return null;
        } else
            return (Runnable) tasksAwaitingExecution.remove(0);
    }

    private void doSuspend(long delayMillis, long waitMillis) {
        SUSPEND_PERMISSION.checkPermission();
        BackgroundTaskExecutor bte;
        synchronized (this) {
            long newSuspendUntil = System.currentTimeMillis() + delayMillis;
            this.suspendUntil = Math.max(this.suspendUntil, newSuspendUntil);
            bte = currentTaskExecutor;
        }

        ActionListener taskRestarter = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startExecutingTasks();
            }};
        Timer t = new Timer((int) delayMillis + 5000, taskRestarter);
        t.setRepeats(false);
        t.start();

        if (bte != null) {
            if (waitMillis > 0)
                bte.waitForTermination(waitMillis);
        }
    }



    private class BackgroundTask implements Comparable, Runnable {

        private Element config;

        private Runnable runnable;

        private boolean runAtStartup;

        private String settingPrefix;

        private String whenDefault;

        private String enableSettingName;
        private String disableSettingName;

        private String sortKey;

        public BackgroundTask(Element configElem, DashboardContext ctx)
                throws Exception {
            config = configElem;

            runnable = (Runnable) ExtensionManager.getExecutableExtension(
                    configElem, CLASS_ATTR, ctx);

            runAtStartup = "true".equalsIgnoreCase(configElem
                    .getAttribute(RUN_AT_STARTUP_ATTR));
            settingPrefix = configElem.getAttribute(SETTING_PREFIX);
            whenDefault = configElem.getAttribute(WHEN_DEFAULT_ATTR);

            enableSettingName = configElem.getAttribute(ENABLE_SETTING_NAME);
            disableSettingName = configElem.getAttribute(DISABLE_SETTING_NAME);
            if (!StringUtils.hasValue(disableSettingName))
                disableSettingName = settingPrefix + ".disabled";

            sortKey = configElem.getAttribute(SORT_ORDINAL);
            if (!StringUtils.hasValue(sortKey))
                sortKey = DEFAULT_SORT_ORDINAL;
            if (sortKey.length() < 8)
            sortKey = "00000000".substring(0, 8 - sortKey.length()) + sortKey;
        }

        public void run() {
            runnable.run();
        }

        public boolean shouldRun(boolean isStartup, int hourOfDay) {
            if (!matches(isStartup, hourOfDay)) {
                logger.log(Level.FINEST, "Task schedule doesn''t match: {0}",
                        this);
                return false;

            } else if (isDisabled()) {
                logger.log(Level.FINEST, "Task is disabled: {0}", this);
                return false;

            } else if (isNotEnabled()) {
                logger.log(Level.FINEST, "Task is not enabled: {0}", this);
                return false;
            }

            return true;
        }

        public boolean matches(boolean isStartup, int hourOfDay) {
            if (isStartup)
                return runAtStartup;
            else
                return hourMatchesSetting(hourOfDay, Settings.getVal(
                        settingPrefix + ".timesOfDay", whenDefault));
        }

        private boolean hourMatchesSetting(int hourOfDay, String settingVal) {
            if (settingVal == null || settingVal.trim().length() == 0)
                return false;
            if ("*".equals(settingVal.trim()))
                return true;

            String[] times = settingVal.split("\\D+"); // split on non-digits
            for (int i = 0; i < times.length; i++)
                try {
                    if (hourOfDay == Integer.parseInt(times[i]))
                        return true;
                } catch (NumberFormatException nfe) {}

            return false;
        }

        private boolean isNotEnabled() {
            return StringUtils.hasValue(enableSettingName)
                    && !Settings.getBool(enableSettingName, false);
        }

        private boolean isDisabled() {
            return StringUtils.hasValue(disableSettingName)
                    && Settings.getBool(disableSettingName, false);
        }

        public int compareTo(Object o) {
            BackgroundTask that = (BackgroundTask) o;
            return this.sortKey.compareTo(that.sortKey);
        }

        public String toString() {
            return runnable.getClass().getName() + " "
                    + ExtensionManager.getDebugDescriptionOfSource(config);
        }

    }


    private class UserTask implements Runnable {

        private String id;

        private Runnable runnable;

        public UserTask(String id, Runnable runnable) {
            this.id = id;
            this.runnable = runnable;
        }

        public void run() {
            runnable.run();
        }

        public String toString() {
            if (id == null)
                return runnable.toString();
            else
                return id;
        }

    }


    private class BackgroundTaskExecutor extends Thread {

        private volatile boolean running = true;

        private Runnable currentTask = null;

        private volatile long currentTaskStartTime = -1;

        public BackgroundTaskExecutor() {
            super("Process Dashboard Background Task Thread");
            setDaemon(true);
            start();
        }

        public void run() {
            while (running) {
                currentTask = getNextTask();
                if (currentTask == null) {
                    running = false;
                    logger.fine("Background tasks completed.");
                    break;
                }

                try {
                    int percent = Settings.getInt(CPU_SETTING, 20);
                    ThreadThrottler
                            .setDefaultThrottlingPercentage(percent / 100.0);
                    ThreadThrottler.setThrottling(percent / 100.0);
                    currentTaskStartTime = System.currentTimeMillis();
                    logger.log(Level.FINE, "Running background task {0}",
                            currentTask);
                    currentTask.run();
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Background task '" + currentTask
                            + "' encountered exception; discaring it", t);
                    definedTasks.remove(currentTask);
                }
                currentTask = null;
                currentTaskStartTime = -1;
            }
        }

        public boolean appearsStuck() {
            if (!running || currentTask == null || currentTaskStartTime == -1)
                return false;
            long now = System.currentTimeMillis();
            long elapsed = now - currentTaskStartTime;
            return elapsed > STUCK_TASK_ELAPSED_TIME;
        }

        public void abort() {
            running = false;

            if (currentTask != null) {
                logger.log(Level.SEVERE, "A background task '" + currentTask
                        + "' appears to be stuck; discarding it");
                definedTasks.remove(currentTask);
            }

            try {
                interrupt();
            } catch (Exception e) {}
        }

        public void waitForTermination(long waitMillis) {
            // try interrupting the current task. (Probably won't have
            // much of an effect.)
            try {
                interrupt();
            } catch (Exception e) {}

            // now, wait up to waitMillis for this thread to terminate.
            try {
                join(waitMillis);
            } catch (Exception e) {}
        }
    }

}
