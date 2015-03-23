// Copyright (C) 2005-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyIterator;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.ThreadThrottler;

public class ExportedDataValueIterator extends IteratorFilter {

    DataRepository data;

    Collection prefixes;

    boolean usingExplicitNames;

    Map timings;

    private static final Logger logger = Logger
            .getLogger(ExportedDataValueIterator.class.getName());


    public ExportedDataValueIterator(DataRepository data, DashHierarchy hier,
            Collection prefixes, Collection metricsIncludes,
            Collection metricsExcludes) {
        this(data, hier, prefixes, metricsIncludes, metricsExcludes, true);
    }

    protected ExportedDataValueIterator(DataRepository data,
            DashHierarchy hier, Collection prefixes,
            Collection metricsIncludes, Collection metricsExcludes, boolean init) {
        this(data, prefixes, getDataNameIterator(data, hier, prefixes,
                metricsIncludes, metricsExcludes), init);
    }

    protected ExportedDataValueIterator(DataRepository data,
            Collection prefixes, Iterator parent, boolean init) {
        super(parent);
        this.data = data;
        this.prefixes = prefixes;
        this.usingExplicitNames = (parent instanceof CartesianDataNameIterator);
        if (logger.isLoggable(Level.FINEST))
            timings = new HashMap();
        if (init)
            init();
    }

    public boolean isUsingExplicitNames() {
        return usingExplicitNames;
    }

    public void iterationFinished() {
        if (timings != null) {
            for (Iterator i = timings.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                System.out.println(e.getKey() + "\t" + e.getValue());
            }
        }
    }

    protected boolean includeInResults(Object o) {
        return Filter.matchesFilter(prefixes, (String) o);
    }

    public Object next() {
        String name = (String) super.next();
        return new DataValue(name);
    }

    private class DataValue implements ExportedDataValue {

        private String name;
        private boolean retrievedValue;
        private SimpleData value;

        public DataValue(String name) {
            this.name = name;
            this.retrievedValue = false;
            this.value = null;
        }

        public String getName() {
            return name;
        }

        public SimpleData getSimpleValue() {
            if (!retrievedValue) {
                long start = 0;
                if (timings != null)
                    start = System.currentTimeMillis();

                value = data.getSimpleValue(name);
                retrievedValue = true;

                if (timings != null) {
                    long finish = System.currentTimeMillis();
                    long elapsed = finish - start;
                    if (elapsed > 0)
                        timings.put(name, new Long(elapsed));
                }
            }

            return value;
        }

    }

    private static Iterator getDataNameIterator(DataRepository data,
            DashHierarchy hier, Collection prefixes,
            Collection metricsIncludes, Collection metricsExcludes) {

        if (metricsExcludes.contains(EXCLUDE_ALL_NONMATCHING_METRICS))
            try {
                return new CartesianDataNameIterator(data, prefixes, hier,
                        metricsIncludes);
            } catch (Exception e) {
                // an exception could be thrown to indicate that some of the
                // include patterns were not recognizable as explicitly
                // enumerated metrics names.
                logger.warning("Cannot respect request for explicitly " +
                                "enumerated metrics");
            }

        return data.getKeys(prefixes, getDataNameHints(metricsIncludes,
                metricsExcludes));
    }

    private static Object getDataNameHints(Collection includes,
            Collection excludes) {

        // For the moment, only optimize a single use case: when the caller
        // doesn't want ANY data to be exported, they have a "." exclude
        // pattern. (This commonly occurs at the team level.) In this case,
        // skip every data element unless it is related to task list exports.
        if (excludes != null && excludes.contains("."))
            return new DataNameFilter.PrefixLocal() {
                public boolean acceptPrefixLocalName(String prefix,
                        String localName) {
                    return TaskListDataWatcher.PATTERNS_OF_INTEREST
                            .matches(localName);
                }
            };

        else
            return null;
    }

    public static final String EXCLUDE_ALL_NONMATCHING_METRICS =
        "All Metrics Not Explicitly Listed";

