// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogWriter;
import net.sourceforge.processdash.util.IteratorFilter;

public class TimeLogExporterXMLv1 implements TimeLogExporter {

    public void dumpTimeLogEntries(TimeLog timeLog, Collection filter,
            OutputStream out) throws IOException {

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

        TimeLogWriter.write(out, entries, false);
        out.flush();
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


}
