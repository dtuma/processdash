// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.macosx;

import net.sourceforge.processdash.tool.launcher.Launcher;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.FallbackObjectFactory;

public class LauncherMacOSX {

    public synchronized static boolean setupOnMacAndCheckForStartupFileOpen() {
        // do nothing on non-mac platforms
        if (!MacGUIUtils.isMacOSX())
            return false;

        // register to receive an "open file" event that might already be
        // waiting for us, if the app was launched to open a shortcut file
        String helperClass = LauncherMacOSX.class.getName() + "Helper";
        new FallbackObjectFactory<Runnable>(Runnable.class)
                .add(helperClass + "Java9") //
                .add(helperClass) //
                .get().run();

        // wait a moment to see if the registration above discovered an inbound
        // "file open" event.
        try {
            LauncherMacOSX.class.wait(1000);
        } catch (InterruptedException e) {
        }

        // let our caller know if we saw an immediate "file open" event
        return sawOpenFileEvent;
    }

    private static volatile boolean sawOpenFileEvent = false;

    public static void openFiles(final String... files) {
        // make a record that we saw a "file open" event
        sawOpenFileEvent = true;

        Thread t = new Thread() {
            public void run() {
                // open the files we were given
                Launcher.launch(files);

                // let our caller know we handled these events
                synchronized (LauncherMacOSX.class) {
                    LauncherMacOSX.class.notify();
                }
            }
        };
        t.setDaemon(false);
        t.start();
    }

}
