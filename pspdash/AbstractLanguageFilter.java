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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/** No-op implementation of the LanguageFilter interface.
 */
public class AbstractLanguageFilter implements LanguageFilter {

    protected static final String COMMENT_START_STR =
        String.valueOf(COMMENT_START);
    protected static final String COMMENT_END_STR =
        String.valueOf(COMMENT_END);

    /** Judge whether this filter is capable of acting as a filter for a
     *  particular file.
     *
     * The default implementation gives the file 50 points if its filename
     * matches, and 30 points if it begins with a matching comment.
     */
    public int languageMatches(String filename, String contents,
                               String options) {
        int result = 0, len;
        String s;

        if (filename != null && filename.length() > 0) {
            String[] filenameEndings = getFilenameEndings();
            if (filenameEndings != null)
                for (int i = filenameEndings.length;   i-- > 0; )
                    if (endsWithIgnoreCase(filename, filenameEndings[i])) {
                        result += 50;
                        break;
                    }
        }

        if (contents != null && contents.length() > 0) {
            String[] commentStarters = getCommentStarters();
            if (commentStarters != null) {
                contents = contents.trim();     // skip initial whitespace.
                for (int i = commentStarters.length;   i-- > 0; )
                    if (startsWithIgnoreCase(contents, commentStarters[i])) {
                        result += 30;
                        break;
                    }
                }
        }

        if (result > 0)
            setOptions(options == null ? "" : options);

        return result;
    }

    protected boolean endsWithIgnoreCase(String s, String e) {
        if (s == null || e == null) return false;
        return s.regionMatches(true, s.length()-e.length(), e, 0, e.length());
    }

    protected boolean startsWithIgnoreCase(String s, String e) {
        if (s == null || e == null) return false;
        return s.regionMatches(true, 0, e, 0, e.length());
    }

    protected void setOptions(String options) { }



    /** Insert flags in a file to highlight the syntax of the language. */
    public void highlightSyntax(StringBuffer file) {
        flagComments(file, getCommentStarters(), getCommentEnders());
    }



    /** Test a line of code to see if it is countable.
     */
    public boolean isSignificant(String line) {
        return (line.trim().length() > 0); // count nonblank lines.
    }



    /** Display HTML describing the options provided by this language filter.
     */
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        out.write("Content-type: text/html\r\n\r\n".getBytes());
        writeOptions(new PrintWriter(out));
    }
    protected void writeOptions(PrintWriter out) {
        out.println("<p><i>No options available.</i></p>");
    }



    protected void flagComments(StringBuffer file,
                                String[] commentStarters,
                                String[] commentEnders) {
        if (commentStarters == null || commentEnders == null) return;

        int begin, b, end, i, selectedStyle, pos = 0;
        String beginPattern, endPattern;
        while (true) {
            begin = Integer.MAX_VALUE;
            selectedStyle = -1;
            for (i = commentStarters.length;   i-- > 0; ) {
                b = StringUtils.indexOf(file, commentStarters[i], pos);
                if (b != -1 && b < begin) {
                    begin = b;
                    selectedStyle = i;
                }
            }

            // no comments found? we're done.
            if (selectedStyle == -1) break;

            // retrieve the strings that describe the comment style we found.
            beginPattern = commentStarters[selectedStyle];
            endPattern   = commentEnders  [selectedStyle];

            // search for the end of the comment.
            end = StringUtils.indexOf
                (file, endPattern, begin + beginPattern.length());
            if (end == -1)
                end = file.length();
            else
                end += endPattern.length();

            // insert comment indicators before and after the string.
            file.insert(end,   COMMENT_END_STR);
            file.insert(begin, COMMENT_START_STR);

            pos = end+2;
        }
        StringUtils.findAndReplace(file, "\n" + COMMENT_END,
                                   COMMENT_END + "\n");
    }

    protected String[] getCommentStarters() { return null; }
    protected String[] getCommentEnders()   { return null; }
    protected String[] getFilenameEndings() { return null; }

}
