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

import java.util.Collection;
import java.util.Iterator;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class DefectFieldMapper {

    private BoundMap map;
    private Collection allowedValues;
    private String destProperty;

    private String translationId;
    private Object translation;

    private String defaultValId;
    private String defaultValue;

    public DefectFieldMapper(BoundMap map, String destProperty,
            String translationId, String defaultValId, Collection allowedValues) {
        this.map = map;
        this.destProperty = destProperty;
        this.translationId = translationId;
        this.defaultValId = defaultValId;
        this.allowedValues = allowedValues;

        map.addPropertyChangeListener(new String[] { translationId,
                defaultValId }, this, "update");
        update();
    }


    public void update() {
        translation = map.get(translationId);
        defaultValue = StringUtils.asString(map.get(defaultValId));

        if (translation == null && defaultValue == null)
            map.put(destProperty, null);
        else
            map.put(destProperty, new PublicValue());
    }

    private String scrubForAllowedValue(String s) {
        if (allowedValues == null || s == null)
            return s;

        s = s.trim();
        if (allowedValues.contains(s))
            return s;
        if (s.length() == 0)
            return null;

        // see if the string is present with a different capitalization; if
        // so, canonicalize the case.
        for (Iterator i = allowedValues.iterator(); i.hasNext();) {
            String oneVal = (String) i.next();
            if (s.equalsIgnoreCase(oneVal))
                return oneVal;
        }

        // the value isn't present.  Return null.
        return null;
    }

    private class PublicValue implements StringMapper {

        public String getString(String str) {
            if (translation instanceof StringMapper) {
                StringMapper sm = (StringMapper) translation;
                str = sm.getString(str);
            }

            str = scrubForAllowedValue(str);

            if (StringUtils.hasValue(str))
                return str;
            else
                return defaultValue;
        }

    }

}
