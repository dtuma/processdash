// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import DistLib.uniform;


public class EVScheduleConfidenceIntervals
{
    public interface Randomizable {
        public void randomize(uniform u);
    }

    private static final int BOOTSTRAP_SIZE = 1000;

    EVSchedule schedule;
    List randomObjects;
    EVMetrics metrics;
    MonteCarloConfidenceInterval cost, date, optimizedDate;

    public EVScheduleConfidenceIntervals(EVSchedule sched,
                                         List randomObjects) {
        this.schedule = sched;
        this.randomObjects = randomObjects;
        this.metrics = sched.getMetrics();
        cost = new MonteCarloConfidenceInterval();
        date = new MonteCarloConfidenceInterval();
        if (metrics instanceof EVMetricsRollup)
            optimizedDate = new MonteCarloConfidenceInterval();

        runSimulation();
    }

    public ConfidenceInterval getCostInterval() {
        return cost;
    }

    public ConfidenceInterval getForecastDateInterval() {
        return date;
    }

    public ConfidenceInterval getOptimizedForecastDateInterval() {
        return optimizedDate;
    }


    private static final boolean USE_RATIO = true;
    private void runSimulation() {
        long start = System.currentTimeMillis();
        uniform random = new uniform();

        int sampleCount = Settings.getInt("ev.simulationSize", BOOTSTRAP_SIZE);
        if (USE_RATIO) {
            double factor = Math.exp(0.75 * Math.log(randomObjects.size()));
            sampleCount = (int) (sampleCount / factor);
            if (sampleCount < 100) sampleCount = 100;
        }
        for (int i = 0;   i < sampleCount;   i++)
            runOneTest(random);

        cost.samplesDone();
        date.samplesDone();
        if (optimizedDate != null)
            optimizedDate.samplesDone();

        long finish = System.currentTimeMillis();
        long elapsed = finish - start;
        System.out.println("schedule simulation took " + elapsed + " ms.");
        if (optimizedDate != null) date.debug = true;
    }

    private void runOneTest(uniform random) {
        randomizeAll(random);

        double forecastCost = metrics.independentForecastCost();
        cost.addSample(forecastCost-metrics.actualTime);
        date.addSample(getTime(metrics.independentForecastDate()));
        if (optimizedDate != null) {
            Date optDate = schedule.getHypotheticalDate(forecastCost, true);
            optimizedDate.addSample(getTime(optDate));
        }
    }

    private void randomizeAll(uniform random) {
        Iterator i = randomObjects.iterator();
        while (i.hasNext())
            ((Randomizable) i.next()).randomize(random);
    }

    private double getTime(Date d) {
        return (d == null ? EVSchedule.NEVER.getTime() : d.getTime());
    }

}
