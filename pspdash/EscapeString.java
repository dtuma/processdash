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

public class EscapeString {

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
