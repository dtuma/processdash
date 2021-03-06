// Copyright (C) 2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.prefs.editor;

import org.w3c.dom.Element;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.prefs.PreferencesForm;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.BoundPasswordField;

public class PreferencesPasswordField extends BoundPasswordField {

    public PreferencesPasswordField(BoundMap map, Element xml) {
        super(map, xml.getAttribute(PreferencesForm.SETTING_TAG));

        String settingName = xml.getAttribute(PreferencesForm.SETTING_TAG);
        map.put(settingName, Settings.getVal(settingName));
    }

}
