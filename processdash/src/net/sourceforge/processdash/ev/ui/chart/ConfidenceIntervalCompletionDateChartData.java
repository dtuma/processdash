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

import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVMetricsRollup;

public class ConfidenceIntervalCompletionDateChartData extends
        ConfidenceIntervalChartData {

    private EVMetrics evMetrics;

    public ConfidenceIntervalCompletionDateChartData(
            ChartEventAdapter eventAdapter, EVMetrics evMetrics) {
        super(eventAdapter, 0, getMaxChartDate());
        this.evMetrics = evMetrics;
    }

    public void recalc() {
        clearSeries();

        maybeAddSeries(evMetrics.getDateConfidenceInterval(), "Forecast");

        if (evMetrics instanceof EVMetricsRollup) {
            EVMetricsRollup rollupMetrics = (EVMetricsRollup) evMetrics;
            maybeAddSeries(rollupMetrics.getOptimizedDateConfidenceInterval(),
                "Optimized_Forecast");
        }
    }

    private static final long HOUR_MILLIS =
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;
    private static final long DAY_MILLIS = 24L /*hours*/ * HOUR_MILLIS;
    private static final long YEAR_MILLIS = 365L /*days*/ * DAY_MILLIS;
    private static final long MAX_CHART_WIDTH = 50L /*years*/ * YEAR_MILLIS;
    static long getMaxChartDate() {
        return System.currentTimeMillis() + MAX_CHART_WIDTH;
    }
}
