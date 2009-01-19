// Copyright (C) 2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.prefs.editor;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.prefs.PreferencesForm;
import net.sourceforge.processdash.ui.lib.binding.BoundCheckBox;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

import org.w3c.dom.Element;

public class PreferencesCheckBox extends BoundCheckBox {

    /** XML tag specifying if the widget has inverted "true" and "false" values */
    public static final String INVERTED_TAG = "inverted";

    private boolean isInverted = false;

    public PreferencesCheckBox(BoundMap map, Element xml) {
        super();

        // Setting the true and false values accordingly, depending on the
        //  "inverted" widget attribute.
        this.isInverted = Boolean.parseBoolean(xml.getAttribute(INVERTED_TAG));
        String trueValue = this.isInverted ? Boolean.FALSE.toString() :
                                             Boolean.TRUE.toString();
        String falseValue = this.isInverted ? Boolean.TRUE.toString() :
                                              Boolean.FALSE.toString();

        // Setting the value according to the current user's settings
        String settingName = xml.getAttribute(PreferencesForm.SETTING_TAG);
        map.put(settingName,
                Boolean.toString(Settings.getBool(settingName, false)));

        init(map, settingName, trueValue, falseValue);
    }

}
