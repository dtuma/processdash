// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.MenuItem;
import java.net.URL;
import javax.swing.JFrame;
import javax.help.*;


/** Wrapper to context-sensitive help functionality.
 *
 * Provides quick access to many common code snippets, while insulating
 * dashboard code from the javax.help.* classes.
 */
class PCSH {

    public static void enableHelpKey(JFrame frame, String id) {
        getHelpBroker().enableHelpKey(frame.getRootPane(), id, null);
    }

    public static void enableHelpKey(Component comp, String id) {
        getHelpBroker().enableHelpKey(comp, id, null);
    }

    public static void enableHelp(Component comp, String helpID) {
        getHelpBroker().enableHelp(comp, helpID, null);
    }

    public static void enableHelpOnButton(Component comp, String helpID) {
        javax.help.CSH.setHelpIDString(comp, helpID);
        getHelpBroker().enableHelpOnButton(comp, helpID, null);
    }

    public static void displayHelpTopic(String helpID) {
        DashHelpBroker help = getHelpBroker();
        setActiveTab(help, "TOC");
        help.setCurrentID(helpID);
        help.setDisplayed(true);
    }

    public static void displaySearchTab() {
        DashHelpBroker help = getHelpBroker();
        setActiveTab(help, "Search");
        help.setDisplayed(true);
    }
    private static void setActiveTab(DashHelpBroker help, String tab) {
        try { help.setCurrentView(tab); } catch (Exception e) {}
    }

    //    public static void setHelpIDString(MenuItem comp, String helpID) {
    //        javax.help.CSH.setHelpIDString(comp, helpID);
    //    }

    private static DashHelpBroker DEFAULT_INSTANCE = null;

    public static DashHelpBroker getHelpBroker() {
        if (DEFAULT_INSTANCE == null) try {
            URL hsURL = TemplateLoader.resolveURL(HELPSET_PATH);

            HelpSet hs = new HelpSet(null,hsURL);
            //System.out.println("Found help set at " + hsURL);

            DEFAULT_INSTANCE = new DashHelpBroker(hs);

            // set the size for the display
            DEFAULT_INSTANCE.setSize(new Dimension(645,495));
        } catch (Exception e) {
            System.out.println("Error on help");
        }

        return DEFAULT_INSTANCE;
    }

    private static final String HELPSET_PATH = "/help/PSPDash.hs";


}
