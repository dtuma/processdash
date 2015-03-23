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

package net.sourceforge.processdash.data.repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Class to find and perform renaming operations against a map of data values.
 *
 * A simple renaming operation is a mapping whose value begins
 * with "<=".  The key is the new name for the data, and the rest
 * of the value is the original name.  So the following lines in a
 * datafile: <pre>
 *    foo="bar
 *    baz=<=foo
 * </pre> would be equivalent to the single line `baz="bar'.
 * Simple renaming operations are correctly transitive, so <pre>
 *   foo=1
 *   bar=<=foo
 *   baz=<=bar
 * </pre> is equivalent to `baz=1'. This will work correctly, no matter
 * what order the lines appear in.
 *
 * Pattern match renaming operations are mappings whose value
 * begins with >~.  The key is a pattern to match, and the value
 * is the substitution expression.  So <pre>
 *    foo 1="one
 *    foo 2="two
 *    foo ([0-9])+=>~$1/foo
 * </pre> would be equivalent to the lines <pre>
 *    1/foo="one
 *    2/foo="two
 * </pre> The pattern must match the original name of the element - not
 * any renamed variant.  Therefore, pattern match renaming operations
 * <b>cannot</b> be chained.  A pattern match operation <b>can</b> be
 * the <b>first</b> renaming operation in a transitive chain, but will
 * neverbe used as the second or subsequent operations in a chain.
 *
 * Finally, renaming operations can influence dataFiles below them in
 * the datafile inheritance chain.  This is, in fact, the #1 reason for
 * the renaming mechanism.  It allows a process datafile to rename
 * elements that appear in end-user project datafiles.
 */
public class DataRenamingOperation implements Serializable {

    private static final String SIMPLE_RENAME_PREFIX = "<=";

    private static final String PATTERN_RENAME_PREFIX = ">~";

    private static final Logger logger = Logger
            .getLogger(DataRenamingOperation.class.getName());

    private DataRenamingOperation() {
    }

    public static boolean isOperation(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith(SIMPLE_RENAME_PREFIX)
                    || str.startsWith(PATTERN_RENAME_PREFIX))
                return true;
        }
        return false;
    }

    private static class Simple extends DataRenamingOperation {
        String oldName;

        public Simple(String oldName) {
            this.oldName = oldName;
        }
    }

    private static class Regexp extends DataRenamingOperation {
        Pattern pattern;

        String replacement;

        public Regexp(String pattern, String replacement) {
            this.pattern = Pattern.compile(pattern);
            this.replacement = replacement;
        }

        public String maybeRename(String name) {
            Matcher m = pattern.matcher(name);
            if (m.matches()) {
                StringBuffer result = new StringBuffer();
                m.appendReplacement(result, replacement);
                return result.toString();
            } else {
                return null;
            }
        }
    }

    /** Find renaming operations in the given map, and initialize them for
     * later use.
     * 
     * Renaming operations will be String values in the map with certain
     * prefixes.  This method will replace those values with objects of type
     * {@link DataRenamingOperation}.
     * 
     * @param dest the map of values to search for data renaming operations.
     */
    static void initRenamingOperations(Map dest) {
        for (Iterator i = dest.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String name = (String) e.getKey();
            Object val = e.getValue();
            Object initVal = maybeInitRenamingOperation(name, val);
            if (initVal != val)
                e.setValue(initVal);
        }
    }

    /** Look at a single name / value pair, and possibly initialize it as
     * a data renaming operation.
     * 
     * @param name the name of a data element to consider
     * @param val the value of that data element
     * @return if <tt>val</tt> is not a renaming operation, it is returned
     *    unchanged (i.e., this will be the identity function).  If it is a
     *    renaming operation, and appropriate {@link DataRenamingOperation}
     *    will be returned.
     */
    static Object maybeInitRenamingOperation(String name, Object val) {
        if (val instanceof String) {
            String str = (String) val;

            if (str.startsWith(SIMPLE_RENAME_PREFIX)) {
                String oldName = str.substring(SIMPLE_RENAME_PREFIX.length());
                return new Simple(oldName);

            } else if (str.startsWith(PATTERN_RENAME_PREFIX)) {
                String pattern = name;
                String replace = str.substring(PATTERN_RENAME_PREFIX.length());
                try {
                    return new Regexp(pattern, replace);
                } catch (PatternSyntaxException pse) {
                    logger.severe("Malformed renaming operation '" + name + "="
                            + str + "'");
                }
            }
        }

        return val;
    }

    /** Perform a set of data renaming operations on a group of data values.
     * 
     * @param values the data values that possibly need renaming
     * @param renameOpSource a map which might contain
     *     {@link DataRenamingOperation} objects, created by the
     *     {@link #initRenamingOperations(Map)} method
     * @return <tt>true</tt> if any renames occurred, false otherwise.
     */
    static boolean performRenames(Map values, Map renameOpSource) {
        if (values == null || renameOpSource == null)
            return false;

        boolean dataWasRenamed = false;
        Map renamingOperations = new HashMap();
        List patternRenamingOperations = new LinkedList();

        // Perform a pass through the operations source map, looking for
        // renaming operations.
        for (Iterator i = renameOpSource.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String name = (String) e.getKey();
            if (values.containsKey(name))
                continue;

            if (e.getValue() instanceof Simple)
                renamingOperations.put(name, ((Simple) e.getValue()).oldName);
            else if (e.getValue() instanceof Regexp)
                patternRenamingOperations.add(e.getValue());
        }

        // For each pattern-style renaming operation, find data names that
        // match the pattern and add the corresponding renaming operation to
        // the regular naming operation list.
        for (Iterator i = patternRenamingOperations.iterator(); i.hasNext();) {
            Regexp r = (Regexp) i.next();

            // scan the value map for matching names.
            for (Iterator j = values.keySet().iterator(); j.hasNext();) {
                String oldName = (String) j.next();
                String newName = r.maybeRename(oldName);
                if (newName != null)
                    renamingOperations.put(newName, oldName);
            }
        }

        // Now perform the renaming operations.
        while (!renamingOperations.isEmpty()) {
            String newName = (String) renamingOperations.keySet().iterator()
                    .next();
            String oldName = (String) renamingOperations.remove(newName);
            Object val = values.remove(oldName);
            while (val == null
                    && (oldName = (String) renamingOperations.remove(oldName)) != null)
                val = values.remove(oldName);

            if (val != null) {
                values.put(newName, val);
                dataWasRenamed = true;
            }
        }

        return dataWasRenamed;
    }
}
