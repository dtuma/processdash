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
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;

/**
 * The JDK 6 implementation of the systray icon. This class supports one tray icon.
 * 
 * @see SystemTrayIcon
 * @see SystemTrayManagement
 * @see DashController
 * @see ProcessDashboard#initializeSystemTray()
 * 
 * @author Max Agapov <magapov@gmail.com>
 * 
 */
public class SystemTrayIconJDK6Impl implements SystemTrayIcon {

    private static final String SEPARATOR = "-";

    /**
     * Our icon in the systray
     */
    private TrayIcon icon;

    /**
     * Process Dashboard object
     */
    ProcessDashboard pdash;

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
        this.pdash = pdash;
        update();
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        try {
            setupIcon();
        } catch (AWTException e) {
            throw new RuntimeException("TrayIcon setup failed", e);
        }

        //update image
        Image img = getImage();
        if (icon.getImage() != img) {
            icon.setImage(img);
        }

        //update menu
        PopupMenu menu = getMenu();
        if (icon.getPopupMenu() != menu){
            icon.setPopupMenu(menu);
        }

        //update tooltip text
        icon.setToolTip(getToolTip());
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
            icon.setImageAutoSize(true);

            // Handle the default (mouse doubleclick) action
            // to restore application view.
            icon.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showApplication();
                }
            });

            // Add our new icon to the system tray
            SystemTray.getSystemTray().add(icon);
        }
    }

    /**
     * Brings the application window to the front.
     */
    private void showApplication() {
        DashController.raiseWindow();
    }

    /**
     * Get an image for the icon in the systray.<br><br>
     * 
     * TODO: Update this method to reflect current application state.
     * 
     * @return an image
     */
    protected Image getImage() {
        return Toolkit.getDefaultToolkit().getImage("filename");
    }

    /**
     * Builds popup menu for the systray icon. This method should also
     * setup menu action listener(s).<br><br>
     * 
     * TODO: Update this method to reflect current application state.
     * 
     * @return popup menu
     */
    protected PopupMenu getMenu() {

        PopupMenu popup = new PopupMenu();

        //Default item
        MenuItem defaultItem = new MenuItem("Show ProcessDash");
        defaultItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showApplication();
            }
        });
        popup.add(defaultItem);

        //Separator
        popup.add(new MenuItem(SEPARATOR));

        //Exit item
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pdash.exitProgram();
            }
        });
        popup.add(exitItem);

        return popup;
    }

    /**
     * Updates icon tooltip text that appears when mouse hovers over the icon.<br><br>
     * 
     * TODO: Update this method to reflect current application state.
     * 
     * @return tooltip text
     */
    protected String getToolTip() {
        return "Process Dash";
    }
}
