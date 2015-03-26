// Copyright (C) 2008-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;

public class MemberChartNameHelper extends HashMap<EVTaskList, String> {

    public MemberChartNameHelper(EVTaskListRollup rollup) {
        Set<String> ambiguousNames = getRepeatedPersonNames(rollup);
        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            String seriesName = getSeriesName(tl, ambiguousNames);
            put(tl, seriesName);
        }
    }

    private String getSeriesName(EVTaskList tl, Set<String> namesToAvoid) {
        String name = tl.getDisplayName();
        String personName = extractPersonName(name);
        if (personName != null && !namesToAvoid.contains(personName))
            return personName;
        else
            return name;
    }

    private Set<String> getRepeatedPersonNames(EVTaskListRollup rollup) {
        Set<String> namesSeen = new HashSet<String>();
        Set<String> repeatedNames = new HashSet<String>();
        for (int i = 0; i < rollup.getSubScheduleCount(); i++) {
            EVTaskList tl = rollup.getSubSchedule(i);
            String personName = extractPersonName(tl.getDisplayName());
            if (namesSeen.contains(personName))
                repeatedNames.add(personName);
            else
                namesSeen.add(personName);
        }
        return repeatedNames;
    }

    private String extractPersonName(String taskListName) {
        if (!taskListName.endsWith(")"))
            return null;

        int parenPos = taskListName.lastIndexOf('(');
        if (parenPos == -1)
            return null;

        return taskListName.substring(parenPos + 1, taskListName.length() - 1);
    }

}
