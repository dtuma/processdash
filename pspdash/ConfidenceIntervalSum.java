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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import DistLib.uniform;

public class ConfidenceIntervalSum extends MonteCarloConfidenceInterval {

    List subintervals;
    uniform u;

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
        u = new uniform();
        super.runSimulation();
    }

    protected double getSample() {
        double result = 0;
        Iterator i = subintervals.iterator();
        while (i.hasNext()) {
            ConfidenceInterval ci = (ConfidenceInterval) i.next();
            u.random();
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
