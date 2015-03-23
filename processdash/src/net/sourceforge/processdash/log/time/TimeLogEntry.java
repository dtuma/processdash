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

/** Describes an entry in a time log.
 */
public interface TimeLogEntry {

    /** @return a value that uniquely identifies this time log entry */
    public long getID();

    /** @return the task this time log entry is logged to, as a String path */
    public String getPath();

    /** @return the date/time this entry began */
    public Date getStartTime();

    /** @return the amount of elapsed time logged in this entry */
    public long getElapsedTime();

    /** @return the amount of interrupt time logged in this entry */
    public long getInterruptTime();

    /** @return the comment associated with this entry, or null if it has no comment */
    public String getComment();

}
