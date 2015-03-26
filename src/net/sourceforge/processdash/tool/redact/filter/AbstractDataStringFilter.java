// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.util.PatternList;

public abstract class AbstractDataStringFilter implements DataFileEntryFilter {

    private Map<PatternList, Method> filters;

    protected String valuePrefix = "\"";

    protected PatternList filenamePatterns;

    public AbstractDataStringFilter() {
        filters = new HashMap<PatternList, Method>();

        // examine all of the methods declared by this object.
        for (Method m : getClass().getMethods()) {

            // we are looking for methods that take a single String parameter
            // and return a String value. If this method doesn't match that
            // profile, skip it.
            if (m.getReturnType() != String.class)
                continue;
            Class[] parameterTypes = m.getParameterTypes();
            if (parameterTypes.length != 1 || parameterTypes[0] != String.class)
                continue;

            // Skip methods that do not have an "EnabledFor" annotation.
            EnabledFor enabledFor = m.getAnnotation(EnabledFor.class);
            if (enabledFor == null)
                continue;

            // The EnabledFor annotation will indicate patterns for the data
            // names that the method will translate. Convert these into a
            // PatternList, and add the result to our map of filters.
            PatternList patternList = new PatternList();
            for (String onePattern : enabledFor.value())
                patternList.addRegexp(onePattern);
            filters.put(patternList, m);
        }
    }

    public void filter(DataFileEntry e) {
        if (filenamePatterns != null
                && !filenamePatterns.matches(e.getFilename()))
            return;

        String value = e.getValue();
        if (value.startsWith(valuePrefix))
            value = value.substring(valuePrefix.length());
        String origValue = value;

        // iterate over the filters that are provided by this object.
        for (Entry<PatternList, Method> oneFilter : filters.entrySet()) {
            if (oneFilter.getKey().matches(e.getKey())) {
                // If this method wants to filter this value, run the filter.
                try {
                    value = (String) oneFilter.getValue().invoke(this, value);
                } catch (Exception ex) {
                    // it would be unusual for the method invocation to throw an
                    // exception.  If it does, discard this data element.
                    ex.printStackTrace();
                    value = null;
                }

                if (value == null) {
                    // if the method returned null, delete this data element.
                    e.setKey(null);
                    return;
                }
            }
        }

        // check to see if the value was changed by this method. If so,
        // update the underlying value of the data element.
        if (!origValue.equals(value))
            e.setValue(valuePrefix + value);
    }

}
