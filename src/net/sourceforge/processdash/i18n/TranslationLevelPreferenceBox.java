// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.w3c.dom.Element;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.prefs.PreferencesForm;
import net.sourceforge.processdash.ui.lib.binding.BoundComboBox;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

public class TranslationLevelPreferenceBox extends BoundComboBox {

    private static final String LIST_NAME = "i18nLevel.options";
    private static final String VALUE_KEY = "VALUE";
    private static final String DISPLAY_KEY = "DISPLAY";
    private static final String[] ACCEPTABLE_VALUES = { "disabled", "low",
            "medium", "high" };

    public TranslationLevelPreferenceBox(BoundMap map, Element xml) {
        super(addOptionsAndPreferenceValueToMap(map, xml), //
                xml.getAttribute(PreferencesForm.SETTING_TAG), //
                LIST_NAME, VALUE_KEY, DISPLAY_KEY, null);
    }

    private static BoundMap addOptionsAndPreferenceValueToMap(BoundMap map,
            Element xml) {
        // register the list of acceptable values
        List comboValues = new ArrayList();
        ResourceBundle resources = map.getResources();
        for (String value : ACCEPTABLE_VALUES) {
            Map valueItem = new HashMap();
            String display = resources.getString("i18nLevel." + value);
            valueItem.put(VALUE_KEY, value);
            valueItem.put(DISPLAY_KEY, display);
            comboValues.add(valueItem);
        }
        map.put(LIST_NAME, comboValues);

        // register the current value of the user preference.
        String settingName = xml.getAttribute(PreferencesForm.SETTING_TAG);
        map.put(settingName, Settings.getVal(settingName, "high"));

        return map;
    }

}
