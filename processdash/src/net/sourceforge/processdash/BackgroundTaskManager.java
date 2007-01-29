// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

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

import org.w3c.dom.Element;


/** This class manages the registration and execution of background tasks.
 * 
 * Background tasks are registered in template.xml files. They can be run at
 * startup and potentially once an hour thereafter.
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



    private static BackgroundTaskManager INSTANCE = null;

    private static DashboardPermission INIT_PERMISSION = new DashboardPermission(
            "backgroundTaskManager.initialize");

    public synchronized static void initialize(DashboardContext dashContext) {
        INIT_PERMISSION.checkPermission();
        if (INSTANCE != null)
            throw new IllegalStateException("Initialization already performed");
        INSTANCE = new BackgroundTaskManager(dashContext);
    }



    private List tasks;

    private BackgroundTaskExecutor currentTaskExecutor;

    private static final Logger logger = Logger
    .getLogger(BackgroundTaskManager.class.getName());


    private BackgroundTaskManager(DashboardContext dashContext) {
        createBackgroundTasks(dashContext);
        if (!tasks.isEmpty()) {
            new BackgroundTaskExecutor(true).start();
            startPeriodicExecutor();
        }
    }

    private void createBackgroundTasks(DashboardContext dashContext) {
        logger.fine("Creating background tasks");

        tasks = Collections.synchronizedList(new ArrayList());
        List configElements = ExtensionManager
                .getXmlConfigurationElements(BACKGROUND_TASK_TAG);
        for (Iterator i = configElements.iterator(); i.hasNext();) {
            Element configElem = (Element) i.next();
            try {
                BackgroundTask bt = new BackgroundTask(configElem, dashContext);
                logger.log(Level.FINE, "Created background task {0}", bt);
                tasks.add(bt);
            } catch (Throwable t) {
                String message = "Unable to create background task "
                        + configElem.getAttribute(CLASS_ATTR) + " "
                        + ExtensionManager.getDebugDescriptionOfSource(configElem);
                logger.log(Level.SEVERE, message, t);
            }
        }
        Collections.sort(tasks);

        logger.log(Level.FINE, "Background tasks ({0}) created", new Integer(
                tasks.size()));
    }


    private void startPeriodicExecutor() {
        int millisPerHour = 60 /* minutes */* 60 /* seconds */* 1000 /* millis */;

        ActionListener periodicTaskInitiator = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new BackgroundTaskExecutor(false).start();
            }};
        Timer t = new Timer(millisPerHour, periodicTaskInitiator);
        t.start();
    }



    private class BackgroundTask implements Comparable {

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

        public boolean shouldRun(boolean isStartup, int hourOfDay) {
            if (!matches(isStartup, hourOfDay)) {
                logger.log(Level.FINER, "Task schedule doesn''t match: {0}",
                        this);
                return false;

            } else if (isDisabled()) {
                logger.log(Level.FINER, "Task is disabled: {0}", this);
                return false;

            } else if (isNotEnabled()) {
                logger.log(Level.FINER, "Task is not enabled: {0}", this);
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



    private class BackgroundTaskExecutor extends Thread {

        private boolean isStartup;

        private volatile boolean running = true;

        private BackgroundTask currentTask = null;

        public BackgroundTaskExecutor(boolean isStartup) {
            super("BackgroundTaskExecutor");
            setDaemon(true);
            this.isStartup = isStartup;
        }

        public void run() {
            synchronized (BackgroundTaskManager.this) {
                if (currentTaskExecutor != null)
                    currentTaskExecutor.abort();
                currentTaskExecutor = this;
            }

            int currentHourOfDay = Calendar.getInstance().get(
                    Calendar.HOUR_OF_DAY);
            if (isStartup)
                logger.fine("Running startup tasks");
            else
                logger.fine("Running periodic tasks");

            for (Iterator i = tasks.iterator(); running && i.hasNext();) {
                BackgroundTask task = (BackgroundTask) i.next();
                if (task.shouldRun(isStartup, currentHourOfDay)) {
                    currentTask = task;
                    try {
                        logger.log(Level.FINE, "Running background task {0}",
                                task);
                        task.runnable.run();
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, "Background task '" + task
                                + "' encountered exception; discaring it", t);
                        i.remove();
                    }
                    currentTask = null;
                }
            }

            synchronized (BackgroundTaskManager.this) {
                if (currentTaskExecutor == this)
                    currentTaskExecutor = null;
            }

            if (isStartup)
                logger.fine("Startup tasks completed");
            else
                logger.fine("Periodic tasks completed");
        }

        public void abort() {
            running = false;

            if (currentTask != null) {
                logger.log(Level.SEVERE, "A background task '" + currentTask
                        + "' appears to be stuck; discarding it");
                tasks.remove(currentTask);
            }

            try {
                interrupt();
            } catch (Exception e) {}
        }
    }

}
