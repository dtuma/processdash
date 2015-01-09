// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;

public class LargeFontsHelper {

    public static void maybeInitialize() {
        maybeInitialize("/net/sourceforge/processdash/userPrefs/useLargeFonts");
    }

    public static void maybeInitialize(String prefName) {
        if (isEnabled(prefName))
            initialize();
    }

    private static boolean isEnabled(String prefName) {
        try {
            if (Boolean.getBoolean(LargeFontsHelper.class.getName()
                    + ".enabled"))
                return true;
        } catch (Exception e) {
        }

        int slashPos = prefName.lastIndexOf('/');
        String node = prefName.substring(0, slashPos);
        String key = prefName.substring(slashPos + 1);
        return Preferences.userRoot().node(node).getBoolean(key, false);
    }

    public static void initialize() {
        installLargerDefaultFonts();
        installTableRowHeightUI();
    }

    private static void installLargerDefaultFonts() {
        System.setProperty("swing.plaf.metal.userFont", "Dialog-17");
        System.setProperty("swing.plaf.metal.systemFont", "Dialog-17");
        System.setProperty("swing.plaf.metal.controlFont", "Dialog-Bold-17");
    }

    private static void installTableRowHeightUI() {
        String uiClassName = UIManager.getDefaults().getString("TableUI");
        if (BasicTableUI.class.getName().equals(uiClassName))
            UIManager.getDefaults().put("TableUI",
                TableRowHeightUI.class.getName());
    }

    public static class TableRowHeightUI extends BasicTableUI {

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);

            if (c instanceof JTable) {
                JTable table = (JTable) c;
                int currentRowHeight = table.getRowHeight();
                if (currentRowHeight < 21)
                    table.setRowHeight(21);
            }
        }

        public static ComponentUI createUI(JComponent c) {
            return new TableRowHeightUI();
        }

    }

}
