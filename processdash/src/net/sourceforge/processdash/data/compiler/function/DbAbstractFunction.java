// Copyright (C) 2013-2014 Tuma Solutions, LLC
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;

public abstract class DbAbstractFunction extends AbstractFunction {

    protected static Logger logger = Logger.getLogger(DbAbstractFunction.class
            .getName());

    /**
     * Return true if the database plugin is at the given version or higher.
     */
    protected boolean isDatabaseVersion(String version) {
        String dbVersion = TemplateLoader.getPackageVersion("tpidw-embedded");
        return dbVersion != null
                && DashPackage.compareVersions(dbVersion, version) >= 0;
    }

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
     *            the initial part of the HQL query. It should include at least
     *            one "WHERE" clause.
     * @param entityName
     *            the alias that the baseQuery uses to refer to the object being
     *            queried
     * @param criteria
     *            a list of search criteria indicating the project, WBS, and
     *            label filter that we should apply to narrow this query
     * @param baseQueryArgs
     *            if the baseQuery contains parameterized values, use these
     *            arguments as the values for those parameters.
     * @return the results of the HQL query
     */
    protected List queryHql(ExpressionContext context, String baseQuery,
            String entityName, List criteria, Object... baseQueryArgs) {
        // get the object for executing database queries
        QueryRunner queryRunner = getDbObject(context, QueryRunner.class);
        if (queryRunner == null)
            return null;

        // build the effective query and associated argument list
        StringBuilder query = new StringBuilder(baseQuery);
        List queryArgs = new ArrayList(Arrays.asList(baseQueryArgs));
        QueryUtils.addCriteriaToHql(query, entityName, queryArgs, criteria);

        // if we know that the query won't return any result, don't bother
        // running it against the database.
        if (query.indexOf(QueryUtils.IMPOSSIBLE_CONDITION) != -1)
            return new ArrayList();

        // run the query
        Object[] queryArgArray = queryArgs.toArray();
        List result = queryRunner.queryHql(query.toString(), queryArgArray);
        return result;
    }

}
