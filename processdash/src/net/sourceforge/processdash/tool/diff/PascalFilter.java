// Copyright (C) 2001-2011 Tuma Solutions, LLC
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




public class PascalFilter extends AbstractLanguageFilter {

    private static final String[] COMMENT_STARTERS = { "//", "{" };
    protected String[] getCommentStarters() { return COMMENT_STARTERS; }

    private static final String[] COMMENT_ENDERS   = { "\n", "}" };
    protected String[] getCommentEnders()   { return COMMENT_ENDERS; }

    private static final String[] STRING_STARTERS = { "'" };
    protected String[] getStringStarters() { return STRING_STARTERS; }

    private static final char[] STRING_ESCAPES = { 0 };
    protected char[] getStringEscapes() { return STRING_ESCAPES; }

    private static final String[] STRING_EMBEDS = { "''" };
    protected String[] getStringEmbeds()   { return STRING_EMBEDS; }

    private static final String[] STRING_ENDERS = { "'" };
    protected String[] getStringEnders()   { return STRING_ENDERS; }

    private static final String[] FILENAME_ENDINGS = { ".pas" };
    protected String[] getDefaultFilenameEndings() {
        return FILENAME_ENDINGS;
    }
}
