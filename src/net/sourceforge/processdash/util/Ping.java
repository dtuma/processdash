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

package net.sourceforge.processdash.util;


import java.net.*;


/** Poor man's ping.
 *
 *  We can't really do ping without resorting to java native methods,
 *  since java doesn't contain support for the ICMP protocol.
 *  Although there are some "java ping" classes out there, they all
 *  use UDP to connect to port 7 on the remote computer (the echo
 *  port).  Windows computers don't listen on the echo port, so this
 *  won't work reliably.
 */
public class Ping implements Runnable {

    public  static final int HOST_NOT_FOUND = 0;
    private static final int LOOKING_FOR_HOST = HOST_NOT_FOUND;
    public  static final int CANNOT_CONNECT = 1;
    private static final int CONNECTING_TO_HOST = CANNOT_CONNECT;
    public  static final int SUCCESS = 2;


    private String hostname;
    private int port;
    private int status;

    private Ping(String hostname, int port) {
        this.hostname = hostname;
        this.port     = port;
    }

    public void run() {
        try {
            status = LOOKING_FOR_HOST;
            InetAddress addr = InetAddress.getByName(hostname);
            status = CONNECTING_TO_HOST;
            Socket s = new Socket(addr, port);
            status = SUCCESS;
            s.close();
        } catch (Exception e) {}
    }

    public static int ping(String hostname, int port, long maxWait) {
        Ping ping = new Ping(hostname, port);
        try {
            Thread t = new Thread(ping);
            t.setDaemon(true);
            t.start();
            t.join(maxWait);
        } catch (InterruptedException ie) {}
        return ping.status;
    }
}
