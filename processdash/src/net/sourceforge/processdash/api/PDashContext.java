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

public interface PDashContext {

    /**
     * Servlets and JSPs can find this context object stored as an attribute of
     * the HttpServletRequest with this name
     */
    String REQUEST_ATTR = "pdash";

    /**
     * @return the hierarchy path of the effective project, in the traditional
     *         format that would be displayed to end users. If no project is in
     *         effect, returns the empty string.
     */
    String getProjectPath();

    /**
     * @return the hierarchy path of the effective project, expressed as a
     *         prefix that could be used when constructing URIs to resources in
     *         other WAR files. If no project is in effect, returns the empty
     *         string.
     */
    String getUriPrefix();

    /**
     * @return an object for querying dashboard data values.
     */
    PDashData getData();

}
