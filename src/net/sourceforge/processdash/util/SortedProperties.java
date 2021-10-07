// Copyright (C) 2021 Tuma Solutions, LLC
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * An subclass of {@link Properties} that arranges for properties to appear in
 * sorted order when files are stored.
 * 
 * Properties can be saved in either XML or key-value format. The default
 * implementation from the JDK writes keys in a nondeterministic order. This
 * class arranges for the saved files to list properties in sorted order, making
 * the files much more human-readable.
 * 
 * @since 2.6.6
 */
public class SortedProperties extends Properties {

    public SortedProperties() {}

    public SortedProperties(Properties defaults) {
        super(defaults);
    }

    // override various methods to return our keys in sorted order. Our goal
    // is to generate a file that is in sorted order; but we can only hope that
    // our superclass calls one of the methods below from its storage logic.

    @Override
    public synchronized Enumeration keys() {
        return sortEnumeration(super.keys());
    }

    @Override
    public Set<Object> keySet() {
        return new TreeSet(super.keySet());
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        TreeMap sortedEntries = new TreeMap();
        for (Entry<Object, Object> e : super.entrySet())
            sortedEntries.put(e.getKey(), e.getValue());
        return sortedEntries.entrySet();
    }

    @Override
    public Enumeration<?> propertyNames() {
        return sortEnumeration(super.propertyNames());
    }

    @Override
    public Set<String> stringPropertyNames() {
        return new TreeSet<String>(super.stringPropertyNames());
    }

    private Enumeration sortEnumeration(Enumeration e) {
        ArrayList l = Collections.list(e);
        Collections.sort(l);
        return Collections.enumeration(l);
    }

}
