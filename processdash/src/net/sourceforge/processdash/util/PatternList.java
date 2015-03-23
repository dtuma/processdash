// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** This class holds a collection of regular expressions, and can tell
 * whether a given string matches any one of the expressions in the list.
 * 
 * This class includes optimizations for certain common regular expression
 * constructs.  If no regular expression characters are found, or if the only
 * constructs encountered are one of the following:
 * <ul>
 * <li>an initial "<tt>^</tt>", to match beginning of string</li>
 * <li>a final "<tt>$</tt>", to match end-of-string</li>
 * <li>a set of pipe-delimited choices "<tt>(foo|bar|baz)</tt>"</li>
 * <li>an optional group "<tt>(foo)?</tt>"</li>
 * </ul>
 * then the tests will be made using (much faster) native String operations
 * instead of Pattern objects.
 */
public class PatternList {

    protected boolean alwaysTrue = false;

    protected List startsWithItems;

    protected List endsWithItems;

    protected List containsItems;

    protected List equalsItems;

    protected List regexpItems;

    public PatternList() {
    }

    public PatternList(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            addRegexp(patterns[i]);
        }
    }

    public PatternList(Collection patterns) {
        for (Iterator i = patterns.iterator(); i.hasNext();) {
            String regexp = (String) i.next();
            addRegexp(regexp);
        }
    }

    public PatternList addRegexp(String regexp) throws PatternSyntaxException {
        if (!isPlainString(regexp)) {
            if (".*".equals(regexp))
                alwaysTrue = true;
            else {
                List brokenDown = tryToBreakDown(regexp);
                if (brokenDown != null) {
                    for (Iterator i = brokenDown.iterator(); i.hasNext();) {
                        addRegexp((String) i.next());
                    }
                } else {
                    regexpItems = addToList(regexpItems, Pattern
                            .compile(regexp));
                }
            }
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
            else if (regexp.length() == 0)
                alwaysTrue = true;
            else
                containsItems = addToList(containsItems, regexp);
        }

        return this;
    }

    /** @since 1.14.4 */
    public PatternList addLiteralEquals(String s) {
        equalsItems = addToList(equalsItems, s);
        return this;
    }

    /** @since 1.14.4 */
    public PatternList addLiteralStartsWith(String s) {
        startsWithItems = addToList(startsWithItems, s);
        return this;
    }

    /** @since 1.14.4 */
    public PatternList addLiteralEndsWith(String s) {
        endsWithItems = addToList(endsWithItems, s);
        return this;
    }

    /** @since 1.14.4 */
    public PatternList addLiteralContains(String s) {
        containsItems = addToList(containsItems, s);
        return this;
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

    public boolean isAlwaysTrue() {
        return alwaysTrue;
    }

    public List getContainsItems() {
        return containsItems;
    }

    public List getEndsWithItems() {
        return endsWithItems;
    }

    public List getEqualsItems() {
        return equalsItems;
    }

    public List getRegexpItems() {
        return regexpItems;
    }

    public List getStartsWithItems() {
        return startsWithItems;
    }

    protected List addToList(List l, Object o) {
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

    protected List tryToBreakDown(String s) {
        if (!mightBeSimpleRegexp(s))
            return null;

        s = StringUtils.findAndReplace(s, ")?", "|)");
        try {
            return breakDownSimplePattern(null, s);
        } catch (Exception e) {
            return null;
        }
    }

    protected List breakDownSimplePattern(List l, String s) throws Exception {
        if (isPlainString(s))
            return addToList(l, s);

        int end = s.indexOf(')');
        if (end != -1) {
            int beg = s.lastIndexOf('(', end);
            if (beg == -1)
                throw new Exception("can't break down");

            String[] choices = s.substring(beg+1, end).split("\\|", -1);
            for (int i = 0; i < choices.length; i++) {
                String oneChoice = s.substring(0, beg) + choices[i]
                        + s.substring(end + 1);
                l = breakDownSimplePattern(l, oneChoice);
            }
        } else if (s.indexOf('|') != -1) {
            String[] choices = s.split("\\|", -1);
            for (int i = 0; i < choices.length; i++) {
                l = breakDownSimplePattern(l, choices[i]);
            }
        } else {
            throw new Exception("can't break down");
        }

        return l;
    }

    protected boolean isPlainString(String s) {
        return containsRegexpChars(s, -1) == false;
    }

    protected boolean mightBeSimpleRegexp(String s) {
        return containsRegexpChars(s, 3) == false;
    }

    protected boolean containsRegexpChars(String s, int cutoff) {
        for (int i = s.length(); i-- > 0;)
            if (POTENTIAL_REGEXP_CHARS.indexOf(s.charAt(i)) > cutoff)
                return true;
        return false;
    }

    private static final String POTENTIAL_REGEXP_CHARS = "()|?[]\\.*+{}";
}
