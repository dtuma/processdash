// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

public class Inverse implements GenericFunction {

    /** The monotonic function to be inverted. */
    private GenericFunction func;

    /** The acceptable error for the calculation */
    private double acceptableDelta;

    /** 1.0 if the function is monotonic increasing, -1.0 if it is
     * monotonic decreasing. */
    private double foobar;

    public Inverse(GenericFunction func, double acceptableError) {
        this.func = func;
        this.acceptableDelta = 2.0 * acceptableError;
        if (func.f(1.0) > func.f(0.0))
            foobar = 1.0;
        else
            foobar = -1.0;
    }

    public double f(double y) {
        double lowerBound = -1.0;
        double upperBound = 1.0;

        while (func.f(foobar * upperBound) < y) {
            lowerBound = upperBound;
            upperBound *= 2.0;
        }
        while (func.f(foobar * lowerBound) > y) {
            upperBound = lowerBound;
            lowerBound *= 2.0;
        }

        double currentGuess;

        while (true) {
            currentGuess = (upperBound + lowerBound) / 2.0;

            if (upperBound - lowerBound < acceptableDelta)
                return foobar * currentGuess;

            if (func.f(foobar * currentGuess) < y)
                lowerBound = currentGuess;
            else
                upperBound = currentGuess;
        }
    }
}

