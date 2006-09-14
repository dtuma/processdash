// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.net.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


public class DashboardURLStreamHandlerFactory extends URLStreamHandler
    implements URLStreamHandlerFactory
{
    private static DashboardURLStreamHandlerFactory INSTANCE = null;
    private static boolean disabled = false;

    public static void disable() {
        disabled = true;
    }

    public static void initialize(WebServer webServer) {
        if (!disabled) {
            if (INSTANCE == null) {
                INSTANCE = new DashboardURLStreamHandlerFactory(webServer);
                URL.setURLStreamHandlerFactory(INSTANCE);
            } else {
                INSTANCE.setWebServer(webServer);
            }
        }
    }


    private WebServer webServer;


    private DashboardURLStreamHandlerFactory(WebServer webServer) {
        this.webServer = webServer;
    }


    private void setWebServer(WebServer webServer) {
        this.webServer = webServer;
    }


    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (WebServer.DASHBOARD_PROTOCOL.equals(protocol))
            return this;
        else
            return null;
    }


    protected URLConnection openConnection(URL u) throws IOException {
        return new DashboardURLConnection(webServer, u);
    }

}
