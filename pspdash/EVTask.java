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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pspdash.data.DataRepository;
import pspdash.data.DataListener;
import pspdash.data.DataEvent;
import pspdash.data.SimpleData;
import pspdash.data.NumberData;
import pspdash.data.NumberFunction;
import pspdash.data.DoubleData;
import pspdash.data.DateData;

public class EVTask implements DataListener {

    public static final String PLAN_TIME_DATA_NAME      = "Estimated Time";
    public static final String ACT_TIME_DATA_NAME       = "Time";
    public static final String DATE_COMPLETED_DATA_NAME = "Completed";
    public static final String IGNORE_PLAN_TIME_NAME    = "Rollup Tag";
    private static final String LEVEL_OF_EFFORT_PREFIX  = "TST-LOE_";
    private static final String TASK_ORDINAL_PREFIX  = "TST-TSK#_";

    public interface Listener {
        public void evNodeChanged(EVTask node);
    }

    EVTask parent = null;
    ArrayList children = new ArrayList();

    String name, fullName, taskListName;
    Listener listener;
    DataRepository data;


    /* The percentage of time a user plans to spend on this task, as a level
     * of effort.
     */
    double planLevelOfEffort = NOT_LEVEL_OF_EFFORT;

    public static final int NOT_LEVEL_OF_EFFORT = -1;
    public static final int ANCESTOR_LEVEL_OF_EFFORT = 0;


    /** Value indicating user-requested reordering/restructuring of the task
     * list.  Values of interest:<ul>
     *
     * <li>0 - indicates that the order is unknown and needs to be inferred
     *     from the context of this node.
     * <li>&gt;= 1 - the order of this node in the list
     * <li>-1 - indicates that the user has explicitly pruned this node.
     * <li>-2 - indicates that this node has inherited its pruned status from
     *     an ancestor.
     */
    int taskOrdinal = INFER_FROM_CONTEXT;

    public static final int INFER_FROM_CONTEXT = 0;
    public static final int USER_PRUNED = -1;
    public static final int ANCESTOR_PRUNED = -2;

    int savedTaskOrdinal = 0;

    /** The time (minutes) the user plans to spend in this node, taken
     * directly from the data repository. */
    double topDownPlanTime;
    /** The time (minutes) the user plans to spend in this node, calculated
     * by adding up plan times of children */
    double bottomUpPlanTime;
    // various flags which determine how we should interpret the top down
    // plan time for this node.
    boolean planTimeEditable, planTimeNull, planTimeUndefined,
        ignorePlanTimeValue = false;



    /** The plan time (minutes) for this node, determined intelligently from
     * the top down and bottom up times for this node. */
    double planTime;
    /** The portion of the plan time that "counts" toward this schedule
     * (minutes) */
    double planValue;
    /** The total plan value spent in this node and all prior nodes. */
    double cumPlanValue;

    /** Actual time (minutes) spent in this node before the start of the
     * schedule */
    double actualPreTime;
    /** Actual time (minutes) spent in this node during the schedule */
    double actualNodeTime;
    /** The total time (minutes) actually spent in this node and its children,
     * both before and during this schedule  */
    double actualTime;
    /** The total time (minutes) actually spent during this schedule
     * in this node and its children */
    double actualCurrentTime;
    /** The total time (minutes) actually spent during this schedule
     * in this node and its children on tasks that count toward earned value */
    double actualDirectTime;
    /** Actual value earned (minutes) in this node and its children. */
    double valueEarned;

    /** The date we plan to start this task */
    Date planStartDate;
    /** The date we actually started this task */
    Date actualStartDate;
    /** The date we plan to complete this task */
    Date planDate;
    /** The date this task was actually completed */
    Date dateCompleted;
    /** True if the user can edit the completion date for this task */
    boolean dateCompletedEditable;

    private static final Date COMPLETION_DATE_NA = EVSchedule.A_LONG_TIME_AGO;


    /** Creates an EVTask suitable for the root of an EVTaskList.  */
    public EVTask(String rootName) {
        this.name = rootName;
        this.fullName = "";
        planTime = cumPlanValue = actualTime = valueEarned =
            topDownPlanTime = bottomUpPlanTime = actualNodeTime = 0;
        planDate = dateCompleted = null;
        listener = null;
        planTimeEditable = dateCompletedEditable = planTimeUndefined = false;
        planTimeNull = true;
        data = null;
    }

    /** Add a child task to this EVTask. */
    public boolean add(EVTask child) {
        if (containsNode(children, child))
            return false;

        child.parent = this;
        children.add(child);
        return true;
    }

