// Copyright (C) 2002-2003 Tuma Solutions, LLC
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
