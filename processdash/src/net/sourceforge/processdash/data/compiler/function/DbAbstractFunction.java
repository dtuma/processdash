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

package net.sourceforge.processdash.data.compiler.function;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;

public abstract class DbAbstractFunction extends AbstractFunction {

    protected static Logger logger = Logger.getLogger(DbAbstractFunction.class
            .getName());

    /**
     * Return an object from the database plugin's registry.
     */
    protected <T> T getDbObject(ExpressionContext context, Class<T> clazz) {
        ListData dbItem = (ListData) context
                .get(DatabasePlugin.DATA_REPOSITORY_NAME);
        if (dbItem == null)
            return null;

        DatabasePlugin plugin = (DatabasePlugin) dbItem.get(0);
        T result = plugin.getObject(clazz);
        return result;
    }

    /**
     * Given a value which is a Java built-in type, try converting it to a
     * corresponding SimpleData type.
     * 
     * @param object
     *            the object to convert. Numbers, Strings, and Dates are
     *            currently supported.
     * @return a SimpleData value, or null if this object does not correspond to
     *         one of the supported types.
     */
    protected SimpleData toSimpleData(Object object) {
        if (object instanceof Number) {
            return new DoubleData(((Number) object).doubleValue());
        } else if (object instanceof String) {
            return StringData.create((String) object);
        } else if (object instanceof Date) {
            return new DateData((Date) object, false);
        } else {
            return null;
        }
    }

    /**
     * Perform an HQL query and return the result.
     * 
     * @param context
     *            the ExpressionContext this function is operating within.
     * @param baseQuery
     *            the initial part of the HQL query, which should not contain
     *            any parameterized values. It should also include at least one
     *            "WHERE" clause.
     * @param entityName
     *            the alias that the baseQuery uses to refer to the object being
     *            queried
     * @param criteria
     *            a list of search criteria indicating the project, WBS, and
     *            label filter that we should apply to narrow this query
     * @param suffix
     *            any optional items that should be appended to the end of the
     *            query (such as GROUP BY clauses)
     * @return the results of the HQL query
     */
    protected List queryHql(ExpressionContext context, String baseQuery,
            String entityName, List criteria, String... suffix) {
        // get the object for executing database queries
        QueryRunner queryRunner = getDbObject(context, QueryRunner.class);
        if (queryRunner == null)
            return null;

        // build the effective query and associated argument list
        StringBuilder query = new StringBuilder(baseQuery);
        List queryArgs = new ArrayList();
        addCriteriaToHQL(entityName, query, queryArgs, criteria);
        for (String s : suffix)
            query.append(" ").append(s);

        // if we know that the query won't return any result, don't bother
        // running it against the database.
        if (query.indexOf(IMPOSSIBLE_CONDITION) != -1)
            return new ArrayList();

        // run the query
        Object[] queryArgArray = queryArgs.toArray();
        List result = queryRunner.queryHql(query.toString(), queryArgArray);
        return result;
    }

    protected void addCriteriaToHQL(String entityName, StringBuilder query,
            List queryArgs, List criteria) {
        criteria = new ArrayList(criteria);
        while (!criteria.isEmpty()) {
            String key = asString(criteria.remove(0));

            if (PROJECT_CRITERIA.equals(key)) {
                addProjectCriteriaToHQL(entityName, query, queryArgs, criteria);
            } else if (WBS_CRITERIA.equals(key)) {
                addWbsCriteriaToHQL(entityName, query, queryArgs, criteria);
            } else if (key != null) {
                logger.warning("Unrecognized query criteria " + key);
            }
        }
    }

    private static final String PROJECT_CRITERIA = "##Project in";

    private void addProjectCriteriaToHQL(String entityName,
            StringBuilder query, List queryArgs, List criteria) {

        List<Integer> projectKeys = extractIntegers(criteria);
        if (projectKeys.isEmpty()) {
            // no such project? Add an always-false criteria
            query.append(IMPOSSIBLE_CONDITION);

        } else if (projectKeys.size() == 1) {
            // one project key? Add a simple equality criteria
            query.append(" and ").append(entityName)
                    .append(".planItem.project.key = ?");
            queryArgs.add(projectKeys.get(0));

        } else {
            // for multiple project keys, use an "in" clause
            query.append(" and ").append(entityName)
                    .append(".planItem.project.key in (?)");
            queryArgs.add(projectKeys);
        }
    }

    private static final String WBS_CRITERIA = "##WBS Element";

    private void addWbsCriteriaToHQL(String entityName, StringBuilder query,
            List queryArgs, List criteria) {

        List<Integer> wbsKeyList = extractIntegers(criteria);
        Integer wbsKey = null;
        if (!wbsKeyList.isEmpty())
            wbsKey = wbsKeyList.get(0);
        if (wbsKey == null)
            // no WBS filter in effect? Don't add any criteria.
            return;

        if (wbsKey < 0) {
            // No such WBS element? Add an always-false criteria
            query.append(IMPOSSIBLE_CONDITION);
            return;
        }

        int fromEndPos = getFromKeywordEndPos(query);
        query.insert(fromEndPos, " WbsElementBridge wbsBridge, ");
        query.append(" and wbsBridge.key.parent.key = ?")
                .append(" and wbsBridge.key.child = ") //
                .append(entityName).append(".planItem.wbsElement");
        queryArgs.add(wbsKey);
    }


    private List<Integer> extractIntegers(List criteria) {
        List<Integer> result = new ArrayList();
        while (!criteria.isEmpty()) {
            Object next = criteria.get(0);
            if (next instanceof NumberData) {
                Integer value = ((NumberData) next).getInteger();
                result.add(value);
                criteria.remove(0);
            } else {
                break;
            }
        }
        return result;
    }

    private int getFromKeywordEndPos(StringBuilder query) {
        Matcher m = FROM_KEYWORD_PAT.matcher(query);
        if (m.find())
            return m.end();
        else
            throw new IllegalArgumentException(
                    "No 'from' clause found in query '" + query + "'");
    }

    private static final Pattern FROM_KEYWORD_PAT = Pattern.compile(
        "\\bfrom\\b", Pattern.CASE_INSENSITIVE);

    private static final String IMPOSSIBLE_CONDITION = " and 1 = 0 ";
}
