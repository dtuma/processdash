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

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.util.StringUtils;


/** @since 1.15.5 */
public class Dblookupwbselement extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context) {
        // get the object for executing database queries
        QueryRunner queryRunner = getDbObject(context, QueryRunner.class);
        if (queryRunner == null)
            return null;

        // retrieve the WBS element name we are being asked to look up.
        String wbsElementName = asString(getArg(arguments, 0));
        if (wbsElementName == null)
            return null;
        if (wbsElementName.startsWith("/"))
            wbsElementName = wbsElementName.substring(1);
        if (wbsElementName.length() == 0)
            return null;
        wbsElementName = StringUtils.limitLength(wbsElementName, 255);

        // retrieve the plan item ID of the item we are being asked to look up.
        String planItemId = asString(getArg(arguments, 1));

        // look up this WBS element in the database.
        try {
            int key;
            List result = queryRunner.queryHql(NAME_QUERY, wbsElementName);
            if ((result == null || result.isEmpty())
                    && StringUtils.hasValue(planItemId))
                result = queryRunner.queryHql(ID_QUERY, planItemId);
            if (result != null && !result.isEmpty()) {
                // extract the value from the result set
                key = (Integer) result.get(0);
            } else {
                // not found? Use an impossible WBS key to result in no matches
                key = -999;
            }
            return new DoubleData(key, false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while calculating", e);
            return null;
        }
    }

    private static final String NAME_QUERY = "select w.key "
            + "from WbsElement w where w.name = ?";
    private static final String ID_QUERY = "select p.wbsElement.key "
            + "from PlanItem p where p.identifier = ?";

}
