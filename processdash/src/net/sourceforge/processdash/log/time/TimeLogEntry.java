// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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
