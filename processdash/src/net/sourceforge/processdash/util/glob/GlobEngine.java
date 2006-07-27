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

package net.sourceforge.processdash.util.glob;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.util.glob.lexer.Lexer;
import net.sourceforge.processdash.util.glob.node.Start;
import net.sourceforge.processdash.util.glob.parser.Parser;

public class GlobEngine {

    /** Search through a list of tagged data for items that match a glob
     * expression.
     * 
     * @param expression the search expression
     *
     * @param taggedData
     * @param tagPrefix
     * @return
     */
    public static Set search(String expression, String tagPrefix,
            List taggedData) {
        if (expression == null || expression.trim().length() == 0)
            return Collections.EMPTY_SET;

        return search(expression, extractTaggedValues(taggedData, tagPrefix));
    }

    public static Set search(String expression, Map taggedData) {
        if (expression == null || expression.trim().length() == 0)
            return Collections.EMPTY_SET;

        Start s = compile(expression);
        if (s == null)
            return Collections.EMPTY_SET;

        GlobSearchEvaluator eval = new GlobSearchEvaluator(taggedData);
        eval.caseStart(s);

        Set matches = eval.getResult();
        if (matches == null || matches.isEmpty())
            return Collections.EMPTY_SET;
        else
            return matches;
    }



    private static Map extractTaggedValues(List taggedData, String tagPrefix) {
        Map taggedValues = new HashMap();

        Set currentData = null;
        String currentTag = null;
        for (Iterator i = taggedData.iterator(); i.hasNext();) {
            Object value = i.next();
            String valueAsTag = isTag(value, tagPrefix);

            if (valueAsTag != null) {
                if (valueAsTag.length() > 0) {
                    currentTag = valueAsTag;
                    currentData = (Set) taggedValues.get(currentTag);
                } else {
                    currentTag = null;
                    currentData = null;
                }

            } else if (currentTag != null) {
                if (currentData == null) {
                    currentData = new HashSet();
                    taggedValues.put(currentTag, currentData);
                }
                currentData.add(value);
            }
        }
        return taggedValues;
    }

    private static String isTag(Object value, String tagPrefix) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith(tagPrefix))
                return str.substring(tagPrefix.length()).toLowerCase();
        }

        return null;
    }

    public static boolean test(String expression, Object value) {
        if (expression == null || expression.trim().length() == 0
                || value == null)
            return false;

        Collection words = asCollection(value);
        if (words == null || words.isEmpty())
            return false;

        Start s = compile(expression);
        if (s == null)
            return false;

        GlobTestEvaluator eval = new GlobTestEvaluator(words);
        eval.caseStart(s);
        return eval.getResult();
    }

    private static Collection asCollection(Object value) {
        if (value instanceof Collection)
            return (Collection) value;
        else if (value instanceof SimpleData)
            return asCollection((SimpleData) value);
        else if (value instanceof SaveableData)
            return asCollection(((SaveableData) value).getSimpleValue());
        else
            return asCollection(String.valueOf(value));
    }

    private static Collection asCollection(SimpleData data) {
        if (!data.test())
            return null;
        if (data instanceof StringData)
            data = ((StringData) data).asList();
        if (data instanceof ListData) {
            Collection words = new HashSet();
            words.addAll(((ListData) data).asList());
            return words;
        } else
            return asCollection(data.format());
    }

    private static Collection asCollection(String text) {
        Collection words = new HashSet();
        String[] tokens = text.split("[,\u0000- ]+");
        words.addAll(Arrays.asList(tokens));
        return words;
    }


    private static final Map COMPILED_EXPRESSIONS = new Hashtable();

    private static final Logger logger = Logger
            .getLogger(GlobEngine.class.getName());


    private static Start compile(String expression) {
        Start result = (Start) COMPILED_EXPRESSIONS.get(expression);
        if (result == null) {
            try {
                Parser p = new Parser(new Lexer(new PushbackReader(
                        new StringReader(expression), 1024)));

                // Parse the input
                result = p.parse();
            } catch (Exception e) {
                logger.warning("Invalid glob expression: " + expression);
                e.printStackTrace();
            }
        }
        return result;
    }
}
