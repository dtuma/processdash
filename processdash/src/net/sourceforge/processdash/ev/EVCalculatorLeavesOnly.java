// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev;

import java.util.LinkedList;


/** This calculator is designed to recalc an EVTaskListData when the client
 * is only interested in the list of earned value leaves.
 */
public class EVCalculatorLeavesOnly extends EVCalculator {

    private EVTask taskRoot;

    public EVCalculatorLeavesOnly(EVTask root) {
        this.taskRoot = root;
    }

    public void recalculate() {
        reorderCompletedTasks = true;

        pruneNodes(taskRoot, false);
        taskRoot.recalcPlanTimes();
        resetData(taskRoot);
        evLeaves = new LinkedList();
        getEVLeaves(taskRoot);
        if (containsTaskOrdinals(taskRoot))
            assignTaskOrdinals(taskRoot, 1);
        sortEVLeafList(evLeaves);
    }

    private boolean containsTaskOrdinals(EVTask task) {
        if (task.taskOrdinal > 0) return true;
        for (int i = 0;   i < task.getNumChildren();   i++)
            if (containsTaskOrdinals(task.getChild(i)))
                return true;

        return false;
    }

    private int assignTaskOrdinals(EVTask task, int defaultOrdinal) {

        if (task.isLevelOfEffortTask())
            return defaultOrdinal;

        if (task.taskOrdinal != EVTask.INFER_FROM_CONTEXT)
            defaultOrdinal = task.taskOrdinal;
        else if (EVTask.containsNode(evLeaves, task))
            task.taskOrdinal = defaultOrdinal;

        for (int i = 0;   i < task.getNumChildren();   i++)
            defaultOrdinal = assignTaskOrdinals
                (task.getChild(i), defaultOrdinal);

        return defaultOrdinal;
    }

    private void resetData(EVTask task) {
        task.actualDirectTime = task.planValue = 1;
        if (task.isEVLeaf())
            task.resetActualDate();
        else
            for (int i = 0;   i < task.getNumChildren();   i++)
                resetData(task.getChild(i));
    }

}
