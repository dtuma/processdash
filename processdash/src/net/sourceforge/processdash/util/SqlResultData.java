// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SqlResultData extends ArrayList {

    private String[] columnNames;

    public SqlResultData(ResultSet rs) throws SQLException {
        columnNames = getColumnNames(rs);
        while (rs.next())
            addRow(rs, columnNames);
    }

    private String[] getColumnNames(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        String[] result = new String[columnCount];
        for (int i = 0;  i < columnCount;  i++)
            result[i] = metaData.getColumnName(i+1);
        return result;
    }

    private void addRow(ResultSet rs, String[] columnNames) throws SQLException {
        Map row = new HashMap();
        for (int i = 0;  i < columnNames.length;  i++) {
            Object value = rs.getObject(i+1);
            row.put(columnNames[i], value);
            row.put(columnNames[i].toUpperCase(), value);
            row.put(columnNames[i].toLowerCase(), value);
        }
        add(row);
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public boolean isSingleValue() {
        return size() == 1 && columnNames.length == 1;
    }

    public Object getSingleValue() {
        if (isSingleValue()) {
            Map m = (Map) get(0);
            return m.get(columnNames[0]);
        } else {
            return null;
        }
    }

}
