// Copyright (C) 2007-2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEntryVOPathFilter;
import net.sourceforge.processdash.log.time.TimeLogWriter;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.StringMapper;

public class TimeLogExporterXMLv1 implements TimeLogExporter {

    private Date maxDate;

    public void dumpTimeLogEntries(TimeLog timeLog, DataContext data,
            Collection filter, OutputStream out) throws IOException {

        Iterator entries;
        if (filter.isEmpty()) {
            entries = Collections.EMPTY_LIST.iterator();
        } else if (filter.size() == 1) {
            String path = (String) filter.iterator().next();
            entries = timeLog.filter(path, null, null);
        } else {
            entries = timeLog.filter(null, null, null);
            entries = new TimeLogFilter(entries, filter);
        }
        TimeLogEntryIterator iter = new TimeLogEntryIterator(entries,
                new PhaseAppender(data));

        TimeLogWriter.write(out, iter, false);
        out.flush();

        maxDate = iter.maxDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    private class TimeLogFilter extends IteratorFilter {

        private Collection filter;

        protected TimeLogFilter(Iterator parent, Collection filter) {
            super(parent);
            this.filter = filter;
            init();
        }

        protected boolean includeInResults(Object o) {
            TimeLogEntry entry = (TimeLogEntry) o;
            String path = entry.getPath();
            return Filter.matchesFilter(filter, path);
        }

    }

    private static class PhaseAppender implements StringMapper {

        private ProcessUtil procUtil;

        private Map pathCache;

        public PhaseAppender(DataContext data) {
            this.procUtil = new ProcessUtil(data);
            this.pathCache = new HashMap();
        }

        public String getString(String path) {
            String result = (String) pathCache.get(path);
            if (result != null)
                return result;

            String phase = procUtil.getEffectivePhase(path, false);
            if (phase == null)
                result = path;
            else
                result = path + "/" + phase;

            pathCache.put(path, result);
            return result;
        }

    }

    private static class TimeLogEntryIterator extends TimeLogEntryVOPathFilter {

        private Date maxDate;

        public TimeLogEntryIterator(Iterator timeLogEntries,
                StringMapper pathRemapper) {
            super(timeLogEntries, pathRemapper);
        }

        @Override
        public Object next() {
            TimeLogEntry tle = (TimeLogEntry) super.next();
            maxDate = DateUtils.maxDate(maxDate, tle.getStartTime());
            return tle;
        }

    }

}
