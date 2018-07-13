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

package net.sourceforge.processdash.tool.launcher;

import net.sourceforge.processdash.tool.launcher.jnlp.JnlpDatasetLauncher;
import net.sourceforge.processdash.tool.launcher.jnlp.JnlpUtil;

public class Launcher {

    public static void main(String[] args) {
        launch(args);
    }

    public static boolean launch(String... items) {
        boolean sawLaunchable = false;

        for (String item : items) {
            if (PersonalDatasetLauncher.isAutoPersonalDataset(item)) {
                PersonalDatasetLauncher.launch();
                sawLaunchable = true;

            } else if (JnlpUtil.isJnlpFilename(item)) {
                JnlpDatasetLauncher.launch(item);
                sawLaunchable = true;
            }
        }

        return sawLaunchable;
    }


    private static final String CURRENT_PROCESS_USED_PROP = //
            Launcher.class.getName() + ".currentProcessUsedForLaunch";

    public synchronized static boolean currentProcessHasBeenUsedForLaunch() {
        return System.getProperty(CURRENT_PROCESS_USED_PROP) != null;
    }

    public synchronized static boolean requestPermissionToLaunchInProcess() {
        // only one requestor can launch per process. If a previous caller has
        // already obtained that permission, deny it to the current caller.
        if (currentProcessHasBeenUsedForLaunch())
            return false;

        // if no one has requested yet, grant the permission to our caller, and
        // set the flag so others will have to launch separately. We store this
        // in a system property (rather than a private class variable) to guard
        // against the case where this class is reloaded in another classloader.
        System.setProperty(CURRENT_PROCESS_USED_PROP, "true");
        return true;
    }

}
