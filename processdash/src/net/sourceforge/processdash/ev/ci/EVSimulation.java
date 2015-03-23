// Copyright (C) 2004-2007 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.FormatUtil;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;


// This class is not used by production dashboard code.  It exists to
// demonstrate and measure the effects of workload balancing on forecast
// completion date prediction intervals.
public class EVSimulation {
    public static void main(String[] args) {
        EVSimulation s = new EVSimulation();
        s.simulate();
    }


    private int teamSize = 10;
    private double hoursPerWeek = 20;
    private int planNumWeeks = 1; // six months
    private double planCost;

    private double costEstErrPercent = 0.5; // +/- 50 %
    private double scheduleRatioEstErr = 0.25; // +/- 25%


    private TeamSchedule teamSchedule;
    private RandomEngine u = new MersenneTwister();
    private Normal normal = new Normal(0, 1, u);
    private MonteCarloConfidenceInterval optimizedDateInterval;
    private MonteCarloConfidenceInterval unoptimizedDateInterval;
    private MonteCarloConfidenceInterval optimizedVarInterval;
    private MonteCarloConfidenceInterval unoptimizedVarInterval;

    public void simulate() {
        planCost = hoursPerWeek * planNumWeeks;
        teamSchedule = new TeamSchedule();
        optimizedDateInterval = new MonteCarloConfidenceInterval();
        optimizedVarInterval = new MonteCarloConfidenceInterval();
        unoptimizedDateInterval = new MonteCarloConfidenceInterval();
        unoptimizedVarInterval = new MonteCarloConfidenceInterval();

        for (int i = 0;   i < 10000; i++)
            runOneTest();
        optimizedDateInterval.samplesDone();
        unoptimizedDateInterval.samplesDone();

        double optWeeks = optimizedDateInterval.getPrediction();
        double unoptWeeks = unoptimizedDateInterval.getPrediction();
        double optRange = getRange(optimizedDateInterval);
        double unoptRange = getRange(unoptimizedDateInterval);
        double rangeRatio = unoptRange / optRange;

        double optVar = Math.abs(optWeeks - planNumWeeks);
        double unoptVar = Math.abs(unoptWeeks - planNumWeeks);
        double varRatio = unoptVar / optVar;

        System.out.println("Team Size: " + teamSize);

        System.out.println("Opt Weeks\t"+optWeeks);
        System.out.println("Unopt Weeks\t"+unoptWeeks);

        System.out.println("Opt Range\t"+optRange);
        System.out.println("Unopt Range\t"+unoptRange);
        System.out.println("Range Ratio\t"+rangeRatio);

        System.out.println("Opt Var\t"+optVar);
        System.out.println("Unopt Var\t"+unoptVar);
        System.out.println("Var Ratio\t"+varRatio);

        double optVar2 = optimizedVarInterval.getPrediction();
        double unoptVar2 = unoptimizedVarInterval.getPrediction();
        double varRatio2 = unoptVar2 / optVar2;

        System.out.println("Opt Var2\t"+optVar2);
        System.out.println("Unopt Var2\t"+unoptVar2);
        System.out.println("Var Ratio2\t"+varRatio2);

        System.out.println();
        System.out.println("Unoptimized hist:");
        printRanges(unoptimizedDateInterval);

        System.out.println();
        System.out.println("Optimized hist:");
        printRanges(optimizedDateInterval);

        System.out.println(" \tOptimized\tUnoptimized");
        System.out.print("UPI 70%\t");
        System.out.print(1-optimizedDateInterval.getUPI(0.70));
        System.out.print("\t");
        System.out.print(1-unoptimizedDateInterval.getUPI(0.70));
        System.out.println();

        System.out.print("UPI 25%\t");
        System.out.print(1-optimizedDateInterval.getUPI(0.25));
        System.out.print("\t");
        System.out.print(1-unoptimizedDateInterval.getUPI(0.25));
        System.out.println();

        System.out.print("Fcst\t");
        System.out.print(1-optimizedDateInterval.getPrediction());
        System.out.print("\t");
        System.out.print(1-unoptimizedDateInterval.getPrediction());
        System.out.println();

        System.out.print("LPI 25%\t");
        System.out.print(1-optimizedDateInterval.getLPI(0.25));
        System.out.print("\t");
        System.out.print(1-unoptimizedDateInterval.getLPI(0.25));
        System.out.println();

        System.out.print("LPI 70%\t");
        System.out.print(1-optimizedDateInterval.getLPI(0.70));
        System.out.print("\t");
        System.out.print(1-unoptimizedDateInterval.getLPI(0.70));
        System.out.println();

        printBuckets(optimizedDateInterval, "Optimized");
        printBuckets(unoptimizedDateInterval, "Unoptimized");
    }

