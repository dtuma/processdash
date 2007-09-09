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

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import net.sourceforge.processdash.Settings;

/**
 * Listens for changes in the state of the dashboard application window, and
 * performs additional system-tray-specific behaviors as appropriate.
 * 
 * @author tuma
 */
public class WindowHandler {

    private Window window;

    private TrayIcon trayIcon;

    private boolean hideOnMinimize;


    public WindowHandler(Window w, TrayIcon icon) {
        this.window = w;
        this.trayIcon = icon;

        // get the value of the "minimize to tray" setting.  The user can
        // alter this setting via the configuration file if they wish.
        this.hideOnMinimize = Settings.getBool("systemTray.minimizeToTray",
            true);
        this.window.addWindowListener(new MinimizeToTraySupport());
    }


    private class MinimizeToTraySupport extends WindowAdapter {
        public void windowIconified(WindowEvent e) {
            if (isHideAppropriate())
                window.setVisible(false);
        }

        private boolean isHideAppropriate() {
            // check to see if minimize to tray has been disabled by a setting
            if (hideOnMinimize == false)
                return false;

            // it is only acceptable to "minimize to tray" if the tray icon is
            // currently visible!  Otherwise, the user would have no way of
            // getting the window back.
            TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
            return Arrays.asList(icons).contains(trayIcon);
        }
    }

}
