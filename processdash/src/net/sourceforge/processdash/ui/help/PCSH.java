// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ui.help;

import java.awt.Component;

import javax.swing.JFrame;

import net.sourceforge.processdash.net.http.WebServer;



/** Wrapper to context-sensitive help functionality.
 *
 * Provides quick access to many common code snippets, while insulating
 * dashboard code from the javax.help.* classes.
 */
public class PCSH {

    public static void enableHelpKey(JFrame frame, String id) {
        getHelpProvider().enableHelpKey(frame.getRootPane(), id);
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
    private static WebServer WEB_SERVER = null;

    public static void setHelpProvider(DashHelpProvider p) {
        if (DEFAULT_INSTANCE == null ||
            DEFAULT_INSTANCE instanceof SimpleHelpProvider)
            DEFAULT_INSTANCE = p;
    }

    public static void setWebServer(WebServer w) {
        // REFACTOR this shouldn't be visible?
        WEB_SERVER = w;
    }

    public static DashHelpProvider getHelpProvider() {
        // first, attempt to create the help broker directly.  This
        // will succeed only if the JavaHelp classes are in the system
        // classpath.
        if (DEFAULT_INSTANCE == null) try {
            Class c = Class.forName
                ("net.sourceforge.processdash.ui.help.DashHelpBroker");
            DEFAULT_INSTANCE = (DashHelpProvider) c.newInstance();
        } catch (Throwable e) { }

        // next, attempt to create a help broker via a JavaHelp add-on
        if (DEFAULT_INSTANCE == null && WEB_SERVER != null) try {
            WEB_SERVER.getRequest("/help/createBroker.class", false);
        } catch (Throwable e) { }

        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new SimpleHelpProvider();
        }

        return DEFAULT_INSTANCE;
    }

}
