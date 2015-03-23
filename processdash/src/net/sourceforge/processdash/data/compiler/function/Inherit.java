// Copyright (C) 2002-2003 Tuma Solutions, LLC
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