    /** Add a child task to this EVTask. */
    public boolean add(int pos, EVTask child) {
        if (containsNode(children, child))
            return false;

        child.parent = this;
        children.add(pos, child);
        return true;
    }

    /** Remove a child task from this EVTask */
    public int remove(EVTask child) {
        int pos = indexOfNode(children, child);
        if (pos != -1)
            children.remove(pos);
        return pos;
    }

    /** Replace a child task of this EVTask
     * WARNING: no checks are performed on the parameters. This method
     * is <b>only</b> meant to be called when recalculations on a child
     * caused a replacement object to be created (rather than just
     * mutations within the existing object).  This method should <b>NOT</b>
     * be used to replace one task with an entirely different task - use
     * remove() and add() for that.
     */
    void replace(int pos, EVTask newChild) {
        children.set(pos, newChild);
        newChild.parent = this;
    }

    public void moveUp(int childPos) {
        if (childPos > 0 && childPos < children.size()) {
            Object a = children.get(childPos-1);
            Object b = children.get(childPos);

            children.set(childPos-1, b);
            children.set(childPos,   a);
        }
    }

    public boolean sameNode(EVTask that) {
        if ("".equals(this.fullName))
            // compare root nodes by examining their node name.
            return cmpStrings(this.name, that.name);
        else
            // compare regular nodes by examining their full name.
            return cmpStrings(this.fullName, that.fullName);
    }
    private boolean cmpStrings(String a, String b) {
        if (a == b) return true;
        if (a != null) return a.equals(b);
        return false;
    }

    /** Creates an EVTask for the tasks in the hierarchy at the given path */
    public EVTask(String taskListName, String hierarchyPath, DataRepository data,
                  PSPProperties hierarchy, Listener listener) {
        this(null, taskListName, hierarchyPath.substring(1), hierarchyPath,
             null, data, hierarchy, listener);
    }


    protected EVTask(EVTask parent, String taskListName, String name,
                     String fullName, PropertyKey key, DataRepository data,
                     PSPProperties hierarchy, Listener listener)
    {
        this.parent = parent;
        this.name = name;
        this.fullName = fullName;
        this.taskListName = taskListName;
        this.data = data;
        this.listener = listener;

        if (getValue(IGNORE_PLAN_TIME_NAME) != null)
            ignorePlanTime();
        else
            setPlanTime(getValue(PLAN_TIME_DATA_NAME));

        setActualTime(getValue(ACT_TIME_DATA_NAME));

        setLevelOfEffort(getValue(getLevelOfEffortDataname()));
        loadLeafOrdinal();

        addChildrenFromHierarchy(fullName, key, data, hierarchy, listener);

        if (isLeaf()) {
            setActualDate(getValue(DATE_COMPLETED_DATA_NAME));
        }
    }

    /** Attempt to find children in the hierarchy, and add them to our
     *  list of children.
     * @return true if any children were found and added.
     */
    protected boolean addChildrenFromHierarchy(String fullName,
                     PropertyKey key, DataRepository data,
                     PSPProperties hierarchy, Listener listener)
    {
        boolean addedChild = false;

        if (key == null)
            // make an attempt to lookup the name in the hierarchy.
            key = hierarchy.findExistingKey(fullName);

        if (key != null) {
            int numKids = hierarchy.getNumChildren(key);
            for (int i = 0;   i < numKids;  i++) {
                PropertyKey child = hierarchy.getChildKey(key, i);
                children.add(new EVTask(this, taskListName,
                                        child.name(), child.path(), child,
                                        data, hierarchy, listener));
                addedChild = true;
            }
        }
        return addedChild;
    }

    public EVTask(Element e) { this(e, null); }
    private EVTask(Element e, String parentName) {
        name = e.getAttribute("name");
        fullName = (parentName == null ? "" : parentName + "/" + name);

        planValue = EVSchedule.getXMLNum(e, "pt");
        if (e.hasAttribute("ptt"))
            planTime = EVSchedule.getXMLNum(e, "ptt");
        else
            planTime = planValue;
        topDownPlanTime = bottomUpPlanTime = planTime;
        actualTime = EVSchedule.getXMLNum(e, "at");
        if (e.hasAttribute("adt"))
            actualDirectTime = EVSchedule.getXMLNum(e, "adt");
        else
            actualDirectTime = actualTime;
        planDate = EVSchedule.getXMLDate(e, "pd");
        dateCompleted = EVSchedule.getXMLDate(e, "cd");
        if (e.hasAttribute("loe"))
            planLevelOfEffort = EVSchedule.getXMLNum(e, "loe");
        if (e.hasAttribute("ord"))
            taskOrdinal = (int) EVSchedule.getXMLNum(e, "ord");
        planTimeEditable = planTimeNull = planTimeUndefined = false;

        NodeList subTasks = e.getChildNodes();
        int len = subTasks.getLength();
        for (int i=0;   i < len;   i++) {
            Node n = subTasks.item(i);
            if (n instanceof Element &&
                "task".equals(((Element) n).getTagName()))
                add(new EVTask((Element) n, fullName));
        }
    }

