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


/**
 * An object allowing JSPs and servlets to access a number of useful services in
 * the Process Dashboard.
 * <p>
 * When the dashboard web server receives an inbound HTTP request, it constructs
 * an instance of this object and stores it as an HttpServletRequest attribute
 * with the name <tt>"pdash"</tt>. JSPs can refer to this object by name in EL
 * expressions. (Servlet authors may prefer to use the {@link #REQUEST_ATTR}
 * constant.)
 */
public interface PDashContext {

    /**
     * The name of the HttpServletRequest attribute where a PDashContext object
     * will be stored.
     */
    String REQUEST_ATTR = "pdash";


    /**
     * Retrieves the hierarchy path and name of the dashboard project that is in
     * effect for this HTTP request.
     * <p>
     * Dashboard web requests often include an initial path prefix which
     * specifies the name of a particular dashboard project. This path prefix
     * appears immediately after the hostname:port, and is followed by a
     * double-slash that indicates the start of the traditional URI for a
     * resource in a bundled web application. For example:
     * 
     * <pre>
     * http://localhost:3000/Project/Team+Project//some/report.jsp
     * </pre>
     * 
     * This URL requests that <tt>"/some/report.jsp"</tt> be displayed in the
     * context of <tt>"/Project/Team Project"</tt>.
     * <p>
     * When a JSP or servlet retrieves the <code>PDashContext</code> object for
     * the current HTTP request, this method can be called to retrieve the name
     * of the "effective project." For the URL above, this method would return
     * <tt>"/Project/Team Project"</tt>.
     * 
     * @return the hierarchy path of the project named in the request URL, in
     *         the traditional format that would be displayed to end users. If
     *         no project was specified in the URL, returns the empty string.
     */
    String getProjectPath();


    /**
     * Retrieves the portion of the request URL which specified the
     * {@linkplain #getProjectPath() effective project} for this HTTP request.
     * <p>
     * If this request did not specify a project, this method will return an
     * empty string. Otherwise, it will return a prefix, ending with a slash,
     * that could be used to construct URLs to other dashboard web resources.
     * <p>
     * For example, in the URL below, this method would return
     * <tt>"/Project/Team+Project/"</tt>:
     * 
     * <pre>
     * http://localhost:3000/Project/Team+Project//some/report.jsp
     * </pre>
     * 
     * @return the portion of the HTTP request URL which specified an effective
     *         project. If the URL did not specify a project, returns the empty
     *         string.
     */
    String getUriPrefix();


    /**
     * Retrieves an object for querying dashboard data values.
     * <p>
     * If the current HTTP request URL specified an
     * {@linkplain #getProjectPath() effective project}, this object will
     * retrieve data values for that project. If the URL did not specify a
     * project, the object returned from this method is unlikely to provide any
     * meaningful data.
     * 
     * @return an object for querying dashboard data values.
     */
    PDashData getData();


    /**
     * Retrieves an object for performing HQL queries against the warehouse.
     * <p>
     * If the current HTTP request URL specified an
     * {@linkplain #getProjectPath() effective project}, queries will be
     * autofiltered to display {@linkplain PDashQuery.FilterMode#CURRENT
     * current} data for that project. If the URL did not specify a project,
     * queries will return current data from all projects.
     * 
     * @return an object for performing HQL queries against the warehouse.
     */
    PDashQuery getQuery();

}
