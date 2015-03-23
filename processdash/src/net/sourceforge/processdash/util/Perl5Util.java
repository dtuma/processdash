// Copyright (C) 2002-2006 Tuma Solutions, LLC
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

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Limit dependence upon a particular regular expression library.
 *
 * This class has been used as the dashboard moved from the oromatcher
 * library, to jregex, to the java.util.regex package.
 * 
 * It automatically caches compiled regular expressions, and handles
 * regular expressions that are written in a perl-like "m/foo/ig" syntax.
 */
public class Perl5Util {

    public class RegexpException extends RuntimeException {}

    private static int MAX_CACHED_PATTERNS = 100;

    private static Map cachedPatterns =
        Collections.synchronizedMap(new MRUCache(MAX_CACHED_PATTERNS));

    /** Determine which character was used in lieu of the slashes */
    private static char getSlashChar(String expression) {
        char c = expression.charAt(0);
        if (c == 's' || c == 'm') return expression.charAt(1);
        return c;
    }

    /** Extract the pattern from a perl 5 regular expression */
    private static String getRegexp(String expression) {
        char c = getSlashChar(expression);
        int beg = expression.indexOf(c) + 1;
        int end = expression.indexOf(c, beg);
        return expression.substring(beg, end);
    }

    /** Extract the replacement from a perl 5 subst expression */
    private static String getReplacement(String expression) {
        char c = getSlashChar(expression);
        int end = expression.lastIndexOf(c);
        int beg = expression.lastIndexOf(c, end-1);
        return expression.substring(beg+1, end);
    }

    /** Extract the flags from a perl 5 regular expression */
    private static String getFlags(String expression) {
        char c = getSlashChar(expression);
        int beg = expression.lastIndexOf(c);
        String result = expression.substring(beg+1);

        int gPos = result.indexOf('g');
        if (gPos == 0) result = result.substring(1);
        else if (gPos != -1)
            result = result.substring(0,gPos) + result.substring(gPos+1);

        return result;
    }

    /** Translate string-style java flags into the numerical constants
     * used by the Pattern class. */
    private static int getJavaFlags(String flags) {
        return getJavaFlag(flags, 'i', 'I', Pattern.CASE_INSENSITIVE)
                | getJavaFlag(flags, 'm', 'M', Pattern.MULTILINE)
                | getJavaFlag(flags, 's', 'S', Pattern.DOTALL);
    }
    private static int getJavaFlag(String flags, char c, char C, int val) {
        return (flags.indexOf(c) == -1 && flags.indexOf(C) == -1 ? 0 : val);
    }

    /** Create a new Pattern object for the given perl 5 regular expression */
    private static Pattern makePattern(String expression) {
        String regex = getRegexp(expression);
        String flags = getFlags(expression);
        int javaFlags = getJavaFlags(flags);
        return Pattern.compile(regex, javaFlags);
    }

    /** Get or create a new Pattern object for the given perl 5
     * regular expression */
    private static Pattern getPattern(String expression) {
        Pattern result = (Pattern) cachedPatterns.get(expression);

        if (result == null) try {
            result = makePattern(expression);
            cachedPatterns.put(expression, result);
        } catch (Throwable t) {
            return null;
        }

        return result;
    }


    /** Return true if the given input matches a substring within the
     * given expression. */
    public boolean match(String expression, String input)
        throws RegexpException
    {
        try {
            return getPattern(expression).matcher(input).find();
        } catch (Throwable t) {
            throw new RegexpException();
        }
    }



    /** Perform the global substitution specified by expression on
     * the given input, and return the result. */
    public String substitute(String oldExpr, String newExpr, String input)
        throws RegexpException
    {
        return substitute(newExpr, input);
    }

    /** Perform the global substitution specified by expression on
     * the given input, and return the result. */
    public String substitute(String expression, String input)
        throws RegexpException
    {
        try {
            Matcher matcher = getPattern(expression).matcher(input);
            String replacement = getReplacement(expression);
            return matcher.replaceAll(replacement);
        } catch (Throwable t) {
            throw new RegexpException();
        }
    }

    /** Quote any special characters in the string to disable their
     * special meaning.
     *
     * The resulting string can be used either as:<ul>
     * <li>a matching expression which will match the literal string
     *     <code>s</code>
     * <li>a substitution replacement that will insert the literal
     *     string <code>s</code>
     * </ul>
     */
    public static String regexpQuote(String s) {
        final String metachars = ".[]\\()?*+{}|^$";

        StringBuffer result = new StringBuffer();
        int length = s.length();
        char c ;

        for (int i=0;   i < length;   i++) {
            c = s.charAt(i);
            if (metachars.indexOf(c) == -1)
                result.append(c);
            else {
                result.append('\\');
                result.append(c);
            }
        }

        return result.toString();
    }

}
