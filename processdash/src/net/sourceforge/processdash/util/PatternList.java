// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/** This class holds a collection of regular expressions, and can tell
 * whether a given string matches any one of the expressions in the list.
 * 
 * This class includes optimizations for beginning-of-string and end-of-string
 * matches:  if the only regular expression characters found are an initial
 * "^" and/or a final "$" (or neither of these), then the comparisons will
 * be made using (much faster) native String operations instead of Pattern
 * objects.
 */
public class PatternList {

    private boolean alwaysTrue = false;

    private List startsWithItems;

    private List endsWithItems;

    private List containsItems;

    private List equalsItems;

    private List regexpItems;

    public PatternList() {
    }

    public PatternList(Collection patterns) {
        for (Iterator i = patterns.iterator(); i.hasNext();) {
            String regexp = (String) i.next();
            addRegexp(regexp);
        }
    }

    public void addRegexp(String regexp) {
        if (!isPlainString(regexp)) {
            if (".*".equals(regexp))
                alwaysTrue = true;
            else
                regexpItems = addToList(regexpItems, Pattern.compile(regexp));
        }

        else if (regexp.startsWith("^")) {
            if (regexp.endsWith("$"))
                equalsItems = addToList(equalsItems, trim(regexp, true, true));
            else
                startsWithItems = addToList(startsWithItems, trim(regexp, true,
                        false));

        } else {
            if (regexp.endsWith("$"))
                endsWithItems = addToList(endsWithItems, trim(regexp, false,
                        true));
            else
                containsItems = addToList(containsItems, regexp);
        }
    }

    public boolean matches(String s) {
        if (alwaysTrue)
            return true;

        if (startsWithItems != null)
            for (Iterator i = startsWithItems.iterator(); i.hasNext();) {
                String item = (String) i.next();
                if (s.startsWith(item))
                    return true;
            }

        if (endsWithItems != null)
            for (Iterator i = endsWithItems.iterator(); i.hasNext();) {
                String item = (String) i.next();
                if (s.endsWith(item))
                    return true;
            }

        if (containsItems != null)
            for (Iterator i = containsItems.iterator(); i.hasNext();) {
                String item = (String) i.next();
                if (s.indexOf(item) != -1)
                    return true;
            }

        if (equalsItems != null)
            for (Iterator i = equalsItems.iterator(); i.hasNext();) {
                String item = (String) i.next();
                if (s.equals(item))
                    return true;
            }

        if (regexpItems != null)
            for (Iterator i = regexpItems.iterator(); i.hasNext();) {
                Pattern item = (Pattern) i.next();
                if (item.matcher(s).find())
                    return true;
            }

        return false;
    }

    private List addToList(List l, Object o) {
        if (l == null)
            l = new ArrayList();
        l.add(o);
        return l;
    }

    private String trim(String s, boolean firstChar, boolean lastChar) {
        if (firstChar)
            s = s.substring(1);
        if (lastChar)
            s = s.substring(0, s.length() - 1);
        return s;
    }

    private boolean isPlainString(String s) {
        for (int i = s.length(); i-- > 0;)
            if (POTENTIAL_REGEXP_CHARS.indexOf(s.charAt(i)) != -1)
                return false;
        return true;
    }

    private static final String POTENTIAL_REGEXP_CHARS = "()[]\\|.?*+{}";
}
