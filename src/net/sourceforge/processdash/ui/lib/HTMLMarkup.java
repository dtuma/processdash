// Copyright (C) 2007-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class HTMLMarkup {

    public static String textToHtml(String markupText) {
        return textToHtml(markupText, null);
    }

    public static String textToHtml(String markupText, String extraLinkAttrs) {
        if (markupText == null || markupText.length() == 0)
            return markupText;

        StringBuilder html = new StringBuilder();
        html.append(HTMLUtils.escapeEntities(markupText));
        markupHyperlinks(html, extraLinkAttrs, null);
        markupFormatting(html);
        return html.toString();
    }


    /**
     * Retrieve a collection of the hyperlinks that appear in the given markup
     * text.
     *
     * @return a Map whose keys are the display text of the hyperlink, and whose
     *         values are the associated URLs. The Map will return links in
     *         the order they appeared in the markup text.
     */
    public static LinkedHashMap<String, String> getHyperlinks(String markupText) {
        LinkedHashMap<String, String> links = new LinkedHashMap<String, String>();
        markupHyperlinks(new StringBuilder(markupText), null, links);
        return links;
    }


    private static void markupHyperlinks(StringBuilder html, String extraAttrs,
            Map<String, String> linkInfo) {
        int pos = 0;
        int len = html.length();
        while (pos < len) {
            Matcher m = HYPERLINK_PATTERN.matcher(html.subSequence(pos, len));
            if (m.find() == false)
                return; // no more hyperlinks found

            // record the boundaries of the hyperlink, as well as its target
            int beg = pos + m.start();
            int end = pos + m.end();
            String href = m.group();
            String text = href;

            // check to see if the hyperlink was part of a wiki-style link
            // of the form [URL text to display].  If so, extract the text to
            // display and adjust the hyperlink boundaries.
            if (beg > 0 && html.charAt(beg-1) == '[') {
                int linkEnd = html.indexOf("]", end);
                if (linkEnd != -1) {
                    text = html.substring(end, linkEnd).trim();
                    beg = beg - 1;
                    end = linkEnd + 1;
                }
            }

            // construct the URL and insert it into the string
            String link = "<a href=\"" + href + "\"" //
                    + (extraAttrs == null ? "" : " " + extraAttrs) + ">" //
                    + text + "</a>";
            html.replace(beg, end, link);
            pos = beg + link.length();
            len = html.length();

            // record hyperlink information if requested
            if (linkInfo != null)
                linkInfo.put(text, href);
        }
    }
    private static final Pattern HYPERLINK_PATTERN = Pattern.compile("http\\S+");

    private static void markupFormatting(StringBuilder html) {
        Matcher m = HORIZONTAL_RULE.matcher(html);
        html.replace(0, html.length(), m.replaceAll("<hr>"));
        StringUtils.findAndReplace(html, "\n", "<br>");
        StringUtils.findAndReplace(html, "  ", "&nbsp;&nbsp;");
    }

    private static final Pattern HORIZONTAL_RULE = Pattern
            .compile("(^|\n)(\\s*-){3,}\\s*(\n|$)");

}
