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

import java.util.List;
import java.util.logging.Level;

import net.sourceforge.processdash.data.ResultSetData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;


/** @since 1.15.5 */
public class Dbsumdefectsbyphase extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (Process_ID, Criteria)
     */
    @SuppressWarnings("unused")
    public Object call(List arguments, ExpressionContext context) {
        String processId = asStringVal(getArg(arguments, 0));
        List criteria = collapseLists(arguments, 1);

        try {
            List result = queryHql(context, BASE_INJ_QUERY, "f", criteria);
            result.addAll(queryHql(context, BASE_REM_QUERY, "f", criteria));
            return new ResultSetData(result, COLUMN_NAMES);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while calculating", e);
            return null;
        }
    }

    private static final String[] COLUMN_NAMES = { "Phase", "Type", "Count" };

    private static final String BASE_INJ_QUERY = "select " //
            + "f.injectedPhase.shortName, " //
            + "'Injected', " //
            + "sum(f.fixCount) " //
            + "from DefectLogFact f " //
            + "where f.versionInfo.current = 1 " //
            + "group by f.injectedPhase.shortName";

    private static final String BASE_REM_QUERY = "select " //
            + "f.removedPhase.shortName, " //
            + "'Removed', " //
            + "sum(f.fixCount) " //
            + "from DefectLogFact f " //
            + "where f.versionInfo.current = 1 " //
            + "group by f.removedPhase.shortName";

}
