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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class ConfidenceIntervalSum extends MonteCarloConfidenceInterval {

    List subintervals;
    RandomEngine u;

    public ConfidenceIntervalSum() {
        subintervals = new LinkedList();
    }

    public void addInterval(ConfidenceInterval i) {
        subintervals.add(i);
    }

    public void intervalsComplete() {
        runSimulation();
    }

    protected void runSimulation() {
        u = new MersenneTwister();
        super.runSimulation();
    }

    protected double getSample() {
        double result = 0;
        Iterator i = subintervals.iterator();
        while (i.hasNext()) {
            ConfidenceInterval ci = (ConfidenceInterval) i.next();
            u.nextDouble();
            result += ci.getRandomValue(u);
        }
        return result;
    }

    public double getLPI(double percentage) {
        // System.out.println("cost interval: getLPI");
        double result = super.getLPI(percentage);
        // System.out.println("cost interval: getLPI = " + result);
        return result;
    }

    public double getUPI(double percentage) {
        // System.out.println("cost interval: getUPI");
        double result = super.getUPI(percentage);
        // System.out.println("cost interval: getUPI = " + result);
        return result;
    }

}
