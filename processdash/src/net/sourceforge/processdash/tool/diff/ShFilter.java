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




public class ShFilter extends AbstractLanguageFilter {

    private static final String[] COMMENT_STARTERS = { "#" };
    protected String[] getCommentStarters() { return COMMENT_STARTERS; }

    private static final String[] COMMENT_ENDERS   = { "\n" };
    protected String[] getCommentEnders()   { return COMMENT_ENDERS; }

    private static final String[] FILENAME_ENDINGS = {
        ".pl", ".sh", ".bash" };
    protected String[] getDefaultFilenameEndings() {
        return FILENAME_ENDINGS;
    }

    protected int doubleCheckFileContents(String contents, int match) {
        // if C programmers begin their program with a compiler
        // directive instead of a comment, that compiler directive
        // could be misinterpreted as a sh-style comment.  Detect that
        // scenario and return LANGUAGE_MISMATCH.
        if (contents.startsWith("#define") ||
            contents.startsWith("#include") ||
            contents.startsWith("#if"))
            return LANGUAGE_MISMATCH;
        else
            return match;
    }
}
