// Copyright (C) 2003-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ci;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVMetricsRollup;
import net.sourceforge.processdash.ev.EVSchedule;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;


public class EVScheduleConfidenceIntervals
{
    public interface Randomizable {
        public void randomize(RandomEngine u);
    }

    private static final int BOOTSTRAP_SIZE = 1000;

    EVSchedule schedule;
    List randomObjects;
    EVMetrics metrics;
    MonteCarloConfidenceInterval cost, date, optimizedDate;
    MonteCarloConfidenceInterval[] indivDates;

    public EVScheduleConfidenceIntervals(EVSchedule sched, List randomObjects) {
        this(sched, randomObjects, false);
    }

    public EVScheduleConfidenceIntervals(EVSchedule sched,
            List randomObjects, boolean keepIndivDates) {
        this.schedule = sched;
        this.randomObjects = randomObjects;
        this.metrics = sched.getMetrics();
        cost = new MonteCarloConfidenceInterval();
        date = new MonteCarloConfidenceInterval();
        if (metrics instanceof EVMetricsRollup)
            optimizedDate = new MonteCarloConfidenceInterval();
        if (keepIndivDates) {
            indivDates = new MonteCarloConfidenceInterval[randomObjects.size()];
            for (int i = 0; i < indivDates.length; i++) {
                indivDates[i] = new MonteCarloConfidenceInterval();
            }
        }

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

    public ConfidenceInterval getIndividualDateInterval(int i) {
        return (indivDates == null ? null : indivDates[i]);
    }

    private static final boolean USE_RATIO = true;
    private void runSimulation() {
        long start = System.currentTimeMillis();
        RandomEngine random = new MersenneTwister();

        int sampleCount = Settings.getInt("ev.simulationSize", BOOTSTRAP_SIZE);
        if (USE_RATIO && indivDates == null) {
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
        if (indivDates != null)
            for (int i = 0; i < indivDates.length; i++)
                indivDates[i].samplesDone();

        long finish = System.currentTimeMillis();
        long elapsed = finish - start;
        System.out.println("schedule simulation took " + elapsed + " ms.");
        if (optimizedDate != null) date.debug = true;
    }

    private void runOneTest(RandomEngine random) {
        randomizeAll(random);

        if (indivDates != null)
            addIndivDateSamples();

        double forecastCost = metrics.independentForecastCost();
        cost.addSample(forecastCost-metrics.actual());
        date.addSample(getTime(metrics.independentForecastDate()));
        if (optimizedDate != null) {
            Date optDate = schedule.getHypotheticalDate(forecastCost, true);
            optimizedDate.addSample(getTime(optDate));
        }
    }

    private void randomizeAll(RandomEngine random) {
        Iterator i = randomObjects.iterator();
        while (i.hasNext())
            ((Randomizable) i.next()).randomize(random);
    }

    private void addIndivDateSamples() {
        for (int i = 0;  i < randomObjects.size(); i++) {
            Object o = randomObjects.get(i);
            if (o instanceof EVSchedule) {
                EVSchedule s = (EVSchedule) o;
                Date forecast = s.getMetrics().independentForecastDate();
                indivDates[i].addSample(getTime(forecast));
            }
        }
    }

    private double getTime(Date d) {
        return (d == null ? EVSchedule.NEVER.getTime() : d.getTime());
    }

}
