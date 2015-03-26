// Copyright (C) 2001-2008 Tuma Solutions, LLC
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


package net.sourceforge.processdash.tool.diff;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.Diff;
import net.sourceforge.processdash.util.StringUtils;


public class LOCDiff {

    static final Resources resource = Resources.getDashBundle("LOCDiff");

    protected int base, added, deleted, modified, total;
    WhitespaceCompareString[] linesA, linesB;
    boolean ignoreComments = true;
    private int tabWidth = 8;
    LanguageFilter filter;

    public LOCDiff(List filters,
                   String fileAStr, String fileBStr,
                   String fileBName, String options) {

        if (options == null || options.trim().length() == 0)
            options = getDefaultOptions(fileBName);
        parseInternalOptions(options);

        // Get an appropriate instance of LanguageFilter.
        filter = getFilter(filters, fileBName, fileBStr, options);

        // call flagComments on each file.
        StringBuffer fileA=new StringBuffer(), fileB=new StringBuffer();
        fileA.append(fileAStr);                fileB.append(fileBStr);
        canonicalizeLineEndings(fileA);        canonicalizeLineEndings(fileB);
        filter.highlightSyntax(fileA);         filter.highlightSyntax(fileB);
        fileAStr = fileA.toString();           fileBStr = fileB.toString();

        // break the files into lines, and create
        // WhitespaceCompareString objects for each line.
        linesA = convertLines(breakLines(fileAStr));
        linesB = convertLines(breakLines(fileBStr));

        // Perform the count.
        performCount();
    }

    public void dispose() {
        filter = null;
        linesA = linesB = null;
    }


    public int getBase()     { return base;     }
    public int getAdded()    { return added;    }
    public int getDeleted()  { return deleted;  }
    public int getModified() { return modified; }
    public int getTotal()    { return total;    }
    public LanguageFilter getFilter() { return filter; }

    void debug(String contents, String filename) {
        try {
            FileWriter fos = new FileWriter("c:\\temp\\" + filename);
            fos.write(contents);
            fos.close();
        } catch (IOException ioe) {}
    }

    private class WhitespaceCompareString {
        /** The original string */
        private String s;

        /** A cached version of the normalized string used for
         *  whitespace-agnostic comparison. Two WhitespaceCompareString
         *  objects are equal if their w components are equal. */
        private String w = null;

        /** Does string <code>s</code> contain any comments? */
        private boolean hasComments;

        WhitespaceCompareString(String s) {
            this.s = s;
            hasComments =
                (s.indexOf(LanguageFilter.COMMENT_START) != -1 ||
                 s.indexOf(LanguageFilter.COMMENT_END)   != -1);
        }

        public String toString() { return s; }
        public int hashCode() { return getWhite().hashCode(); }
        public boolean equals(Object o) {
            WhitespaceCompareString ow = (WhitespaceCompareString) o;
            return getWhite().equals(ow.getWhite());
        }

        public void ignoreCommentsChanged() {
            if (hasComments)    // if the string contains comments,
                w = null;       // discard the cached normalized version.
        }

        private String getWhite() {
            if (w == null) {
                String result = s;
                if (hasComments && ignoreComments)
                    result = stripComments(result);
                result = canonicalizeWhitespace(result);
                w = result;
            }
            return w;
        }
    }

    protected WhitespaceCompareString[] convertLines(List lines) {
        int i = lines.size();
        WhitespaceCompareString [] lineArray = new WhitespaceCompareString[i];
        while (i-- > 0)
            lineArray[i] = new WhitespaceCompareString((String) lines.get(i));
        return lineArray;
    }

    protected void setIgnoreComments(boolean b) {
        // if this does not reflect a change in the setting, then there
        if (ignoreComments == b) return; // is nothing to do.  return.

        ignoreComments = b; // save the new value.

        // invalidate the WhitespaceCompareString objects as necessary.
        for (int i = linesA.length;   i-- > 0; )
            linesA[i].ignoreCommentsChanged();
        for (int i = linesB.length;   i-- > 0; )
            linesB[i].ignoreCommentsChanged();
    }

