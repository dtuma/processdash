// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class DefectFieldMapper {

    private BoundMap map;
    private String destProperty;

    private String translationId;
    private Object translation;

    private String defaultValId;
    private String defaultValue;

    public DefectFieldMapper(BoundMap map, String destProperty,
            String translationId, String defaultValId) {
        this.map = map;
        this.destProperty = destProperty;
        this.translationId = translationId;
        this.defaultValId = defaultValId;

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

    private class PublicValue implements StringMapper {

        public String getString(String str) {
            if (translation instanceof StringMapper) {
                StringMapper sm = (StringMapper) translation;
                str = sm.getString(str);
            }

            if (StringUtils.hasValue(str))
                return str;
            else
                return defaultValue;
        }

    }

}
