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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DataComparator;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.DataRepository;

public class Sort extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        String name = asString(getArg(arguments, 0));
        if (name == null) return null;

        List pairs = new ArrayList();
        Iterator i = collapseLists(arguments, 1).iterator();
        String path, dataName;
        while (i.hasNext()) {
            path = asStringVal(i.next());
            if (path == null) continue;

            dataName = DataRepository.createDataName(path, name);
            pairs.add(new Pair(path, context.get(dataName)));
        }

        Object [] pairArray = pairs.toArray();
        Arrays.sort(pairArray);

        ListData result = new ListData();
        for (int j = 0;   j < pairArray.length;   j++)
            result.add(((Pair) pairArray[j]).prefix);
        result.setImmutable();

        return result;
    }

    private class Pair implements Comparable {
        public String prefix;
        public SimpleData val;
        public Pair(String p, SimpleData v) { prefix = p; val = v; }
        public int compareTo(Object o) {
            return DataComparator.instance.compare(val, ((Pair) o).val);
        }
    }
}
