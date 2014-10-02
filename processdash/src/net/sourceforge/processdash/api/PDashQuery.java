// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.api;

import java.util.List;
import java.util.Map;

/**
 * An object providing the ability to perform object-oriented queries against
 * team project data.
 * <p>
 * The Team Dashboard contains an embedded data warehouse which is populated by
 * team project data. This object makes it possible to perform object-oriented
 * queries against the data in the embedded warehouse. Queries should be written
 * using Hibernate Query Language (HQL).
 * <p>
 * This object can be retrieved from a {@link PDashContext}, and will (by
 * default) automatically filter all queries so they return current data from
 * the project specified by that context. If the context does not name any
 * project, this object will (by default) return data for all of the projects in
 * the Team Dashboard.
 */
public interface PDashQuery extends Map<String, Object> {

    /**
     * Options specifying the type of automatic filtering that should be applied
     * to queries.
     */
    public enum FilterMode {

        /** Do not perform any filtering of query results. */
        NONE,

        /**
         * Filter query results so that only current data is returned.
         * <p>
         * When new data is loaded into the warehouse, rows for historical data
         * are retained and stamped with an effective end date. New rows are
         * loaded and marked with a "current" flag.
         * <p>
         * This filter mode indicates that queries should automatically select
         * the most up-to-date, current data, and that rows representing
         * obsolete historical data should be discarded.
         */
        CURRENT,

        /**
         * Filter query results to return only {@linkplain #CURRENT current
         * data} from the {@linkplain PDashContext#getProjectPath() effective
         * project}
         */
        PROJECT,

        /**
         * Filter query results to return only {@linkplain #CURRENT current
         * data} from the {@linkplain PDashContext#getProjectPath() effective
         * project}. In addition, the query should respect any component or
         * label filters that have been applied by the user.
         */
        ALL
    }


    /**
     * Perform a query and return the results.
     * <p>
     * This method can automatically enhance the HQL query to apply common
     * filters to the data. To accomplish this, all entities in the query must
     * be given aliases using an "AS" clause. To disable or customize
     * autofiltering, pass a {@link FilterMode} object as an extra argument to
     * the query.
     * 
     * @param query
     *            an HQL query to execute
     * @param args
     *            a list of arguments that will be matched against <tt>?</tt>
     *            placeholders in the query, plus an optional FilterMode
     * @return the results of executing the HQL query
     */
    List query(String query, Object... args);


    /**
     * Perform a simple query and return the result.
     * <p>
     * This method (inherited from {@link Map}) is provided for convenience from
     * JSP EL expressions. This allows the construction of succinct EL
     * expressions such as:
     * 
     * <pre>
     * pdash.query['from DefectLogFact as defect']
     * </pre>
     * 
     * If the query contains <tt>?</tt> placeholders, the arguments can be
     * provided by enclosing each argument in square braces and appending them
     * to the expression:
     * 
     * <pre>
     * pdash.query['from DefectLogFact as d
     *             where d.injectedPhase.shortName = ?
     *               and d.removedPhase.shortName = ?']['Design']['Code']
     * </pre>
     * 
     * @param query
     *            an HQL query to execute
     * @return the results of executing the HQL query
     */
    Object get(Object query);


    /**
     * Return the full HQL that was executed as a result of the previous query.
     * <p>
     * Queries performed through this object benefit from autofiltering by
     * default. The autofiltering logic works by augmenting the HQL query before
     * passing it to Hibernate. After a query has been performed, the augmented
     * HQL query string can be retrieved via this method.
     * <p>
     * If a particular query is not returning the results you expect, this
     * method may be helpful in troubleshooting.
     * 
     * @return the full HQL that was executed as a result of the previous query.
     */
    public String getLastHql();

}
