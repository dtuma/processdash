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

public interface PDashQuery extends Map<String, Object> {

    Object NO_AUTOFILTER = new Object();

    /**
     * Perform a query and return the results.
     * 
     * By default, this method will automatically enhance the HQL query so it
     * only returns current data from the effective project. To accomplish this,
     * all entities in the query must be given aliases using an "AS" clause. To
     * disable autofiltering, pass the {@link #NO_AUTOFILTER} object as an extra
     * argument to the query.
     * 
     * @param query
     *            an HQL query to execute
     * @param args
     *            a list of arguments that will be matched against <tt>?</tt>
     *            placeholders in the query
     * @return the results of executing the HQL query
     */
    List query(String query, Object... args);


    /**
     * Perform a simple query and return the result.
     * 
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
     * @param key
     *            an HQL query to execute
     * @return the results of executing the HQL query
     */
    Object get(Object query);


    /**
     * Return the full HQL that was executed as a result of the previous query.
     * 
     * Queries performed through this object benefit from autofiltering by
     * default. This autofiltering works by augmenting the query passed in. If a
     * query is not returning the results you expect, you may wish to view the
     * augmented HQL expression. This property allows that expression to be
     * retrieved.
     * 
     * @return the full HQL that was executed as a result of the previous query.
     */
    public String getLastHql();

}
