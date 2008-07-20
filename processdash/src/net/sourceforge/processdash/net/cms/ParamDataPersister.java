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

package net.sourceforge.processdash.net.cms;

import java.util.Map;

public interface ParamDataPersister {

    /** Return a string that identifies this type of persister */
    public String getIdentifier();

    /** Look at text that was previously persisted, and translate it into
     * a query string that can be appended to the query parameters for
     * snippet invocation.  (The returned query string, if nonempty,
     * will begin with an ampersand.)
     */
    public String getQueryString(String persistedText);

    /** Look at data that was posted by a form, and translate it into
     * the text content that should be used to persist state.
     *
     * @param postedData a map of posted data, already stripped of its
     *     snippet namespace.  This method is free to modify the Map.
     */
    public String getTextToPersist(Map postedData);

}
