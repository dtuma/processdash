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


package net.sourceforge.processdash.ev.ci;

import DistLib.uniform;

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
    public double getRandomValue(uniform u);

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
