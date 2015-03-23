// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer;

import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.BoundComboBox;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

public class DefaultPhaseSelector extends BoundComboBox {

    private static final String ID_PREFIX = DefaultPhaseSelector.class
            .getName();

    public static final String PHASE_LIST_ID = ID_PREFIX + ".Phase_List";

    public static final String INJ_PHASE_ID = ID_PREFIX + ".Injection_Phase";

    public static final String REM_PHASE_ID = ID_PREFIX + ".Removal_Phase";

    public DefaultPhaseSelector(BoundMap map, Element xml) {
        super(map, getPropertyName(xml), PHASE_LIST_ID);
    }

    private static String getPropertyName(Element xml) {
        String tagName = xml.getTagName().toLowerCase();
        if (tagName.indexOf("inj") != -1)
            return INJ_PHASE_ID;
        else
            return REM_PHASE_ID;
    }

}
