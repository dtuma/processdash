// Copyright (C) 2006-2013 Tuma Solutions, LLC
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.glob.GlobEngine;
import net.sourceforge.processdash.util.glob.TaggedDataListSource;

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
        Set results = GlobEngine.search(expression, tagPrefix, taggedData,
            new DeferredDataLookup(context));

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

    /** @since 1.14.5 */
    public static Set<String> getTags(ListData taggedData) {
        if (taggedData == null || taggedData.size() < 3)
            return Collections.EMPTY_SET;

        String tagPrefix = asString(taggedData.get(0));
        if (!StringUtils.hasValue(tagPrefix))
            return Collections.EMPTY_SET;

        List tagList = taggedData.asList().subList(1, taggedData.size());
        return GlobEngine.getTags(tagPrefix, tagList);
    }

    private static class DeferredDataLookup implements TaggedDataListSource {
        private ExpressionContext context;

        protected DeferredDataLookup(ExpressionContext context) {
            this.context = context;
        }

        public List getTaggedData(String dataName) {
            ListData result = ListData.asListData(context.get(dataName));
            return (result == null ? null : result.asList());
        }

    }

}
