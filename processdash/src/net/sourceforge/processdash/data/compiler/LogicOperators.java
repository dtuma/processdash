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
