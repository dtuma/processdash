// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import org.jfree.data.xy.XYZDataset;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListFilter;
import net.sourceforge.processdash.ev.EVTaskListGroupFilter;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserFilter;

public class TimeRatioMemberTrackingChartData extends XYChartData implements
        XYZDataset, DataPointLimitable {

    private EVTaskListRollup rollup;
    private int maxDataPoints;
    private String permissionID;

    public TimeRatioMemberTrackingChartData(ChartEventAdapter eventAdapter,
            EVTaskListRollup rollup, int maxDataPoints, String permissionID) {
        super(eventAdapter);
        this.rollup = rollup;
        this.maxDataPoints = maxDataPoints;
        this.permissionID = permissionID;
    }

    public int getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(int maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
        recalc();
        dataChanged();
    }

    public void recalc() {
        clearSeries();

        // see if the user has permission to view personal data in this chart
        UserFilter f = GroupPermission.getGrantedMembers(permissionID);
        if (f == null)
            return;
        EVTaskListFilter pf = new EVTaskListGroupFilter(f);

        MemberChartNameHelper nameHelper = new MemberChartNameHelper(rollup);
        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            String personalDataID = tl.getPersonalDataID();
            if (personalDataID != null && !pf.include(personalDataID))
                continue;

            EVSchedule subsched = tl.getSchedule();
            String seriesName = nameHelper.get(tl);
            maybeAddSeries(subsched.getTimeRatioTrackingChartSeries(
                seriesName, maxDataPoints));
        }
    }

    public Number getZ(int seriesNum, int itemNum) {
        return ((XYZChartSeries) series.get(seriesNum)).getZ(itemNum);
    }

    public double getZValue(int seriesNum, int itemNum) {
        return getZ(seriesNum, itemNum).doubleValue();
    }

}
