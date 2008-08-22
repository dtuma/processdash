// Copyright (C) 2008 Tuma Solutions, LLC
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
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.ci.SingleValueConfidenceInterval;

public class ConfidenceIntervalMemberCompletionDateChartData extends
    ConfidenceIntervalChartData {

    private EVTaskListRollup rollup;

    public ConfidenceIntervalMemberCompletionDateChartData(
            ChartEventAdapter eventAdapter, EVTaskListRollup rollup) {
        super(eventAdapter, 0, ConfidenceIntervalCompletionDateChartData
                .getMaxChartDate());
        this.rollup = rollup;
    }

    protected void recalc() {
        this.series.clear();

        Set<String> ambiguousNames = getRepeatedPersonNames();

        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            String seriesName = getSeriesName(tl, ambiguousNames);

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

    private String getSeriesName(EVTaskList tl, Set<String> namesToAvoid) {
        String name = tl.getDisplayName();
        String personName = extractPersonName(name);
        if (personName != null && !namesToAvoid.contains(personName))
            return personName;
        else
            return name;
    }

    private Set<String> getRepeatedPersonNames() {
        Set<String> namesSeen = new HashSet<String>();
        Set<String> repeatedNames = new HashSet<String>();
        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            String personName = extractPersonName(tl.getDisplayName());
            if (namesSeen.contains(personName))
                repeatedNames.add(personName);
            else
                namesSeen.add(personName);
        }
        return repeatedNames;
    }

    private String extractPersonName(String taskListName) {
        if (!taskListName.endsWith(")"))
            return null;

        int parenPos = taskListName.lastIndexOf('(');
        if (parenPos == -1)
            return null;

        return taskListName.substring(parenPos + 1, taskListName.length() - 1);
    }

}
