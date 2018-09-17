// Copyright (C) 2018 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Workflowqualitycalc extends Dbgetresultvalue {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (String valueKey)
     * 
     * valueKey should be of the form "numericPhaseID//columnName".
     * 
     * This method will retrieve the precomputed quality plan, which should be
     * stored in a data element called [Quality Plan]. Then it will look up the
     * given phase, and return the value from the named column.
     */
    public Object call(List arguments, ExpressionContext context) {
        Object result = null;
        try {
            // build a list of args for the parent call. Get the result set from
            // the precomputed quality plan, and pass it as the first arg
            List subargs = new ArrayList();
            subargs.add(context.get(Workflowqualityplan.DATA_NAME));

            // get the valueKey and reformat it into args for the parent
            String valueKey = asStringVal(getArg(arguments, 0));
            String[] keyParts = valueKey.split("//");
            subargs.add(asSimpleData("id=" + keyParts[0]));
            subargs.add(asSimpleData(keyParts[1]));

            // call the parent and get the result
            result = super.call(subargs, context);
        } catch (Exception e) {
        }

        if (result != null)
            return result;
        else
            return ImmutableDoubleData.READ_ONLY_NAN;
    }

}
