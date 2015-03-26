// Copyright (C) 2005-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.PatternList;

public class TaskListDataWatcher extends IteratorFilter {

    private Set taskListNames;

    public TaskListDataWatcher(Iterator parent) {
        super(parent);
        taskListNames = new HashSet();
        init();
    }

    public Set getTaskListNames() {
        return taskListNames;
    }

    protected boolean includeInResults(Object o) {
        ExportedDataValue val = (ExportedDataValue) o;
        String dataName = val.getName();

        int pos = dataName.indexOf(TASK_ORD_PREF);
        if (pos != -1)
            taskListNames.add(dataName.substring(pos + TASK_ORD_PREF.length()));

        pos = dataName.indexOf(RELATED_SCHEDULE_DATA_PREFIX);
        if (pos != -1)
            addTaskListNames(val);

        return true;
    }

    private void addTaskListNames(ExportedDataValue val) {
        SimpleData d = val.getSimpleValue();
        if (!d.test())
            return;

        ListData list = null;
        if (d instanceof ListData)
            list = (ListData) d;
        else if (d instanceof StringData)
            list = ((StringData) d).asList();

        if (list != null)
            for (int i = 0;  i < list.size();  i++)
                taskListNames.add(list.get(i));
    }

    private static String TASK_ORD_PREF = "/"
            + EVTaskListData.TASK_ORDINAL_PREFIX;
    private static String RELATED_SCHEDULE_DATA_PREFIX = "/Related_EV_Schedule";
    static final PatternList PATTERNS_OF_INTEREST = new PatternList()
            .addRegexp(TASK_ORD_PREF.substring(1))
            .addRegexp(RELATED_SCHEDULE_DATA_PREFIX.substring(1));

}
