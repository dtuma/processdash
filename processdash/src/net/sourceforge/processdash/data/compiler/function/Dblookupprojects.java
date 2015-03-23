// Copyright (C) 2013 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.SubscribingExpressionContext;
import net.sourceforge.processdash.tool.db.ProjectLocator;

/** @since 1.15.5 */
public class Dblookupprojects extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context) {

        // retrieve the list of IDs that we wish to look up, and the name of
        // the data element we are looking them up for
        List projectIDs = collapseLists(arguments, 0);
        String listenerName = asStringVal(context
                .get(SubscribingExpressionContext.LISTENERVAR_NAME));

        try {
            ProjectLocator loc = getDbObject(context, ProjectLocator.class);
            if (loc == null)
                return null;

            ListData result = new ListData();
            for (Iterator i = projectIDs.iterator(); i.hasNext();) {
                Integer key = null;

                String oneProjectId = asStringVal(i.next());
                if (oneProjectId != null)
                    key = loc.getKeyForProject(oneProjectId, listenerName);

                if (key == null)
                    key = -999;
                result.add(new ImmutableDoubleData(key, false, true));
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