    /** Count up all the differences between the files. */
    protected void performCount() {

        // Compute the differences between the two files.
        setIgnoreComments(true);
        Diff diff = new Diff(linesA, linesB);
        Diff.change change = diff.diff_2(false);

        // Begin by counting Base LOC and Total LOC
        int signifDeleted, signifAdded;
        base  = countSignificantLines(filter, linesA, 0, linesA.length);
        total = countSignificantLines(filter, linesB, 0, linesB.length);
        added = deleted = modified = 0;

        // Look at each change in the list.
        while (change != null) {
            // For each change, find out how many of the inserted and
            // deleted lines are significant (i.e., countable).
            signifDeleted = countSignificantLines
                (filter, linesA, change.line0, change.deleted);
            signifAdded = countSignificantLines
                (filter, linesB, change.line1, change.inserted);

            // calculate running metrics.
            if (signifAdded > signifDeleted) {
                added += (signifAdded - signifDeleted);
                modified += signifDeleted;
            } else {
                deleted += (signifDeleted - signifAdded);
                modified += signifAdded;
            }

            // advance to the next change in the list.
            change = change.link;
        }
    }

    protected int countSignificantLines(LanguageFilter filter,
                                        Object[] lines,
                                        int firstLineNum,
                                        int lineCount) {
        int result = 0;
        String line;
        while (lineCount-- > 0) {
            line = lines[firstLineNum + lineCount].toString();
            line = stripComments(line);
            if (isLineSignificant(filter, line))
                result++;
        }
        return result;
    }

    protected boolean isLineSignificant(LanguageFilter filter, String line) {
        return filter.isSignificant(line);
    }


    public void displayHTMLRedlines(PrintWriter out) {
        // Compute the differences between the two files.
        setIgnoreComments(false);
        Diff diff = new Diff(linesA, linesB);
        Diff.change c = diff.diff_2(false);

        // print a table header
        out.println("<table class='locDiff' cellpadding=0 cellspacing=0 border=0>");

        int bLineNumber = 0;
        while (c != null) {
            // print the normal region preceeding this change.
            printRegion(out, linesB, bLineNumber, c.line1, NORMAL);

            // print any deleted lines.
            printRegion(out, linesA, c.line0, c.line0 + c.deleted, DELETE);

            // print any added lines
            bLineNumber = c.line1 + c.inserted;
            printRegion(out, linesB, c.line1, bLineNumber, ADD);

            // go to the next change.
            c = c.link;
        }

        // print the normal region at the end of the document
        printRegion(out, linesB, bLineNumber, linesB.length, NORMAL);

        out.println("</table>");
    }

    protected static final int NORMAL = 0;
    protected static final int ADD    = 1;
    protected static final int DELETE = 2;

    private static final String[] ROW_BEGIN = {
        "<tr><td>&nbsp;</td><td><pre>",
        "<tr><td class='locAddHdr'>&nbsp;</td>"+
            "<td class='locAddBody'><pre>",
        "<tr><td class='locDelHdr'>&nbsp;</td>"+
            "<td class='locDelBody'><pre>" };

//    private static final String[] ROW_END = {
//        "</tt></td></tr>",
//        "</font></tt></b></td></tr>",
//        "</font></tt></strike></td></tr>" };
//    private static final String[] COMMENT_FONT = {
//        "<font color='32cd32'>",
//        "<font color='32cdcc'>",
//        "<font color='ff00ff'>" };

    protected void printRegion(PrintWriter out, Object[] lines,
                               int beginIndex, int endIndex,
                               int type) {
        if (endIndex <= beginIndex) return;

        out.print(ROW_BEGIN[type]);
        for (int lineNum = beginIndex;   lineNum < endIndex;   ) {
            String fixupLine = fixupLine(lines[lineNum].toString(), type);
            out.print(fixupLine);
            if (++lineNum < endIndex && !fixupLine.endsWith("</div>"))
                out.println();
        }
        out.println("</pre></td></tr>");
    }

    static final String COMMENT_START_STR =
        String.valueOf(LanguageFilter.COMMENT_START);
    static final String COMMENT_END_STR   =
        String.valueOf(LanguageFilter.COMMENT_END);

