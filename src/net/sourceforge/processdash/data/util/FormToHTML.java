// Copyright (C) 2001-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.util;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


/** This class can export dashboard forms to HTML format, by replacing all
 *  &lt;INPUT&gt;, &lt;SELECT&gt;, and &lt;TEXTAREA&gt; elements with
 *  applicable HTML representing the corresponding data.
 */
public class FormToHTML {

    public static void translate(StringBuffer text, DataRepository data,
            String prefix) {
        int beg, end, end2;
        data.waitForCalculations();

        // dashboard forms have clearly delineated table cells because
        // of the squares drawn around <INPUT> tags.  When those
        // squares disappear, the tables are hard to read.  Enhance
        // readability by adding thin table borders.  Also, define a
        // style that will help Excel format numbers properly.
        if ((beg = findTag(text, STYLE_END, 0)) != -1) {
            text.insert(beg, EXTRA_STYLE_DECL);
        } else if ((beg = findTag(text, HEAD_END, 0)) != -1) {
            text.insert(beg, "</style>")
                .insert(beg, EXTRA_STYLE_DECL)
                .insert(beg, "<style>");
        }

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

    private static final String STYLE_END = "</style";
    private static final String HEAD_END = "</head";
    private static final String INPUT_TAG = "<input";
    private static final String SELECT_TAG = "<select";
    private static final String SELECT_END = "</select";
    private static final String TEXTAREA_TAG = "<textarea";
    private static final String TEXTAREA_END = "</textarea";



    private static final String EXCEL_TIME_FMT_CLASS = "excelTimeFmt";
    private static final String EXCEL_CLASS_DECL = " class='" +
        EXCEL_TIME_FMT_CLASS + "'";
    private static final String EXCEL_TIME_FMT_STYLE =
        "vnd.ms-excel.numberformat: [h]\\:mm";
    private static final String EXCEL_STYLE_DECL = " style='" +
        EXCEL_TIME_FMT_STYLE + "'";

    private static final String EXTRA_STYLE_DECL =
        " table { border: 1px solid grey; border-collapse: collapse } " +
        " td    { border: 1px solid grey } " +
        " ." + EXCEL_TIME_FMT_CLASS + " { " + EXCEL_TIME_FMT_STYLE + "} ";

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
        boolean isTimeValue = false;
        if (checkmark) {
            result = (value != null && value.test() ? "&#9745;": "&#9744;");
        } else if (value instanceof NumberData) {
            int numDigits = inputName.digitFlag();
            double val = ((NumberData) value).getDouble();
            if (InterpreterFactory.isPercentInputName(inputName))
                result = FormatUtil.formatPercent(val, numDigits);
            else if (InterpreterFactory.isTimeInputName(inputName)) {
                result = FormatUtil.formatTime(val, true);
                isTimeValue = true;
            } else
                result = FormatUtil.formatNumber(val, numDigits);
        } else if (value != null) {
            result = HTMLUtils.escapeEntities(value.format());
        }

        // interpolate it into the StringBuffer
        text.replace(beg, end, result);
        end = beg + result.length();

        if (isTimeValue)
            end += insertTimeFormattingForExcel(text, beg);

        // return the new value of end
        return end;
    }

    private static int insertTimeFormattingForExcel(StringBuffer text, int end) {
        int beg = StringUtils.lastIndexOf(text, "<", end);
        if (beg == -1) return 0;

        // take a look at the sequence of characters from the previous "<"
        // to the end of input, and see if that looks like a <td> tag.
        CharSequence possibleTag = text.subSequence(beg, end);
        Matcher m = TD_TAG_PATTERN.matcher(possibleTag);
        if (!m.matches())
            // the newly replaced text doesn't appear to be inside a <td>
            // tag.  Do nothing and exit.
            return 0;

        int contentBeg = beg + m.start(1);
        String tagContents = m.group(1);

        m = CLASS_ATTR_PATTERN.matcher(tagContents);
        if (!m.find()) {
            // there is no "class" attribute present.  Add one.
            text.insert(contentBeg, EXCEL_CLASS_DECL);
            return EXCEL_CLASS_DECL.length();
        }

        // The tag already has a "class" attribute, and Excel isn't
        // standards-conformant enough (surprise, surprise) to recognize
        // multiple whitespace-delimited class names.  So we'll need to fall
        // back and use the "style" attribute.

        m = STYLE_ATTR_PATTERN.matcher(tagContents);
        if (m.find()) {
            // the tag contents already contains a "style" attribute.  We just
            // need to amend it.
            int insertPos = contentBeg + m.end();
            text.insert(insertPos, EXCEL_TIME_FMT_STYLE + "; ");
            return EXCEL_TIME_FMT_STYLE.length() + 2;
        } else {
            // the tag does not contain a "style" attribute.  Add one.
            text.insert(contentBeg, EXCEL_STYLE_DECL);
            return EXCEL_STYLE_DECL.length();
        }
    }
    private static Pattern TD_TAG_PATTERN =
        Pattern.compile("<t[dh]([^<>]*)>\\s*", Pattern.CASE_INSENSITIVE);
    private static Pattern CLASS_ATTR_PATTERN =
        Pattern.compile("\\s+class\\s*=", Pattern.CASE_INSENSITIVE);
    private static Pattern STYLE_ATTR_PATTERN =
        Pattern.compile("\\s+style\\s*=\\s*['\"]", Pattern.CASE_INSENSITIVE);
}
