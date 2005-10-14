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


public interface TimeLogIOConstants {

    public static final String DOC_ROOT_ELEM = "timeLogEntries";
    public static final String TIME_ELEM = "time";
    public static final String ID_ATTR = "id";
    public static final String PATH_ATTR = "path";
    public static final String START_ATTR = "start";
    public static final String DELTA_ATTR = "delta";
    public static final String INTERRUPT_ATTR = "interrupt";
    public static final String COMMENT_ATTR = "comment";
    public static final String FLAG_ATTR = "flag";

    public static final String FLAG_CHARS = " AMDR";

    public static final String ENCODING = "UTF-8";

}
