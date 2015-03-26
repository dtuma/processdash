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

package net.sourceforge.processdash.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sourceforge.processdash.util.StringUtils;


/** @since 1.15.5 */
public class ResultSetData implements SimpleData {

    private String[] columnNames;

    private List resultSet;

    public ResultSetData(List resultSet, String[] columnNames) {
        this.columnNames = columnNames;
        this.resultSet = resultSet;
    }

    public List getData() {
        return resultSet;
    }

    public boolean test() {
        return resultSet != null && !resultSet.isEmpty();
    }

    public boolean isDefined() {
        return resultSet != null;
    }

    public boolean equals(SimpleData val) {
        return this == val;
    }

    public int getColumnPos(String colName) {
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equalsIgnoreCase(colName))
                return i;
        }
        return -1;
    }

    public String format() {
        if (resultSet == null)
            return "";

        StringBuilder result = new StringBuilder();
        result.append("(")
                .append(StringUtils.join(Arrays.asList(columnNames), ", "))
                .append(")\n");

        for (Object oneResultSetItem : resultSet) {
            format(result, oneResultSetItem);
            result.append("\n");
        }
        return result.toString();
    }

    private void format(StringBuilder result, Object obj) {
        if (obj instanceof Object[]) {
            format(result, Arrays.asList((Object[]) obj));
        } else if (obj instanceof Collection) {
            format(result, (Collection) obj);
        } else {
            result.append(obj);
        }
    }

    private void format(StringBuilder result, Collection c) {
        result.append("[");
        boolean needComma = false;
        for (Object item : c) {
            if (needComma)
                result.append(", ");
            format(result, item);
            needComma = true;
        }
        result.append("]");
    }

    public String saveString() {
        return StringUtils.findAndReplace(format(), "\n", "\\n");
    }

    public void dispose() {
        resultSet = null;
    }


    // No-op implementations of most SimpleData methods

    public boolean isEditable() {
        return false;
    }

    public void setEditable(boolean e) {}

    public SaveableData getEditable(boolean e) {
        return this;
    }

    public void setDefined(boolean d) {}

    public SimpleData getSimpleValue() {
        return this;
    }

    public SimpleData parse(String val) {
        return null;
    }

    public boolean lessThan(SimpleData val) {
        return false;
    }

    public boolean greaterThan(SimpleData val) {
        return false;
    }

}
