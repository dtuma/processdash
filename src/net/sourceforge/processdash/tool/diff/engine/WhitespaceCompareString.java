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

package net.sourceforge.processdash.tool.diff.engine;

import java.util.StringTokenizer;


public class WhitespaceCompareString {

    /** The original string */
    private String s;

    /**
     * A cached version of the normalized string used for whitespace-agnostic
     * comparison. Two WhitespaceCompareString objects are equal if their w
     * components are equal.
     */
    private String w;

    WhitespaceCompareString(String s) {
        this.s = s;
        this.w = null;
    }

    public String toString() {
        return s;
    }

    public int hashCode() {
        return getWhite().hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof WhitespaceCompareString) {
            WhitespaceCompareString that = (WhitespaceCompareString) o;
            return this.getWhite().equals(that.getWhite());
        } else {
            return false;
        }
    }

    private String getWhite() {
        if (w == null)
            w = canonicalizeWhitespace(s);

        return w;
    }


    public static String canonicalizeWhitespace(String str) {
        // begin by trimming whitespace from the string.
        String result = str.trim();

        // if the resulting trimmed string is already canonical, return it.
        // (this will save a LOT of memory)
        if (whitespaceIsCanonical(result))
            return result;

        StringBuilder buf = new StringBuilder();
        StringTokenizer tok = new StringTokenizer(result, WHITESPACE);
        while (tok.hasMoreTokens())
            buf.append(" ").append(tok.nextToken());
        result = buf.toString();
        if (result.length() > 0)
            result = result.substring(1);
        return result;
    }

    public static boolean whitespaceIsCanonical(String str) {
        // if the string contains any whitespace OTHER than the space
        // character, it is not canonical.
        for (int i = WHITESPACE.length(); i-- > 1;)
            if (str.indexOf(WHITESPACE.charAt(i)) != -1)
                return false;

        // if the string contains two spaces next to each other, it is
        // not canonical.
        return (str.indexOf("  ") == -1);
    }

    private static final String WHITESPACE = " \t\r\n\f";

}
