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

public class StudentsT {

    private int degreesOfFreedom;

    public StudentsT(int degreesOfFreedom) {
        this.degreesOfFreedom = degreesOfFreedom;
    }


    public double alpha(double t) { return alpha(t, 0.0001); }
    public double alpha(double t, double acceptable_error) {
        return new T_Dist(degreesOfFreedom, acceptable_error).f(t);
    }

    public double probability(double alpha) {
        return probability(alpha, 0.0001);
    }
    public double probability(double alpha, double acceptable_error) {
        return new Inverse(new T_Dist(degreesOfFreedom, acceptable_error/10),
                           acceptable_error).f(alpha);
    }

    static final double TOLERANCE = 0.0000001;

    private class T_Term implements GenericFunction {
        int n;
        double gamma_constant;

        public T_Term(int degreesOfFreedom) {
            n = degreesOfFreedom;
            gamma_constant = ( gamma((n + 1) / 2.0) /
                               (Math.sqrt(n * Math.PI) * gamma(n / 2.0)) );
        }

        public double f(double u) {
            return gamma_constant * Math.pow(1.0 + (u * u / n),
                                             (n + 1) / (-2.0));
        }

        private double gamma(double x) throws ArithmeticException {

            if (x < 0.0)
                throw new ArithmeticException
                    ("Argument to gamma must be a positive multiple of 0.5");
            else if (Math.abs(x - 1.0) < TOLERANCE)
                return 1.0;
            else if (Math.abs(x - 0.5) < TOLERANCE)
                return Math.sqrt(Math.PI);
            else
                return (x - 1.0) * gamma(x - 1.0);
        }
    }

    private class T_Dist implements GenericFunction {
        private GenericFunction t_func;
        double acceptable_error;

        public T_Dist(int degreesOfFreedom, double acceptable_error) {
            t_func = new T_Term(degreesOfFreedom);
            this.acceptable_error = acceptable_error;
        }

        public double f(double x) {
            return Integrator.integrate(t_func, 0.0, x, acceptable_error);
        }
    }
}
