// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.net.InetAddress;

public class ComputerName {

    public static String getName() {
        // on Windows, the computer name is in an environment variable
        String envVal = System.getenv("COMPUTERNAME");
        if (StringUtils.hasValue(envVal) == false)
            envVal = System.getenv("HOSTNAME");
        if (StringUtils.hasValue(envVal))
            return envVal;

        // on Unix/Linux/Mac OS X, call the "hostname" application
        try {
            Process p = Runtime.getRuntime().exec("hostname");
            byte[] out = RuntimeUtils.collectOutput(p, true, false);
            String execVal = new String(out).trim();
            if (StringUtils.hasValue(execVal))
                return execVal;
        } catch (Exception e) {
        }

        // if the above techniques failed, try using the network name
        try {
            String inetVal = InetAddress.getLocalHost().getHostName();
            if (StringUtils.hasValue(inetVal))
                return inetVal;
        } catch (Exception e) {
        }

        // could not identify a hostname
        return null;
    }

}
