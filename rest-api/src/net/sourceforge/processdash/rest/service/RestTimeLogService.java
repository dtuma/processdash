// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEntryComparator;

public class RestTimeLogService {

    private static RestTimeLogService svc;

    public static RestTimeLogService get() {
        if (svc == null)
            svc = new RestTimeLogService();
        return svc;
    }


    private DashboardContext ctx;

    private DashboardTimeLog timeLog;

    private RestTimeLogService() {
        ctx = RestDashContext.get();
        timeLog = (DashboardTimeLog) ctx.getTimeLog();
    }

    public List<TimeLogEntry> allTimeLogEntries() throws IOException {
        return timeLogEntries(null, null, null);
    }

    public List<TimeLogEntry> timeLogEntries(String path, Date from, Date to)
            throws IOException {
        Enumeration<TimeLogEntry> e = timeLog.filter(path, from, to);
        List<TimeLogEntry> result = Collections.list(e);
        Collections.sort(result, TimeLogEntryComparator.INSTANCE);
        return result;
    }

}
