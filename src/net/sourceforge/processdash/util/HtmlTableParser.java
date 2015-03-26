// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlTableParser {

    /**
     * Return true if the given string appears to contain HTML table data that
     * this class could parse.
     */
    public static boolean containsHtmlTableData(String text) {
        return ROW_END.matcher(text).find()
                && CELL_START.matcher(text).find()
                && CELL_END.matcher(text).find();
    }

    /**
     * Find a TABLE within a block of HTML, parse it, and return a
     * two-dimensional list of Strings with the contents of each table cell.
     * 
     * This class is not designed to handle nested HTML tables.
     * 
     * @param html
     *            the block of HTML; can contain other data before and after the
     *            table construct
     * @param plainText
     *            true if the cell data should be returned as plain text; false
     *            if HTML cell content should be returned.
     */
    public static List<List<String>> parseTable(String html, boolean plainText) {
        // Strip off any data before the <table> tag, if present.
        Matcher m = TABLE_START.matcher(html);
        if (m.find())
            html = html.substring(m.end());

        // strip off any data after the </table> tag, if present.
        m = TABLE_END.matcher(html);
        if (m.find())
            html = html.substring(0, m.start());

        List<List<String>> result = new ArrayList<List<String>>();
        int beg = 0;
        m = ROW_END.matcher(html);
        while (m.find()) {
            String oneRow = html.substring(beg, m.start());
            beg = m.end();
            parseOneRow(result, oneRow, plainText);
        }
        maybeFixupFirstRow(result);
        return result;
    }

    private static void parseOneRow(List<List<String>> rows, String oneRow,
            boolean plainText) {
        List<String> result = new ArrayList<String>();
        Matcher cellStart = CELL_START.matcher(oneRow);
        Matcher cellEnd = CELL_END.matcher(oneRow);
        int beg = 0;
        while (cellStart.find(beg)) {
            beg = cellStart.end();
            if (cellEnd.find(beg)) {
                String cellContents = oneRow.substring(beg, cellEnd.start());
                if (plainText)
                    cellContents = toPlainText(cellContents);
                result.add(cellContents);
                beg = cellEnd.end();
            }
        }
        if (result.size() > 0)
            rows.add(result);
    }

    private static String toPlainText(String cellContents) {
        cellContents = stripTags(cellContents.trim());
        cellContents = HTMLUtils.unescapeEntities(cellContents);
        return cellContents.replace((char) 160, ' ');
    }

    private static String stripTags(String html) {
        if (html.indexOf('<') == -1)
            return html;

        StringBuffer buf = new StringBuffer();
        Matcher m = TAG_PAT.matcher(html);
        int beg = 0;
        while (m.find()) {
            buf.append(html.substring(beg, m.start()));
            if (m.group(1) != null)
                buf.append("<br/>");
            beg = m.end();
        }
        buf.append(html.substring(beg));

        m = WHITESPACE_PAT.matcher(buf.toString());
        String result = m.replaceAll(" ");

        m = LINEBREAK_PAT.matcher(result);
        result = m.replaceAll("\n");

        return result;
    }

    /**
     * When people select data from HTML, it is easy for them to select only
     * part of the first row.  When that happens, the values in the first
     * row will appear to be in different column positions than the subsequent
     * rows.  If that appears to be the case, add some empty cells to the
     * beginning of the first row to make up the difference.
     */
    private static void maybeFixupFirstRow(List<List<String>> rows) {
        if (rows.size() > 1) {
            List<String> firstRow = rows.get(0);
            List<String> secondRow = rows.get(1);
            int lengthDiff = secondRow.size() - firstRow.size();
            for (int i = 0;  i < lengthDiff;  i++)
                firstRow.add(0, "");
        }
    }

    private static final Pattern TABLE_START = Pattern.compile("<table[^>]*>",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern TABLE_END = Pattern.compile("</table>",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern ROW_END = Pattern.compile("</tr>",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_START = Pattern.compile("<t[dh][^>]*>",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern CELL_END = Pattern.compile("</t[dh]>",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern TAG_PAT = Pattern.compile(
        "<(div|p|br|li)?[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern WHITESPACE_PAT = Pattern.compile("\\s+");

    private static final Pattern LINEBREAK_PAT = Pattern
            .compile("\\s*<br/>\\s*");
}
