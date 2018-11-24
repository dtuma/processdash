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

package net.sourceforge.processdash.tool.diff.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.diff.ui.LOCDiffDialog.Panel;

public class TemplateLOCDiffPanelLocator {

    public static final String EXTENSION_TAG = "locDiffPanel";

    public static List<LOCDiffDialog.Panel> getPanels() {
        try {
            // get the list of panels declared by extensions
            Map<String, Panel> panels = new TreeMap();
            for (Object ext : ExtensionManager
                    .getExecutableExtensions(EXTENSION_TAG, null)) {
                if (ext instanceof Panel) {
                    Panel panel = (Panel) ext;
                    panels.put(panel.getId(), panel);
                }
            }

            // return a sorted list of panels
            return new ArrayList<Panel>(panels.values());
        } catch (Exception e) {
            // if any errors occur, use hardcoded panel creation logic instead
            return HardcodedLOCDiffPanelLocator.getPanels();
        }
    }

}
