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

import javax.help.*;
import java.awt.Toolkit;
// import javax.swing.JSplitPane;

public class DashHelpBroker extends DefaultHelpBroker implements HelpBroker {

    private static DashHelpBroker DEFAULT_INSTANCE = null;

    DashHelpBroker() {
        super();
        DEFAULT_INSTANCE = this;
    }

    // this wrapper class should allow me to set the icon on the top of the
    // helpset viewer to match the dashboard stopwatch icon
    DashHelpBroker(HelpSet hs) {
        super(hs);
        initPresentation();

        frame.setIconImage(Toolkit.getDefaultToolkit().createImage
            (getClass().getResource("icon32.gif")));

        DEFAULT_INSTANCE = this;
//      jsplit.setDividerLocation(0.25);
    }

    public void setHelpSet(HelpSet hs) {
        super.setHelpSet(hs);
        initPresentation();

        frame.setIconImage(Toolkit.getDefaultToolkit().createImage
            (getClass().getResource("icon32.gif")));

//      splitpane.setDividerLocation(0.25);
    }

    public static DashHelpBroker getInstance() { return DEFAULT_INSTANCE; }
}
