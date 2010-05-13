// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.clipboard;

import java.util.ArrayList;
import java.util.List;

public class TabularDataColumn {

    private int pos;

    private String name;

    private String display;

    public TabularDataColumn(int pos, String name) {
        this.pos = pos;

        if (name == null)
            name = "";
        name = name.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        name = name.trim();
        this.name = name;

        if (name.length() > MAX_DISPLAY_LEN)
            display = name.substring(0, MAX_DISPLAY_LEN - 3) + "...";
        else
            display = name;
    }

    public int getPos() {
        return pos;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TabularDataColumn) {
            TabularDataColumn that = (TabularDataColumn) obj;
            return this.name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return display;
    }

    private static final int MAX_DISPLAY_LEN = 20;

    public static final TabularDataColumn NONE_SELECTED = new TabularDataColumn(
            -1, ClipboardDefectData.resources.getString("No_Column_Selected"));

    public static List<TabularDataColumn> buildColumns(List<String> names) {
        List<TabularDataColumn> result = new ArrayList<TabularDataColumn>();
        for (int i = 0; i < names.size(); i++) {
            result.add(new TabularDataColumn(i, names.get(i)));
        }
        return result;
    }

}
