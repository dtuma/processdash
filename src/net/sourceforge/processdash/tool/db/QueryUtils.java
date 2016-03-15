// Copyright (C) 2013-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;


public class QueryUtils {

    public static final String PROJECT_CRITERIA = "##Project in";

    public static final String WBS_CRITERIA = "##WBS Element";

    public static final String LABEL_CRITERIA = "##Label Group";

    public static final String IS_CURRENT_CRITERIA = "##Is Current";

    public static final String IMPOSSIBLE_CONDITION = " 1 = 0 ";


    private static Logger logger = Logger.getLogger(QueryUtils.class
            .getName());

    public static DatabasePlugin getDatabasePlugin(DataContext data) {
        return getDatabasePlugin(data, false);
    }

    public static DatabasePlugin getDatabasePlugin(DataContext data,
            boolean waitForAllProjects) {
        ListData pluginItem = (ListData) data
                .getValue(DatabasePlugin.DATA_REPOSITORY_NAME);
        if (pluginItem == null)
            return null;

        DatabasePlugin databasePlugin = (DatabasePlugin) pluginItem.get(0);
        if (waitForAllProjects)
            waitForAllProjects(databasePlugin);
        return databasePlugin;
    }

    public static void waitForAllProjects(DatabasePlugin databasePlugin) {
        databasePlugin.getObject(ProjectLocator.class).getKeyForProject(
            "wait-for-all-projects", null);
    }

    public static <T> List<T> pluckColumn(List data, int column) {
        List result = new ArrayList(data.size());
        for (Iterator i = data.iterator(); i.hasNext();) {
            Object[] row = (Object[]) i.next();
            result.add(row[column]);
        }
        return result;
    }

    public static <K, V> Map<K, V> mapColumns(List data, int... columns) {
        Map result = new LinkedHashMap();
        mapColumns(result, data, columns);
        return result;
    }

    public static void mapColumns(Map dest, List<Object[]> data, int... columns) {
        int keyCol = (columns.length == 2 ? columns[0] : 0);
        int valCol = (columns.length == 2 ? columns[1] : 1);
        for (Object[] row : data)
            dest.put(row[keyCol], row[valCol]);
    }

    public static <T> T singleValue(List l) {
        if (l == null || l.isEmpty())
            return null;
        else
            return (T) l.get(0);
    }


    public static String addCriteriaToHqlIfEntityPresent(StringBuilder query,
            String entityName, List queryArgs, List criteria) {

        // if there are no criteria to add, do nothing.
        if (criteria == null || criteria.isEmpty())
            return null;

        // if this query does not contain the named entity, do nothing.
        int pos = getFromKeywordEndPos(query);
        pos = getIdentifierEndPos(query, entityName, pos);
        if (pos < 1)
            return null;

        // find the "as" clause that follows the entity name.
        Matcher m = AS_CLAUSE_PAT.matcher(query).region(pos, query.length());
        if (!m.lookingAt())
            throw new IllegalArgumentException(
                    "HQL autofiltering logic requires the '" + entityName
                            + "' entity to be followed by an 'as' clause.");
        String alias = m.group(1);

        // now add the criteria
        addCriteriaToHql(query, alias, queryArgs, criteria);
        return alias;
    }

    private static final Pattern AS_CLAUSE_PAT = Pattern.compile(
        "\\s+as\\s+(\\w+)", Pattern.CASE_INSENSITIVE);


