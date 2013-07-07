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
public class Dbsumsize extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (Criteria)
     */
    public Object call(List arguments, ExpressionContext context) {
        List criteria = collapseLists(arguments, 0);

        try {
            List rawData = queryHql(context, BASE_QUERY, "f", criteria);
            return new ResultSetData(rawData, COLUMN_NAMES);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while calculating", e);
            return null;
        }
    }

    private static final String[] COLUMN_NAMES = { "Metric", "Type", "Base",
        "Added", "Deleted", "Modified", "Reused", "A&M", "Total" };

    private static final String BASE_QUERY = "select "
            + "f.sizeMetric.shortName, " //
            + "f.measurementType.name, " //
            + "sum(f.baseSize), " //
            + "sum(f.addedSize), " //
            + "sum(f.deletedSize), " //
            + "sum(f.modifiedSize), " //
            + "sum(f.reusedSize), " //
            + "sum(f.addedAndModifiedSize), " //
            + "sum(f.totalSize) " //
            + "from SizeFact f " //
            + "where f.versionInfo.current = 1 " //
            + "group by f.sizeMetric.shortName, f.measurementType.name";

}
