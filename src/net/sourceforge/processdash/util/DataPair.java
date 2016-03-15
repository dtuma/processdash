// Copyright (C) 2014-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

public class DataPair {

    public double plan, actual;

    public DataPair() {}

    public DataPair(DataPair src) {
        this.plan = src.plan;
        this.actual = src.actual;
    }

    public DataPair add(DataPair add) {
        this.plan += add.plan;
        this.actual += add.actual;
        return this;
    }

    public DataPair add(double add) {
        this.plan += add;
        this.actual += add;
        return this;
    }

    public DataPair subtract(DataPair subtract) {
        this.plan -= subtract.plan;
        this.actual -= subtract.actual;
        return this;
    }

    public DataPair subtract(double subtract) {
        this.plan -= subtract;
        this.actual -= subtract;
        return this;
    }

    public DataPair multiply(DataPair multiply) {
        this.plan *= multiply.plan;
        this.actual *= multiply.actual;
        return this;
    }

    public DataPair multiply(double multiply) {
        this.plan *= multiply;
        this.actual *= multiply;
        return this;
    }

    public DataPair divide(DataPair divide) {
        this.plan /= divide.plan;
        this.actual /= divide.actual;
        return this;
    }

    public DataPair divide(double divide) {
        this.plan /= divide;
        this.actual /= divide;
        return this;
    }

    public void replaceNaN(double replacementValue) {
        if (Double.isNaN(plan))
            plan = replacementValue;
        if (Double.isNaN(actual))
            actual = replacementValue;
    }

    @Override
    public String toString() {
        return "(" + plan + ", " + actual + ")";
    }

}