    protected SimpleData getValue(String name) { return getValue(name, true); }
    protected SimpleData getValue(String name, boolean notify) {
        String dataName = data.createDataName(fullName, name);
        if (notify && listener != null)
            data.addDataListener(dataName, this, false);
        return data.getSimpleValue(dataName);
    }


    public boolean plannedTimeIsEditable() {
        return (planTimeEditable &&
                (planLevelOfEffort != ANCESTOR_LEVEL_OF_EFFORT));
    }
    public boolean completionDateIsEditable() {
        return isLeaf() && dateCompletedEditable &&
            !isLevelOfEffortTask() && !isUserPruned();
    }

    protected void setPlanTime(SimpleData time) {
        if (time instanceof NumberData) {
            if (!ignorePlanTimeValue) {
                topDownPlanTime = ((NumberData) time).getDouble();
                if (Double.isNaN(topDownPlanTime) ||
                    Double.isInfinite(topDownPlanTime))
                    topDownPlanTime = 0.0;
                planTimeEditable = time.isEditable();
                planTimeUndefined = !time.isDefined();
                planTimeNull = false;
            }
        } else {
            planTimeNull = (time == null);
            topDownPlanTime = 0;
            planTimeEditable = true;
            planTimeUndefined = false;
        }
    }

    protected void ignorePlanTime() {
        ignorePlanTimeValue = true;
        topDownPlanTime = 0;
        planTimeNull = true;
        planTimeEditable = planTimeUndefined = false;
    }

    private String getLevelOfEffortDataname() {
        return LEVEL_OF_EFFORT_PREFIX + taskListName;
    }

    public boolean isLevelOfEffortTask() {
        return (planLevelOfEffort >= 0);
    }

    private void setLevelOfEffort(SimpleData levelOfEffort) {
        if (levelOfEffort instanceof NumberData) {
            planLevelOfEffort = ((NumberData) levelOfEffort).getDouble();
            if (!(planLevelOfEffort > 0 && planLevelOfEffort < 1))
                planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
        } else {
            planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
        }
    }

    private void userSetLevelOfEffort(String value) {
        double p = NOT_LEVEL_OF_EFFORT;

        if (value == null || value.trim().length() == 0) {
            p = 0;
        } else try {
            Number percentage = percentFormatter.parse(value);
            p = percentage.doubleValue();
        } catch (ParseException e) {}

        if (p == 0) {
            planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
            // erase the level of effort in the data repository
            data.userPutValue
                (data.createDataName(fullName, getLevelOfEffortDataname()),
                 null);
        } else if (p > 0 && p < 1) {
            planLevelOfEffort = p;
            // save this level of effort to the data repository
            data.userPutValue
                (data.createDataName(fullName, getLevelOfEffortDataname()),
                 new DoubleData(planLevelOfEffort, true));
        }
    }


    private void loadLeafOrdinal() {
        SimpleData d = getValue(TASK_ORDINAL_PREFIX + taskListName);
        if (d instanceof NumberData)
            taskOrdinal = savedTaskOrdinal = ((NumberData) d).getInteger();
    }

    /** Save any structural data about this node to the repository.
     */
    void saveData(String newTaskListName) {
        if (fullName != null && fullName.length() > 0) {
            String oldDataName = null;
            if (savedTaskOrdinal != INFER_FROM_CONTEXT)
                oldDataName = TASK_ORDINAL_PREFIX + taskListName;

            if (newTaskListName != null && taskOrdinal != INFER_FROM_CONTEXT &&
                taskOrdinal != ANCESTOR_PRUNED) {
                String newDataName = TASK_ORDINAL_PREFIX + newTaskListName;
                if (newDataName.equals(oldDataName)) oldDataName = null;
                if (taskOrdinal != savedTaskOrdinal || oldDataName != null) {
                    SimpleData d = new DoubleData(taskOrdinal, false);
                    String dataName = data.createDataName(fullName, newDataName);
                    data.putValue(dataName, d);
                }
            }

            if (oldDataName != null) {
                String dataName = data.createDataName(fullName, oldDataName);
                data.putValue(dataName, null);
            }
        }

        taskListName = newTaskListName;
        savedTaskOrdinal = taskOrdinal;

        for (int i = 0;   i < getNumChildren();   i++)
            getChild(i).saveData(newTaskListName);
    }

