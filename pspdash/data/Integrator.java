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

public class Integrator {

    public GenericFunction func;
    public double default_acceptable_error;

    public Integrator(GenericFunction func) {
        this(func, 0.0001);
    }

    public Integrator(GenericFunction func, double default_acceptable_error) {
        this.func = func;
        this.default_acceptable_error = default_acceptable_error;
    }

    public double integrate(double low, double high) {
        return integrate(func, low, high, default_acceptable_error);
    }


    /** starting number of intervals for simpson integration.
     * This MUST be an even number. */
    private static final int DEFAULT_INITIAL_INTERVALS = 10;

    /** perform simpson's rule to integrate the function.
     * @param low the lower bound for integration
     * @param high the upper bound for integration.
     * @param intervals the number of intervals to use with simpson's
     * integration algoritm.  This  MUST be an even number.
     */
    private static double simpson(GenericFunction func,
                                  double low, double high,
                                  int intervals) {

        double W = (high - low) / intervals;
        double sum = func.f(low) + func.f(high);

        for (int indx = 1;   indx < intervals;  indx++)
            sum += ((indx & 1) == 1 ? 4.0 : 2.0) * func.f(low + W*indx);

        return W * sum / 3.0;
    }

    public static double integrate(GenericFunction func,
                                   double low, double high,
                                   double acceptable_error) {

        double previous_value, current_value, difference;
        int intervals = DEFAULT_INITIAL_INTERVALS;

        previous_value = simpson(func, low, high, intervals);

        while (true) {
            intervals *= 2;
            current_value = simpson(func, low, high, intervals);
            difference    = current_value - previous_value;
            if (difference < 0.0)
                difference *= -1.0;

            if (difference < acceptable_error)
                break;

            previous_value = current_value;
        }

        return current_value;
    }

}
