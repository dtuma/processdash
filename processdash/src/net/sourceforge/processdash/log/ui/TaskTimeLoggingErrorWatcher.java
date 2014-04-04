// Copyright (C) 2009-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.util.DateUtils;

public class TaskTimeLoggingErrorWatcher implements ActionListener,
        PropertyChangeListener {

    private Component parent;

    private ActiveTaskModel activeTaskModel;

    private TimeLoggingModel timeLoggingModel;

    private DataRepository data;

    private Set<String> pathsToIgnore;

    private long playButtonStartTime;

    private Timer timer;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.TimeLoggingWarning");

    public TaskTimeLoggingErrorWatcher(Component parent,
            ActiveTaskModel activeTaskModel, TimeLoggingModel timeLoggingModel,
            DataRepository data) {
        this.parent = parent;
        this.activeTaskModel = activeTaskModel;
        this.timeLoggingModel = timeLoggingModel;
        this.data = data;
        this.pathsToIgnore = new HashSet<String>();

        this.timer = new Timer((int) DateUtils.HOUR, this);
        this.timer.setInitialDelay(20 * (int) DateUtils.SECONDS);

        timeLoggingModel.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (TimeLoggingModel.PAUSED_PROPERTY.equals(propName)) {
            Boolean isPaused = (Boolean) evt.getNewValue();
            if (isPaused) {
                playButtonStartTime = -1;
                timer.stop();
            } else {
                timer.restart();
                playButtonStartTime = System.currentTimeMillis();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        checkForCompletedTask();
        checkForRunawayTimer();
    }

    private void checkForCompletedTask() {
        // is this completion checker disabled? If so, do nothing.
        if (isCompletionCheckingDisabled())
            return;

        // if we don't have an active path, do nothing.
        String taskPath = activeTaskModel.getPath();
        if (taskPath == null)
            return;

        // Check to see if the current task or any of its ancestors
        // have been marked complete.
        StringBuffer effectivePath = new StringBuffer(taskPath);
        Object completionDate = data.getInheritableValue(effectivePath,
            "Completed");
        if (completionDate instanceof DateData) {
            DateData dd = (DateData) completionDate;

            // if the user hasn't already given us the OK to log time
            // to this path, display a message.
            String completedPath = effectivePath.toString();
            if (pathsToIgnore.contains(completedPath) == false)
                showCompletedTaskTimeLoggingAlert(completedPath, dd.getValue());
        }
    }

    private void showCompletedTaskTimeLoggingAlert(String path, Date date) {
        String messageText = resources.format("Message_FMT", path, date);
        JCheckBox disableCheckbox = new JCheckBox(resources
                .getString("Disable"));
        disableCheckbox.setFocusPainted(false);
        Object[] message = { messageText.split("\n"), " ", disableCheckbox };

        boolean continueLoggingTime = showTimeLoggingAlert(message);
        if (continueLoggingTime)
            pathsToIgnore.add(path);

        if (disableCheckbox.isSelected())
            setCompletionCheckingDisabled(true);
    }

    private void checkForRunawayTimer() {
        if (playButtonStartTime < 0)
            return;

        long elapsed = System.currentTimeMillis() - playButtonStartTime;
        long elapsedHours = elapsed / DateUtils.HOURS;
        int cutoffHours = Settings.getInt(RUNAWAY_SETTING_NAME, 5);
        if (elapsedHours < cutoffHours || cutoffHours < 1)
            return;

        String path = activeTaskModel.getPath();
        String[] message = resources.formatStrings("Runaway_Message_FMT", path);
        boolean continueLoggingTime = showTimeLoggingAlert(message);
        if (continueLoggingTime == false)
            JOptionPane.showMessageDialog(parent,
                resources.getString("Runaway_Cleanup_Advice"),
                resources.getString("Title"), JOptionPane.PLAIN_MESSAGE);
    }

    private boolean showTimeLoggingAlert(Object message) {
        DashController.raiseWindow();
        Toolkit.getDefaultToolkit().beep();

        String title = resources.getString("Title");

        String yesOption = resources.getString("Yes");
        String noOption = resources.getString("No");
        String[] options = { yesOption, noOption };

        int userChoice = JOptionPane.showOptionDialog(parent, message, title,
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
            options, yesOption);
        if (userChoice == 1) {
            timeLoggingModel.stopTiming();
            return false;
        } else {
            return true;
        }
    }

    private boolean isCompletionCheckingDisabled() {
        return Settings.getBool(SETTING_NAME, true) == false;
    }

    private void setCompletionCheckingDisabled(boolean d) {
        InternalSettings.set(SETTING_NAME, d ? "false" : "true");
    }

    private static final String SETTING_NAME = "timer.warnOnCompletedTasks";

    private static final String RUNAWAY_SETTING_NAME = "timer.runawayTimeCutoff";

}
