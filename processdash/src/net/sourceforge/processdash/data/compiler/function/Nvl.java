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

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Nvl extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        SimpleData arg;

        for (int i = 0;   i < arguments.size();   i++)
            if (!isBadValue(arg = getArg(arguments, i)))
                return arg;

        return null;
    }

    private static boolean isBadValue(SimpleData data) {
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