    protected void setActualDate(SimpleData date) {
        if (date instanceof DateData) {
            dateCompleted = ((DateData) date).getValue();
            dateCompletedEditable = date.isEditable();
        } else {
            dateCompleted = null;
            dateCompletedEditable = true;
        }
    }

    protected void setActualTime(SimpleData time) {
        if (time instanceof NumberData) {
            // look in the repository to see if this value is a simple
            // number, or a calculation.  We aren't interested in
            // calculations - just simple numbers.
            String dataName =
                data.createDataName(fullName, ACT_TIME_DATA_NAME);
            Object val = data.getValue(dataName);
            if (val != null &&
                (!(val instanceof DoubleData) ||
                 val instanceof NumberFunction)) return;

            actualNodeTime = ((NumberData) time).getDouble();
            if (Double.isNaN(actualNodeTime) ||
                Double.isInfinite(actualNodeTime))
                actualNodeTime = 0.0;
        } else {
            actualNodeTime = 0;
        }
    }

    public void userSetPlanTime(Object aValue) {
        if ((aValue instanceof String && ((String) aValue).trim().endsWith("%")) ||
            (isLevelOfEffortTask() && (aValue == null || "".equals(aValue)))) {
            userSetLevelOfEffort((String) aValue);
        } else if (plannedTimeIsEditable() && aValue instanceof String) {
            long planTime = -1;

            // parse the value to obtain a number of minutes
            if (((String) aValue).length() > 0)
                planTime = TimeLogEditor.parseTime((String) aValue);

            // if the user is obviously correcting a top-down/bottom-up
            // mismatch, then just treat the input the same as if the
            // user had deleted the top-down estimate.
            if (hasTopDownBottomUpError() &&
                Math.abs(planTime - bottomUpPlanTime) < 0.9)
                planTime = -1;

            if (planTime != -1) {
                this.planTime = topDownPlanTime = bottomUpPlanTime = planTime;
                planTimeNull = planTimeUndefined = false;
                // save those minutes to the data repository
                data.userPutValue(data.createDataName(fullName,
                                                      PLAN_TIME_DATA_NAME),
                                  new DoubleData(planTime, true));
                userSetLevelOfEffort(null);
            } else {
                this.planTime = topDownPlanTime = bottomUpPlanTime;
                data.userPutValue(data.createDataName(fullName,
                                                      PLAN_TIME_DATA_NAME),
                                  null);
                planTimeNull = true;
                planTimeUndefined = false;
                userSetLevelOfEffort(null);
            }
        }
    }

    public void userSetActualDate(Object aValue) {
        if (completionDateIsEditable()) {
            String dataName =
                data.createDataName(fullName, DATE_COMPLETED_DATA_NAME);

            // save the Date object to the data repository
            if (aValue instanceof Date) {
                dateCompleted = (Date) aValue;
                data.userPutValue(dataName, new DateData(dateCompleted, true));
            } else {
                dateCompleted = null;
                data.userPutValue(dataName, null);
            }
        }
    }

