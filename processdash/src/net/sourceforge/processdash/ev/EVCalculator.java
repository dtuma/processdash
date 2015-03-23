//Copyright (C) 2003-2013 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalProvider;
import net.sourceforge.processdash.ev.ci.EVConfidenceIntervalUtils;
import net.sourceforge.processdash.ev.ci.LogCenteredConfidenceInterval;
import net.sourceforge.processdash.util.StringUtils;


public abstract class EVCalculator {

    public static final String FIXED_EFFECTIVE_DATE_SETTING = "ev.effectiveDate";

    protected List evLeaves;
    protected Date scheduleStartDate;
    protected ConfidenceIntervalProvider costIntervalProvider;
    protected ConfidenceIntervalProvider timeErrIntervalProvider;
    protected EVSnapshot baselineDataSource;
    protected boolean reorderCompletedTasks = true;
    protected Properties metadata;


    public abstract void recalculate();


    public List getEVLeaves() {
        return evLeaves;
    }

    protected void sortEVLeafList(List<EVTask> evLeaves) {
        Collections.sort(evLeaves, new EVLeafComparator(evLeaves));
        insertTasksWithinPspProcess(evLeaves);
    }

    private class EVLeafComparator implements Comparator {
        private Map<Object, Integer> origOrder;
        private EVLeafComparator(List<EVTask> origList) {
            origOrder = new IdentityHashMap();
            int pos = 0;
            for (EVTask t : origList)
                origOrder.put(t, pos++);
        }
        public int compare(Object o1, Object o2) {
            EVTask t1 = (EVTask) o1;
            EVTask t2 = (EVTask) o2;

            // put completed tasks at the front of the list, in order of
            // completion
            int result = compareDates(t1.dateCompleted, t2.dateCompleted);
            if (result != 0) return result;

            // next, order by task ordinal
            result = t1.taskOrdinal - t2.taskOrdinal;
            if (result != 0) return result;

            // finally, return items in the order they appeared in the
            // original list.
            result = getOrigPos(t1) - getOrigPos(t2);
            return result;
        }
        private int compareDates(Date a, Date b) {
            if (!reorderCompletedTasks) {
                if (a != null && a.compareTo(scheduleStartDate) > 0) a = null;
                if (b != null && b.compareTo(scheduleStartDate) > 0) b = null;
            }
            if (a == b) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.compareTo(b);
        }
        private int getOrigPos(Object obj) {
            Integer pos = origOrder.get(obj);
            return (pos == null ? -1 : pos.intValue());
        }
        public boolean equals(Object obj) {
            return this == obj;
        }
        public int hashCode() {
            return super.hashCode();
        }
    }

    protected void insertTasksWithinPspProcess(List<EVTask> evLeaves) {
        List<EVTask> pspPostmortemTasks = findPspPostmortemPhaseTasks(evLeaves);
        for (EVTask onePostmortemTask : pspPostmortemTasks)
            maybeReorderTasksAfterPostmortemTask(evLeaves, onePostmortemTask);
    }

    private List<EVTask> findPspPostmortemPhaseTasks(List<EVTask> evLeaves) {
        List<EVTask> result = new LinkedList<EVTask>();
        for (EVTask oneLeaf : evLeaves) {
            if (isPspPostmortemPhase(oneLeaf))
                result.add(oneLeaf);
        }
        return result;
    }

    private boolean isPspPostmortemPhase(EVTask task) {
        return (task.isLeaf() //
                && "Postmortem".equals(task.getName()) //
                && task.isNodeTypeImplicit() //
                && "Postmortem".equals(task.getNodeType()) //

                // Look to see if the parent task contains other PSP phases.
                // But do not check for the review phases, since this could be
                // a PSP0-PSP1.1 task. And do not check for Postmortem, because
                // we already know that it is present.
                && taskContainsChildren(task.getParent(), "Planning",
                        "Design", "Code", "Compile", "Test"));
    }

    private boolean taskContainsChildren(EVTask parentTask,
            String... childNames) {
        if (parentTask == null)
            return false;
        for (String oneChildName : childNames) {
            if (!taskContainsChildNamed(parentTask, oneChildName))
                return false;
        }
        return true;
    }