    private static class CartesianDataNameIterator implements Iterator {
        private DataContext data;
        private Iterator prefixKeys;
        private Map names;

        private String workingPrefix;
        private Iterator workingNames;

        public CartesianDataNameIterator(DataContext data, Collection prefixes,
                DashHierarchy hier, Collection namePatterns) {
            this.data = data;
            this.prefixKeys = enumeratePrefixes(prefixes, hier);
            this.names = enumerateNames(namePatterns);

            getNextPrefix();
        }

        private Iterator enumeratePrefixes(Collection prefixes,
                final DashHierarchy hier) {
            Iterator iter = PropertyKeyIterator.getForPrefixes(hier, prefixes);
            Iterator result = new IteratorFilter(iter) {
                { init(); }
                protected boolean includeInResults(Object o) {
                    PropertyKey key = (PropertyKey) o;
                    return StringUtils.hasValue(hier.pget(key).getDataFile());
                }};
            return result;
        }

        private Map enumerateNames(Collection namePatterns) {
            PatternList pl = new PatternList();
            for (Iterator i = namePatterns.iterator(); i.hasNext();)
                pl.addRegexp(fixupPattern((String) i.next()));

            if (pl.getContainsItems() != null || pl.getRegexpItems() != null
                    || pl.getStartsWithItems() != null
                    || pl.getEqualsItems() != null)
                throw new UnsupportedOperationException();

            Map result = new HashMap();
            for (Iterator i = pl.getEndsWithItems().iterator(); i.hasNext();) {
                String oneItem = (String) i.next();
                String rootTag = null;
                int tagEnd = oneItem.indexOf(TAG_SEP_CHAR);
                if (tagEnd != -1) {
                    rootTag = oneItem.substring(0, tagEnd);
                    oneItem = oneItem.substring(tagEnd + 1);
                }

                if (oneItem.startsWith("/"))
                    oneItem = oneItem.substring(1);

                Set s = (Set) result.get(rootTag);
                if (s == null) {
                    s = new HashSet();
                    result.put(rootTag, s);
                }
                s.add(oneItem);
            }

            return result;
        }

        private String fixupPattern(String pattern) {
            if (!pattern.startsWith("{"))
                return pattern;
            int bracePos = pattern.indexOf('}');
            if (bracePos < 2)
                return pattern;
            int barPos = pattern.lastIndexOf('|', bracePos);
            if (barPos == -1)
                return pattern.substring(1, bracePos) + TAG_SEP_CHAR
                        + pattern.substring(bracePos + 1);
            else
                return '(' + pattern.substring(1, bracePos) + ")"
                        + TAG_SEP_CHAR + pattern.substring(bracePos + 1);
        }

        private void getNextPrefix() {
            while (prefixKeys.hasNext()) {
                PropertyKey nextKey = (PropertyKey) prefixKeys.next();
                workingPrefix = nextKey.path();
                if (!workingPrefix.endsWith("/"))
                    workingPrefix = workingPrefix + "/";
                workingNames = getNamesForPrefix(workingPrefix);
                if (workingNames.hasNext())
                    return;
            }
            workingPrefix = null;
            workingNames = null;
        }

        private Iterator getNamesForPrefix(String prefix) {
            Set result = new HashSet();
            for (Iterator i = names.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String tagName = (String) e.getKey();
                if (StringUtils.hasValue(tagName)) {
                    String dataName = prefix + tagName;
                    SimpleData sd = data.getSimpleValue(dataName);
                    if (sd == null || sd.test() == false)
                        continue;
                }
                result.addAll((Set) e.getValue());
            }
            return result.iterator();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return workingPrefix != null && workingNames.hasNext();
        }

        public Object next() {
            ThreadThrottler.tick();

            String nextName = (String) workingNames.next();
            String result = workingPrefix + nextName;

            if (!workingNames.hasNext())
                getNextPrefix();

            return result;
        }

        private static final char TAG_SEP_CHAR = '\u0001';

    }
}
