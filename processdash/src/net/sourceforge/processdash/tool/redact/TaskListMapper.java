// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.StringMapper;

public class TaskListMapper implements StringMapper {

    /** Mapping from local task list names to task list IDs */
    private Map<String, String> taskListIDs;

    public TaskListMapper(RedactFilterData data) throws IOException {
        taskListIDs = new HashMap<String, String>();

        BufferedReader in = data.getFile("global.dat");
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = TASK_LIST_ID_DECL_PAT.matcher(line);
            if (m.matches())
                taskListIDs.put(m.group(1).replace(EQ_REPL, '='), m.group(2));
        }
    }

    private static final Pattern TASK_LIST_ID_DECL_PAT = Pattern
            .compile("Task-Schedule/(.*)/Task List ID=\"(.*)");

    public String getString(String str) {
        return hashTaskListName(str);
    }

    public String hashTaskListName(String taskListName) {
        if (taskListName == null || taskListName.trim().length() == 0)
            return taskListName;

        Matcher m = XML_TASK_LIST_NAME_PAT.matcher(taskListName);
        if (m.matches())
            return m.group(1) + m.group(2) + hashTaskListId(m.group(1));

        String hashData = taskListIDs.get(taskListName);
        if (hashData == null)
            hashData = taskListName;
        return hashTaskListId(hashData);
    }

    public String hashTaskListId(String id) {
        return "Task List " + RedactFilterUtils.hash(id);
    }

    private static final Pattern XML_TASK_LIST_NAME_PAT = Pattern
            .compile("(.*)(#XMLID/.*/)([^/]+)");

    private static final char EQ_REPL = DataRepository.EQUALS_SIGN_REPL;

}
