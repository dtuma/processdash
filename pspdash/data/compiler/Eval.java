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

import pspdash.data.DataRepository;
import pspdash.data.SimpleData;
import pspdash.data.DoubleData;
import pspdash.data.ListData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Eval extends AbstractFunction {

    private static Map scriptCache =
        Collections.synchronizedMap(new HashMap());

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        String expression = asString(getArg(arguments, 0));
        if (expression == null || expression.length() == 0)
            return null;

        String prefix = asString(getArg(arguments, 1));

        CompiledScript script = (CompiledScript) scriptCache.get(expression);
        if (script == null) {
            if (scriptCache.containsKey(expression))
                return null;

            else try {
                script = Compiler.compile(expression);
                scriptCache.put(expression, script);
            } catch (CompilationException e) {
                scriptCache.put(expression, null);
                return null;
            }
        }

        try {
            Stack stack = new ListStack();
            if (prefix != null)
                context = new RelativeExpressionContext(context, prefix);
            script.run(stack, context);
            return stack.pop();
        } catch (ExecutionException ee) {
            return null;
        }
    }

}
