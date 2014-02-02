// Copyright (C) 2011-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.Component;
import java.io.File;

import net.sourceforge.processdash.Settings;

public class SimpleInternalLauncher {

    private InstanceLauncherFactory launcherFactory;

    private DashboardProcessFactory processFactory;


    public void launchFile(Component comp, File file) {
        if (launcherFactory == null) {
            launcherFactory = new InstanceLauncherFactory();
            launcherFactory.setShowMessageForUnrecognizedFile(true);
        }

        DashboardInstance launcher = launcherFactory.getLauncher(comp, file);
        if (launcher == null)
            return;

        try {
            DashboardProcessFactory processFactory = getProcessFactory();
            new LaunchThread(launcher, processFactory).start();
        } catch (Exception e) {
            e.printStackTrace();
            QuickLauncher.showError(e.getMessage());
        }
    }

    private DashboardProcessFactory getProcessFactory() throws Exception {
        if (processFactory == null) {
            processFactory = new DashboardProcessFactoryForking();

            // The following settings are appropriate selections when opening a
            // data backup ZIP file. In the future, when we extend the "Open
            // Dataset" feature to support other modes, these settings will need
            // to be revisited.
            addSetting("backup.extraDirectories=");
            addSetting("export.disableAutoExport=true");
            addSetting("templates.disableSearchPath=true");

            String userLang = System.getProperty("user.language");
            if (userLang != null)
                processFactory.addVmArg("-Duser.language=" + userLang);
        }

        return processFactory;
    }

    private void addSetting(String setting) {
        processFactory.addVmArg("-D" + Settings.SYS_PROP_PREFIX + setting);
    }

    private class LaunchThread extends Thread {
        DashboardInstance launcher;
        DashboardProcessFactory processFactory;
        public LaunchThread(DashboardInstance l, DashboardProcessFactory f) {
            this.launcher = l;
            this.processFactory = f;
            setDaemon(true);
        }
        public void run() {
            launcher.launch(processFactory);
            launcher.waitForCompletion();
        }
    }

}
