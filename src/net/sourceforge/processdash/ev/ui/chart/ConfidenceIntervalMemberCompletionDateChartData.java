// Copyright (C) 2008-2017 Tuma Solutions, LLC
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

import java.util.Date;

import org.jfree.data.DomainInfo;
import org.jfree.data.Range;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListFilter;
import net.sourceforge.processdash.ev.EVTaskListGroupFilter;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.ci.SingleValueConfidenceInterval;
import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserFilter;

public class ConfidenceIntervalMemberCompletionDateChartData extends
    ConfidenceIntervalChartData implements DomainInfo {

    private EVTaskListRollup rollup;

    private String permissionID;

    private Double lowerBound, upperBound;

    public ConfidenceIntervalMemberCompletionDateChartData(
            ChartEventAdapter eventAdapter, EVTaskListRollup rollup,
            String permissionID) {
        super(eventAdapter, 0, ConfidenceIntervalCompletionDateChartData
                .getMaxChartDate());
        this.rollup = rollup;
        this.permissionID = permissionID;
    }

    public void recalc() {
        clearSeries();
        lowerBound = upperBound = null;

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

            String seriesName = nameHelper.get(tl);

            // by default, attempt to add a series based on the forecast date
            // confidence interval
            if (maybeAddSeries(tl.getSchedule().getMetrics()
                    .getDateConfidenceInterval(), seriesName))
                continue;

            // if no confidence interval is available, see if this schedule
            // is 100% complete.  If so, draw a vertical line on the chart.
            Date completionDate = tl.getTaskRoot().getActualDate();
            if (completionDate != null
                    && maybeAddSeries(new SingleValueConfidenceInterval(
                            completionDate.getTime()), seriesName))
                continue;

            // if no interval is available and we're less than 100% complete,
            // see if they have a forecast date, and draw that as a single
            // point on the chart.
            Date forecastDate = tl.getSchedule().getMetrics()
                    .independentForecastDate();
            if (forecastDate != null && !forecastDate.equals(EVSchedule.NEVER))
                maybeAddSeries(new SinglePointXYChartSeries(seriesName,
                        forecastDate.getTime(), 0));
        }
    }

    @Override
    protected boolean maybeAddSeries(XYChartSeries s) {
        if (super.maybeAddSeries(s)) {
            double seriesLow = s.getX(0).doubleValue();
            if (lowerBound == null || seriesLow < lowerBound)
                lowerBound = seriesLow;

            int i = (int) (s.getItemCount() * 0.75);
            double seriesHigh = s.getX(i).doubleValue();
            if (upperBound == null || seriesHigh > upperBound)
                upperBound = seriesHigh;

            return true;
        } else {
            return false;
        }
    }

    public Range getDomainBounds(boolean includeInterval) {
        return new Range(getDomainLowerBound(includeInterval),
                getDomainUpperBound(includeInterval));
    }

    public double getDomainLowerBound(boolean includeInterval) {
        return lowerBound == null ? 0 : lowerBound.doubleValue();
    }

    public double getDomainUpperBound(boolean includeInterval) {
        return upperBound == null ? 0 : upperBound.doubleValue();
    }

}
