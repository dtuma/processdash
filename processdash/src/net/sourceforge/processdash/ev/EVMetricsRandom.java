// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import DistLib.uniform;


public class EVMetricsRandom extends EVMetrics {


    protected double randomDTPI;
    protected double randomForecastTotalCost;
    protected Date randomForecastDate;


    public EVMetricsRandom(EVMetrics origMetrics) {
        super(false);
        this.costInterval = origMetrics.costInterval;
        this.timeErrInterval = origMetrics.timeErrInterval;
        this.currentDate = origMetrics.currentDate;
        this.actualTime = origMetrics.actualTime;
    }


    public void randomize(EVSchedule s, uniform random) {
        randomDTPI = 1 / timeErrInterval.getRandomValue(random);

        double randomIncompleteCost = costInterval.getRandomValue(random);
        randomForecastTotalCost = actualTime + randomIncompleteCost;
        recalcForecastDate(s);
        randomForecastDate = forecastDate;
    }


    protected void recalcForecastDate(EVSchedule s) {
        forecastDate = s.getHypotheticalDate(independentForecastCost(), true);
        if (forecastDate.compareTo(currentDate) < 0)
            forecastDate = currentDate;
    }


    public double independentForecastCost() {
        return randomForecastTotalCost;
    }



    public Date independentForecastDate() {
        return randomForecastDate;
    }


    public double directTimePerformanceIndex() {
        return randomDTPI;
    }

}
