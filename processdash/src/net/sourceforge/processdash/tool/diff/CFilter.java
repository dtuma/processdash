// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.diff;




public class CFilter extends AbstractLanguageFilter {

    private static final String[] COMMENT_STARTERS = { "//", "/*" };
    protected String[] getCommentStarters() { return COMMENT_STARTERS; }

    private static final String[] COMMENT_ENDERS   = { "\n", "*/" };
    protected String[] getCommentEnders()   { return COMMENT_ENDERS; }

    private static final String[] FILENAME_ENDINGS = {
        ".c", ".cpp", ".c++", ".h", ".java" };
    protected String[] getDefaultFilenameEndings() {
        return FILENAME_ENDINGS;
    }

    private boolean countBraces = false;

    protected void setOptions(String options) {
        countBraces = (options.indexOf("+{") != -1);
    }

    public boolean isSignificant(String line) {
        line = line.trim();
        switch (line.length()) {
        case 0: return false;
        case 1: return (countBraces ||
                        "{}".indexOf(line.charAt(0)) == -1);
        default: return true;
        }
    }

    public String[][] getOptions() { return OPTIONS; }
    protected String[][] OPTIONS = {
        { "+{", ("Count a curly brace on a line by itself "+
                 "as a line of code.") },
        { "-{", ("Do not count a curly brace on a line by itself "+
                 "as a line of code <b>(default)</b>.") }
    };

    protected int doubleCheckFileContents(String contents, int match) {
        // C programmers might begin their program with a compiler
        // directive instead of a comment - detect that and add in the
        // related points.
        if (contents.startsWith("#include") ||
            contents.startsWith("#define") ||
            contents.startsWith("#if"))
            return match + 30;
        else
            return match;
    }
}
