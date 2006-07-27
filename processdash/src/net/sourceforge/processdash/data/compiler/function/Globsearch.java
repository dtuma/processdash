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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.glob.GlobEngine;

public class Globsearch extends AbstractFunction {

    public Object call(List arguments, ExpressionContext context) {
        String expression = asString(getArg(arguments, 0));
        if (!StringUtils.hasValue(expression))
            // if there is no search expression, return null (instead of the
            // empty list) as an indicator to clients that no search was
            // performed.
            return null;

        List taggedData = collapseLists(arguments, 1);
        if (taggedData.size() < 3)
            return ListData.EMPTY_LIST;

        String tagPrefix = asString(taggedData.remove(0));
        Set results = GlobEngine.search(expression, tagPrefix, taggedData);

        if (results == null || results.isEmpty())
            return ListData.EMPTY_LIST;

        ListData result = new ListData();
        for (Iterator i = results.iterator(); i.hasNext();)
            result.add(i.next());

        try {
            result.sortContents(String.CASE_INSENSITIVE_ORDER);
        } catch (Exception e) {}

        return result;
    }

}
