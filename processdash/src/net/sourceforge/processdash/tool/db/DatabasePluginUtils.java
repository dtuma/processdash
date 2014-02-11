// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabasePluginUtils {

    public static String getTaskIdFromPlanItemId(String planItemId) {
        int pos = planItemId.indexOf(':');
        pos = planItemId.indexOf(':', pos + 1);
        if (pos == -1)
            return planItemId;

        Matcher m = DATABASE_TASK_ID_PAT.matcher(planItemId);
        if (!m.matches())
            return planItemId;
        if (m.group(4) == null)
            return m.group(1);
        else
            return m.group(1) + "/" + m.group(4);

    }

    private static final Pattern DATABASE_TASK_ID_PAT = Pattern
            .compile("(\\w+:\\w+)(:(\\d+/)?(.+))?");

    public static int getKeyForDate(Date d) {
        return getKeyForDate(d, 0);
    }

    public static int getKeyForDate(Date d, int adjustment) {
        if (adjustment != 0)
            d = new Date(d.getTime() + adjustment);

        String fmt = DATABASE_DATE_FMT.format(d);
        return Integer.parseInt(fmt);
    }

    private static DateFormat DATABASE_DATE_FMT = new SimpleDateFormat(
            "yyyyMMdd");

}
