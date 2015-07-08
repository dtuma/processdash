// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import teamdash.wbs.ConflictCapableDataColumn;

public class BlameValueList extends TreeMap<BlamePoint, String> {

    private ConflictCapableDataColumn column;

    public BlameValueList(String initialValue) {
        put(BlamePoint.INITIAL, initialValue);
    }

    public ConflictCapableDataColumn getColumn() {
        return column;
    }

    public void setColumn(ConflictCapableDataColumn column) {
        this.column = column;
    }

    public boolean columnMatches(String columnID) {
        return column != null && columnID.equals(column.getColumnID());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        Iterator<Entry<BlamePoint, String>> i = entrySet().iterator();
        result.append(i.next().getValue());
        while (i.hasNext()) {
            Entry<BlamePoint, String> e = i.next();
            result.append(" ==> ").append(e.getKey()) //
                    .append(" ==> ").append(e.getValue());
        }
        return result.toString();
    }

}
