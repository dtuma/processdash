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

import java.io.*;
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
