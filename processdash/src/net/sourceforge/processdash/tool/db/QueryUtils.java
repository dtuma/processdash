// Copyright (C) 2013 Tuma Solutions, LLC
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
import java.util.Iterator;
import java.util.List;
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

    public static final String IMPOSSIBLE_CONDITION = " 1 = 0 ";


    private static Logger logger = Logger.getLogger(QueryUtils.class
            .getName());

    public static DatabasePlugin getDatabasePlugin(DataContext data) {
        ListData pluginItem = (ListData) data
                .getValue(DatabasePlugin.DATA_REPOSITORY_NAME);
        if (pluginItem == null)
            return null;
        else
            return (DatabasePlugin) pluginItem.get(0);
    }

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
        int wherePos = getWhereKeywordEndPos(query);
        String trailingQueryPart = query.substring(wherePos);
        query.setLength(wherePos);
        queryArgs = maybeSplitQueryArgs(queryArgs, trailingQueryPart);

        criteria = new ArrayList(criteria);
        while (!criteria.isEmpty()) {
            String key = asString(criteria.remove(0));

            if (PROJECT_CRITERIA.equals(key)) {
                addProjectCriteriaToHql(query, entityName, queryArgs, criteria);
            } else if (WBS_CRITERIA.equals(key)) {
                addWbsCriteriaToHql(query, entityName, queryArgs, criteria);
            } else if (LABEL_CRITERIA.equals(key)) {
                addLabelCriteriaToHql(query, entityName, queryArgs, criteria);
            } else if (key != null) {
                logger.warning("Unrecognized query criteria " + key);
            }
        }

        // add the final (split) portion of the query back.
        query.append(trailingQueryPart);
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

}
