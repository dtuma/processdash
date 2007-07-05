// Copyright (C) 2005-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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
        if (init)
            init();
    }

    public boolean isUsingExplicitNames() {
        return usingExplicitNames;
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

        public DataValue(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public SimpleData getSimpleValue() {
            return data.getSimpleValue(name);
        }

    }

    private static Iterator getDataNameIterator(DataRepository data,
            DashHierarchy hier, Collection prefixes,
            Collection metricsIncludes, Collection metricsExcludes) {

        if (metricsExcludes.contains(EXCLUDE_ALL_NONMATCHING_METRICS))
            try {
                return new CartesianDataNameIterator(prefixes, hier,
                        metricsIncludes);
            } catch (Exception e) {
                // an exception could be thrown to indicate that some of the
                // include patterns were not recognizable as explicitly
                // enumerated metrics names.
                logger.severe("Cannot respect request for explicitly " +
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
        private Iterator prefixKeys;
        private Collection names;

        private String workingPrefix;
        private Iterator workingNames;

        public CartesianDataNameIterator(Collection prefixes,
                DashHierarchy hier, Collection namePatterns) {
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

        private Collection enumerateNames(Collection namePatterns) {
            PatternList pl = new PatternList(namePatterns);
            if (pl.getContainsItems() != null || pl.getRegexpItems() != null
                    || pl.getStartsWithItems() != null
                    || pl.getEqualsItems() != null)
                throw new UnsupportedOperationException();

            List result = new ArrayList(pl.getEndsWithItems().size());
            for (Iterator i = pl.getEndsWithItems().iterator(); i.hasNext();) {
                String oneItem = (String) i.next();
                if (oneItem.startsWith("/"))
                    oneItem = oneItem.substring(1);
                result.add(oneItem);
            }

            return result;
        }

        private void getNextPrefix() {
            if (prefixKeys.hasNext()) {
                PropertyKey nextKey = (PropertyKey) prefixKeys.next();
                workingPrefix = nextKey.path();
                if (!workingPrefix.endsWith("/"))
                    workingPrefix = workingPrefix + "/";
                workingNames = names.iterator();
            } else {
                workingPrefix = null;
                workingNames = null;
            }
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

    }
}
