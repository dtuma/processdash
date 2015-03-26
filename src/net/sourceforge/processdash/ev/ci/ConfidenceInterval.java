// Copyright (C) 2003-2008 Tuma Solutions, LLC
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

public interface ConfidenceInterval {

    /** Some confidence intervals are sensitive to the value they are
     * correcting.  Clients should call setInput() with the input value
     * before calling other methods on the confidence interval.
     */
    public void setInput(double input);

    /** Return the forecast value produced by translating the input
     * value according to correction algorithm contained in this
     * confidence interval.
     */
    public double getPrediction();

    /**
     * Return the value of the confidence interval at a specific percentage
     * point in the cumulative probability distribution.
     * 
     * @param percentage a number greater than 0.0 and less than 1.0
     * @return the value of the distribution at the given percentage
     */
    public double getQuantile(double percentage);

    /** Return the lower end of the confidence interval for a
     * particular confidence percentage.
     */
    public double getLPI(double percentage);

    /** Return the upper end of the confidence interval for a
     * particular confidence percentage.
     */
    public double getUPI(double percentage);

    /** Return a random value from the distribution upon which
     * this confidence interval is based.
     */
    public double getRandomValue(RandomEngine u);

    /** Return a value indicating how viable this confidence interval
     * seems. Numbers less than 0 indicate invalid confidence
     * intervals which should not be used for planning purposes.
     */
    public double getViability();


    public final double CANNOT_CALCULATE = -100.0;
    public final double SERIOUS_PROBLEM = -10.0;
    public final double ACCEPTABLE = 0.0;
    public final double NOMINAL = 5.0;

}
