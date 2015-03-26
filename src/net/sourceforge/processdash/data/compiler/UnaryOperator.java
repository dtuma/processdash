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

import net.sourceforge.processdash.data.SimpleData;

abstract class UnaryOperator implements Instruction {

    private String operatorString = null;

    UnaryOperator(String op) {
        operatorString = op;
    }

    public void execute(Stack stack, ExpressionContext context)
        throws ExecutionException
    {
        Object operand = null;
        try {
            operand = stack.pop();
        } catch (Exception e) {
            throw new ExecutionException("execution stack is empty");
        }
        try {
            stack.push(operate((SimpleData) operand));
        } catch (ClassCastException cce) {
            throw new ExecutionException("ClassCastException");
        }
    }

    protected abstract SimpleData operate(SimpleData operand);

    public String toString() { return operatorString; }
}
