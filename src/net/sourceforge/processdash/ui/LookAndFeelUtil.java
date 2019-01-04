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

package net.sourceforge.processdash.ui;

import java.util.prefs.Preferences;

import javax.swing.UIManager;

import net.sourceforge.processdash.ui.lib.WindowsGUIUtils;
import net.sourceforge.processdash.util.RuntimeUtils;

public class LookAndFeelUtil {

    public static void setDefaultLAF() {
        try {
            if (shouldUseSystemLAF()) {
                String laf = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(laf);
                RuntimeUtils.addPropagatedSystemProperty("swing.defaultlaf",
                    laf);
            }
            tweakLAFDefaults();
        } catch (Exception e) {
        }
    }

    private static boolean shouldUseSystemLAF() {
        // the system look and feel is currently only supported on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows"))
            return false;

        // for development/testing purposes, allow the user to override the
        // default user preference by setting a system property
        String sysprop = System.getProperty("useSystemLAF");
        if (sysprop != null)
            return sysprop.equals("true");

        // by default, the look-and-feel flag is stored in user preferences
        Preferences prefs = Preferences.userRoot()
                .node("net/sourceforge/processdash/userPrefs");
        return prefs.getBoolean("useSystemLAF", true);
    }

    private static void tweakLAFDefaults() {
        if (WindowsGUIUtils.isWindowsLAF()) {
            setMinIntDefault("Tree.rowHeight", 18);
        }
    }

    private static void setMinIntDefault(String key, int minValue) {
        int currentVal = UIManager.getInt(key);
        if (currentVal > 0 && currentVal < minValue)
            UIManager.put(key, minValue);
    }

}
