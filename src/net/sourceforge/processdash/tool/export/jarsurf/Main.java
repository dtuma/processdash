// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.jarsurf;

import java.io.IOException;
import java.io.ObjectInputStream;

import net.sourceforge.processdash.ui.lib.BrowserLauncher;


public class Main {

    public static final String JARDATA_FILENAME = "/files/jardata.ser";

    public static void main(String[] args) {
        try {
            // retrieve the serialized data file from within this jarfile
            ObjectInputStream objIn = new ObjectInputStream
                (Main.class.getResourceAsStream(JARDATA_FILENAME));
            JarData jarData = (JarData) objIn.readObject();

            // start a new zip web server
            JarWebServer w = new JarWebServer(jarData);
            w.setShutdownWaitTime(15);
            w.start();

            // open the default page in the user's web browser
            String url = w.getURL(jarData.getDefaultFile());
            BrowserLauncher.openURL(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