    protected static NumberFormat percentFormatter =
        NumberFormat.getPercentInstance();
    protected static NumberFormat intPercentFormatter =
        NumberFormat.getPercentInstance();
    static {
        percentFormatter.setMaximumFractionDigits(1);
        intPercentFormatter.setMaximumFractionDigits(0);
    }
    static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent))
            percent = 0;
        if (true || percent > 0.99 || percent < -0.99)
            return intPercentFormatter.format(percent);
        else
            return percentFormatter.format(percent);
    }
    static String formatIntPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent))
            percent = 0;
        return intPercentFormatter.format(percent);
    }


    static String formatTime(double ttime) {
        // time is in minutes.
        double time = Math.floor(ttime + 0.5); // round to the nearest minute
        int hours = (int) (time / 60);
        int minutes = (int) (time % 60);
        if (minutes < 10)
            return hours + ":0" + minutes;
        else
            return hours + ":" + minutes;
    }

    public int getNumChildren() { return children.size(); }
    public int getChildIndex(Object child) {
        if (child instanceof EVTask)
            return indexOfNode(children, (EVTask) child);
        else
            return -1;
    }
    public boolean isLeaf() { return children.isEmpty(); }
    public EVTask getChild(int pos) { return (EVTask) children.get(pos); }
    public EVTask getParent() { return parent; }
    public String toString() { return name; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getPlanTime() {
        if (planLevelOfEffort == ANCESTOR_LEVEL_OF_EFFORT) return "";
        else if (planLevelOfEffort > 0) return formatPercent(planLevelOfEffort);
        else return formatTime(planTime);
    }
    public String getPlanDirectTime() {
        if (isValuePruned() && planValue == 0) return "";
        else return formatTime(planValue);
    }
    public boolean hasPlanTimeError() {
        return (hasTopDownBottomUpError() || planTimeIsMissing());
    }
    private boolean hasTopDownBottomUpError() {
        // TODO: think about whether top-down-bottom-up errors in
        // chronologically pruned tasks should be highlighted.
        return (!isValuePruned() && //!isLevelOfEffortTask() && !isTotallyPruned() &&
            (bottomUpPlanTime > 0) &&
            (Math.abs(planTime - bottomUpPlanTime) > 0.5));
    }
    private boolean planTimeIsMissing() {
        return (!isValuePruned() &&
                planTimeEditable && (planTimeNull || planTimeUndefined));
    }
    public String getPlanTimeError() {
        if (hasTopDownBottomUpError())
            return "top-down/bottom-up mismatch (bottom-up = " +
                formatTime(bottomUpPlanTime) + ")";
        if (planTimeIsMissing())
            return "plan time is missing";
        return null;
    }
    public String getActualTime(double totalActualTime) {
        if (isLevelOfEffortTask())
            return formatPercent(actualTime / totalActualTime);
        else return formatTime(actualTime);
    }
    public String getActualDirectTime(double totalActualTime) {
        if (//isLevelOfEffortTask() || isTotallyPruned() ||
            (isValuePruned() && actualDirectTime == 0)) return "";
        else return formatTime(actualDirectTime);
    }
    public String getPlanValue(double totalPlanValue) {
        if (isValuePruned() && planValue == 0) return "";
        return formatPercent(planValue/totalPlanValue);
    }

    public String getCumPlanTime() {
        if (isValuePruned() && cumPlanValue == 0) return "";
        return formatTime(cumPlanValue);
    }
    public String getCumPlanValue(double totalPlanValue) {
        if (isValuePruned() && cumPlanValue == 0) return "";
        return formatPercent(cumPlanValue/totalPlanValue);
    }
    public Date getPlanDate() {
        if (isValuePruned()) return null;
        return planDate;
    }
    public Date getActualDate() {
        if (isLevelOfEffortTask() || isTotallyPruned() ||
            dateCompleted == COMPLETION_DATE_NA) return null;
        return dateCompleted;
    }
    public String getPercentComplete() {
        if (valueEarned == 0 || planValue == 0 || isLevelOfEffortTask())
            return "";
        else return formatIntPercent(valueEarned / planValue);
    }
    public String getPercentSpent() {
        if (actualTime == 0 || planTime == 0 || isValuePruned()) return "";
        // percent spent applies to all time, not just the current schedule.
        else return formatIntPercent(actualTime / planTime);
    }
    public String getValueEarned(double totalPlanTime) {
        if (isValuePruned() && valueEarned == 0) return "";
        else if (dateCompleted != null || valueEarned != 0.0)
            return formatPercent(valueEarned/totalPlanTime);
        else
            return "";
    }
    private String taskError = null;
    public boolean hasTaskError() { return taskError != null; }
    public String getTaskError() { return taskError; }
    public void setTaskError(String err) { taskError = err; }


    /** Gets the path from the root to the receiver. */
    public EVTask[] getPath() { return getPathToRoot(this, 0); }

    protected EVTask[] getPathToRoot(EVTask aNode, int depth) {
        EVTask[] retNodes;

        if(aNode == null) {
            if(depth == 0)
                return null;
            else
                retNodes = new EVTask[depth];
        }
        else {
            depth++;
            retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }


    /** Get a list of the leaf tasks under this task.
     *
     * elements in the list will be EVTask objects.
     */
    public List getLeafTasks() {
        ArrayList result = new ArrayList();
        getLeafTasks(result);
        return result;
    }
    protected void getLeafTasks(List list) {
        if (isEVLeaf()) {
            if (!isLevelOfEffortTask() && !isUserPruned())
                list.add(this);
        } else
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).getLeafTasks(list);
    }
    public boolean isEVLeaf() {
        return (isLeaf() || (planTime > 0 && bottomUpPlanTime == 0));
    }



    public void recalc(EVSchedule schedule, TimeLog log) {
        resetRootValues();
        recalcPlanTimes();
        recalcPlanCumTime(0.0);
        recalcActualTimes();
        recalcDateCompleted();
        recalcPlanValue();
        schedule.prepForEvents();
        schedule.cleanUp();
        Date effDate = dateCompleted;
        if (effDate == null) effDate = getTestingEffDate();
        if (effDate == null) effDate = new Date();
        recalcPlanDates(schedule);
        for (int i = log.v.size();   i-- > 0;   )
            saveTimeLogInfo(schedule, (TimeLogEntry) log.v.get(i));
        schedule.setEffectiveDate(effDate);
        schedule.getMetrics().reset(schedule.getStartDate(), effDate,
                                    schedule.getPeriodStart(effDate),
                                    schedule.getPeriodEnd(effDate));
        checkForNodeErrors(schedule.getMetrics(), 0,
                           new ArrayList(), new ArrayList());
        checkForScheduleErrors(schedule.getMetrics(), schedule);
        recalcMetrics(schedule.getMetrics());
        schedule.getMetrics().recalcComplete(schedule);
        schedule.firePreparedEvents();
    }

    public void simpleRecalc(EVSchedule schedule) {
        recalcPlanTimes();
        recalcPlanCumTime(0.0);
        recalcPlanValue();
        checkForNodeErrors(schedule.getMetrics(), 0,
                           new ArrayList(), new ArrayList());
        checkForScheduleErrors(schedule.getMetrics(), schedule);
    }

    public Date getTestingEffDate() {
        String setting = Settings.getVal("ev.effectiveDate");
        if (setting == null) return null;
        try {
            return new Date(Long.parseLong(setting));
        } catch (Exception e) {
            return null;
        }
    }

    protected void resetRootValues() {
        planTime = cumPlanValue = actualTime = valueEarned =
            topDownPlanTime = bottomUpPlanTime = 0;
        planDate = dateCompleted = null;
    }

    public double recalcPlanTimes() {
        if (isLeaf())
            planTime = bottomUpPlanTime = topDownPlanTime;
        else {
            bottomUpPlanTime = 0;
            for (int i = 0;   i < getNumChildren();   i++)
                bottomUpPlanTime += getChild(i).recalcPlanTimes();

            if (bottomUpPlanTime == 0)
                return (planTime = topDownPlanTime);
            else if (!planTimeNull && topDownPlanTime > 0)
                planTime = topDownPlanTime;
            else {
                planTime = bottomUpPlanTime;
                planTimeEditable = false;
            }
        }
        return bottomUpPlanTime;
    }

    public double recalcPlanCumTime(double prevCumTime) {
        if (isLeaf())
            // for leaves, add our plan time to the total.
            cumPlanValue = prevCumTime + planTime;
        else if (isEVLeaf()) {
            // if we aren't a leaf, but we're an EVLeaf, our children can't
            // help us. Figure out cum time ourselves, then tell them what it
            // is so they can display the same thing.
            cumPlanValue = prevCumTime + planTime;
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).recalcPlanCumTime(cumPlanValue);
        } else {
            // for nonleaves, ask each of our children to recalc.
            cumPlanValue = prevCumTime;
            for (int i = 0;   i < getNumChildren();   i++)
                cumPlanValue = getChild(i).recalcPlanCumTime(cumPlanValue);
        }
        return cumPlanValue;
    }

    public double recalcActualTimes() {
        actualTime = actualNodeTime;
        if (!isLeaf()) {
            // for nonleaves, ask each of our children to recalc.
            for (int i = 0;   i < getNumChildren();   i++)
                actualTime += getChild(i).recalcActualTimes();
        }
        return actualTime;
    }

    public void recalcPlanValue() {

        if (isLeaf())
            valueEarned = (dateCompleted == null ? 0 : planTime);
        else if (isEVLeaf()) {
            valueEarned = (dateCompleted == null ? 0 : planTime);
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).recalcPlanValue();
        } else {
            valueEarned = 0;
            // for nonleaves, ask each of our children to recalc.
            for (int i = 0;   i < getNumChildren();   i++) {
                getChild(i).recalcPlanValue();
                valueEarned += getChild(i).valueEarned;
            }
        }
    }

    public void recalcDateCompleted() {
        if (isLeaf()) return;

        for (int i = 0;   i < getNumChildren();   i++) {
            if (getChild(i).isTotallyPruned()) continue;
            getChild(i).recalcDateCompleted();
        }

        recalcParentDateCompleted();
    }

    void recalcParentDateCompleted() {
        Date d, result = COMPLETION_DATE_NA;
        for (int i = 0;   i < getNumChildren();   i++) {
            if (getChild(i).isTotallyPruned()) continue;
            d = getChild(i).dateCompleted;
            if (d == null)
                result = null;
            else if (result != null && result.compareTo(d) < 0)
                result = d;
        }
        dateCompletedEditable = false;
        dateCompleted = result;
    }

    public void recalcPlanDates(EVSchedule schedule) {
        if (isEVLeaf()) {
            planDate = schedule.getPlannedCompletionDate
                (cumPlanValue, cumPlanValue);
            if (dateCompleted != null)
                schedule.saveCompletedTask(dateCompleted, planTime);

            if (!isLeaf())
                for (int i = getNumChildren();   i-- > 0;   )
                    getChild(i).recalcPlanDates(schedule);

        } else {
            for (int i = getNumChildren();   i-- > 0;   )
                getChild(i).recalcPlanDates(schedule);
            planDate = getChild(getNumChildren()-1).planDate;
        }
    }


    public void checkForNodeErrors(EVMetrics metrics, int depth,
                                   List rootChildList,
                                   List otherNodeList) {
        switch (depth) {
        case 0:                 // this is the root
            break;

        case 1:                 // this is a child of the root.
            if (containsNode(rootChildList, this) ||
                containsNode(otherNodeList, this)) {
                metrics.addError("The task \"" + fullName + "\" appears in "+
                                 "the task list more than once.", this);
                setTaskError("Duplicate task");
            } else
                setTaskError(null);
            rootChildList.add(this);
            break;

        default:
            int pos = indexOfNode(rootChildList, this);
            if (pos != -1) {
                EVTask t = (EVTask) rootChildList.get(pos);
                metrics.addError("The task \"" + t.fullName + "\" appears in "+
                                 "the task list more than once.", t);
                t.setTaskError("Duplicate task");
            }
            setTaskError(null);
            otherNodeList.add(this);
        }

        if (hasTopDownBottomUpError())
            metrics.addError("The top-down estimate of " + getPlanTime() +
                             " for task \"" + fullName + "\" does not " +
                             "agree with the bottom-up estimate of " +
                             formatTime(bottomUpPlanTime) + ". (Consider " +
                             "editing or deleting the top-down estimate " +
                             "for task \"" + fullName + "\", or modifying " +
                             "the estimates of the tasks underneath it.)",
                             this);
        if (planTimeIsMissing())
            metrics.addError
                ("You still need to estimate the time required for task \"" +
                 fullName + "\".", this);

        for (int i = 0;   i < getNumChildren();   i++)
            getChild(i).checkForNodeErrors(metrics, depth+1,
                                           rootChildList, otherNodeList);
    }

    public void checkForScheduleErrors(EVMetrics metrics, EVSchedule sched) {
        EVSchedule.Period p = sched.get(0);
        if (p.actualDirectTime > 0.0)
            metrics.addError("You have logged time to some of the tasks " +
                             "in your task list before the start of the " +
                             "first time period in your schedule. (Consider "+
                             "modifying the schedule to begin earlier.)",
                             this);
        if (p.cumEarnedValue > 0.0)
            metrics.addError("Some of the tasks in your task list were " +
                             "completed before the start of the " +
                             "first time period in your schedule. (Consider "+
                             "modifying the schedule to begin earlier.)",
                             this);
    }

    public void recalcMetrics(EVMetrics metrics) {
        if (isEVLeaf())
            metrics.addTask(planTime, actualTime, planDate, dateCompleted);
        else {
            for (int i = getNumChildren();   i-- > 0;   )
                getChild(i).recalcMetrics(metrics);
            // if they logged time against a non-leaf node, it counts
            // against their metrics right away.  Treat it as an
            // imaginary task with no planned time, which should have
            // been completed instantaneously when the schedule started
            if (actualNodeTime > 0)
                metrics.addTask(0, actualNodeTime, null, metrics.startDate());
        }
    }


    public boolean saveTimeLogInfo(EVSchedule schedule, TimeLogEntry e) {
        String entryPath = e.getPath();
        if (entryPath.equals(fullName)) {
            schedule.saveActualTime(e.getStartTime(), e.getElapsedTime());
            return true;
        }

        // If this is a parent node, and the time log entry begins
        // with our full name, dispatch this to our children.
        if (!isLeaf() &&
            (fullName == null || fullName.length() == 0 ||
             entryPath.startsWith(fullName)))

            for (int i = children.size();   i-- > 0;  )  // dispatch loop
                if (getChild(i).saveTimeLogInfo(schedule, e))
                    return true;

        return false;
    }


    //
    // DataListener interface
    //

    public void dataValueChanged(DataEvent e) {
        if (handleEvent(e)) notifyListener();
    }

    public void dataValuesChanged(Vector v) {
        boolean needsNotify = false;
        for (int i = v.size();  i-- > 0; )
            if (handleEvent((DataEvent) v.elementAt(i)))
                needsNotify = true;

        if (needsNotify) notifyListener();
    }

    protected boolean handleEvent(DataEvent e) {
        String dataName = e.getName();
        if (!dataName.startsWith(fullName+"/")) return false;
        dataName = dataName.substring(fullName.length()+1);
        if (PLAN_TIME_DATA_NAME.equals(dataName))
            setPlanTime(e.getValue());
        else if (ACT_TIME_DATA_NAME.equals(dataName))
            setActualTime(e.getValue());
        else if (DATE_COMPLETED_DATA_NAME.equals(dataName))
            setActualDate(e.getValue());
        else if (dataName.startsWith(LEVEL_OF_EFFORT_PREFIX))
            setLevelOfEffort(e.getValue());
        else
            return false;
        return true;
    }

    protected void notifyListener() {
        Listener l = listener;
        if (l != null) l.evNodeChanged(this);
    }

    public void destroy() {
        if (listener != null) {
            listener = null;
            data.deleteDataListener(this);
        }
        for (int i=children.size();   i-- > 0; )
            getChild(i).destroy();
        children.clear();
    }

    public void saveToXML(StringBuffer result) {
        result.append("<task name='").append(XMLUtils.escapeAttribute(name))
            .append("' pt='").append(planValue)
            .append("' at='").append(actualTime);
        if (planTime != planValue)
            result.append("' ptt='").append(planTime);
        if (planDate != null)
            result.append("' pd='").append(EVSchedule.saveDate(planDate));
        if (dateCompleted != null)
            result.append("' cd='").append(EVSchedule.saveDate(dateCompleted));
        if (isLevelOfEffortTask())
            result.append("' loe='").append(planLevelOfEffort);
        if (taskOrdinal != -1)
            result.append("' ord='").append(taskOrdinal);

        if (isLeaf())
            result.append("'/>");
        else {
            result.append("'>");
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).saveToXML(result);
            result.append("</task>");
        }
    }

    static int indexOfNode(List list, EVTask node) {
        int i;
        if (node != null && list != null && (i = list.size()) > 0)
            while (i-- > 0)
                if (node.sameNode((EVTask) list.get(i)))
                    return i;
        return -1;
    }
    static boolean containsNode(List list, EVTask node) {
        return indexOfNode(list, node) != -1;
    }

    protected boolean isValuePruned() {
        return isLevelOfEffortTask() || isTotallyPruned() || isChronologicallyPruned();
    }

    public boolean isChronologicallyPruned() {
        return (dateCompleted != null && planDate == null && planValue == 0);
    }

    public boolean isUserPruned() {
        return (taskOrdinal == USER_PRUNED || taskOrdinal == ANCESTOR_PRUNED);
    }
    protected boolean isTotallyPruned() {
        return (isUserPruned() && planValue == 0);
    }

    public void setUserPruned(boolean prune) {
        if (prune)
            taskOrdinal = USER_PRUNED;
        else if (taskOrdinal == USER_PRUNED || taskOrdinal == ANCESTOR_PRUNED)
            taskOrdinal = 1; // fixme - we need to assign this a value that places it in the right place in the task order.
    }

    public EVTask getTaskForPath(String fullPath) {
        // check to see if our fullName is a perfect match for this path.
        if (fullName != null && fullName.equals(fullPath)) return this;

        // if we could not possibly be the parent of a node matching
        // fullPath, return null.
        if (fullName != null) {
            if (fullPath.length() <= fullName.length()) return null;
            if (!fullPath.startsWith(fullName)) return null;
            if (fullPath.charAt(fullName.length()) != '/') return null;
        }

        // see if any of our children would like to claim fullPath as theirs.
        for (int i = children.size();   i-- > 0;  ) { // dispatch loop
            EVTask result = getChild(i).getTaskForPath(fullPath);
            if (result != null) return result;
        }

        // None of our children claimed the path.
        if (fullName == null || fullName.length() == 0)
            // if this is the root node, don't claim it either.
            return null;
        else
            // otherwise, claim it as our own.
            return this;
    }
}