    protected String fixupLine(String line, int type) {
        StringBuffer buf = new StringBuffer(line);

        // convert tabs to spaces. -
        int tabPos = StringUtils.indexOf(buf, "\t"), spacesNeeded;
        while (tabPos != -1) {
            spacesNeeded = tabWidth - (tabPos - countInvisibleChars(buf, tabPos)) % tabWidth;
            buf.replace(tabPos, tabPos+1,
                        "        ".substring(0, spacesNeeded));
            tabPos = StringUtils.indexOf(buf, "\t", tabPos);
        }

        // escape HTML entities.
        StringUtils.findAndReplace(buf, "&",  "&amp;");
        StringUtils.findAndReplace(buf, "<",  "&lt;");
        StringUtils.findAndReplace(buf, ">",  "&gt;");
        StringUtils.findAndReplace(buf, "\"", "&quot;");

        // highlight comments.
        StringUtils.findAndReplace(buf, COMMENT_START_STR, "<span class='comment'>");
        StringUtils.findAndReplace(buf, COMMENT_END_STR,   "</span>");

        return buf.toString();
    }
    private int countInvisibleChars(StringBuffer s, int endPos) {
        int result = 0;
        while (endPos-- > 0)
            switch (s.charAt(endPos)) {
                case LanguageFilter.COMMENT_START:
                case LanguageFilter.COMMENT_END: result++;
            }
        return result;
    }

    protected List breakLines(String s) {
        StringTokenizer tok = new StringTokenizer(s, GOOD_LINE_ENDING, true);
        String line = "", token;
        ArrayList result = new ArrayList();
        boolean inComment = false;
        int commentStart, commentEnd;

        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if (token.equals(GOOD_LINE_ENDING)) {
                result.add(line);
                line = "";
            } else {
                line = token;
                if (inComment) line = COMMENT_START_STR + line;

                commentStart = line.lastIndexOf(LanguageFilter.COMMENT_START);
                commentEnd   = line.lastIndexOf(LanguageFilter.COMMENT_END);
                if (commentStart < commentEnd) inComment = false;
                if (commentStart > commentEnd) inComment = true;

                if (inComment) line = line + COMMENT_END_STR;
            }
        }
        if (line.length() > 0) result.add(line);

