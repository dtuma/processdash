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

package pspdash.data.compiler;

import pspdash.data.SimpleData;
import pspdash.data.DoubleData;
import pspdash.data.NumberData;

class MathOperators {

    private MathOperators() {}

    public static final Instruction ADD = new BinaryMathOperator("+") {
            protected double calc(double l, double r) { return l + r; } };

    public static final Instruction SUBTRACT = new BinaryMathOperator("-") {
            protected double calc(double l, double r) { return l - r; } };

    public static final Instruction MULTIPLY = new BinaryMathOperator("*") {
            protected double calc(double l, double r) { return l * r; } };

    public static final Instruction DIVIDE = new BinaryMathOperator("/") {
            protected double calc(double l, double r) { return l / r; } };

}


class BinaryMathOperator extends BinaryOperator {

    static final SimpleData NOT_A_NUMBER = new DoubleData(Double.NaN, false);

    public BinaryMathOperator(String op) { super(op); }

    protected SimpleData operate(SimpleData left, SimpleData right) {
        if (! (left instanceof NumberData && right instanceof NumberData))
            return NOT_A_NUMBER;
        double leftVal  = ((NumberData) left).getDouble();
        double rightVal = ((NumberData) right).getDouble();

        return new DoubleData(calc(leftVal, rightVal), false);
    }

    protected double calc(double left, double right) { return 0.0; }
}
