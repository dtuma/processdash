// Copyright (C) 2007-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.systray;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        try {
            setupIcon();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "TrayIcon setup failed", e);
            return;
        }

        this.pdash = pdash;
        reminder = new Reminder(icon, pdash);

        imageHandler = new IconImageHandler(pdash, icon);
        tooltipHandler =  new TooltipHandler(pdash, icon);
        windowHandler = new WindowHandler(pdash, icon);
        menuHandler = new MenuHandler(pdash, icon, reminder);
        mouseHandler = new MouseHandler(pdash, icon, menuHandler, imageHandler);
    }

    /**
     * Creates TrayIcon with an image, adds action listener and shows icon in the
     * <code>SystemTray</code>.
     * 
     * @throws AWTException
     */
    private void setupIcon() throws AWTException {
        if (icon == null) {
            // Create instance with an image
            icon = new TrayIcon(getImage());

            // Add our new icon to the system tray
            SystemTray.getSystemTray().add(icon);
        }
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

}
