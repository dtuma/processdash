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


package net.sourceforge.processdash.util;

import jregex.Matcher;
import jregex.Pattern;
import jregex.Replacer;

import java.util.*;


/*
    Timing Notes, made with a large input dataset:
    - using the old regex package (oromatcher), dashboard took 24 seconds to
        start up.
    - using the new regex package (jregex), dashboard took 23 seconds to start.
    - using both, dashboard took 31 seconds to start.

    Inference:
    - 14 seconds of activity elsewhere
    - oromatcher took 10 seconds
    - jregex took 7 seconds

 */



/** Limit dependence upon a particular regular expression library.
 *
 * Eventually when process dashboard moves to Java 1.4, we can just
 * use the built-in java.util.regex package.  But this won't be a
 * realistic option until Java 1.4 is available on all platforms,
 * including Mac OS X
 */
public class Perl5Util {

    public class RegexpException extends RuntimeException {}

    private static int MAX_CACHED_PATTERNS = 100;

    private static Map cachedPatterns =
        Collections.synchronizedMap(new MRUCache());

//*old*/ private com.oroinc.text.perl.Perl5Util perl5 =
//*old*/     new com.oroinc.text.perl.Perl5Util();

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

    /** Create a new Pattern object for the given perl 5 regular expression */
    private static Pattern makePattern(String expression) {
        String regex = getRegexp(expression);
        String flags = getFlags(expression);
        if (flags.length() > 0)
            return new Pattern(regex, flags);
        else
            return new Pattern(regex);
    }

    /** Get or create a new Pattern object for the given perl 5
     * regular expression */
    private static synchronized Pattern getPattern(String expression) {
        Pattern result = (Pattern) cachedPatterns.get(expression);

        if (result == null) try {
            result = makePattern(expression);
            cachedPatterns.put(expression, result);
        } catch (Throwable t) {
            return null;
        }

        return result;
    }

    private Matcher matcher = null;


    /** Return true if the given input matches a substring within the
     * given expression. */
    public synchronized boolean match(String expression, char input[])
        throws RegexpException
    {
        try {
 /*new*/    matcher = getPattern(expression).matcher();
 /*new*/    matcher.setTarget(input, 0, input.length);
 /*new*/    boolean newResult = matcher.find();
//*old*/    boolean oldResult = perl5.match(expression, input);
//*cmp*/   if (newResult != oldResult)
//*cmp*/      showError("match disagreement("+expression+","+
//*cmp*/                         new String(input)+")"+
//*cmp*/                         "\n\toldResult=" + oldResult +
//*cmp*/                         "\n\tnewResult=" + newResult);
//*old*/    return oldResult;
 /*new*/    return newResult;
        } catch (Throwable t) {
            throw new RegexpException();
        }
    }

    /** Return true if the given input matches a substring within the
     * given expression. */
    public synchronized boolean match(String expression, String input)
        throws RegexpException
    {
        try {
 /*new*/    matcher = getPattern(expression).matcher();
 /*new*/    matcher.setTarget(input);
 /*new*/    boolean newResult = matcher.find();
//*old*/    boolean oldResult = perl5.match(expression, input);
//*cmp*/    if (newResult != oldResult)
//*cmp*/        showError("match disagreement("+expression+","+input+")"+
//*cmp*/                           "\n\toldResult=" + oldResult +
//*cmp*/                           "\n\tnewResult=" + newResult);
//*old*/    return oldResult;
 /*new*/    return newResult;
        } catch (Throwable t) {
            throw new RegexpException();
        }
    }



    private static Map cachedReplacers =
        Collections.synchronizedMap(new MRUCache());

    /** Create a new Replacer object for the given perl 5 substitution
     * expression */
    private static synchronized Replacer makeReplacer(String expression) {
        Pattern pattern = makePattern(expression);
        String replacement = getReplacement(expression);
        return new Replacer(pattern, replacement, true);
    }

    /** Get or create a new Replacer object for the given perl 5
     * substitution expression */
    private static synchronized Replacer getReplacer(String expression) {
        Replacer result = (Replacer) cachedReplacers.get(expression);

        if (result == null) try {
            result = makeReplacer(expression);
            cachedReplacers.put(expression, result);
        } catch (Throwable t) {
            return null;
        }

        return result;
    }




    /** Perform the global substitution specified by expression on
     * the given input, and return the result. */
    public synchronized String substitute(String expression, String input)
        throws RegexpException
    {
        return substitute(expression, expression, input);
    }

    /** Perform the global substitution specified by expression on
     * the given input, and return the result. */
    public synchronized String substitute(String oldExpr,
                                          String newExpr, String input)
        throws RegexpException
    {
        try {
 /*new*/    matcher = null;
 /*new*/    Replacer r = getReplacer(newExpr);
 /*new*/    String newResult = null;
 /*new*/    synchronized (r) { newResult = r.replace(input); }

//*old*/    String oldResult = perl5.substitute(oldExpr, input);
//*cmp*/    if (!newResult.equals(oldResult)) {
//*cmp*/        showError("substitute disagreement!"+
//*cmp*/                           "\n\toldExpr=" + StringUtils.findAndReplace
//*cmp*/                           (oldExpr, "\n", "\\n") +
//*cmp*/                           "\n\tnewExpr=" + StringUtils.findAndReplace
//*cmp*/                           (newExpr, "\n", "\\n") +
//*cmp*/                           "\n\tinput="+input+
//*cmp*/                           "\n\told="+oldResult+
//*cmp*/                           "\n\tnew="+newResult);
//*cmp*/        }

//*old*/    return oldResult;
 /*new*/    return newResult;
        } catch (Throwable t) {
            throw new RegexpException();
        }
    }

    /** Returns the part of the input preceding that last match found */
    public String preMatch() {
 /*new*/if (matcher == null) return null;
 /*new*/String newResult = matcher.prefix();
//*old*/String oldResult = perl5.preMatch();
//*cmp*/if (!newResult.equals(oldResult))
//*cmp*/    showError("preMatch disagreement\n\told="+oldResult+
//*cmp*/              "\n\tnew="+newResult);
//*old*/return oldResult;
 /*new*/return newResult;
    }

    /** Returns the part of the input following that last match found */
    public String postMatch() {
 /*new*/if (matcher == null) return null;
 /*new*/String newResult = matcher.suffix();
//*old*/String oldResult = perl5.postMatch();
//*cmp*/if (!newResult.equals(oldResult))
//*cmp*/    showError("postMatch disagreement\n\told="+oldResult+
//*cmp*/              "\n\tnew="+newResult);
//*old*/return oldResult;
 /*new*/return newResult;
    }

    /** Returns the contents of one of the parenthesized subgroups of
     * the last match found. */
    public String group(int group) {
 /*new*/if (matcher == null) return null;
 /*new*/String newResult = matcher.group(group);
//*old*/String oldResult = perl5.group(group);
//*cmp*/if (!newResult.equals(oldResult))
//*cmp*/    showError("group disagreement\n\told="+oldResult+
//*cmp*/              "\n\tnew="+newResult);
//*old*/return oldResult;
 /*new*/return newResult;
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

    private static void showError(String error) {
        System.out.println(error);
        javax.swing.JOptionPane.showMessageDialog
            (null, new javax.swing.JEditorPane("text/plain", error));
    }

}
