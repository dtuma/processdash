// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class EVCalculatorXML extends EVCalculator {


    private EVTask taskRoot;
    private EVSchedule schedule;


    public EVCalculatorXML(EVTask root, EVSchedule schedule,
                           boolean reorderCompletedTasks) {
        this.taskRoot = root;
        this.schedule = schedule;
        this.reorderCompletedTasks = reorderCompletedTasks;
    }


    public void recalculate() {
        // mark ANCESTOR_PRUNED nodes appropriately
        pruneNodes(taskRoot, false);

        // calculate top-down and bottom-up plan times
        taskRoot.recalcPlanTimes();

        // create and sort the list of EV leaves
        scheduleStartDate = schedule.getStartDate();
        evLeaves = new LinkedList();
        getEVLeaves(taskRoot);
        sortEVLeafList(evLeaves);

        // calculate cumulative plan value and value earned
        calcTaskValues(evLeaves);
        recalcValueEarned(taskRoot);

        // check for errors in the task list
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                                    new LinkedList(), new LinkedList());
    }


    private double calcTaskValues(List evLeaves) {
        double cumPlanValue = 0;
        Date startDate = scheduleStartDate;
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();

            cumPlanValue += task.planValue;
            task.cumPlanValue = cumPlanValue;

            if (task.dateCompleted != null)
                task.valueEarned = task.planValue;

        }
        return cumPlanValue;
    }


    private void recalcValueEarned(EVTask task) {
        if (!task.isLeaf() && !task.isEVLeaf()) {
            task.valueEarned = 0;
            // for nonleaves, ask each of our children to recalc.
            for (int i = 0;   i < task.getNumChildren();   i++) {
                EVTask child = task.getChild(i);
                recalcValueEarned(child);
                task.valueEarned += child.valueEarned;
            }
        }
    }

}