    private boolean taskContainsChildNamed(EVTask parentTask, String childName) {
        for (int i = parentTask.getNumChildren();  i-- > 0; ) {
            if (childName.equals(parentTask.getChild(i).getName()))
                return true;
        }
        return false;
    }

    private void maybeReorderTasksAfterPostmortemTask(List<EVTask> evLeaves,
            EVTask onePmTask) {
        // find this task within the ev leaf list.
        int pmTaskPos = evLeaves.indexOf(onePmTask);
        if (pmTaskPos == -1)
            return;

        // identify the EVTask which is the parent of the PSP task which
        // contains this postmortem phase.
        EVTask pspTask = onePmTask.getParent();
        if (pspTask == null)
            return;
        EVTask parentOfPspTask = pspTask.getParent();
        if (parentOfPspTask == null)
            return;
        int pspChildPos = parentOfPspTask.getChildIndex(pspTask);
        if (pspChildPos == -1)
            return;

        // get a list of the official, acceptable phases in the controlling
        // process definition.
        ListData phaseListSpec = onePmTask.getAcceptableNodeTypes();
        if (phaseListSpec == null || !phaseListSpec.test())
            return;
        List phaseList = phaseListSpec.asList();
        int postmortemPos = phaseList.indexOf("Postmortem");
        if (postmortemPos == -1)
            return;

        // find the tasks that immediately follow this postmortem task which
        // are reordering candidates, and move them if applicable.
        int planTaskOrdinal = pspTask.getChild(0).taskOrdinal;
        int pmTaskOrdinal = onePmTask.taskOrdinal;
        int lastTaskOrdinal = pmTaskOrdinal;
        EVTask lastReorderedTask = null;
        for (int i = pspChildPos; ++i < parentOfPspTask.getNumChildren(); ) {
            // Examine each of the siblings that follows the PSP task.
            EVTask oneSubsequentTask = parentOfPspTask.getChild(i);
            int oneSubsequentTaskPos = evLeaves.indexOf(oneSubsequentTask);

            // we only reorder leaf tasks.  If we find a child that is not a
            // leaf task, break out of the reordering loop.
            if (oneSubsequentTaskPos == -1)
                break;

            // if we find a task whose ordinal precedes the planning phase or
            // follows the postmortem phase, we can be certain that the user
            // has reordered that task at some time in the past.  In that case,
            // stop searching for reorderable tasks.
            if (oneSubsequentTask.taskOrdinal < planTaskOrdinal
                    || oneSubsequentTask.taskOrdinal > pmTaskOrdinal)
                break;

            // tasks are only reorderable if they inherited their ordinal from
            // the task that precedes them in the hierarchy.  If their ordinal
            // differs, that implies that they were moved by the user at some
            // time in the past. If so, don't try moving this task; but still
            // keep looking at subsequent tasks to see if they need to be moved.
            if (oneSubsequentTask.taskOrdinal == lastTaskOrdinal) {

                // find the nominal position in the leaf list where this task
                // should be inserted, based on its type.
                int insertionPos = getInsertionPosWithinPspPhases(evLeaves,
                    oneSubsequentTask, pmTaskPos, phaseList,
                    lastReorderedTask);

                // when the insertion pos routine returns -1, this is a flag
                // that we should stop searching for reorderable tasks.
                if (insertionPos == -1)
                    break;

                // If the task needs to be reordered, do it.
                if (insertionPos != oneSubsequentTaskPos
                        && insertionPos != oneSubsequentTaskPos + 1) {
                    evLeaves.remove(oneSubsequentTaskPos);
                    if (insertionPos > oneSubsequentTaskPos)
                        insertionPos--;
                    evLeaves.add(insertionPos, oneSubsequentTask);
                    pmTaskPos = evLeaves.indexOf(onePmTask);
                }
            }

            // Keep track of the last task and ordinal we've seen.
            lastReorderedTask = oneSubsequentTask;
            lastTaskOrdinal = oneSubsequentTask.taskOrdinal;
        }
    }

