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

package net.sourceforge.processdash.data.compiler;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;

public class RelationalOperators {

    static final SimpleData TRUE  = new DoubleData(1.0, false);
    static final SimpleData FALSE = new DoubleData(0.0, false);

    private RelationalOperators() {}


    public static final Instruction EQ = new BinaryRelationalOperator("==") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return left.equals(right); } };

    public static final Instruction NEQ = new BinaryRelationalOperator("!=") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return ! left.equals(right); } };

    public static final Instruction LT = new BinaryRelationalOperator("<") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return left.lessThan(right); } };

    public static final Instruction LTEQ = new BinaryRelationalOperator("<=") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return left.lessThan(right) || left.equals(right); } };

    public static final Instruction GT = new BinaryRelationalOperator(">") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return left.greaterThan(right); } };

    public static final Instruction GTEQ = new BinaryRelationalOperator(">=") {
        protected boolean calc(SimpleData left, SimpleData right) {
            return left.greaterThan(right) || left.equals(right); } };
}


class BinaryRelationalOperator extends BinaryOperator {

    public BinaryRelationalOperator(String op) { super(op); }

    protected SimpleData operate(SimpleData left, SimpleData right) {
        if (left == null || right == null) return RelationalOperators.FALSE;
        return (calc(left, right) ? RelationalOperators.TRUE
                                  : RelationalOperators.FALSE);
    }

    protected boolean calc(SimpleData left, SimpleData right) {
        return false;
    }
}