        return result;
    }

    protected void canonicalizeWhitespace(String [] lines) {
        for (int i = lines.length;   i-- > 0; )
            lines[i] = canonicalizeWhitespace(lines[i]);
    }
    protected String canonicalizeWhitespace(String str) {
        // begin by trimming whitespace from the string.  if the resulting
        // trimmed string is already canonical, return it (this will save
        // a LOT of memory)
        if (whitespaceIsCanonical(str = str.trim())) return str;

        StringBuffer buf = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(str, WHITESPACE);
        while (tok.hasMoreTokens())
            buf.append(" ").append(tok.nextToken());
        String result = buf.toString();
        if (result.length() > 0) result = result.substring(1);
        return result;
    }
    protected boolean whitespaceIsCanonical(String str) {
        // if the string contains any whitespace OTHER than the space
        // character, it is not canonical.
        for (int i = WHITESPACE.length();  i-- > 1;  )
            if (str.indexOf(WHITESPACE.charAt(i)) != -1)
                return false;

        // if the string contains two spaces next to each other, it is
        // not canonical.
        return (str.indexOf("  ") == -1);
    }
    private static final String WHITESPACE =
        " \t\r\n\f" + COMMENT_START_STR + COMMENT_END_STR;


    protected String getDefaultOptions(String fileName) {
        if (fileName == null) return null;

        String suffix = "";
        int dotPos = fileName.lastIndexOf('.');
        if (dotPos != -1)
            suffix = fileName.substring(dotPos).toLowerCase();

        String settingName = "pspdiff.options"+suffix;
        return Settings.getVal(settingName);
    }

    private void parseInternalOptions(String options) {
        if (options != null) {
            Matcher m = TABWIDTH_SETTING_PATTERN.matcher(options);
            if (m.find())
                tabWidth = Integer.parseInt(m.group(1));
        }
    }
    private static final Pattern TABWIDTH_SETTING_PATTERN =
        Pattern.compile("-tabWidth=(\\d+)", Pattern.CASE_INSENSITIVE);

    protected LanguageFilter getFilter(List languageFilters, String filename,
                                       String contents, String options) {
        LanguageFilter resultFilter = null;

        if (languageFilters != null) {
            Iterator i = languageFilters.iterator();
            int currentRating, resultRating = 0;
            while (i.hasNext()) {
                LanguageFilter currentFilter = null;
                try {
                    currentFilter = (LanguageFilter) i.next();
                } catch (ClassCastException e) {}
                if (currentFilter == null) continue;

                currentRating =
                    currentFilter.languageMatches(filename, contents, options);

                if (currentRating > resultRating) {
                    resultRating = currentRating;
                    resultFilter = currentFilter;
                }
            }
        }

        if (resultFilter == null) {
            resultFilter = new DefaultFilter();
        }

        this.filter = resultFilter;
        return resultFilter;
    }

    public static void printFiltersAndOptions(List languageFilters,
                                              PrintWriter out) {
        out.print("<h2>");
        out.print(resource.getHTML("Global_Options"));
        out.println("</h2><table border>");
        printOption(out, "-tabWidth=<i>n</i>",
                    resource.getString("Tab_Width_HTML"));
        out.println("</table>");

        if (languageFilters == null)
            return;

        Iterator i = languageFilters.iterator();
        LanguageFilter filter;
        String filterName;
        while (i.hasNext()) {
            filter = (LanguageFilter) i.next();

            filterName = AbstractLanguageFilter.getFilterName(filter);
            out.print("<h2>");
            out.print(filterName);
            out.println("</h2><table border>");
            printOption(out, "-lang=" + filterName,
                        resource.getString("Force_Explanation"));
            String[][] options = filter.getOptions();
            if (options != null)
                for (int j = 0;   j < options.length;   j++) {
                    String[] option = options[j];
                    if (option != null && option.length == 2)
                        printOption(out, option[0], option[1]);
                }
            out.println("</table>");
        }
    }
    private static void printOption(PrintWriter out, String opt, String text){
        out.print("<tr><td align=center><tt>");
        out.print(opt);
        out.print("</tt></td>\n<td><i>");
        out.print(text);
        out.println("</i></td></tr>");
    }

    protected String stripComments(String str) {
        if (str.indexOf(LanguageFilter.COMMENT_START) == -1)
            // efficiently handle degenerate case: if there are no
            // comments in the string, just return it without change.
            return str;

        StringBuffer buf = new StringBuffer(str);
        stripComments(buf);
        return buf.toString();
    }
    protected void stripComments(StringBuffer buf) {
        int beg, end;
        while ((beg = StringUtils.indexOf(buf, COMMENT_START_STR)) != -1) {
            end = StringUtils.indexOf(buf, COMMENT_END_STR, beg);
            if (end == -1) return;

            buf.delete(beg, end+1);
        }
    }

    static final String BAD_LINE_ENDING_A = "\r\n";
    static final String BAD_LINE_ENDING_B = "\r";
    static final String GOOD_LINE_ENDING  = "\n";

    protected static void canonicalizeLineEndings(StringBuffer buf) {
        int pos;
        boolean fixedSomeLines = false;

        while ((pos = StringUtils.indexOf(buf, BAD_LINE_ENDING_A)) != -1) {
            fixedSomeLines = true;
            buf.replace(pos, pos + BAD_LINE_ENDING_A.length(),
                        GOOD_LINE_ENDING);
        }

        if (!fixedSomeLines)
            while ((pos = StringUtils.indexOf(buf, BAD_LINE_ENDING_B)) != -1) {
                buf.replace(pos, pos + BAD_LINE_ENDING_B.length(),
                            GOOD_LINE_ENDING);
            }
    }

    public static String getCssText() {
        return "body { " +
                    "background-color: #ffffff " +
                "}\n" +
                "td.nowrap { " +
                    "white-space: nowrap " +
                "}\n" +
                "pre { " +
                    "margin-top: 0px; " +
                    "margin-bottom: 0px " +
                "}\n" +
                "table.locDiff td { " +
                    "white-space: pre; " +
                    "font-family: monospace; " +
                    "font-size: 10pt " +
                "}\n" +
                "span.comment { " +
                    "color: #32cd32 " +
                "}\n" +
                "td.locDelHdr { background-color: #ff0000 }\n" +
                "td.locDelBody { " +
                    "text-decoration: line-through; " +
                    "color: #ff0000 " +
                "}\n" +
                "td.locDelBody span.comment { " +
                    "color: #ff00ff " +
                "}\n" +
                "td.locAddHdr { background-color: #0000ff }\n" +
                "td.locAddBody { " +
                     "color: #0000ff; " +
                     "font-weight: bold " +
                 "}\n" +
                 "td.locAddBody span.comment { " +
                     "color: #32cdcc " +
                 "}\n";
    }
}
