// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class HTMLUtils {

    public static String escapeEntities(String value) {
        StringTokenizer tok = new StringTokenizer(value, "<>&'\"", true);
        StringBuffer result = new StringBuffer();
        String token;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if      ("<".equals(token))  result.append("&lt;");
            else if (">".equals(token))  result.append("&gt;");
            else if ("&".equals(token))  result.append("&amp;");
            else if ("'".equals(token))  result.append("&apos;");
            else if ("\"".equals(token)) result.append("&quot;");
            else                         result.append(token);
        }
        return result.toString();
    }

    /** parse the inner contents of a tag as a set of
     * attrName=attrValue pairs
     * @param contents the text beginning after the tag name and ending
     *        before the closing ">" or "/>"
     */
    public static Map parseAttributes(String contents) {
        HashMap result = new HashMap();
        if (contents == null || contents.length() == 0) return result;

        String attrs = contents, name, value;
        int equalsPos, spacePos;
        while ((equalsPos = attrs.indexOf('=')) != -1) {
            name = attrs.substring(0, equalsPos).trim();
            attrs = attrs.substring(equalsPos+1).trim();
            if (attrs.length() == 0) break;
            if (name.startsWith(">") || name.startsWith("/>")) break;

            int endPos;
            if (attrs.charAt(0) == '\'' || attrs.charAt(0) == '"') {
                endPos = attrs.indexOf(attrs.charAt(0), 1);
                if (endPos == -1) {
                    value = attrs; attrs = "";
                } else {
                    value = attrs.substring(1, endPos);
                    attrs = attrs.substring(endPos+1);
                }
            } else if (attrs.charAt(0) == '[') {
                endPos = attrs.indexOf(']', 1);
                if (endPos == -1) {
                    value = attrs; attrs = "";
                } else {
                    value = attrs.substring(0, endPos+1);
                    attrs = attrs.substring(endPos+1);
                }
            } else {
                endPos = whitespacePos(attrs);
                if (endPos == -1) endPos = attrs.length();
                value = attrs.substring(0, endPos);
                attrs = attrs.substring(endPos);
            }
            result.put(name, value);
        }
        return result;
    }

    private static int whitespacePos(String t) {
        int result = t.indexOf(' ');
        if (result == -1) result = t.indexOf('\t');
        if (result == -1) result = t.indexOf('\r');
        if (result == -1) result = t.indexOf('\n');
        return result;
    }
}
