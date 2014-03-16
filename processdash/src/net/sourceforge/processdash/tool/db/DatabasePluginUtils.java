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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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


    /**
     * Return the identifier that would be used in the database for the Phase
     * object with a given workflow ID in a particular team project.
     */
    public static String getWorkflowPhaseIdentifier(String projectID,
            String workflowSourceId) {
        return "WF:" + projectID + ":" + workflowSourceId;
    }


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


    public static Map<Integer, String> getDashPathsForPlanItems(
            QueryRunner query, Collection planItemKeys) {

        // retrieve path information about the given plan items
        List rawData = query.queryHql(PATH_LOOKUP_QUERY, planItemKeys);
        Map<Integer, String> result = new HashMap<Integer, String>();
        for (Iterator i = rawData.iterator(); i.hasNext();) {
            Object[] row = (Object[]) i.next();
            Integer key = (Integer) row[0];
            String projectName = (String) row[1];
            int wbsElementLen = ((Number) row[2]).intValue();
            String wbsElementName = (String) row[3];
            String taskName = (String) row[4];

            // construct the full path of this task
            StringBuilder path = new StringBuilder();
            if (!projectName.startsWith("/"))
                path.append("/Project/");
            path.append(projectName);
            if (wbsElementLen > 0)
                path.append("/").append(wbsElementName);
            if (taskName != null)
                path.append("/").append(taskName);
            result.put(key, path.toString());
        }
        return result;
    }

    private static final String PATH_LOOKUP_QUERY = "select p.key, " //
            + "p.project.name, p.wbsElement.nameLength, " //
            + "p.wbsElement.name, task.name " //
            + "from PlanItem p " //
            + "left join p.task task " //
            + "where p.key in (?)";

}
