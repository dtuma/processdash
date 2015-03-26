// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.XMLUtils;

public class RedactFilterUtils {

    public static List getExtensions(RedactFilterData data, String tagName) {
        List result = ExtensionManager.getExecutableExtensions(tagName, null);
        for (Iterator i = result.iterator(); i.hasNext();) {
            Object item = i.next();
            if (isDisabled(data, item)) {
                i.remove();
            } else {
                try {
                    setFields(data, item);
                } catch (Exception e) {
                    System.out.println("Unexpected error while "
                            + "initializing object of type " + item.getClass());
                    e.printStackTrace();
                    i.remove();
                }
            }
        }
        return result;
    }

    private static boolean isDisabled(RedactFilterData data, Object item) {
        return isDisabled(data, item.getClass().getAnnotation(EnabledFor.class));
    }

    private static boolean isDisabled(RedactFilterData data,
            EnabledFor enabledFor) {
        // check to see the EnabledFor annotation is present. If not, we do
        // not auto-disable it.
        if (enabledFor == null)
            return false;

        // Look at the filter IDs that enable this item. If one matches the
        // current set of filters, we do not auto-disable it.
        for (String id : enabledFor.value())
            if (data.isFiltering(id))
                return false;

        // This item defines an EnabledFor annotation, but none of its filter
        // IDs are in effect. The item is disabled.
        return true;
    }

    public static void setFields(RedactFilterData data, Object item) {
        Class clazz = item.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                Class fieldType = field.getType();
                if (fieldType == RedactFilterData.class) {
                    setField(item, field, data);
                } else if (fieldType == Boolean.TYPE) {
                    EnabledFor enabledFor = field
                            .getAnnotation(EnabledFor.class);
                    if (enabledFor != null)
                        setField(item, field, !isDisabled(data, enabledFor));
                } else {
                    Object value = data.getHelper(fieldType);
                    if (value != null)
                        setField(item, field, value);
                }
            }
            clazz = clazz.getSuperclass();
        }

        try {
            item.getClass().getMethod("afterPropertiesSet").invoke(item);
        } catch (Exception e) {
        }
    }

    private static void setField(Object item, Field f, Object value) {
        try {
            f.setAccessible(true);
            f.set(item, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final StringMapper HASH_STRING = new StringMapper() {
        public String getString(String str) {
            return hash(str);
        }
    };

    public static final String hash(String s) {
        return hash(s, false, 99);
    }

    public static final String hash(String s, boolean ignoreCase, int maxChars) {
        if (s == null || s.length() == 0)
            return "";

        if (ignoreCase)
            s = s.toLowerCase();
        s = maybeEscapeXml(s);
        int hashcode = Math.abs(s.hashCode());

        StringBuilder result = new StringBuilder();
        while (hashcode > 0 && result.length() < maxChars) {
            int oneDigit = hashcode % 26;
            result.append(HASH_CHARS.charAt(oneDigit));
            hashcode = hashcode / 26;
        }
        return result.toString();
    }

    private static final String HASH_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String maybeEscapeXml(String s) {
        for (String entity : XML_ENTITIES)
            if (s.contains(entity))
                return s;
        return XMLUtils.escapeAttribute(s);
    }

    private static final String[] XML_ENTITIES = new String[] { "&lt;", "&gt;",
            "&amp;", "&quot;", "&apos;", "&#" };


    public static int getIndentLevel(String line) {
        String indent = getXmlAttr(line, "indentLevel");
        if (indent == null)
            return -1;
        else
            return Integer.parseInt(indent);
    }

    public static String getXmlTagName(String tag) {
        if (tag == null)
            return null;

        int pos = tag.indexOf('<');
        if (pos == -1)
            return null;

        int beg = pos + 1;
        if (tag.charAt(beg) == '/')
            return null;

        int end = tag.indexOf(' ', beg);
        if (end == -1)
            return null;

        return tag.substring(beg, end);
    }

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
            return tag.substring(0, pos[1] + 1) + newVal
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

    public static String[] slurpLines(Reader r) throws IOException {
        BufferedReader in = bufferReader(r);
        ArrayList<String> result = new ArrayList<String>();
        String line;
        while ((line = in.readLine()) != null)
            result.add(line);
        return result.toArray(new String[result.size()]);
    }

    public static BufferedReader bufferReader(Reader r) {
        if (r instanceof BufferedReader) {
            return (BufferedReader) r;
        } else {
            return new BufferedReader(r);
        }
    }

}
