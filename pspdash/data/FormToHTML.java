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

package pspdash.data;

import java.util.Iterator;
import java.util.Map;

import pspdash.StringUtils;
import pspdash.HTMLUtils;

/** This class can export dashboard forms to HTML format, by replacing all
 *  &lt;INPUT&gt;, &lt;SELECT&gt;, and &lt;TEXTAREA&gt; elements with
 *  applicable HTML representing the corresponding data.
 */
public class FormToHTML {

    public static void translate(StringBuffer text, DataRepository data,
                                 String prefix) {
        int beg, end, end2;

        // find and replace all the <INPUT> tags.
        end = 0;
        Map attrs = null;
        String type;
        while ((beg = findTag(text, INPUT_TAG, end)) != -1 &&
               (end = findTagEnd(text, beg)) != -1) {
            attrs = getAttributes(text, beg, end, INPUT_TAG);
            type = getAttribute(attrs, "type");
            if ("text".equalsIgnoreCase(type))
                end = replaceMatch(text, beg, end, data,
                                   getAttribute(attrs, "name"), prefix, false);
            else if ("checkbox".equalsIgnoreCase(type))
                end = replaceMatch(text, beg, end, data,
                                   getAttribute(attrs, "name"), prefix, true);
            else {
                text.delete(beg, end);
                end = beg;
            }
        }

        // find and replace all the <SELECT> tags.
        end = 0;
        while ((beg = findTag(text, SELECT_TAG, end)) != -1 &&
               (end = findTagEnd(text, beg)) != -1 &&
               (end2 = findTag(text, SELECT_END, end)) != -1 &&
               (end2 = findTagEnd(text, end2)) != -1)
            end = replaceMatch(text, beg, end2, data,
                               getName(text, beg, end, SELECT_TAG),
                               prefix, false);

        // find and replace all the <TEXTAREA> tags.
        end = 0;
        while ((beg = findTag(text, TEXTAREA_TAG, end)) != -1 &&
               (end = findTagEnd(text, beg)) != -1 &&
               (end2 = findTag(text, TEXTAREA_END, end)) != -1 &&
               (end2 = findTagEnd(text, end2)) != -1) {
            end = replaceMatch(text, beg, end2, data,
                               getName(text, beg, end, TEXTAREA_TAG),
                               prefix, false);
            text.insert(end, "</pre>");
            text.insert(beg, "<pre>");
            end += 11;
        }
    }

    private static final String INPUT_TAG = "<input";
    private static final String SELECT_TAG = "<select";
    private static final String SELECT_END = "</select";
    private static final String TEXTAREA_TAG = "<textarea";
    private static final String TEXTAREA_END = "</textarea";

    // tag should already start with "<".
    public static int findTag(StringBuffer text, String tag, int start) {
        return StringUtils.indexOf(text, tag, start, true);
    }
    public static int findTagEnd(StringBuffer text, int start) {
        return StringUtils.indexOf(text, ">", start)+1;
    }
    public static Map getAttributes(StringBuffer text, int beg, int end,
                                    String tagStart) {
        beg += tagStart.length();
        end--; if (text.charAt(end) == '/') end--;
        String tagContents = text.substring(beg, end);
        return HTMLUtils.parseAttributes(tagContents);
    }
    public static String getAttribute(Map attrs, String attrName) {
        String result = (String) attrs.get(attrName);
        if (result == null)
            result = (String) attrs.get(attrName.toUpperCase());
        if (result == null) {
            Iterator i = attrs.keySet().iterator();
            String key;
            while (i.hasNext())
                if ("name".equalsIgnoreCase(key = (String) i.next())) {
                    result = (String) attrs.get(key);
                    break;
                }
        }
        return result;
    }
    public static String getName(StringBuffer text, int beg, int end,
                                 String tagStart) {
        return getAttribute(getAttributes(text, beg, end, tagStart), "name");
    }


    protected static int replaceMatch(StringBuffer text, int beg, int end,
                                      DataRepository data,
                                      String name, String prefix,
                                      boolean checkmark) {
        // parse the name as an InputName
        InputName inputName = new InputName(name, prefix);

        // look up the appropriate value
        SimpleData value = data.getSimpleValue(inputName.name);
        String result = "";
        if (checkmark)
            result = (value != null && value.test() ? "*": "");
        else if (value instanceof NumberData) {
            int numDigits = inputName.digitFlag();
            double val = ((NumberData) value).getDouble();
            if (inputName.hasFlag('%') || inputName.name.indexOf('%') != -1)
                result = PercentInterpreter.getString(val, numDigits);
            else
                result = DoubleData.formatNumber(val, numDigits);
        } else if (value != null)
            result = value.format();
        result = HTMLUtils.escapeEntities(result);

        // interpolate it into the StringBuffer
        text.replace(beg, end, result);

        // return the new value of end
        return beg + result.length();
    }
}
