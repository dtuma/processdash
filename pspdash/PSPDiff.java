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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class PSPDiff {

    private int base, added, deleted, modified, total;
    WhitespaceCompareString[] linesA, linesB;
    boolean ignoreComments = true;
    LanguageFilter filter; ResourcePool pool;

    public PSPDiff(TinyWebServer web,
                   String fileAStr, String fileBStr,
                   String fileBName, String options) {

        // Get an appropriate instance of LanguageFilter.
        filter = getFilter(web, fileBName, fileBStr, options);

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
        if (pool != null) pool.release(filter);
        filter = null;
        pool = null;
        linesA = linesB = null;
    }


    public int getBase()     { return base;     }
    public int getAdded()    { return added;    }
    public int getDeleted()  { return deleted;  }
    public int getModified() { return modified; }
    public int getTotal()    { return total;    }

    private void debug(String contents, String filename) {
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
                String result = canonicalizeWhitespace(s);
                if (hasComments && ignoreComments)
                    result = stripComments(result);
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
            if (filter.isSignificant(line))
                result++;
        }
        return result;
    }


    public void displayHTMLRedlines(PrintWriter out) {
        // Compute the differences between the two files.
        setIgnoreComments(false);
        Diff diff = new Diff(linesA, linesB);
        Diff.change c = diff.diff_2(false);

        // print a table header
        out.println("<table cellpadding=0 cellspacing=0 border=0>");

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
        "<tr><td><tt>&nbsp;</tt></td><td><tt>",
        "<tr><td bgcolor='#0000ff'><tt>&nbsp;</tt></td>"+
            "<td><b><tt><font color='#0000ff'>",
        "<tr><td bgcolor='#ff0000'><tt>&nbsp;</tt></td>"+
            "<td><strike><tt><font color='#ff0000'>" };

    private static final String[] ROW_END = {
        "</tt></td></tr>",
        "</font></tt></b></td></tr>",
        "</font></tt></strike></td></tr>" };
    private static final String[] COMMENT_FONT = {
        "<font color='32cd32'>",
        "<font color='32cdcc'>",
        "<font color='ff00ff'>" };

    protected void printRegion(PrintWriter out, Object[] lines,
                               int beginIndex, int endIndex,
                               int type) {
        if (endIndex <= beginIndex) return;

        out.print(ROW_BEGIN[type]);
        for (int lineNum = beginIndex;   lineNum < endIndex;   ) {
            out.print(fixupLine(lines[lineNum].toString(), type));
            if (++lineNum < endIndex) out.println("<BR>");
        }
        out.println(ROW_END[type]);
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
            spacesNeeded = 8 - (tabPos - countInvisibleChars(buf, tabPos)) % 8;
            buf.replace(tabPos, tabPos+1,
                        "        ".substring(0, spacesNeeded));
            tabPos = StringUtils.indexOf(buf, "\t", tabPos);
        }

        // escape HTML entities.
        StringUtils.findAndReplace(buf, "&",  "&amp;");
        StringUtils.findAndReplace(buf, "<",  "&lt;");
        StringUtils.findAndReplace(buf, ">",  "&gt;");
        StringUtils.findAndReplace(buf, "\"", "&quot;");

        // convert spaces to &nbsp;
        StringUtils.findAndReplace(buf, " ", "&nbsp;");

        // highlight comments.
        StringUtils.findAndReplace(buf, COMMENT_START_STR, COMMENT_FONT[type]);
        StringUtils.findAndReplace(buf, COMMENT_END_STR,   "</font>");

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

        FileWriter fos = null;
        try {fos= new FileWriter("c:\\temp\\break");} catch (Exception e) {}

        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if (token.equals(GOOD_LINE_ENDING)) {
                result.add(line);
                try {fos.write(line); }  catch (Exception e) {}
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

        try { fos.close(); }catch (Exception e) {}
        return result;
    }

    protected void canonicalizeWhitespace(String [] lines) {
        for (int i = lines.length;   i-- > 0; )
            lines[i] = canonicalizeWhitespace(lines[i]);
    }
    protected String canonicalizeWhitespace(String str) {
        StringBuffer buf = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(str, WHITESPACE);
        while (tok.hasMoreTokens())
            buf.append(" ").append(tok.nextToken());
        String result = buf.toString();
        if (result.length() > 0) result = result.substring(1);
        return result;
    }
    private static final String WHITESPACE =
        " \t\r\n\f" + COMMENT_START_STR + COMMENT_END_STR;

    static List languageFilters = null;

    protected static void init(TinyWebServer web) {
        List filterNames = TemplateLoader.getLanguageFilters();
        Iterator i = filterNames.iterator();
        String name = null;
        while (i.hasNext()) try {
            name = (String) i.next();
            web.getRequest("/" + name, false);
        } catch (Exception e) {
            System.err.println("Couldn't initialize language filter '" +
                               name + "':");
            e.printStackTrace();
        }
        languageFilters = filterNames;
    }

    protected LanguageFilter getFilter(TinyWebServer web, String filename,
                                       String contents, String options) {
        if (languageFilters == null) init(web);

        Iterator i = languageFilters.iterator();
        String filterName;
        int            currentRating, resultRating = 0;
        LanguageFilter currentFilter, resultFilter = null;
        ResourcePool   currentPool,   resultPool   = null;
        while (i.hasNext()) {
            filterName = (String) i.next();
            try {
                currentPool = web.getCGIPool(filterName);
                currentFilter = (LanguageFilter) currentPool.get();
            } catch (Exception e) {
                // This could be a null pointer exception, because there was
                // no CGIPool for the given filter, or a class cast exception
                // if the CGI script returned is not a LanguageFilter.
                continue;
            }
            if (currentFilter == null) continue;

            currentRating =
                currentFilter.languageMatches(filename, contents, options);

            if (currentRating > resultRating) {
                if (resultPool != null) resultPool.release(resultFilter);
                resultRating = currentRating;
                resultFilter = currentFilter;
                resultPool   = currentPool;
            } else {
                currentPool.release(currentFilter);
            }
        }
        this.filter = resultFilter;
        this.pool = resultPool;
        return resultFilter;
    }

    protected String stripComments(String str) {
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

    protected void canonicalizeLineEndings(StringBuffer buf) {
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
}
