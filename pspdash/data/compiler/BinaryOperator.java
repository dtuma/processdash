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

abstract class BinaryOperator implements Instruction {

    private String operatorString = null;

    BinaryOperator(String op) {
        operatorString = op;
    }

    public void execute(Stack stack, ExpressionContext context)
        throws ExecutionException
    {
        Object left = null, right = null;
        try {
            right = stack.pop();
            left = stack.pop();
        } catch (Exception e) {
            throw new ExecutionException("execution stack is empty");
        }
        try {
            stack.push(operate((SimpleData) left, (SimpleData) right));
        } catch (ClassCastException cce) {
            throw new ExecutionException("ClassCastException");
        }
    }

    protected abstract SimpleData operate(SimpleData left, SimpleData right);

    public String toString() { return operatorString; }
}
