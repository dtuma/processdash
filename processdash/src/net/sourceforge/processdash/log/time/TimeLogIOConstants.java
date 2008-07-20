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
