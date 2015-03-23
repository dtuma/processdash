// Copyright (C) 2004-2008 Tuma Solutions, LLC
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
        if (protocol != null
                && protocol.startsWith(WebServer.DASHBOARD_PROTOCOL))
            return this;
        else
            return null;
    }


    protected URLConnection openConnection(URL u) throws IOException {
        String protocol = u.getProtocol();
        if (DashboardHelpURLConnection.DASHHELP_PROTOCOL.equals(protocol))
            return new DashboardHelpURLConnection(webServer, u);
        else
            return new DashboardURLConnection(webServer, u);
    }

}