    public static List addCriteriaToHql(StringBuilder query, String entityName,
            List queryArgs, List criteria) {
        // create a query argument list if one was not provided.
        if (queryArgs == null)
            queryArgs = new ArrayList();

        // if there are no criteria to add, do nothing.
        if (criteria == null || criteria.isEmpty())
            return queryArgs;

        // split the query in two parts, so the first part ends with the
        // keyword "WHERE" and a space. this will make it simpler to append
        // new WHERE clauses.
        int wherePos = getOrInsertWhereKeywordEndPos(query);
        String trailingQueryPart = query.substring(wherePos);
        query.setLength(wherePos);
        List splitArgs = maybeSplitQueryArgs(queryArgs, trailingQueryPart);

        criteria = new ArrayList(criteria);
        while (!criteria.isEmpty()) {
            String key = asString(criteria.remove(0));

            if (PROJECT_CRITERIA.equals(key)) {
                addProjectCriteriaToHql(query, entityName, splitArgs, criteria);
            } else if (WBS_CRITERIA.equals(key)) {
                addWbsCriteriaToHql(query, entityName, splitArgs, criteria);
            } else if (LABEL_CRITERIA.equals(key)) {
                addLabelCriteriaToHql(query, entityName, splitArgs, criteria);
            } else if (IS_CURRENT_CRITERIA.equals(key)) {
                addIsCurrentCriteriaToHql(query, entityName);
            } else if (key != null) {
                logger.warning("Unrecognized query criteria " + key);
            }
        }

        // add the final (split) portion of the query back.
        query.append(trailingQueryPart);

        // remove a superfluous "always true" clause if present
        int pos = query.indexOf(ALWAYS_TRUE_CLAUSE);
        if (pos != -1)
            query.delete(pos, pos + ALWAYS_TRUE_CLAUSE.length());

        logger.finest("Constructed query: " + query);

        // return the query args list
        return queryArgs;
    }

    private static List maybeSplitQueryArgs(List queryArgs,
            String trailingQueryPart) {
        if (trailingQueryPart.indexOf('?') == -1)
            return queryArgs;

        String[] parts = trailingQueryPart.split("\\?", -1);
        int numParams = parts.length - 1;
        if (numParams > queryArgs.size())
            throw new IllegalArgumentException("not enough query args provided");

        return queryArgs.subList(0, queryArgs.size() - numParams);
    }



    private static void addProjectCriteriaToHql(StringBuilder query,
            String entityName, List queryArgs, List criteria) {

        // get the list of project keys to include. Discard any negative values
        // (which are flags for an error condition and not real project keys)
        List<Integer> projectKeys = extractIntegers(criteria);
        for (Iterator i = projectKeys.iterator(); i.hasNext();) {
            if ((Integer) i.next() < 0)
                i.remove();
        }

        if (projectKeys.isEmpty()) {
            // no such project? Add an always-false criteria
            query.append(IMPOSSIBLE_CONDITION).append("and ");

        } else if (projectKeys.size() == 1) {
            // one project key? Add a simple equality criteria
            query.append(entityName).append(".planItem.project.key = ? and ");
            queryArgs.add(projectKeys.get(0));

        } else {
            // for multiple project keys, use an "in" clause
            query.append(entityName)
                    .append(".planItem.project.key in (?) and ");
            queryArgs.add(projectKeys);
        }
    }

    private static void addWbsCriteriaToHql(StringBuilder query,
            String entityName, List queryArgs, List criteria) {

        Integer wbsKey = extractInteger(criteria);
        if (wbsKey == null) {
            // no WBS filter in effect? Don't add any criteria.
            return;

        } else if (wbsKey < 0) {
            // No such WBS element? Add an always-false criteria
            query.append(IMPOSSIBLE_CONDITION).append("and ");
            return;
        }

        int fromEndPos = getFromKeywordEndPos(query);
        query.insert(fromEndPos, " WbsElementBridge wbsBridge, ");
        query.append("wbsBridge.key.parent.key = ?")
                .append(" and wbsBridge.key.child = ") //
                .append(entityName).append(".planItem.wbsElement and ");
        queryArgs.add(wbsKey);
    }

    private static void addLabelCriteriaToHql(StringBuilder query,
            String entityName, List queryArgs, List criteria) {

        Integer labelGroupKey = extractInteger(criteria);
        if (labelGroupKey == null) {
            // no label filter? Don't add any criteria
            return;
        } else if (labelGroupKey < 0) {
            // No matching labels? Add an always-false criteria
            query.append(IMPOSSIBLE_CONDITION).append("and ");
            return;
        }

        int fromEndPos = getFromKeywordEndPos(query);
        query.insert(fromEndPos, " StudyGroup studyGroup, ");
        query.append("studyGroup.item.group = ?")
                .append(" and studyGroup.item.member = ") //
                .append(entityName).append(".planItem.key and ");
        queryArgs.add(labelGroupKey);
    }

