//PSP Dashboard - Data Automation Tool for PSP-like processes
//Copyright (C) 1999  United States Air Force
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//The author(s) may be contacted at:
//OO-ALC/TISHD
//Attn: PSP Dashboard Group
//6137 Wardleigh Road
//Hill AFB, UT 84056-5843
//
//E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.util.Date;

public abstract class EVCalculator {

    /*
    public EVCalculator(String taskListName, EVTask root, EVSchedule schedule) {

    }
    */

    public abstract void recalculate();


    protected void resetRootData(EVTask taskRoot) {
        taskRoot.topDownPlanTime = taskRoot.bottomUpPlanTime = 0;
        taskRoot.dateCompleted = null;
        taskRoot.dateCompletedEditable = false;
        taskRoot.taskOrdinal = 1;
    }

    protected void resetNodeData(EVTask task) {
        task.planDate = task.planStartDate = task.actualStartDate = null;

        task.planTime = task.planValue = task.cumPlanValue =
            task.actualPreTime = task.actualNodeTime = task.actualDirectTime =
            task.actualTime = task.actualCurrentTime = task.valueEarned = 0;
    }


    protected void sumUpNodeData(EVTask task) {
        if (!task.isLevelOfEffortTask() && !task.isUserPruned())
            task.actualDirectTime = task.actualNodeTime;

        task.actualTime = task.actualNodeTime + task.actualPreTime;
        task.actualCurrentTime = task.actualNodeTime;

        for (int i = task.getNumChildren();   i-- > 0;   ) {
            EVTask child = task.getChild(i);

            task.planValue += child.planValue;
            task.cumPlanValue =
                Math.max(task.cumPlanValue, child.cumPlanValue);

            task.actualTime        += child.actualTime;
            task.actualCurrentTime += child.actualCurrentTime;
            task.actualDirectTime  += child.actualDirectTime;
            task.valueEarned       += child.valueEarned;

            task.planStartDate =
                minStartDate(task.planStartDate, child.planStartDate);
            task.actualStartDate =
                minStartDate(task.actualStartDate, child.actualStartDate);

            task.planDate = maxPlanDate(task.planDate, child.planDate);
        }
    }


    protected Date minStartDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) < 0) return a;
        return b;
    }


    protected Date maxPlanDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) > 0) return a;
        return b;
    }

}