    private double getRange(ConfidenceInterval i) {
        return i.getUPI(0.7) - i.getLPI(0.7);
    }

    private String fmtPct(double percent) {
        return FormatUtil.formatPercent(percent);
    }

    private void printRanges(MonteCarloConfidenceInterval i) {
        double p = i.getProbability(1.0);
        System.out.println("On time: " + fmtPct(p));
        p = i.getProbability(1.2) - p;
        System.out.println("< 20% late: " + fmtPct(p));
        p = i.getProbability(1.5) - p;
        System.out.println("21-50% late: " + fmtPct(p));
        p = i.getProbability(2.0) - p;
        System.out.println("50-100% late: " + fmtPct(p));
        p = i.getProbability(3.0) - p;
        System.out.println("101-200% late: " + fmtPct(p));
        p = 1 - p;
        System.out.println(">200% late: " + fmtPct(p));
    }

    private void printBuckets(ConfidenceInterval i, String label) {

        System.out.println(" \t"+label);
        System.out.print("UPI 70%\t");
        System.out.print(1-i.getUPI(0.70));
        System.out.println();

        System.out.print("UPI 25%\t");
        System.out.print(1-i.getUPI(0.25));
        System.out.println();

        System.out.print("Fcst\t");
        System.out.print(1-i.getPrediction());
        System.out.println();

        System.out.print("LPI 25%\t");
        System.out.print(1-i.getLPI(0.25));
        System.out.println();

        System.out.print("LPI 70%\t");
        System.out.print(1-i.getLPI(0.70));
        System.out.println();
    }

    private void runOneTest() {
        teamSchedule.randomize();
        double forecastWeeks = teamSchedule.getForecastWeeks();
        optimizedDateInterval.addSample(forecastWeeks);
        optimizedVarInterval.addSample(planNumWeeks - forecastWeeks);

        double unoptimizedWeeks = teamSchedule.getUnoptimizedWeeks();
        unoptimizedDateInterval.addSample(unoptimizedWeeks);
        unoptimizedVarInterval.addSample(planNumWeeks - unoptimizedWeeks);
    }

    private double getRandomCost() {
        double result = normal.nextDouble(planCost, planCost*costEstErrPercent);
        return Math.max(planCost * 0.1, result);
    }

    double getRandomRate2() {
        double randomPct = u.nextDouble() / 2;
        double randomRatio = 0.75 + randomPct;
        return hoursPerWeek * randomRatio;
    }

    private double getRandomRate() {
        double randomPct = normal.nextDouble(0, scheduleRatioEstErr);
        double randomRatio = Math.min(2, Math.max(0, randomPct + 1));
        return hoursPerWeek * randomRatio;
    }


    private class Schedule {
        protected double actualCost;
        protected double actualRate;

        public void randomize() {
            actualCost = getRandomCost();
            actualRate = getRandomRate();
        }

        public double getForecastWeeks() {
            double numWeeks = actualCost / actualRate;
            return numWeeks;
        }
    }

    private class TeamSchedule extends Schedule {
        Schedule[] subSchedules;
        double unoptimizedWeeks;

        public TeamSchedule() {
            subSchedules = new Schedule[teamSize];
            for (int i = 0; i < teamSize; i++) {
                subSchedules[i] = new Schedule();
            }
        }

        public void randomize() {
            actualCost = actualRate = unoptimizedWeeks = 0;
            for (int i = 0; i < subSchedules.length; i++) {
                subSchedules[i].randomize();
                actualCost += subSchedules[i].actualCost;
                actualRate += subSchedules[i].actualRate;
                unoptimizedWeeks = Math.max
                    (unoptimizedWeeks, subSchedules[i].getForecastWeeks());
            }
        }

        public double getUnoptimizedWeeks() {
            return unoptimizedWeeks;
        }
    }
}