    private int getInsertionPosWithinPspPhases(List<EVTask> evLeaves,
            EVTask taskToReorder, int pmTaskPos, List phaseList,
            EVTask lastReorderedTask) {
        // get the type of this task, and find that type's numeric position
        // within the official list of phases.
        String taskType = taskToReorder.getNodeType();
        int taskTypePos = phaseList.indexOf(taskType);
        if (taskTypePos == -1)
            return -1; // unrecognized phase type?  Abort.

        // retrieve the numeric position of certain critical PSP phases.
        int designPos = phaseList.indexOf("Design");
        int testPos = phaseList.indexOf("Test");

        // We DO NOT want to sort PSP Postmortem phases at the very end of the
        // task list, after things like integration and system test.  To avoid
        // this, we check to see if the type of the reorderable task follows
        // the PSP unit test phase.  If so, do not reorder it.  (Note that we
        // WILL consider reordering if it is a PSP unit test phase; this
        // allows people to define several types of unit testing that they wish
        // to perform.)
        if (taskTypePos > testPos)
            return -1;

        // walk backward in the flat view for tasks preceding this task.
        int insertAfterPos = pmTaskPos - 1;
        while (insertAfterPos > 0) {
            // examine a task in the task list to see if it should be a
            // predecessor of the potentially reorderable task.
            EVTask predecessorCandidate = evLeaves.get(insertAfterPos);

            // reorderable tasks should not be reordered relative to each
            // other.  if we encounter the previous task that was reordered,
            // stop and insert this new task after it.
            if (predecessorCandidate == lastReorderedTask)
                return insertAfterPos + 1;

            // if this predecessor has a type that precedes the type of
            // the reorderable task in question, we should insert the
            // reorderable task after this predecessor.
            String predecessorType = predecessorCandidate.getNodeType();
            int predecessorTypePos = phaseList.indexOf(predecessorType);
            if (predecessorTypePos <= taskTypePos)
                return insertAfterPos + 1;

            // The PSP "Design" phase is immediately preceded by the "Planning"
            // phase.  Tasks never need to be moved before the start of the PSP
            // process itself; so the earliest that we will ever insert a task
            // is immediately before "Design".  If our target predecessor
            // already precedes "Design", we should stop our backward search.
            if (predecessorTypePos < designPos)
                return insertAfterPos + 1;

            // It doesn't appear that we've found the insertion pos yet.
            // Move backward in the list and keep searching.
            insertAfterPos--;
        }

        // if our PSP task was the first item in a task list, the loop above
        // could terminate just before reaching the initial planning phase.  In
        // that case, we want to insert our task just after planning.
        return 1;
    }


