// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.help.CSH;
import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;

public class DashHelpBroker extends DefaultHelpBroker
    implements HelpBroker, DashHelpProvider, TinyCGI {


    public DashHelpBroker() throws HelpSetException {
        super();

        URL hsURL = null;
        try {
            hsURL = new URL(Browser.mapURL(HELPSET_PATH));
        } catch (MalformedURLException mue) {
            throw new HelpSetException("Couldn't create helpset url");
        }

        HelpSet hs = new HelpSet(null,hsURL);
        //System.out.println("Found help set at " + hsURL);

        setHelpSet(hs);
        setSize(new Dimension(645,495));
    }

    public void setHelpSet(HelpSet hs) {
        super.setHelpSet(hs);
        initPresentation();

        try {
            Window w = getWindowPresentation().getHelpWindow();
            if (w instanceof Frame)
                ((Frame) w).setIconImage
                    (DashboardIconFactory.getWindowIconImage());
        } catch (Throwable t) {
            // an old version of JavaHelp in the system classpath will
            // cause this to fail.  It's no big deal - the window will
            // just have a different icon.  Life goes on.
        }
    }


    public void enableHelpKey(Component comp, String helpID) {
        enableHelpKey(comp, helpID, null);
    }
    public void enableHelp(Component comp, String helpID) {
        enableHelp(comp, helpID, null);
    }
    public void enableHelpOnButton(Component comp, String helpID) {
        javax.help.CSH.setHelpIDString(comp, helpID);
        enableHelpOnButton(comp, helpID, null);
    }
    public void displayHelpTopic(String helpID) {
        setActiveTab("TOC");
        setCurrentID(helpID);
        setDisplayed(true);
    }
    public void displaySearchTab() {
        setActiveTab("Search");
        setDisplayed(true);
    }
    private void setActiveTab(String tab) {
        try { setCurrentView(tab); } catch (Exception e) {}
    }
    public String getHelpIDString(Component comp) {
        return CSH.getHelpIDString(comp);
    }

    private static final String HELPSET_PATH = "/help/PSPDash.hs";


    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        PCSH.setHelpProvider(this);
        out.write("Content-type: text/html\r\n\r\n".getBytes());
        out.flush();
    }

}
