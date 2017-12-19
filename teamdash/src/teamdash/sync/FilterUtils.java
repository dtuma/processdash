// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringMapper;

public class FilterUtils {

    public static String getXmlAttr(String tag, String attrName) {
        int[] pos = findAttr(tag, attrName);
        if (pos == null)
            return null;
        else
            return tag.substring(pos[1] + 1, pos[2]);
    }

    public static String discardXmlAttr(String tag, String attrName) {
        return replaceXmlAttr(tag, attrName, null);
    }

    public static String replaceXmlAttr(String tag, String attrName,
            Object replacement) {
        int[] pos = findAttr(tag, attrName);
        if (pos == null)
            return tag;

        String newVal = null;
        if (replacement instanceof StringMapper) {
            String currentVal = tag.substring(pos[1] + 1, pos[2]);
            newVal = ((StringMapper) replacement).getString(currentVal);
        } else if (replacement != null) {
            newVal = replacement.toString();
        }

        if (newVal == null)
            return tag.substring(0, pos[0]) + tag.substring(pos[2] + 1);
        else
            return tag.substring(0, pos[1] + 1) //
                    + HTMLUtils.escapeEntities(newVal) //
                    + tag.substring(pos[2]);
    }

    /**
     * @return null if the attribute is not found. Otherwise, an array of three
     *         integers, indicating
     *         <ul>
     *         <li>The position of the space character that precedes the
     *         attribute name</li>
     *         <li>The position of the opening quote delimiter</li>
     *         <li>The position of the ending quote delimiter</li>
     *         </ul>
     */
    private static int[] findAttr(String tag, String attrName) {
        if (tag == null)
            return null;

        String attrPat = ATTR_PATS.get(attrName);
        if (attrPat == null) {
            attrPat = " " + attrName + "=";
            ATTR_PATS.put(attrName, attrPat);
        }
        int pos = tag.indexOf(attrPat);
        if (pos == -1)
            return null;

        int delimPos = pos + attrName.length() + 2;
        char delimChar = tag.charAt(delimPos);
        int end = tag.indexOf(delimChar, delimPos + 1);
        if (end == -1)
            return null;

        return new int[] { pos, delimPos, end };
    }

    private static Map<String, String> ATTR_PATS = new HashMap();

}
