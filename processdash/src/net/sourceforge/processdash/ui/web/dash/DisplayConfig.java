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

package net.sourceforge.processdash.ui.web.dash;


import java.io.IOException;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

import pspdash.DashController;
import pspdash.PSPDashboard;


/** CGI script to print out the DNS name of the web server.
 */
public class DisplayConfig extends TinyCGIBase {

    protected void writeContents() throws IOException {
        if (parameters.containsKey("serverName"))
            printServerName();
        else if (parameters.containsKey("config"))
            printConfigFile();
        else
            printUserConfig();

        out.flush();
    }

    private void printServerName() {
        out.print(WebServer.getHostName());
    }

    private void printConfigFile() {
        out.print(DashController.getSettingsFileName());
    }

    private void printUserConfig() {
        out.println("<HTML><HEAD><TITLE>User settings</TITLE></HEAD>");
        out.println("<BODY><H1>Your settings</H1>");

        out.print("<P>Your configuration file is:<PRE>     ");
        out.println(DashController.getSettingsFileName());
        out.println("</PRE></P>");

        out.print("<P>Your data is located in the directory:<PRE>     ");
        out.println(PSPDashboard.getDefaultDirectory());
        out.println("</PRE></P>");

        out.println("</BODY></HTML>");
    }

}
