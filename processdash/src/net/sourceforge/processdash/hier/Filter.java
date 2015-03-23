// Copyright (C) 2000-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Vector;

/**
 * This class implements a primitive filter for hierarchy paths. If the string
 * begins with one of the hierarchy prefixes in the Vector, or if the Vector is
 * null, it passes.
 */
public class Filter {

    public static boolean matchesFilter(Vector theFilter, String name) {
        // this method is preserved for binary compatibility reasons.
        return matchesFilter((Collection) theFilter, name);
    }

    public static boolean matchesFilter(Collection theFilter, String name) {
        if (theFilter == null)
            return true;

        if (theFilter instanceof List) {
            List theFilterList = (List) theFilter;
            int size = theFilter.size();
            if (size == 1)
                // super-optimization for single-element lists (our most
                // common case)
                return pathMatches(name, (String) theFilterList.get(0));

            else if (theFilter instanceof RandomAccess) {
                // optimization for random access lists (avoid creating an
                // iterator instance below)
                for (int i=size;  i-- > 0;)
                    if (pathMatches(name, (String) theFilterList.get(i)))
                        return true;
                return false;
            }
        }

        for (Iterator iter = theFilter.iterator(); iter.hasNext();) {
            String oneFilter = (String) iter.next();
            if (pathMatches(name, oneFilter))
                return true;
        }
        return false;
    }

    public static boolean pathMatches(String path, String prefix) {
        return pathMatches(path, prefix, true);
    }

    public static boolean pathMatches(String path, String prefix,
            boolean includeChildren) {
        // this method gets called a LOT, often in tight loops.  Optimize it
        // like crazy.
        if (path == null || prefix == null)
            return false;

        int prefixLen = prefix.length();
        int pathLen = path.length();
        if (pathLen < prefixLen)
            // this can't be a match if the prefix is longer than the path.
            return false;

        // The String.startsWith() method walks through the two strings from
        // front to back, comparing characters.  Since our prefixes often share
        // a lot of common initial strings (e.g. "/Project"), that test may
        // make it a long way into the prefix before discovering a character
        // mismatch.  If the prefix mismatches, it will be MUCH rarer for the
        // final character in the prefix to exactly match the corresponding
        // character in the path.  Check that first and potentially avoid the
        // more time-consuming startsWith() check.
        if (prefixLen > 0
                && path.charAt(prefixLen-1) != prefix.charAt(prefixLen-1))
            return false;

        if (!path.startsWith(prefix))
            return false;

        if (pathLen > prefixLen) {
            if (includeChildren) {
                if (path.charAt(prefixLen) != '/')
                    return false;
            } else
                return false;
        }

        return true;
    }

}
