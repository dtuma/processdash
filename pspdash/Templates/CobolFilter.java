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


import pspdash.AbstractLanguageFilter;
import pspdash.StringUtils;

import java.util.StringTokenizer;

public class CobolFilter extends AbstractLanguageFilter {

    private static final String[] FILENAME_ENDINGS = {
        ".cob", ".cbl" };
    protected String[] getDefaultFilenameEndings() {
        return FILENAME_ENDINGS;
    }

    protected int doubleCheckFileContents(String contents, int match) {
        // check for a line number on the first line - an indication
        // that this might be a cobol program.
        if (Character.isDigit(contents.charAt(0)))
            return match + 30;
        else
            return match;
    }


    /** Insert flags in a file to highlight the syntax of the language.
     */
    public void highlightSyntax(StringBuffer file) {
        int lineBeg = 0, lineEnd;
        // iterate over all the lines in the file.
        while (lineBeg < file.length()) {
            lineEnd = StringUtils.indexOf(file, "\n", lineBeg);
            if (lineEnd == -1) lineEnd = file.length();

            // delete line numbers from the beginning of the line.
            while (lineBeg < lineEnd &&
                   Character.isDigit(file.charAt(lineBeg))) {
                file.deleteCharAt(lineBeg);
                lineEnd--;
            }

            // if this is a comment, flag it appropriately
            if (lineBeg < lineEnd &&
                "*/".indexOf(file.charAt(lineBeg)) != -1) {
                file.insert(lineEnd, COMMENT_END);
                file.insert(lineBeg, COMMENT_START);
                lineEnd += 2;
            }

            lineBeg = lineEnd+1;
        }
    }

    private boolean countEnd = false, countExit = false, countDot = false;

    protected void setOptions(String options) {
        options = options.toUpperCase();
        countEnd  = (options.indexOf("+END")  != -1);
        countExit = (options.indexOf("+EXIT") != -1);
        countDot  = (options.indexOf("+.")    != -1);
    }
    public boolean isSignificant(String line) {
        line = line.trim();
        if (line.length() == 0) return false; // don't count empty lines.
        if (line.equals(".")) return countDot;
        if (startsWithIgnoreCase(line, "END")) return countEnd;
        if (startsWithIgnoreCase(line, "EXIT")) return countExit;
        return true;
    }

    protected String[][] getOptions() { return OPTIONS; }
    protected String[][] OPTIONS = {
        { "+END", "Count </I><TT>END-</TT> <I>clauses as lines of code." },
        { "-END", ("Do not count </I><TT>END-</TT> <I>clauses as lines of "+
                   "code <B>(default)</B>.") },
        { "+EXIT", "Count </I><TT>EXIT</TT> <I>statements as lines of code." },
        { "-EXIT", ("Do not count </I><TT>EXIT</TT> <I>statements as lines "+
                    "of code <B>(default)</B>.") },
        { "+.", "Count a period on a line by itself as a line of code." },
        { "-.", ("Do not count a period on a line by itself as a line of "+
                 "code <B>(default)</B>.") }
    };
}
