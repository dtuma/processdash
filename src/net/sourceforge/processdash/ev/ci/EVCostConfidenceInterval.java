//Copyright (C) 2003-2008 Tuma Solutions, LLC
//Process Dashboard - Data Automation Tool for high-maturity processes
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 3
//of the License, or (at your option) any later version.
//
//Additional permissions also apply; see the README-license.txt
//file in the project root directory for more information.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, see <http://www.gnu.org/licenses/>.
//
//The author(s) may be contacted at:
//    processdash@tuma-solutions.com
//    processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.ci.AbstractConfidenceInterval.DataPoint;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

import cern.jet.random.engine.RandomEngine;



public class EVCostConfidenceInterval extends DelegatingConfidenceInterval
        implements XMLPersistableConfidenceInterval {

    private static final int LINEAR_RATIO = 1;

    private static final int LOG_CENTERED_RATIO = 2;


    public EVCostConfidenceInterval(List tasks) {
        this(tasks, LINEAR_RATIO);
    }

    public EVCostConfidenceInterval(Object[] taskLists) {
        this(getTasksForTaskLists(taskLists), LOG_CENTERED_RATIO);
    }

    public EVCostConfidenceInterval(List tasks, int method) {
        if (tasks == null || tasks.size() == 0) {
            delegate = null;
            return;
        }

        AbstractConfidenceInterval interval = null;
        switch (method) {
        case LINEAR_RATIO:
            interval = new LinearRatioConfidenceInterval();
            break;

        case LOG_CENTERED_RATIO:
            interval = new LogCenteredConfidenceInterval();
            break;

        default:
            throw new IllegalArgumentException(
                    "Unrecognized cost interval method");
        }
        delegate = interval;

        Iterator i = tasks.iterator();
        while (i.hasNext()) {
            Object item = i.next();
            if (item instanceof EVTask) {
                EVTask task = (EVTask) item;
                if (task.getDateCompleted() != null)
                    interval.addDataPoint(task.getPlanValue(),
                        task.getActualDirectTime());

            } else if (item instanceof DataPoint) {
                DataPoint point = (DataPoint) item;
                interval.addDataPoint(point.x, point.y);
            }
        }
        interval.dataPointsComplete();
    }


    public EVCostConfidenceInterval(Element xml) {
        delegate = new LinearRatioConfidenceInterval(xml);
    }


    @Override
    public double getRandomValue(RandomEngine u) {
        double result;
        for (int trialCount = 20;  trialCount-- > 0; ) {
            result = super.getRandomValue(u);
            if (result >= 0 || Double.isNaN(result))
                return result;
        }
        return 0;
    }


    private static List getTasksForTaskLists(Object[] taskLists) {
        Map<String, EVTaskList> uniqueLists = new HashMap<String, EVTaskList>();
        getUniqueTaskLists(taskLists, uniqueLists);

        ArrayList result = new ArrayList();
        for (EVTaskList taskList : uniqueLists.values()) {
            List<EVTask> evLeaves = taskList.getFilteredLeaves(null);
            result.addAll(evLeaves);
        }
        return result;
    }

    private static void getUniqueTaskLists(Object[] taskLists,
            Map<String, EVTaskList> results) {
        for (int i = 0; i < taskLists.length; i++) {
            EVTaskList taskList = (EVTaskList) taskLists[i];

            if (taskList instanceof EVTaskListRollup) {
                EVTaskListRollup rollup = (EVTaskListRollup) taskList;
                List subTaskLists = rollup.getSubSchedules();
                getUniqueTaskLists(subTaskLists.toArray(), results);

            } else {
                String key = taskList.getID();
                if (!StringUtils.hasValue(key))
                    key = taskList.getTaskListName();
                results.put(key, taskList);
            }
        }

    }
}
