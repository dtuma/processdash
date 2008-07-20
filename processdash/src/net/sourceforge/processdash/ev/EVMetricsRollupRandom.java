// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.util.Date;

public class EVMetricsRollupRandom extends EVMetricsRollup {

    EVMetrics[] metrics;

    public EVMetricsRollupRandom(EVScheduleRollup rollupSched) {
        super(false);
        metrics = new EVMetrics[rollupSched.subSchedules.size()];
        actualTime = 0;
        for (int i = 0;   i < metrics.length;   i++) {
            EVSchedule s = (EVSchedule) rollupSched.subSchedules.get(i);
            metrics[i] = s.getMetrics();
            actualTime += metrics[i].actualTime;
        }
    }

    public double independentForecastCost() {
        double result = 0;
        for (int i = 0;   i < metrics.length;   i++)
            result += metrics[i].independentForecastCost();

        return result;
    }

    public Date independentForecastDate() {
        Date result = null;
        for (int i = 0;   i < metrics.length;   i++)
            result = EVScheduleRollup.maxDate
                (result, metrics[i].independentForecastDate());

        return result;
    }

}
