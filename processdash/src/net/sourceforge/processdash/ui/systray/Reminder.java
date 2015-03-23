// Copyright (C) 2007-2009 Tuma Solutions, LLC
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
package net.sourceforge.processdash.ui.systray;

import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.Timer;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

/**
 * Time logging reminder that is associated with the dashboard tray icon.
 * 
 * @author Ali El Gamal <alielgamal@gmail.com>
 * 
 */
public class Reminder {

    /**
     * A user setting which is used to enable/disable the reminder
     */
    public static String DISABLED_SETTING = "reminder.disabled";

    /**
     * The property name that determines whether the reminder is disabled or not
     */
    public static String DISABLED_PROPERTY = "disabled";

    /**
     * A user setting which is used to set the timeout for the reminder
     */
    public static String TIMEOUT_SETTING = "reminder.timout";

    /**
     * The property name that determines the timeout value for the reminder
     */
    public static String TIMEOUT_PROPERTY = "timeout";

    private static int DEFAULT_TIMEOUT = 15;

    static final Resources resource = Resources
            .getDashBundle("ProcessDashboard.SysTray.Menu.Reminder");

    private final Timer reminderTimer;

    private TrayIcon icon;

    private ProcessDashboard pdash;

    private PropertyChangeSupport propertyChangeSupport;

    // This property change listener stops or restarts the timer if applicable
    // whenever the time logging model notifies it with a change in the active
    // task or the paused property.
    private PropertyChangeListener pcl = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            String pName = evt.getPropertyName();
            if (pName.equals(TimeLoggingModel.ACTIVE_TASK_PROPERTY)
                    || pName.equals(TimeLoggingModel.PAUSED_PROPERTY)) {
                stopOrRestartTimer();
            }
        }

    };

    public Reminder(TrayIcon icon, ProcessDashboard pdash) {

        this.icon = icon;
        this.pdash = pdash;
        propertyChangeSupport = new PropertyChangeSupport(this);

        ActionListener al = EventHandler.create(ActionListener.class, this,
                "remind");
        reminderTimer = new Timer(this.getTimeout() * 1000 * 60, al);
        stopOrRestartTimer();

        // Restart the timer whenever the user uses the dashboard to stop/start
        // or change the currently active task
        TimeLoggingModel tlm = pdash.getTimeLoggingModel();
        tlm.addPropertyChangeListener(pcl);
    }

    /**
     * Determines whether the reminder is disabled or not.
     * 
     * @return <code>true</code> if the reminder is disabled,
     *         <code>false</code> otherwise.
     * @see #setDisabled(boolean)
     */
    public boolean isDisabled() {
        return Settings.getBool(Reminder.DISABLED_SETTING, false);
    }

    /**
     * Enables/Disables the reminder
     * 
     * @param disabled
     *                <code>true</code> to disable the reminder,
     *                <code>false</code> to enable it.
     * @see #isDisabled()
     */
    public void setDisabled(boolean disabled) {
        boolean oldValue = isDisabled();
        InternalSettings.set(Reminder.DISABLED_SETTING, "" + disabled);
        stopOrRestartTimer();
        propertyChangeSupport.firePropertyChange(Reminder.DISABLED_PROPERTY,
                oldValue, disabled);
    }

    /**
     * Retrieves the reminder's timeout. The timeout is the time to sleep
     * between each reminder message.
     * 
     * @return the reminder's timeout in seconds.
     * @see #setTimeout(int)
     */
    public int getTimeout() {
        return Settings.getInt(Reminder.TIMEOUT_SETTING,
                Reminder.DEFAULT_TIMEOUT);
    }

    /**
     * Changes the reminder's timeout.
     * 
     * @param timeout
     *                the new timeout in minutes.
     * @see #getTimeout()
     */
    public void setTimeout(int timeout) {
        int oldValue = getTimeout();
        InternalSettings.set(Reminder.TIMEOUT_SETTING, timeout + "");
        reminderTimer.setDelay(timeout * 1000 * 60);
        reminderTimer.setInitialDelay(timeout * 1000 * 60);
        stopOrRestartTimer();
        propertyChangeSupport.firePropertyChange(Reminder.TIMEOUT_PROPERTY,
                oldValue, timeout);
    }

    /**
     * Stop or restart the timer, as applicable based on current environment.
     */
    private void stopOrRestartTimer() {
        if (!isDisabled() && pdash.getTimeLoggingModel().isLoggingAllowed())
            reminderTimer.restart();
        else
            reminderTimer.stop();
    }

    /**
     * Shows the reminder to the user
     */
    public void remind() {
        if (SystemTrayIconJDK6Impl.isVisible(icon) == false)
            return;

        String messageKey;
        if (pdash.getTimeLoggingModel().isPaused()) {
            messageKey = "Pause_Reminder_FMT";
        } else {
            messageKey = "Active_Reminder_FMT";
        }
        String windowTitle = pdash.getTitle();
        String msgBody = Reminder.resource.format(messageKey, windowTitle,
                pdash.getActiveTaskModel().getPath());
        String msgTitle = Reminder.resource.format("Title_FMT", windowTitle);
        Reminder.this.icon.displayMessage(msgTitle, msgBody,
                TrayIcon.MessageType.NONE);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeSupport.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeSupport.removePropertyChangeListener(pcl);
    }

}
