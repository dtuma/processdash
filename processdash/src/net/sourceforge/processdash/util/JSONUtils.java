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

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONUtils {

    /**
     * Look up an extended property within a JSON object.
     * 
     * @param m
     *            the JSONObject
     * @param path
     *            a path name containing names of properties separated by
     *            periods.
     * @param ignoreMissing
     *            true if a missing property is not considered to be an error
     *            condition
     * @return the value retrieved by looking up the named properties. If any of
     *         the properties name a list, the listed objects will be traversed
     *         and this object will return a list as well.
     */
    public static <T> T lookup(Map m, String path, boolean ignoreMissing) {
        String nextPath = null;

        int dotPos = path.indexOf('.');
        if (dotPos != -1) {
            nextPath = path.substring(dotPos + 1);
            path = path.substring(0, dotPos);
        }

        Object result = m.get(path);
        if (result == null) {
            if (ignoreMissing)
                return null;
            else
                throw new IllegalArgumentException("did not find attr " + path
                        + " of object " + m);
        }

        if (nextPath != null) {
            if (result instanceof List) {
                List nextResult = new ArrayList();
                for (Map item : (List<Map>) result) {
                    Object itemResult = lookup(item, nextPath, ignoreMissing);
                    if (itemResult instanceof List)
                        nextResult.addAll((List) itemResult);
                    else if (itemResult != null)
                        nextResult.add(itemResult);
                }
                result = nextResult;

            } else if (result instanceof Map) {
                result = lookup((Map) result, nextPath, ignoreMissing);

            } else {
                throw new IllegalArgumentException("Could not look up attr "
                        + nextPath + " of object " + result);
            }
        }

        return (T) result;
    }

}
