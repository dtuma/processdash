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

import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;

import pspdash.data.DataRepository;
import javax.swing.*;

public class DashController {

    static PSPDashboard dash = null;
    private static String localAddress = "127.0.0.1";

    public static void setDashboard(PSPDashboard dashboard) {
        dash = dashboard;
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {}
    }

    public static void checkIP(Object remoteAddress) throws IOException {
        if (!"127.0.0.1".equals(remoteAddress) &&
            !localAddress.equals(remoteAddress))
            throw new IOException();
    }

    public static void raiseWindow() {
        if (dash.getState() == dash.ICONIFIED)
            dash.setState(dash.NORMAL);
        dash.show();
        dash.toFront();
    }

}
