// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Filterdatanames extends AbstractFunction {

    /**
     * Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context) {
        ListData dataNames = asList(getArg(arguments, 0));
        String token = asString(getArg(arguments, 1));
        ListData paths = asList(getArg(arguments, 2));
        if (dataNames == null || !dataNames.test() //
                || token == null || token.length() == 0 //
                || paths == null || !paths.test())
            return ListData.EMPTY_LIST;

        ListData result = new ListData();
        if (!token.startsWith("/"))
            token = "/" + token;
        HashSet pathSet = new HashSet(paths.asList());
        for (Iterator i = dataNames.asList().iterator(); i.hasNext();) {
            String oneDataName = (String) i.next();
            int pos = oneDataName.lastIndexOf(token);
            if (pos != -1) {
                String onePath = oneDataName.substring(0, pos);
                if (pathSet.contains(onePath))
                    result.add(oneDataName);
            }
        }
        return result;
    }

}
