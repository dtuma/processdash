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

package net.sourceforge.processdash.tool.launcher.installer;

import java.io.File;
import java.util.prefs.Preferences;

import com.izforge.izpack.installer.ScriptParser;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;

class LauncherInstallerPaths {

    static String getDefaultInstallationPath() {
        File appDir = DirectoryPreferences.getApplicationDirectory(true);
        File instDir = new File(appDir, "install");
        File launcherDir = new File(instDir, "launcher");
        return launcherDir.getAbsolutePath();
    }

    static void setInstallatedPath(String path, String version) {
        Preferences prefs = getPrefs();
        prefs.put(ScriptParser.INSTALL_PATH, path);
        prefs.put(ScriptParser.APP_VER, version);
    }

    static String getInstalledPath() {
        return getPrefs().get(ScriptParser.INSTALL_PATH, null);
    }

    static String getInstalledVersion() {
        return getPrefs().get(ScriptParser.APP_VER, null);
    }

    static Preferences getPrefs() {
        return Preferences.userNodeForPackage(LauncherInstallerPaths.class);
    }

}
