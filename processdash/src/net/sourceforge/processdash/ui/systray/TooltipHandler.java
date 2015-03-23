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

package net.sourceforge.processdash.ui.systray;

import java.awt.TrayIcon;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

/**
 * Manages changes to the tooltip for the dashboard tray icon.
 * 
 * This class registers as a listener for relevant changes in application
 * state, and updates the tray icon's tooltip appropriately.
 * 
 * @author tuma
 */
public class TooltipHandler {


    private ProcessDashboard pdash;

    private TrayIcon trayIcon;

    private TimeLoggingModel timeLoggingModel;

    private ActiveTaskModel activeTaskModel;

    private String currentTooltip = null;

    private MessageFormat tooltipFormat;

    private int maxTooltipLength;


    private static final Resources res = Resources
            .getDashBundle("ProcessDashboard.SysTray.Tooltip");



    public TooltipHandler(ProcessDashboard pdash, TrayIcon icon) {
        this.pdash = pdash;
        this.trayIcon = icon;

        // get the time logging model
        this.timeLoggingModel = pdash.getTimeLoggingModel();

        // get the active task model
        this.activeTaskModel = pdash.getActiveTaskModel();

        // determine OS-specific defaults
        configureTooltipFormat();

        // register for change notification
        PropertyChangeListener pcl = EventHandler.create(
            PropertyChangeListener.class, this, "update");
        pdash.addPropertyChangeListener("title", pcl);
        timeLoggingModel.addPropertyChangeListener(pcl);
        activeTaskModel.addPropertyChangeListener(pcl);

        update();
    }

    public void update() {
        String mainTitle = pdash.getTitle();
        String path = activeTaskModel.getPath();
        if (path == null)
            path = ""; // could possibly occur during startup/shutdown sequences

        String timingMessage;
        if (!timeLoggingModel.isLoggingAllowed())
            timingMessage = res.getString("Timing_Disabled");
        else if (timeLoggingModel.isPaused())
            timingMessage = res.getString("Paused");
        else
            timingMessage = res.getString("Timing");

        String newTooltip = formatTooltip(mainTitle, path, timingMessage,
            maxTooltipLength);

        if (currentTooltip == null || !currentTooltip.equals(newTooltip)) {
            currentTooltip = newTooltip;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    trayIcon.setToolTip(currentTooltip);
                }
            });
        }
    }

    private String formatTooltip(String title, String path,
            String timingMessage, int maxLen) {

        // first try the happy path: format the string normally.
        String[] args = new String[] { title, path, timingMessage };
        String result = tooltipFormat.format(args);

        // check to see if the result is too long.
        int resultLen = result.length();
        if (resultLen > maxLen) {
            int charsToChop = (resultLen - maxLen) + ELLIPSE.length();
            if (path.length() > charsToChop) {
                // if possible, chop some of the path and replace with "..."
                args[1] = ELLIPSE + path.substring(charsToChop);
                result = tooltipFormat.format(args);
            } else {
                // otherwise, just display as much of the path as will fit.
                charsToChop = (path.length() - maxLen) + ELLIPSE.length();
                if (charsToChop > 0)
                    result = ELLIPSE + path.substring(charsToChop);
                else
                    result = path;
            }
        }

        return result;
    }

    private static final String ELLIPSE = "...";

    /**
     * Select appropriate default values for the tooltip format
     */
    private void configureTooltipFormat() {
        try {
            String formatSpec = Settings.getVal("systemTray.tooltipFormat");
            tooltipFormat = new MessageFormat(formatSpec);
        } catch (Exception e) {
            tooltipFormat = new MessageFormat("{1}");
        }

        maxTooltipLength = Settings.getInt("systemTray.tooltipMaxLen", 64);
    }

}
