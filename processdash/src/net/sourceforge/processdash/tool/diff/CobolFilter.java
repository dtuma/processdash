// Copyright (C) 2001-2003 Tuma Solutions, LLC
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


import net.sourceforge.processdash.util.StringUtils;


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

    public String[][] getOptions() { return OPTIONS; }
    protected String[][] OPTIONS = {
        { "+END",  resources.getString("Cobol.End_Count_HTML") },
        { "-END",  resources.getString("Cobol.End_Ignore_HTML") },
        { "+EXIT", resources.getString("Cobol.Exit_Count_HTML") },
        { "-EXIT", resources.getString("Cobol.Exit_Ignore_HTML") },
        { "+.",    resources.getString("Cobol.Period_Count_HTML") },
        { "-.",    resources.getString("Cobol.Period_Ignore_HTML") }
    };
}
