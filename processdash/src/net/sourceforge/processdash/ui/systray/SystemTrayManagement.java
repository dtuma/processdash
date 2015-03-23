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

import net.sourceforge.processdash.util.FallbackObjectFactory;

/**
 * Utility class to access the application system tray icon. Currently,
 *  only one icon is supported. The class attempts to initialize
 *  the icon using JDK 6 systray support. If the underlying JDK doesn't
 *  support systray API, then initialization will fall back to the default
 *  implementation, which simply does nothing.
 * 
 * @author Max Agapov <magapov@gmail.com>
 *
 */
public class SystemTrayManagement {

    /** A user setting which is used to enable/disable the system tray icon */
    public static final String DISABLED_SETTING = "systemTray.disabled";

    /**
     * the icon object
     */
    private static final SystemTrayIcon icon = initialize();

    /**
     * Private constructor to enforce non-instantiability
     */
    private SystemTrayManagement() {}

    /**
     * Initialize system tray icon using available implementation.
     * 
     * @return system tray icon
     */
    private static SystemTrayIcon initialize() {
        return new FallbackObjectFactory<SystemTrayIcon>(SystemTrayIcon.class)
                .add("SystemTrayIconJDK6Impl").get();
    }

    public static SystemTrayIcon getIcon() {
        return icon;
    }

}
