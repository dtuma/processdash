// Copyright (C) 2005-2007 Tuma Solutions, LLC
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

import java.text.NumberFormat;

import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;


// This class is not used by production dashboard code.  It exists to
// demonstrate and measure the effects of workload balancing on forecast
// completion date prediction intervals.
public class SimpleEVSimulation {

    private static NumberFormat FMT;

    public static void main(String[] args) {
        FMT = NumberFormat.getPercentInstance();
        FMT.setMaximumFractionDigits(1);

        printResults("Individual", new IndivEVInterval());

        for (int i = 1;  i <= 10;  i++) {
            printResults("Unoptimized, team size="+i, new UnbalancedTeamEVInterval(i));
            printResults("Optimized, team size="+i, new BalancedTeamEVInterval(i));
        }
    }

    private static void printResults(String title, EvInterval ci) {
        ci.runSimulation();

        System.out.println(title);
        System.out.println("70% LPI = " + FMT.format(ci.getLPI(0.7)));
        System.out.println("pred = " + FMT.format(ci.getPrediction()));
        System.out.println("70% UPI = " + FMT.format(ci.getUPI(0.7)));
        System.out.println("# samples = " + ci.getNumSamples());
        System.out.println();
    }

    private static class EvInterval extends MonteCarloConfidenceInterval {
        protected Normal normal = new Normal(0, 1, new MersenneTwister());
        protected double costEstErrPercent = 0.5; // +/- 50 %
        protected double scheduleRatioEstErr = 0.25; // +/- 25%
        protected double planCost = 1;
        protected double planRate = 1;

        protected double getRandomCost() {
            double actualCost = normal.nextDouble(planCost, planCost*costEstErrPercent);
            actualCost = Math.max(planCost * 0.1, actualCost);
            return actualCost;
        }

        protected double getRandomRate() {
            double actualRate = normal.nextDouble(planRate, planRate*scheduleRatioEstErr);
            actualRate = Math.min(planRate*2, Math.max(0, actualRate));
            return actualRate;
        }

        protected int getBaseNumSamples() {
            return 5000;
        }


        public int getNumSamples() {
            return samples.size();
        }
    }

    private static class IndivEVInterval extends EvInterval {

        protected double getSample() {
            double planCost = this.planCost;
            double planRate = this.planRate;

            double actualCost = getRandomCost();
            double actualRate = getRandomRate();

            return 1 - (actualCost / planCost) * (planRate / actualRate);
        }

    }

    private static class BalancedTeamEVInterval extends EvInterval {
        private int teamSize;

        public BalancedTeamEVInterval(int teamSize) {
                this.teamSize = teamSize;
        }

        protected double getSample() {
            double planCost = this.planCost * teamSize;
            double planRate = this.planRate * teamSize;

            double actualCost = 0;
            double actualRate = 0;

            for (int i = teamSize;   i-- > 0; ) {
                actualCost += getRandomCost();
                actualRate += getRandomRate();
            }

            return 1 - (actualCost / planCost) * (planRate / actualRate);
        }
    }



    private static class UnbalancedTeamEVInterval extends EvInterval {
        private int teamSize;

        public UnbalancedTeamEVInterval(int teamSize) {
            this.teamSize = teamSize;
        }

        protected double getSample() {
            double planCost = this.planCost * teamSize;
            double planRate = this.planRate * teamSize;
            double planDuration = planCost / planRate;

            double actualDuration = 0;

            for (int i = teamSize;   i-- > 0; ) {
                double actualIndivCost = getRandomCost();
                double actualIndivRate = getRandomRate();

                double actualIndivDuration = actualIndivCost / actualIndivRate;
                actualDuration = Math.max(actualDuration, actualIndivDuration);
            }

            return 1 - (actualDuration / planDuration);
        }
    }

}
