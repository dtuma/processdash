// Copyright (C) 2011 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.StringUtils;

public class Genericsizeunits extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        String variable = asStringVal(getArg(arguments, 0));
        if (variable == null) return null;
        String defaultValue = asStringVal(getArg(arguments, 1));

        StringData result = null;
        String units = asString(context.get(variable));
        if (StringUtils.hasValue(units) && !units.equals(defaultValue)) {
            int semicolonPos = units.lastIndexOf(';');
            if (semicolonPos != -1)
                units = units.substring(semicolonPos+1);
            if (units.length() > 0)
                result = StringData.create(units);
        }

        return new DescribedValue(result, context.resolveName(variable));
    }
}
