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
            w.start();

            // open the default page in the user's web browser
            String url = w.getURL(jarData.getDefaultFile());
            BrowserLauncher.openURL(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
