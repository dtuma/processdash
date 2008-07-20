// Copyright (C) 2001-2006 Tuma Solutions, LLC
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
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.DataRepository;

public class Sumfor extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        double result = 0.0, val;

        String name = asString(getArg(arguments, 0));
        if (name == null) return null;

        Iterator i = collapseLists(arguments, 1).iterator();
        String path, dataName, alias;
        alias = NO_ALIAS_YET;
        SimpleData sVal = null;
        while (i.hasNext()) {
            path = asStringVal(i.next());
            if (path == null) continue;

            dataName = DataRepository.createDataName(path, name);
            if (alias == NO_ALIAS_YET) alias = dataName; else alias = null;
            sVal = context.get(dataName);
            val = asDouble(sVal);
            if (!Double.isNaN(val) && !Double.isInfinite(val))
                result += val;
        }
        if (alias != null && alias != NO_ALIAS_YET && sVal != null)
            return new DescribedValue(sVal, context.resolveName(alias));
        else if (result == 0)
            return ImmutableDoubleData.READ_ONLY_ZERO;
        else
            return new DoubleData(result);
    }
    private static final String NO_ALIAS_YET = "";
}
