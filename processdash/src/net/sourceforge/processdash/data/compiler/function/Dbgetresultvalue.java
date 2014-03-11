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

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ResultSetData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.NullSafeObjectUtils;


/** @since 1.15.5 */
public class Dbgetresultvalue extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Expected arguments: (ResultSet, String... keys, String targetColumn)
     * 
     * Each "key" should be of the form "ColumnName=Value", identifying the name
     * of a column in the result set, and the value we are searching for.
     * 
     * The targetColumn should be the name of the column we want to select when
     * we find the matching row. The targetColumn can optionally be surrounded
     * by "sum()", which will instruct this method to find all of the matching
     * rows and add their values together.
     */
    public Object call(List arguments, ExpressionContext context) {
        SimpleData arg0 = getArg(arguments, 0);
        if (!(arg0 instanceof ResultSetData))
            return null;

        ResultSetData rs = (ResultSetData) arg0;
        if (!rs.test())
            return null;

        List toFind = collapseLists(arguments, 1);
        if (toFind.isEmpty())
            return null;

        boolean sum = false;
        String targetColName = asStringVal(toFind.remove(toFind.size() - 1));
        if (targetColName.toLowerCase().startsWith("sum(")
                && targetColName.endsWith(")")) {
            sum = true;
            targetColName = targetColName.substring(4,
                targetColName.length() - 1);
        }

        int targetCol = rs.getColumnPos(targetColName);
        if (targetCol == -1)
            return null;

        int[] findColumns = new int[toFind.size()];
        String[] findValues = new String[toFind.size()];
        for (int i = 0; i < findColumns.length; i++) {
            String findItem = asString(toFind.get(i));
            int eqPos = findItem.indexOf('=');
            if (eqPos == -1)
                return null;

            String findColumnName = findItem.substring(0, eqPos);
            findColumns[i] = rs.getColumnPos(findColumnName);
            if (findColumns[i] == -1)
                return null;

            findValues[i] = findItem.substring(eqPos + 1);
        }

        double sumResult = 0;
        List<Object[]> rawResultData = rs.getData();
        for (Object[] oneRow : rawResultData) {
            if (matches(oneRow, findColumns, findValues)) {
                if (targetCol >= oneRow.length) {
                    return null;
                } else if (sum) {
                    Object oneVal = oneRow[targetCol];
                    if (oneVal instanceof Number) {
                        sumResult += ((Number) oneVal).doubleValue();
                    } else if (oneVal != null) {
                        return ImmutableDoubleData.BAD_VALUE;
                    }
                } else {
                    return toSimpleData(oneRow[targetCol]);
                }
            }
        }

        if (sum)
            return new DoubleData(sumResult, false);
        else
            return null;
    }

    private boolean matches(Object[] row, int[] columns, String[] values) {
        for (int i = 0; i < columns.length; i++) {
            int col = columns[i];
            String val = values[i];

            if (col >= row.length)
                return false;

            String rowVal = asString(row[col]);
            if (!NullSafeObjectUtils.EQ(val, rowVal))
                return false;
        }

        return true;
    }

}
