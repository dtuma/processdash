// Copyright (C) 2001-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.help;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JFrame;



/** Wrapper to context-sensitive help functionality.
 *
 * Provides quick access to many common code snippets, while insulating
 * dashboard code from the javax.help.* classes.
 */
public class PCSH {

    public static void enableHelpKey(JFrame frame, String id) {
        getHelpProvider().enableHelpKey(frame.getRootPane(), id);
    }

    public static void enableHelpKey(JDialog dialog, String id) {
        getHelpProvider().enableHelpKey(dialog.getRootPane(), id);
    }

    public static void enableHelpKey(Component comp, String id) {
        getHelpProvider().enableHelpKey(comp, id);
    }

    public static void enableHelp(Component comp, String helpID) {
        getHelpProvider().enableHelp(comp, helpID);
    }

    public static void enableHelpOnButton(Component comp, String helpID) {
        getHelpProvider().enableHelpOnButton(comp, helpID);
    }

    public static void displayHelpTopic(String helpID) {
        getHelpProvider().displayHelpTopic(helpID);
    }

    public static boolean isSearchSupported() {
        return !(getHelpProvider() instanceof SimpleHelpProvider);
    }

    public static void displaySearchTab() {
        getHelpProvider().displaySearchTab();
    }

    public static String getHelpIDString(Component comp) {
        return getHelpProvider().getHelpIDString(comp);
    }

    private static DashHelpProvider DEFAULT_INSTANCE = null;

    public static void setHelpProvider(DashHelpProvider p) {
        if (DEFAULT_INSTANCE == null ||
            DEFAULT_INSTANCE instanceof SimpleHelpProvider)
            DEFAULT_INSTANCE = p;
    }

    public static DashHelpProvider getHelpProvider() {
        // first, attempt to create the help broker using the default provider.
        // This will succeed if the JavaHelp classes are in the system
        // classpath, or if the JavaHelp add-on is installed.
        if (DEFAULT_INSTANCE == null) try {
            DEFAULT_INSTANCE = new DashHelpBroker();
        } catch (Throwable e) { }

        // If we were unable to create a JavaHelp-based broker, create a
        // default browser-based provider instead.
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new SimpleHelpProvider();
        }

        return DEFAULT_INSTANCE;
    }

}
