// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.team;

public class WeekData {

    public static final int TYPE_DEFAULT = 0;

    public static final int TYPE_EXCEPTION = 1;

    public static final int TYPE_OUTSIDE_SCHEDULE = 2;

    public static final int TYPE_START = 3;

    public static final int TYPE_END = 4;


    public static final WeekData WEEK_START = new WeekData(0, TYPE_START);

    public static final WeekData WEEK_END = new WeekData(0, TYPE_END);

    public static final WeekData WEEK_OUTSIDE_SCHEDULE = new WeekData(0,
            TYPE_OUTSIDE_SCHEDULE);


    /** The number of hours in the given week */
    private double hours;

    private int type;


    public WeekData(double time, int type) {
        this.hours = time;
        this.type = type;
    }

    public double getHours() {
        return hours;
    }

    public int getType() {
        return type;
    }

    public boolean isInsideSchedule() {
        return isInsideSchedule(type);
    }

    public static boolean isInsideSchedule(int type) {
        return type == WeekData.TYPE_DEFAULT || type == WeekData.TYPE_EXCEPTION;
    }

}
