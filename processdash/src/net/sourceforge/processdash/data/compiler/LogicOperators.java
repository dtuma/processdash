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

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;

public class LogicOperators {

    public static final SimpleData TRUE  = ImmutableDoubleData.TRUE;
    public static final SimpleData FALSE = ImmutableDoubleData.FALSE;

    private LogicOperators() {}

    public static final Instruction AND = new BinaryLogicOperator("&&") {
            protected boolean calc(boolean l, boolean r) { return l && r; } };

    public static final Instruction OR = new BinaryLogicOperator("||") {
            protected boolean calc(boolean l, boolean r) { return l || r; } };

    public static final Instruction NOT = new UnaryOperator("!") {
            protected SimpleData operate(SimpleData operand) {
                if (operand == null)
                    return TRUE;
                else if (operand.test() == true)
                    return FALSE;
                else
                    return TRUE;
            } };
}

class BinaryLogicOperator extends BinaryOperator {

    public BinaryLogicOperator(String op) { super(op); }

    protected SimpleData operate(SimpleData left, SimpleData right) {
        boolean leftVal  = (left  == null ? false : left.test());
        boolean rightVal = (right == null ? false : right.test());
        return (calc(leftVal, rightVal) ? LogicOperators.TRUE
                                        : LogicOperators.FALSE);
    }

    protected boolean calc(boolean left, boolean right) { return false; }
}
