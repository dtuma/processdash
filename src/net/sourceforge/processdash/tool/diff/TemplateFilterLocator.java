// Copyright (C) 2005-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.templates.ExtensionManager;


public class TemplateFilterLocator {

    public static final String EXTENSION_TAG = "locFilter";

    public static List getFilters() {
        List result = new ArrayList();

        // Retrieve the filters that are declared by dashboard add-ons
        List addOnFilters = ExtensionManager.getExecutableExtensions(
            EXTENSION_TAG, "class", ConfigurableLanguageFilter.class.getName(),
            "requires", null);

        // Build a list of valid filters and their names
        Set<String> addOnFilterNames = new HashSet<String>();
        for (Iterator i = addOnFilters.iterator(); i.hasNext();) {
            Object filter = i.next();
            if (filter instanceof LanguageFilter) {
                result.add(filter);
                String name = AbstractLanguageFilter.getFilterName(filter);
                addOnFilterNames.add(name);
            }
        }

        // Now, add any built-in filters whose names were not overriden
        // by a contributed filter. (This allows someone to replace a
        // built-in filter by contributing a replacement with the same name.)
        List builtInFilters = HardcodedFilterLocator.getFilters();
        for (Iterator i = builtInFilters.iterator(); i.hasNext();) {
            Object filter = (Object) i.next();
            String name = AbstractLanguageFilter.getFilterName(filter);
            if (!addOnFilterNames.contains(name))
                result.add(filter);
        }

        return result;
    }

}
