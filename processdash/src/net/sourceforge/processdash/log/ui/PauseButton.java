// Copyright (C) 2000-2008 Tuma Solutions, LLC
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

import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.SoundClip;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;


public class PauseButton extends DropDownButton implements ActionListener,
        PropertyChangeListener, ApplicationEventListener {

    private Icon pausedIcon;
    private Icon timingIcon;
    private String timingTip;
    private String pausedTip;
    private String disabledTip;
    private SoundClip timingSound = null;

    private boolean quiet;

    private TimeLoggingModel loggingModel;

    public PauseButton(TimeLoggingModel loggingModel) {
        super();
        this.loggingModel = loggingModel;
        loggingModel.addPropertyChangeListener(this);

        PCSH.enableHelp(this, "PlayPause");
        PCSH.enableHelpKey(getMenu(), "PlayPause");

        Resources res = Resources.getDashBundle("ProcessDashboard.Pause");
        timingTip = res.getString("Pause_Tip");
        pausedTip = res.getString("Continue_Tip");
        disabledTip = res.getString("Disabled_Tip");

        setMainButtonMargin(new Insets(0,0,0,0));
        pausedIcon = padIcon(DashboardIconFactory.getPausedIcon());
        timingIcon = padIcon(DashboardIconFactory.getTimingIcon());
        getButton().setDisabledIcon(
            padIcon(DashboardIconFactory.getTimingDisabledIcon()));
        getButton().setFocusPainted(false);
        getButton().addActionListener(this);
        setRunFirstMenuOption(false);

        loadUserSettings();
        updateAppearance();

        if (quiet) {
            timingSound = new SoundClip(null);
        } else {
            timingSound = new SoundClip(getClass().getResource("timing.wav"));
        }
    }
    private Icon padIcon(Icon icon) {
        if (MacGUIUtils.isMacOSX())
            return new PaddedIcon(icon, -1, 0, -1, -2);
        else
            return icon;
    }

    private void updateAppearance() {
        boolean paused = loggingModel.isPaused();
        getButton().setIcon(paused ? pausedIcon : timingIcon);
        if (loggingModel.isLoggingAllowed()) {
            getButton().setToolTipText(paused ? pausedTip : timingTip);
            setEnabled(true);
        } else {
            getButton().setToolTipText(disabledTip);
            setEnabled(false);
        }
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) e.getSource();
            if (setPath(item.getText()))
                loggingModel.setPaused(false);
            else
                getMenu().remove(item);
        } else {
            loggingModel.setPaused(!loggingModel.isPaused());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (TimeLoggingModel.PAUSED_PROPERTY.equals(propertyName)
                || TimeLoggingModel.ACTIVE_TASK_PROPERTY.equals(propertyName)) {
            if (loggingModel.isPaused() == false)
                timingSound.play();
            updateAppearance();
        } else if (TimeLoggingModel.RECENT_PATHS_PROPERTY.equals(propertyName)) {
            reloadPaths();
        }
    }



    public boolean setPath(String path) {
        if (loggingModel.getActiveTaskModel() != null &&
                loggingModel.getActiveTaskModel().setPath(path))
            return true;
        else {
            // They've gone and edited their hierarchy, and the
            // requested node no longer exists! Beep to let them
            // know there was a problem, then remove this item
            // from the history list so they can't select it again
            // in the future.
            loggingModel.stopTiming();
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    public boolean setPhase(String phase) {
        if (loggingModel.getActiveTaskModel() != null &&
                loggingModel.getActiveTaskModel().setPhase(phase))
            return true;
        else {
            // They have navigated to a new portion of the hierarchy,
            // where the current phase is not present.  Beep to let them
            // know there was a problem.
            loggingModel.stopTiming();
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    private void reloadPaths() {
        JMenu menu = getMenu();
        menu.removeAll();

        Iterator recentItems = loggingModel.getRecentPaths().iterator();
        while (recentItems.hasNext()) {
            String path = (String) recentItems.next();
            JMenuItem itemToAdd = new JMenuItem(path);
            itemToAdd.addActionListener(this);
            menu.add(itemToAdd);
        }
    }

    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            saveUserSettings();
        }
    }


    private void loadUserSettings() {
        // Load the user setting for audible feedback
        quiet = Settings.getBool("pauseButton.quiet", false);

        // Load time multiplier setting
        String mult = Settings.getVal("timer.multiplier");
        if (mult != null) try {
            loggingModel.setMultiplier(Double.parseDouble(mult));
        } catch (NumberFormatException nfe) {}

        // Load the saved history list, if it is available.
        String history = Settings.getVal("pauseButton.historyList");
        if (history != null) {
            List paths = Arrays.asList(history.split("\t"));
            Collections.reverse(paths);
            loggingModel.setRecentPaths(paths);
        }

        // Load the user setting for history size
        loggingModel.setMaxRecentPathsRetained(Settings.getInt(
                "pauseButton.historySize", 10));
    }

    private void saveUserSettings() {
        // the only item that could have changed is the history list.
        List recentPaths = loggingModel.getRecentPaths();
        String settingResult = null;

        if (!recentPaths.isEmpty()) {
            StringBuffer setting = new StringBuffer();
            for (int i = recentPaths.size(); i-- > 0; )
                setting.append(recentPaths.get(i)).append('\t');
            settingResult = setting.substring(0, setting.length()-1);
        }

        // save the setting.
        InternalSettings.set("pauseButton.historyList", settingResult);
    }


}