    private static void addIsCurrentCriteriaToHql(StringBuilder query,
            String entityName) {
        query.append(entityName).append(".versionInfo.current = 1 and ");
    }

    private static String asString(Object o) {
        if (o instanceof String) return (String) o;
        if (o instanceof SimpleData) return ((SimpleData)o).format();
        if (o != null) return o.toString();
        return null;
    }

    private static Integer extractInteger(List criteria) {
        List<Integer> result = extractIntegers(criteria);
        if (result.isEmpty())
            return null;
        else
            return result.get(0);
    }

    private static List<Integer> extractIntegers(List criteria) {
        List<Integer> result = new ArrayList();
        while (!criteria.isEmpty()) {
            Object next = criteria.get(0);
            Integer value = null;
            if (next instanceof Number) {
                value = ((Number) next).intValue();
            } else if (next instanceof NumberData) {
                value = ((NumberData) next).getInteger();
            } else if (next instanceof String) {
                try {
                    value = (int) Double.parseDouble((String) next);
                } catch (Exception e) {
                    break;
                }
            } else {
                break;
            }
            result.add(value);
            criteria.remove(0);
        }
        return result;
    }

    private static int getOrInsertWhereKeywordEndPos(StringBuilder query) {
        try {
            // if the query already contains a WHERE clause, return its end pos
            return getWhereKeywordEndPos(query);
        } catch (IllegalArgumentException iae) {
        }

        Matcher m = WHERE_INSERT_PAT.matcher(query);
        if (m.find()) {
            // if the query includes a GROUP BY, ORDER BY, or HAVING clause,
            // insert a new WHERE clause immediately before.
            int insertPos = m.start();
            query.insert(insertPos, "where 1 = 1 ");
            return insertPos + 6;
        } else {
            // otherwise, append a WHERE clause to the end of the query
            query.append(" where 1 = 1");
            return query.length() - 5;
        }
    }
    private static final String ALWAYS_TRUE_CLAUSE = " and 1 = 1";

    private static final Pattern WHERE_INSERT_PAT = Pattern.compile(
        "\\b((group|order)\\s+by|having)\\s", Pattern.CASE_INSENSITIVE);

    private static int getFromKeywordEndPos(StringBuilder query) {
        return getKeywordEndPos(query, FROM_KEYWORD_PAT, "FROM");
    }

    private static final Pattern FROM_KEYWORD_PAT = Pattern.compile(
        "\\bfrom\\s", Pattern.CASE_INSENSITIVE);

    private static int getWhereKeywordEndPos(StringBuilder query) {
        return getKeywordEndPos(query, WHERE_KEYWORD_PAT, "WHERE");
    }

    private static final Pattern WHERE_KEYWORD_PAT = Pattern.compile(
        "\\bwhere\\s", Pattern.CASE_INSENSITIVE);

    private static int getKeywordEndPos(StringBuilder query, Pattern pattern,
            String keyword) {
        Matcher m = pattern.matcher(query);
        if (m.find())
            return m.end();
        else
            throw new IllegalArgumentException("No '" + keyword
                    + "' clause found in query '" + query + "'");
    }

    private static int getIdentifierEndPos(StringBuilder query,
            String identifier, int fromPos) {
        Matcher m = getIdentifierPattern(identifier).matcher(query);
        if (m.find(fromPos))
            return m.end();
        else
            return -1;
    }

    private static Pattern getIdentifierPattern(String identifier) {
        Pattern result = IDENTIFIER_PATTERNS.get(identifier);
        if (result == null) {
            // search for identifiers in a case-sensitive manner
            result = Pattern.compile("\\b\\Q" + identifier + "\\E\\b");
            IDENTIFIER_PATTERNS.put(identifier, result);
        }
        return result;
    }

    private static final Map<String, Pattern> IDENTIFIER_PATTERNS = new Hashtable();

}
