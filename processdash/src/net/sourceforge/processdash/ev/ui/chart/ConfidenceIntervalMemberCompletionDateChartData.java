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

import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;

public class ConfidenceIntervalMemberCompletionDateChartData extends
        ConfidenceIntervalChartData {

    private EVTaskListRollup rollup;

    public ConfidenceIntervalMemberCompletionDateChartData(
            ChartEventAdapter eventAdapter, EVTaskListRollup rollup) {
        super(eventAdapter);
        this.rollup = rollup;
    }

    protected void recalc() {
        this.series.clear();

        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            maybeAddSeries(tl.getSchedule().getMetrics()
                    .getDateConfidenceInterval(), getSeriesName(tl));
        }
    }

    private String getSeriesName(EVTaskList tl) {
        String name = tl.getDisplayName();
        if (name.endsWith(")")) {
            int parenPos = name.lastIndexOf('(');
            return name.substring(parenPos + 1, name.length() - 1);
        } else
            return name;
    }

}
