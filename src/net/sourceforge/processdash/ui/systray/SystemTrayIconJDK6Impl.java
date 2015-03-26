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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.ui.DashboardIconFactory;

/**
 * The JDK 6 implementation of the systray icon. This class supports one tray icon.
 * 
 * @see SystemTrayIcon
 * @see SystemTrayManagement
 * @see ProcessDashboard#initializeSystemTray()
 * 
 * @author Max Agapov <magapov@gmail.com>
 * 
 */
public class SystemTrayIconJDK6Impl implements SystemTrayIcon {

    /**
     * Our icon in the systray
     */
    private TrayIcon icon;

    /**
     * True if the icon is currently visible in the tray.
     */
    private boolean isVisible;

    /**
     * Process Dashboard object
     */
    ProcessDashboard pdash;

    /**
     * Object to recompute and update our icon.
     */
    IconImageHandler imageHandler;

    /**
     * Object to recompute and update our tooltip.
     */
    TooltipHandler tooltipHandler;

    /**
     * Object to handle "minimize to system tray" behavior
     */
    WindowHandler windowHandler;

    /**
     * Object to recompute and update the contents of our popup menu
     */
    MenuHandler menuHandler;

    /**
     * Object to respond to mouse events on our icon
     */
    MouseHandler mouseHandler;

    /**
     * Object to generate reminders for the user
     */
    Reminder reminder;

    /**
     * Object to observe changes in user settings
     */
    UserSettingHandler userSettingHandler;

    private static final Logger logger = Logger
            .getLogger(SystemTrayIconJDK6Impl.class.getName());

    /**
     * No argument constructor. Throws an exception if
     * SystemTray is not supported
     * @throws UnsupportedOperationException
     */
    public SystemTrayIconJDK6Impl() {
        if (!SystemTray.isSupported()) {
            throw new UnsupportedOperationException(
                    "System tray is not supported");
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        if (icon == null || visible == this.isVisible)
            return;

        if (visible) {
            try {
                SystemTray.getSystemTray().add(icon);
                this.isVisible = true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "TrayIcon could not be displayed", e);
            }
        }
        else {
            // if the main window is currently "minimized to the tray" when we
            // remove the tray icon, the user would have no way of getting it
            // back.  Raise the window to make certain that doesn't happen.
            DashController.raiseWindow();

            SystemTray.getSystemTray().remove(icon);
            this.isVisible = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (icon != null) {
            SystemTray.getSystemTray().remove(icon);
            icon = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(ProcessDashboard pdash) {
        if (this.pdash != null || this.icon != null) {
            logger.warning("SystemTrayIconJDK6Impl already initialized");
            return;
        }

        this.icon = new TrayIcon(getImage());
        this.isVisible = false;
        this.pdash = pdash;

        reminder = new Reminder(icon, pdash);
        imageHandler = new IconImageHandler(pdash, icon);
        tooltipHandler =  new TooltipHandler(pdash, icon);
        windowHandler = new WindowHandler(pdash, this);
        menuHandler = new MenuHandler(pdash, icon, reminder);
        mouseHandler = new MouseHandler(pdash, icon, menuHandler, imageHandler);
        userSettingHandler = new UserSettingHandler(this);
    }

    /**
     * Get an image for the icon in the systray.
     * 
     * @return an image
     */
    protected Image getImage() {
        Dimension iconSize = SystemTray.getSystemTray().getTrayIconSize();
        int size = Math.min(iconSize.width, iconSize.height);
        return DashboardIconFactory.getApplicationImage(size, true);
    }

    /** Convenience routine: return true if an icon is currently visible */
    protected static boolean isVisible(TrayIcon icon) {
        TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
        return Arrays.asList(icons).contains(icon);
    }
}
