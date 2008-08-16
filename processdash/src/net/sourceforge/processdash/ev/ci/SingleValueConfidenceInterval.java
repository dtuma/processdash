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

package net.sourceforge.processdash.ev.ci;

import cern.jet.random.engine.RandomEngine;

public class SingleValueConfidenceInterval implements ConfidenceInterval {

    private double singleValue;

    private double viability;

    public SingleValueConfidenceInterval(double singleValue) {
        this(singleValue, NOMINAL);
    }

    public SingleValueConfidenceInterval(double singleValue, double viability) {
        this.singleValue = singleValue;
        this.viability = viability;
    }


    public double getLPI(double percentage) {
        return singleValue;
    }

    public double getPrediction() {
        return singleValue;
    }

    public double getQuantile(double percentage) {
        return singleValue;
    }

    public double getRandomValue(RandomEngine u) {
        return singleValue;
    }

    public double getUPI(double percentage) {
        return singleValue;
    }

    public double getViability() {
        return viability;
    }

    public void setInput(double input) {}

}
