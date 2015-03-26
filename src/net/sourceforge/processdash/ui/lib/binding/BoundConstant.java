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

package net.sourceforge.processdash.ui.lib.binding;

import java.util.Map;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundConstant {

    public BoundConstant(BoundMap map, Element xml) {
        this((Map) map, xml);
    }

    protected BoundConstant(Map map, Element xml) {
        String id = xml.getAttribute("id");
        Object value = null;
        String type = XMLUtils.getAttribute(xml, "type", "text");
        if ("text".equalsIgnoreCase(type))
            value = getStringValue(xml);
        else if ("number".equalsIgnoreCase(type))
            value = getNumberValue(xml);

        map.put(id, value);

    }

    private String getStringValue(Element xml) {
        String value = xml.getAttribute("value");
        if (!StringUtils.hasValue(value))
            value = XMLUtils.getTextContents(xml);
        return value;
    }

    private Double getNumberValue(Element xml) {
        try {
            return new Double(getStringValue(xml));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
