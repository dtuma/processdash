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

import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.HTMLUtils;


class SimpleHelpProvider implements DashHelpProvider {

    public void enableHelpKey(Component comp, String id) {}
    public void enableHelp(Component comp, String helpID) {}
    public void enableHelpOnButton(Component comp, String helpID) {}

    public void displayHelpTopic(String helpID) {
        Browser.launch("help/frame.html?" + HTMLUtils.urlEncode(helpID));
    }
    public void displaySearchTab() {
        Browser.launch("help/frame.html");
    }
    public String getHelpIDString(Component comp) { return null; }

}
