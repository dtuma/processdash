// Copyright (C) 2011-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.installer;

import java.io.File;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAction;
import com.izforge.izpack.installer.PanelActionConfiguration;
import com.izforge.izpack.installer.ScriptParser;
import com.izforge.izpack.util.AbstractUIHandler;
import com.izforge.izpack.util.os.RegistryDefaultHandler;
import com.izforge.izpack.util.os.RegistryHandler;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;

public class LauncherPreInstallAction implements PanelAction {

    private AutomatedInstallData installdata;

    public void initialize(PanelActionConfiguration configuration) {}

    public void executeAction(AutomatedInstallData installdata,
            AbstractUIHandler handler) {
        this.installdata = installdata;
        setUninstallationRegistryName();
        setDefaultInstallDir();
        setShortcutPrefs();
    }

    private void setUninstallationRegistryName() {
        // By default, the uninstaller will use the version number of the
        // application when registering for uninstallation. But we allow
        // people to install the dashboard on top of itself over and over.
        // We wouldn't want the user to see multiple uninstallation options
        // in the "Add/Remove Programs" dialog (one for each version), because
        // they can only uninstall the application once anyway.
        RegistryHandler rh = RegistryDefaultHandler.getInstance();
        if (rh != null) {
            String name = installdata.getVariable(ScriptParser.APP_NAME);
            rh.setUninstallName(name);
        }
    }

    private void setDefaultInstallDir() {
        File appDir = DirectoryPreferences.getApplicationDirectory(true);
        File instDir = new File(appDir, "install");
        File launcherDir = new File(instDir, "launcher");
        installdata.setVariable(ScriptParser.INSTALL_PATH,
            launcherDir.getAbsolutePath());
    }

    private void setShortcutPrefs() {
        // create shortcuts on the desktop by default
        installdata.setVariable("DesktopShortcutCheckboxEnabled", "true");
    }

}
