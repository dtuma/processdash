// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.util.Date;
import java.util.Iterator;

import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.util.IteratorFilter;

public class TimeLogIteratorFilter extends IteratorFilter {

    private String path;

    private Date from;

    private Date to;

    public TimeLogIteratorFilter(Iterator parent, String path, Date from,
            Date to) {
        super(parent);
        this.from = from;
        this.path = path;
        this.to = to;
        init();
    }

    protected boolean includeInResults(Object o) {
        return matches((TimeLogEntry) o, path, from, to, true);
    }

    public static boolean matches(TimeLogEntry tle, String prefix, Date from,
            Date to, boolean includeChildren) {
        if (prefix != null) {
            // check to see if the time log entry falls within the desired
            // hierarchy location
            if (!Filter.pathMatches(tle.getPath(), prefix, includeChildren))
                return false;
        }

        if (tle.getStartTime() != null) {
            // check to see if the time log entry is after the desired
            // time frame
            if (to != null && tle.getStartTime().after(to))
                return false;

            // check to see if the time log entry is before the desired
            // time frame
            if (from != null && tle.getStartTime().before(from))
                return false;
        }

        return true;
    }

}
