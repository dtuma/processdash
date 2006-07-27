// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Intersects extends AbstractFunction {

    public Object call(List arguments, ExpressionContext context) {
        if (arguments.size() == 0)
            return getResult(Collections.EMPTY_SET);

        Iterator i = arguments.iterator();

        Set intersection;

        ListData firstItem = asList((SimpleData) i.next());
        if (firstItem == null)
            intersection = Collections.EMPTY_SET;
        else
            intersection = new HashSet(firstItem.asList());

        while (!intersection.isEmpty() && i.hasNext()) {
            ListData nextItem = asList((SimpleData) i.next());
            if (nextItem == null)
                intersection = Collections.EMPTY_SET;
            else
                intersection.retainAll(nextItem.asList());
        }

        return getResult(intersection);
    }

    protected Object getResult(Set intersection) {
        if (intersection == null || intersection.isEmpty())
            return ImmutableDoubleData.FALSE;
        else
            return ImmutableDoubleData.TRUE;
    }

}
