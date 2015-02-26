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

package net.sourceforge.processdash.ev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class EVCalculatorLeavesOnlyRollup extends EVCalculator {

    private List<EVTaskListData> taskLists;

    public EVCalculatorLeavesOnlyRollup(EVTaskListRollup rollup) {
        taskLists = new ArrayList<EVTaskListData>();
        gatherPlainTaskLists(rollup);
    }

    private void gatherPlainTaskLists(EVTaskListRollup rollup) {
        for (EVTaskList child : rollup.getSubSchedules()) {
            child.useFastRecalcInterval();
            if (child instanceof EVTaskListData) {
                EVTaskListData plainTaskList = (EVTaskListData) child;
                ((EVCalculatorData) plainTaskList.calculator)
                        .setDisableConfidenceIntervals(true);
                taskLists.add(plainTaskList);

            } else if (child instanceof EVTaskListRollup) {
                gatherPlainTaskLists((EVTaskListRollup) child);
            }
        }
    }

    public void recalculate() {
        evLeaves = new ArrayList();
        for (EVTaskListData plainTaskList : taskLists) {
            EVCalculatorLeavesOnly.resetData(plainTaskList.getTaskRoot());
            plainTaskList.recalc();
            evLeaves.addAll(plainTaskList.calculator.getEVLeaves());
        }
        Collections.sort(evLeaves, LEAF_SORTER);
    }

    private static class EVLeafComparator implements Comparator<EVTask> {

        public int compare(EVTask t1, EVTask t2) {
            Date d1 = t1.getReplanDate();
            Date d2 = t2.getReplanDate();
            if (d1 == d2) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        }

    }

    private static final EVLeafComparator LEAF_SORTER = new EVLeafComparator();

}
