// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.MenuItem;
import java.net.URL;
import javax.swing.JFrame;
//import javax.help.*;


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

    public static void displaySearchTab() {
        getHelpProvider().displaySearchTab();
    }

    public static String getHelpIDString(Component comp) {
        return getHelpProvider().getHelpIDString(comp);
    }

    private static DashHelpProvider DEFAULT_INSTANCE = null;
    private static TinyWebServer WEB_SERVER = null;

    public static void setHelpProvider(DashHelpProvider p) {
        if (DEFAULT_INSTANCE == null ||
            DEFAULT_INSTANCE instanceof SimpleHelpProvider)
            DEFAULT_INSTANCE = p;
    }

    static void setWebServer(TinyWebServer w) {
        WEB_SERVER = w;
    }

    public static DashHelpProvider getHelpProvider() {
        // first, attempt to create the help broker directly.  This
        // will succeed only if the JavaHelp classes are in the system
        // classpath.
        if (DEFAULT_INSTANCE == null) try {
            Class c = Class.forName("pspdash.DashHelpBroker");
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
