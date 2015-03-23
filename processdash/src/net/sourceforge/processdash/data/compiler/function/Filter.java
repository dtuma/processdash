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

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.compiler.ListStack;
import net.sourceforge.processdash.data.compiler.LocalExpressionContext;

public class Filter extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        CompiledScript script = null;
        try {
            script = (CompiledScript) arguments.get(0);
        } catch (ClassCastException cce) { }
        if (script == null) return null;

        ListData result = new ListData();
        LocalExpressionContext lContext = new LocalExpressionContext(context);
        ListStack stack = new ListStack();
        Iterator i = collapseLists(arguments, 1).iterator();
        Object item;
        while (i.hasNext()) try {
            lContext.setLocalValue(item = i.next());
            stack.clear();
            script.run(stack, lContext);
            handleItem(result, item, stack.pop());
        } catch (Exception e) {}
        return result;
    }

    protected void handleItem(ListData result, Object local, Object val) {
        if (test(val)) result.add(local);
    }

    private static boolean test(Object o) {
        return (o instanceof SimpleData && ((SimpleData) o).test());
    }
}
