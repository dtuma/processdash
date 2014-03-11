// Copyright (C) 2013-2014 Tuma Solutions, LLC
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
public class Dbsumtimebyphase extends DbAbstractFunction {

    private boolean usePhaseMappings = isDatabaseVersion("1.3");

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (Process_ID, Criteria)
     */
    public Object call(List arguments, ExpressionContext context) {
        String processId = asStringVal(getArg(arguments, 0));
        List criteria = collapseLists(arguments, 1);

        try {
            List rawData;
            if (usePhaseMappings) {
                rawData = queryHql(context, PHASE_MAP_BASE_QUERY, "f",
                    criteria, processId);
                rawData.addAll(queryHql(context, PHASE_MAP_UNCAT_QUERY, "f",
                    criteria));
            } else {
                rawData = queryHql(context, BASE_QUERY, "f", criteria);
            }
            return new ResultSetData(rawData, COLUMN_NAMES);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while calculating", e);
            return null;
        }
    }

    private static final String[] COLUMN_NAMES = { "Phase", "Plan", "Actual" };

    private static final String BASE_QUERY = "select "
            + "coalesce(phase.shortName, 'No Phase '), " //
            + "sum(f.planTimeMin), " //
            + "sum(f.actualTimeMin) " //
            + "from TaskStatusFact f " //
            + "left outer join f.planItem.phase phase "
            + "where f.versionInfo.current = 1 " //
            + "group by phase.shortName";

    private static final String PHASE_MAP_BASE_QUERY = "select "
            + "phase.shortName, " //
            + "sum(f.planTimeMin), " //
            + "sum(f.actualTimeMin) " //
            + "from TaskStatusFact f " //
            + "left outer join f.planItem.phase.mapsToPhase phase "
            + "where f.versionInfo.current = 1 "
            + "and phase.process.identifier = ? " //
            + "group by phase.shortName";

    private static final String PHASE_MAP_UNCAT_QUERY = "select "
            + "'No Phase ', " //
            + "coalesce(sum(f.planTimeMin), 0), " //
            + "coalesce(sum(f.actualTimeMin), 0) " //
            + "from TaskStatusFact f " //
            + "where f.versionInfo.current = 1 "
            + "and f.planItem.phase is null";

}