    public void debugEVLeafList(List evLeaves) {
        System.out.println("EV Leaves:");
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask element = (EVTask) i.next();
            System.out.println(element.fullName);
        }
    }

    protected static void resetRootData(EVTask taskRoot) {
        taskRoot.topDownPlanTime = taskRoot.bottomUpPlanTime = 0;
        taskRoot.dateCompleted = null;
        taskRoot.dateCompletedEditable = false;
        //taskRoot.taskOrdinal = 1;
    }

    protected static void resetNodeData(EVTask task) {
        task.planDate = task.planStartDate = task.actualStartDate =
            task.replanStartDate = task.forecastStartDate =
            task.replanDate = task.forecastDate = null;

        task.overspentPlanDates = task.overspentReplanDates =
            task.overspentForecastDates = null;

        task.planTime = task.planValue = task.cumPlanValue =
            task.actualPreTime = task.actualNodeTime = task.actualDirectTime =
            task.actualTime = task.actualCurrentTime = task.valueEarned = 0;
    }


    protected static void sumUpNodeData(EVTask task) {
        if (!task.isLevelOfEffortTask() && !task.isUserPruned())
            task.actualDirectTime = task.actualNodeTime;

        task.actualTime = task.actualNodeTime + task.actualPreTime;
        task.actualCurrentTime = task.actualNodeTime;

        Date replanDate = EVSchedule.A_LONG_TIME_AGO;
        Date forecastDate = EVSchedule.A_LONG_TIME_AGO;

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
            task.replanStartDate =
                minStartDate(task.replanStartDate, child.replanStartDate);
            task.forecastStartDate =
                minStartDate(task.forecastStartDate, child.forecastStartDate);
            task.actualStartDate =
                minStartDate(task.actualStartDate, child.actualStartDate);

            task.planDate = maxPlanDate(task.planDate, child.planDate);

            if (child.isValuePruned() == false) {
                if (child.replanDate != null || child.planValue > 0)
                    replanDate = maxForecastDate(replanDate, child.replanDate);
                if (child.forecastDate  != null || child.planValue > 0)
                    forecastDate = maxForecastDate(forecastDate, child.forecastDate);
            }
        }

        if (task.replanDate == null && replanDate != EVSchedule.A_LONG_TIME_AGO)
            task.replanDate = replanDate;
        if (task.forecastDate == null
                && forecastDate != EVSchedule.A_LONG_TIME_AGO)
            task.forecastDate = forecastDate;
    }


    protected String getSetting(String name, String defaultValue) {
        String result = null;
        if (metadata != null)
            result = metadata.getProperty(name);
        if (!StringUtils.hasValue(result))
            result = Settings.getVal("ev." + name, defaultValue);
        return result;
    }

    protected boolean getBoolSetting(String name, boolean defaultVal) {
        String value = getSetting(name, null);
        if (StringUtils.hasValue(value))
            return Boolean.valueOf(value);
        else
            return defaultVal;
    }


    /**
     * If a preset/unvarying effective date has been registered, return it.
     * 
     * During normal operations, the effective date is always the current
     * date/time.  However, when opening a historical data backup, the
     * effective date is generally the date when the backup was saved instead.
     * 
     * @return the fixed effective date if one has been registered. If no
     *     effective date has been registered, returns null.
     */
    public static Date getFixedEffectiveDate() {
        String setting = Settings.getVal(FIXED_EFFECTIVE_DATE_SETTING);
        if (setting == null) return null;
        try {
            return new Date(Long.parseLong(setting));
        } catch (Exception e) {
            return null;
        }
    }


    public static Date minStartDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) < 0) return a;
        return b;
    }

    public static Date minStartDate(Collection dates) {
        Date result = null;
        if (dates != null) {
            for (Iterator i = dates.iterator(); i.hasNext();)
                result = minStartDate(result, (Date) i.next());
        }
        return result;
    }


    protected static Date maxPlanDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) > 0) return a;
        return b;
    }


    protected static Date maxForecastDate(Date a, Date b) {
        if (a == null || b == null) return null;
        if (a.compareTo(b) > 0) return a;
        return b;
    }

    protected static boolean badDouble(double d) {
        return Double.isInfinite(d) || Double.isNaN(d);
    }

    public static boolean badDate(Date d) {
        if (d == null) return true;
        if (d.getTime() < DAY_MILLIS) return true;
        if (d.getTime() > Long.MAX_VALUE - DAY_MILLIS) return true;
        return false;
    }

    protected void pruneNodes(EVTask task, boolean parentIsPruned) {
        // inherit prune value from parent.
        boolean taskIsPruned = parentIsPruned;
        if (task.pruningFlag == EVTask.USER_UNPRUNED) {
            // this task has been explicitly "unpruned"
            taskIsPruned = false;
            if (parentIsPruned == false)
                // use INFER_FROM_CONTEXT if it would have the same effect
                // as an explicit unpruning.
                task.pruningFlag = EVTask.INFER_FROM_CONTEXT;
        } else if (task.pruningFlag == EVTask.USER_PRUNED)
            // this task has been explicitly pruned.
            taskIsPruned = true;
        else {
            task.pruningFlag =
                (taskIsPruned ? EVTask.ANCESTOR_PRUNED
                              : EVTask.INFER_FROM_CONTEXT);
        }

        for (int i = 0;   i < task.getNumChildren();   i++)
            pruneNodes(task.getChild(i), taskIsPruned);
    }


    protected void getEVLeaves(EVTask task) {
        if (task.isEVLeaf()) {
            if (!task.isUserPruned() && !task.isLevelOfEffortTask())
                evLeaves.add(task);
        } else {
            for (int i = 0;   i < task.getNumChildren();   i++)
                getEVLeaves(task.getChild(i));
        }
    }

    protected class HistoricalCostIntervalProvider implements
            ConfidenceIntervalProvider {

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            double cpi = taskList.getSchedule().getMetrics()
                    .costPerformanceIndex();
            return fetchHistoricalInterval(
                EVConfidenceIntervalUtils.Purpose.TASK_COST, 1.0 / cpi);
        }
    }

    protected class HistoricalTimeErrIntervalProvider implements
            ConfidenceIntervalProvider {

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            double dtpi = taskList.getSchedule().getMetrics()
                    .directTimePerformanceIndex();
            return fetchHistoricalInterval(
                EVConfidenceIntervalUtils.Purpose.SCHEDULE_TIME_ERR, 1.0 / dtpi);
        }
    }

    protected ConfidenceInterval fetchHistoricalInterval(
            EVConfidenceIntervalUtils.Purpose purpose, double newRatio) {
        String histData = getSetting(
            EVMetadata.Forecast.Ranges.SAVED_HIST_DATA, null);
        ConfidenceInterval result = EVConfidenceIntervalUtils
                .getConfidenceInterval(histData, purpose);
        if (result == null)
            return null;

        if (!badDouble(newRatio) && newRatio > 0
                && result instanceof LogCenteredConfidenceInterval) {
            ((LogCenteredConfidenceInterval) result).recenter(newRatio);
        }
        return result;
    }

    protected abstract class MetadataDrivenCIProvider implements
            ConfidenceIntervalProvider {

        ConfidenceIntervalProvider currentPlanProvider;

        ConfidenceIntervalProvider histDataProvider;

        private MetadataDrivenCIProvider(
                ConfidenceIntervalProvider currentPlanProvider,
                ConfidenceIntervalProvider histDataProvider) {
            this.currentPlanProvider = currentPlanProvider;
            this.histDataProvider = histDataProvider;
        }

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            Double input = getInput(taskList);

            if (currentPlanProvider != null
                    && getBoolSetting(
                        EVMetadata.Forecast.Ranges.USE_CURRENT_PLAN, true)) {
                ConfidenceInterval currentInterval =  checkInterval(
                    currentPlanProvider.getConfidenceInterval(taskList), input);
                if (currentInterval != null)
                    return currentInterval;
            }

            if (histDataProvider != null
                    && getBoolSetting(
                        EVMetadata.Forecast.Ranges.USE_HIST_DATA, false)) {
                ConfidenceInterval histInterval = checkInterval(
                    histDataProvider.getConfidenceInterval(taskList), input);
                if (histInterval != null)
                    return histInterval;
            }

            return null;
        }

        protected ConfidenceInterval checkInterval(ConfidenceInterval ci,
                Double input) {
            if (ci == null)
                return null;

            if (input != null)
                ci.setInput(input);

            if (ci.getViability() < ConfidenceInterval.ACCEPTABLE)
                return null;

            return ci;
        }

        protected abstract Double getInput(EVTaskList taskList);
    }

    protected class CostIntervalProvider extends MetadataDrivenCIProvider {

        protected CostIntervalProvider(ConfidenceIntervalProvider current) {
            super(current, new HistoricalCostIntervalProvider());
        }

        @Override
        protected Double getInput(EVTaskList taskList) {
            return taskList.getSchedule().getMetrics().incompleteTaskPlanTime();
        }

    }

    protected class TimeIntervalProvider extends MetadataDrivenCIProvider {

        protected TimeIntervalProvider(ConfidenceIntervalProvider current) {
            super(current, new HistoricalTimeErrIntervalProvider());
        }

        @Override
        protected Double getInput(EVTaskList taskList) {
            return null;
        }

    }



    public void setBaselineDataSource(EVSnapshot baselineDataSource) {
        this.baselineDataSource = baselineDataSource;
    }

    public EVSnapshot getBaselineDataSource() {
        return baselineDataSource;
    }

    protected void recalcBaselineData(EVTask taskRoot) {
        EVTask baselineTaskRoot = findBaselineTaskRoot(taskRoot);
        calcBaselineTaskData(taskRoot, baselineTaskRoot);
    }

    protected EVTask findBaselineTaskRoot(EVTask taskRoot) {
        if (baselineDataSource == null)
            return null;
        EVTask baselineDataRoot = baselineDataSource.getTaskList()
                .getTaskRoot();
        return baselineDataRoot.findByTaskIDs(taskRoot.getTaskIDs());
    }

    protected static void calcBaselineTaskData(EVTask taskRoot,
            EVTask baselineRoot) {
        if (baselineRoot == null) {
            resetBaselineData(taskRoot);
        } else {
            Map<String, EVTask> baselineIdCache = new HashMap<String, EVTask>();
            buildTaskIdCache(baselineIdCache, baselineRoot);
            calcBaselineTaskData(taskRoot, baselineRoot, baselineIdCache);
        }
    }

    private static void calcBaselineTaskData(EVTask task, EVTask baselineSrc,
            Map<String, EVTask> baselineIdCache) {
        for (int i = task.getNumChildren(); i-- > 0;)
            calcBaselineTaskData(task.getChild(i), baselineIdCache, baselineSrc);

        copyBaselineData(task, baselineSrc, true);
    }

    private static void calcBaselineTaskData(EVTask task,
            Map<String, EVTask> baselineIdCache, EVTask baselineParent) {
        EVTask baselineSrc = findInBaseline(task, baselineParent,
            baselineIdCache);
        calcBaselineTaskData(task, baselineSrc, baselineIdCache);
    }

    private static EVTask findInBaseline(EVTask task, EVTask baselineParent,
            Map<String, EVTask> baselineIdCache) {
        // sanity check on parameters
        if (task == null)
            return null;

        // if the given task has task IDs, use the cache to look them up in the
        // baseline.
        List taskIDs = task.getTaskIDs();
        if (taskIDs != null && baselineIdCache != null) {
            String assignedToSuffix = getAssignedToSuffix(task);
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                String taskID = (String) i.next();
                if (assignedToSuffix != null)
                    taskID = taskID + assignedToSuffix;
                EVTask result = baselineIdCache.get(taskID);
                if (result != null)
                    return result;
            }
        }

        // the current task could not be found in the baseline ID cache.
        // see if it can be located by name under the baseline parent.
        if (baselineParent != null) {
            for (int i = baselineParent.getNumChildren();  i-- > 0; ) {
                EVTask baselineChild = baselineParent.getChild(i);
                if (baselineChild.getName().equals(task.getName()))
                    return baselineChild;
            }
        }

        // no luck!  Return null.
        return null;
    }

    /** Discard any baseline data for the given task, and all its children */
    protected static void resetBaselineData(EVTask task) {
        task.baselineStartDate = null;
        task.baselineDate = null;
        task.baselineTime = 0;
        for (int i = task.getNumChildren();   i-- > 0; )
            resetBaselineData(task.getChild(i));
    }

    /** Initialize the baseline data for a task.
     * 
     * @param task the task whose baseline data should be set.
     * @param baselineSrc the task in the baseline data source that corresponds
     *       to the given task
     * @param sum when baselineSrc is null, if this parameter is true, the
     *       baseline data for the task will be summed from its children.
     *       Otherwise, if this parameter is false, the baseline data for the
     *       task will be reset.
     */
    protected static void copyBaselineData(EVTask task, EVTask baselineSrc,
            boolean sum) {
        if (baselineSrc == null) {
            Date startDate = null;
            Date date = null;
            double time = 0;

            if (sum) {
                for (int i = task.getNumChildren();   i-- > 0; ) {
                    EVTask child = task.getChild(i);
                    time += child.baselineTime;
                    startDate = minStartDate(startDate, child.baselineStartDate);
                    date = maxPlanDate(date, child.baselineDate);
                }
            }

            task.baselineStartDate = startDate;
            task.baselineDate = date;
            task.baselineTime = time;
        } else {
            task.baselineStartDate = baselineSrc.planStartDate;
            task.baselineDate = baselineSrc.planDate;
            task.baselineTime = baselineSrc.planTime;
        }
    }

    /** Populate a Map so it can be used to look up EVTask objects by their
     * task IDs.
     */
    protected static void buildTaskIdCache(Map<String, EVTask> cache,
            EVTask task) {
        for (int i = task.getNumChildren(); i-- > 0;)
            buildTaskIdCache(cache, task.getChild(i));

        List ids = task.getTaskIDs();
        if (ids != null) {
            String assignedToSuffix = getAssignedToSuffix(task);
            for (Iterator i = ids.iterator(); i.hasNext();) {
                String id = (String) i.next();
                cache.put(id, task);
                if (assignedToSuffix != null)
                    cache.put(id+assignedToSuffix, task);
            }
        }
    }

    private static String getAssignedToSuffix(EVTask task) {
        List assignedTo = task.getAssignedTo();
        if (assignedTo == null || assignedTo.size() != 1)
            return null;
        else
            return "~" + assignedTo.get(0);
    }

    static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

}
