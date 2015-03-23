// Copyright (C) 2006-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util.glob;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/** Class for testing matches against expressions containing the glob
 * characters '?' and '*'
 * 
 * Non-glob characters are compared in a case-insensitive manner.
 * 
 * Special, efficient handling is provided for:<ul>
 *   <li>plain text expressions (containing neither '?' nor '*')</li>
 *   <li>prefix expressions (regular text ending in '*')</li>
 *   <li>suffix expressions (regular text starting with '*')</li>
 *   </ul>
 */
public class GlobPattern {

    private Evaluator evaluator;

    public GlobPattern(String glob) {
        if (glob == null)
            throw new NullPointerException();
        evaluator = getEvaluator(glob);
    }

    public boolean test(String text) {
        return evaluator.test(text);
    }


    public static boolean test(String glob, String text) {
        if (glob == null || text == null)
            return false;

        Evaluator e = getEvaluator(glob);
        return e.test(text);
    }

    private static final Map COMPILED_GLOBS = new Hashtable();

    private static Evaluator getEvaluator(String glob) {
        Evaluator result = (Evaluator) COMPILED_GLOBS.get(glob);
        if (result == null) {
            result = buildEvaluator(glob);
            COMPILED_GLOBS.put(glob, result);
        }
        return result;
    }

    private static Evaluator buildEvaluator(String glob) {
        int splatPos = glob.indexOf('*');
        int lastSplatPos = glob.lastIndexOf('*');
        int questPos = glob.indexOf('?');

        if (questPos == -1 && splatPos == -1)
            return new PlainTextEvaluator(glob);
        else if (questPos == -1 && splatPos == (glob.length()-1))
            return new PrefixEvaluator(glob);
        else if (questPos == -1 && lastSplatPos == 0)
            return new SuffixEvaluator(glob);
        else
            return new PatternEvaluator(glob);
    }

    private interface Evaluator {
        public boolean test(String s);
    }

    private static class PlainTextEvaluator implements Evaluator {
        String text;

        public PlainTextEvaluator(String text) {
            this.text = text;
        }

        public boolean test(String s) {
            return text.equalsIgnoreCase(s);
        }

    }

    private static class PrefixEvaluator implements Evaluator {
        String prefix;

        public PrefixEvaluator(String glob) {
            this.prefix = glob.substring(0, glob.length()-1);
        }

        public boolean test(String s) {
            return s != null
                && s.regionMatches(true, 0, prefix, 0, prefix.length());
        }
    }

    private static class SuffixEvaluator implements Evaluator {
        String suffix;

        public SuffixEvaluator(String glob) {
            this.suffix = glob.substring(1);
        }

        public boolean test(String s) {
            return s != null
                && s.regionMatches(true, s.length() - suffix.length(),
                        suffix, 0, suffix.length());
        }
    }

    private static class PatternEvaluator implements Evaluator {
        Pattern pattern;

        public PatternEvaluator(String glob) {
            StringTokenizer tok = new StringTokenizer(glob, "?*", true);
            StringBuffer regexp = new StringBuffer();
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken();
                if ("?".equals(token))
                    regexp.append(".");
                else if ("*".equals(token))
                    regexp.append(".*");
                else
                    regexp.append("\\Q").append(token).append("\\E");
            }
            pattern = Pattern.compile(regexp.toString(),
                    Pattern.CASE_INSENSITIVE);
        }

        public boolean test(String s) {
            return s != null && pattern.matcher(s).matches();
        }
    }
}
