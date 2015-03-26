// Copyright (C) 2006 Tuma Solutions, LLC
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
