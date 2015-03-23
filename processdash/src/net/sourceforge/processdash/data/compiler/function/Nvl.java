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

package net.sourceforge.processdash.data.compiler.function;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.compiler.ListStack;

public class Nvl extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        ListStack stack = null;
        for (Iterator iter = arguments.iterator(); iter.hasNext();) {
            Object arg = iter.next();

            if (arg instanceof CompiledScript) {
                try {
                    CompiledScript script = (CompiledScript) arg;
                    if (stack == null)
                        stack = new ListStack();
                    else
                        stack.clear();
                    script.run(stack, context);
                    arg = stack.pop();
                } catch (Exception e) {}
            }

            if (arg instanceof SimpleData
                    && !isBadValue((SimpleData) arg))
                return arg;
        }

        return null;
    }

    protected boolean isBadValue(SimpleData data) {
        // null values are "bad".
        if (data == null) return true;

        // numbers with non-real values are "bad".
        if (data instanceof DoubleData) {
            double d = ((DoubleData) data).getDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) return true;
        }

        // everything else is "good".
        return false;
    }
}
