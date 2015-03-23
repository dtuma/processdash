// Copyright (C) 2000-2003 Tuma Solutions, LLC
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

public class EscapeString {

    public static String escape(String src, char escChar, String escapeChars) {
        if (src == null || src.length() == 0 || escapeChars == null)
            return src;

        StringBuffer result = new StringBuffer();
        char c;
        for (int i = 0;   i < src.length();  i++) {
            c = src.charAt(i);
            if (c == escChar || escapeChars.indexOf(c) != -1)
                result.append(escChar);
            result.append(c);
        }

        return result.toString();
    }

    public static String escape(String src, char escChar,
                                String escapeChars, String escapeTransl) {
        if (src == null || src.length() == 0 ||
            escapeChars == null || escapeTransl == null)
            return src;

        StringBuffer result = new StringBuffer();
        char c;
        int pos;
        for (int i = 0;   i < src.length();  i++) {
            c = src.charAt(i);
            if ((pos = escapeChars.indexOf(c)) != -1)
                result.append(escChar).append(escapeTransl.charAt(pos));
            else if (c == escChar)
                result.append(escChar).append(escChar);
            else
                result.append(c);
        }

        return result.toString();
    }

    public static String unescape(String src, char escChar,
                                  String escapeChars, String escapeTransl) {
        if (src == null || src.indexOf(escChar) == -1 ||
            escapeChars == null || escapeTransl == null)
            return src;

        StringBuffer result = new StringBuffer();
        int pos, which;
        char w;

        while ((pos = src.indexOf(escChar)) != -1) {
            result.append(src.substring(0, pos));
            w = src.charAt(pos+1);
            which = escapeTransl.indexOf(w);
            result.append(which == -1 ? w : escapeChars.charAt(which));

            src = src.substring(pos+2);
        }

        result.append(src);

        return result.toString();
    }

    // escapeChars contains all the characters to be 'escaped' (not incl escChar)
    public static String applyEscape (String src,
                                      char   escChar,
                                      String escapeChars) {
        String t = src;
        if (src != null) {
            int escIndex = t.indexOf (escChar);
            while (escIndex >= 0) {
                if (escIndex == 0)
                    t = escChar + escChar + t.substring (escIndex + 1);
                else if (escIndex == t.length () - 1)
                    t = t.substring (0, escIndex) + escChar + escChar;
                else
                    t = t.substring (0, escIndex) + escChar + escChar +
                        t.substring (escIndex + 1);
                escIndex = t.indexOf (escChar, escIndex + 2);
            }
            for (int ii = 0; ii < escapeChars.length(); ii++) {
                char chk;
                try {
                    chk = escapeChars.charAt (ii);
                } catch (Exception e) { continue; }
                escIndex = t.indexOf (chk);
                while (escIndex >= 0) {
                    if (escIndex == 0)
                        t = escChar + chk + t.substring (escIndex + 1);
                    else if (escIndex == t.length () - 1)
                        t = t.substring (0, escIndex) + escChar + chk;
                    else
                        t = t.substring (0, escIndex) + escChar + chk +
                            t.substring (escIndex + 1);
                    escIndex = t.indexOf (chk, escIndex + 2);
                }

            }
        }
        return t;
    }

    public static String removeEscape (String src,
                                       char   escChar) {
        String t = src;
        if (src != null) {
            int ii = 0;
            while ( ii < t.length()) {
                if (t.charAt (ii) == escChar) {
                    if (ii == 0)
                        t = t.substring (ii + 1);
                    else if (ii == t.length() - 1)
                        t = t.substring (0, ii);
                    else
                        t = t.substring (0, ii) + t.substring (ii + 1);
                }
                ii++;
            }
        }
        return t;
    }

    public static int indexOf (String eString, int ch,
                               int fromIndex, char escChar) {
        int fi = Math.max (fromIndex, 0);
        int idx;
        do {
            idx = eString.indexOf (ch, fi);
            fi = idx + 1;
        } while ((idx > fromIndex) && (eString.charAt (fi - 1) == escChar));
        return idx;
    }

}
