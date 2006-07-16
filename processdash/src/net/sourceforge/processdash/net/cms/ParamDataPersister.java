// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

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
