// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.compiler;

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;

class MathOperators {

    private MathOperators() {}

    public static final Instruction ADD = new BinaryMathOperator("+") {
            protected double calc(double l, double r) { return l + r; } };

    public static final Instruction SUBTRACT = new BinaryMathOperator("-") {
            protected double calc(double l, double r) { return l - r; } };

    public static final Instruction MULTIPLY = new BinaryMathOperator("*") {
            protected double calc(double l, double r) { return l * r; } };

    public static final Instruction DIVIDE = new BinaryMathOperator("/") {
            protected double calc(double l, double r) {
                return (r == 0 ? Double.POSITIVE_INFINITY : l / r); } };

}

/*
 * Within the dashboard, we assign special meanings to two IEEE double values:
 *
 *   o When a calculation cannot be performed because a referenced
 *     data element is missing or invalid, we use the double value "NaN".
 *
 *   o When a calculation results in a divide-by-zero error, we use
 *     the double value "positive infinity."
 *
 * Because these values have special meanings, we need to perform some slight
 * overrides to standard Java floating point arithmetic.  For example, the
 * Java language spec defines that Infinity / Infinity = NaN.  That wouldn't
 * make sense according to our special meanings assigned above.
 *
 * So the modified logic for mathematical operations is this:
 *
 * (1) LOGIC: Return NaN if either of the two operands is missing
 *     (null), not of numeric type, or NaN.  EFFECT: bad or missing
 *     values in a calculation will cause the calculation to evaluate
 *     to "bad value", and this "bad value" will in turn propagate to
 *     any calculations it is used in.
 *
 * (2) LOGIC: If either operand is infinite, return positive infinity.
 *     EFFECT: divide-by-zero errors result in infinity, and these
 *     infinity values will propagate to other calculations that
 *     reference them.
 *
 * Note that special case (1) takes precedence over special case (2).
 */

class BinaryMathOperator extends BinaryOperator {

    public BinaryMathOperator(String op) { super(op); }

    protected SimpleData operate(SimpleData left, SimpleData right) {
        if (! (left instanceof NumberData && right instanceof NumberData))
            return ImmutableDoubleData.BAD_VALUE;

        double leftVal  = ((NumberData) left).getDouble();
        double rightVal = ((NumberData) right).getDouble();

        if (Double.isNaN(leftVal) || Double.isNaN(rightVal))
            return ImmutableDoubleData.BAD_VALUE;

        if (Double.isInfinite(leftVal) || Double.isInfinite(rightVal))
            return ImmutableDoubleData.DIVIDE_BY_ZERO;

        return new ImmutableDoubleData(calc(leftVal, rightVal), false, true);
    }

    // no-op, meant to be overwritten.
    protected double calc(double left, double right) { return 0.0; }
}
