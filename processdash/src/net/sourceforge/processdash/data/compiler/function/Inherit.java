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

package net.sourceforge.processdash.data.compiler.function;

import java.util.List;

import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Inherit extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        String prefix = asStringVal
            (context.get(ExpressionContext.PREFIXVAR_NAME));
        String name = asStringVal(getArg(arguments, 0));
        if (prefix == null || name == null || name.length() == 0) return null;

        while (prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length()-1);

        String variable;
        Object result;
        int pos;

        while (true) {
            if (prefix == null || prefix.length() == 0) return null;
            pos = prefix.lastIndexOf('/');
            prefix = (pos == -1 ? "" : prefix.substring(0, pos));

            variable = prefix + "/" + name;
            result = context.get(variable);
            if (result != null)
                return new DescribedValue
                    (result, context.resolveName(variable));
        }
    }
}
