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

    public interface Listener {
        public void evNodeChanged(EVTask node);
    }

    EVTask parent = null;
    ArrayList children = new ArrayList();

    String name, fullName;
    double planTime,  cumPlanTime,  actualTime;  // expressed in minutes
    double actualNodeTime, valueEarned;          // expressed in minutes
    double topDownPlanTime, bottomUpPlanTime;    // expressed in minutes
    Date planDate, dateCompleted;
    boolean planTimeEditable, planTimeNull, dateCompletedEditable;
    boolean ignorePlanTimeValue = false;
    Listener listener;

    DataRepository data;

    /** Creates an EVTask suitable for the root of an EVTaskList.  */
    public EVTask(String rootName) {
        this.name = rootName;
        this.fullName = "";
        planTime = cumPlanTime = actualTime = valueEarned =
            topDownPlanTime = bottomUpPlanTime = actualNodeTime = 0;
        planDate = dateCompleted = null;
        listener = null;
        planTimeEditable = dateCompletedEditable = false;
        planTimeNull = true;
        data = null;
    }

    /** Add a child task to this EVTask. */
    public void add(EVTask child) {
        if (children.contains(child))
            return;

        child.parent = this;
        children.add(child);
    }

    /** Add a child task to this EVTask. */
    public void add(int pos, EVTask child) {
        if (children.contains(child))
            return;

        child.parent = this;
        children.add(pos, child);
    }

    public int remove(EVTask child) {
        int pos = children.indexOf(child);
        if (pos != -1)
            children.remove(pos);
        return pos;
    }

    public void moveUp(int childPos) {
        if (childPos > 0 && childPos < children.size()) {
            Object a = children.get(childPos-1);
            Object b = children.get(childPos);

            children.set(childPos-1, b);
            children.set(childPos,   a);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof EVTask)) return false;
        EVTask that = (EVTask) obj;
        return (this.fullName == that.fullName ||
                (this.fullName != null && fullName.equals(that.fullName)));
    }

    /** Creates an EVTask for the tasks in the hierarchy at the given path */
    public EVTask(String hierarchyPath, DataRepository data,
                  PSPProperties hierarchy, Listener listener) {
        this(null, hierarchyPath.substring(1), hierarchyPath, null, data,
             hierarchy, listener);
    }


    protected EVTask(EVTask parent, String name, String fullName,
                     PropertyKey key, DataRepository data,
                     PSPProperties hierarchy, Listener listener)
    {
        this.parent = parent;
        this.name = name;
        this.fullName = fullName;
        this.data = data;
        this.listener = listener;

        if (getValue(IGNORE_PLAN_TIME_NAME) != null)
            ignorePlanTime();
        else
            setPlanTime(getValue(PLAN_TIME_DATA_NAME));

        setActualTime(getValue(ACT_TIME_DATA_NAME));

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
                children.add(new EVTask(this, child.name(), child.path(),
                                        child, data, hierarchy, listener));
                addedChild = true;
            }
        }
        return addedChild;
    }

    public EVTask(Element e) { this(e, null); }
    private EVTask(Element e, String parentName) {
        name = e.getAttribute("name");
        fullName = (parentName == null ? "" : parentName + "/" + name);

        planTime = EVSchedule.getXMLNum(e, "pt");
        topDownPlanTime = bottomUpPlanTime = planTime;
        actualTime = EVSchedule.getXMLNum(e, "at");
        planDate = EVSchedule.getXMLDate(e, "pd");
        dateCompleted = EVSchedule.getXMLDate(e, "cd");

        NodeList subTasks = e.getChildNodes();
        for (int i=0;   i < subTasks.getLength();   i++) {
            Node n = subTasks.item(i);
            if (n instanceof Element &&
                "task".equals(((Element) n).getTagName()))
                add(new EVTask((Element) n, fullName));
        }
    }

    protected SimpleData getValue(String name) {
        String dataName = data.createDataName(fullName, name);
        if (listener != null)
            data.addDataListener(dataName, this, false);
        return data.getSimpleValue(dataName);
    }


    public boolean plannedTimeIsEditable() { return planTimeEditable; }
    public boolean completionDateIsEditable() {
        return isLeaf() && dateCompletedEditable;
    }

    protected void setPlanTime(SimpleData time) {
        if (time instanceof NumberData) {
            if (!ignorePlanTimeValue) {
                topDownPlanTime = ((NumberData) time).getDouble();
                if (Double.isNaN(topDownPlanTime) ||
                    Double.isInfinite(topDownPlanTime))
                    topDownPlanTime = 0.0;
                planTimeEditable = time.isEditable();
            }
        } else {
            planTimeNull = (time == null);
            topDownPlanTime = 0;
            planTimeEditable = true;
        }
    }

    protected void ignorePlanTime() {
        ignorePlanTimeValue = true;
        topDownPlanTime = 0;
        planTimeNull = true;
        planTimeEditable = false;
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

    public void setPlanTime(Object aValue) {
        if (plannedTimeIsEditable() && aValue instanceof String) {
            if (((String) aValue).length() > 0) {
                // parse the value to obtain a number of minutes
                long planTime = TimeLogEditor.parseTime((String) aValue);
                if (planTime != -1) {
                    this.planTime = topDownPlanTime =
                        bottomUpPlanTime = planTime;
                    // save those minutes to the data repository
                    data.putValue(data.createDataName(fullName,
                                                      PLAN_TIME_DATA_NAME),
                                  new DoubleData(planTime, true));
                }
            } else {
                this.planTime = topDownPlanTime = bottomUpPlanTime;
                data.putValue(data.createDataName(fullName,
                                                  PLAN_TIME_DATA_NAME),
                              null);
            }
        }
    }
    public void setActualDate(Object aValue) {
        if (completionDateIsEditable()) {
            String dataName =
                data.createDataName(fullName, DATE_COMPLETED_DATA_NAME);

            // save the Date object to the data repository
            if (aValue instanceof Date) {
                dateCompleted = (Date) aValue;
                data.putValue(dataName, new DateData(dateCompleted, true));
            } else {
                dateCompleted = null;
                data.putValue(dataName, null);
            }
        }
    }

    protected static NumberFormat percentFormatter =
        NumberFormat.getPercentInstance();
    static {
        percentFormatter.setMaximumFractionDigits(1);
    }
    static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent))
            percent = 0;
        return percentFormatter.format(percent);
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
    public int getChildIndex(Object child) { return children.indexOf(child); }
    public boolean isLeaf() { return children.isEmpty(); }
    public EVTask getChild(int pos) { return (EVTask) children.get(pos); }
    public EVTask getParent() { return parent; }
    public String toString() { return name; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getPlanTime() { return formatTime(planTime); }
    public boolean hasPlanTimeError() {
        return (Math.abs(planTime - bottomUpPlanTime) > 0.5);
    }
    public String getPlanTimeError() {
        if (!hasPlanTimeError()) return null;
        return "top-down/bottom-up mismatch (bottom-up = " +
            formatTime(bottomUpPlanTime) + ")";
    }
    public String getActualTime() { return formatTime(actualTime); }
    public String getPlanValue(double totalPlanTime) {
        return formatPercent(planTime/totalPlanTime); }
    public String getCumPlanTime() { return formatTime(cumPlanTime); }
    public String getCumPlanValue(double totalPlanTime) {
        return formatPercent(cumPlanTime/totalPlanTime); }
    public Date getPlanDate() { return planDate; }
    public Date getActualDate() { return dateCompleted; }
    public String getValueEarned(double totalPlanTime) {
        if (dateCompleted != null || valueEarned != 0.0)
            return formatPercent(valueEarned/totalPlanTime);
        else
            return "";
    }


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
        if (isLeaf())
            list.add(this);
        else
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).getLeafTasks(list);
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
        if (effDate == null) effDate = new Date();
        recalcPlanDates(schedule);
        for (int i = log.v.size();   i-- > 0;   )
            saveTimeLogInfo(schedule, (TimeLogEntry) log.v.get(i));
        schedule.setEffectiveDate(effDate);
        schedule.getMetrics().reset(schedule.getStartDate(), effDate,
                                    schedule.getPeriodStart(effDate),
                                    schedule.getPeriodEnd(effDate));
        recalcMetrics(schedule.getMetrics());
        schedule.getMetrics().recalcComplete(schedule);
        schedule.firePreparedEvents();
    }

    public void simpleRecalc() {
        recalcPlanCumTime(0.0);
        recalcPlanValue();
    }

    protected void resetRootValues() {
        planTime = cumPlanTime = actualTime = valueEarned =
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

            if (bottomUpPlanTime == 0 ||
                (!planTimeNull && topDownPlanTime > 0))
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
            cumPlanTime = prevCumTime + planTime;
        else {
            // for nonleaves, ask each of our children to recalc.
            cumPlanTime = prevCumTime;
            for (int i = 0;   i < getNumChildren();   i++)
                cumPlanTime = getChild(i).recalcPlanCumTime(cumPlanTime);

            /*
            // If we didn't really have a value for planTime, compute it
            // from our children.
            if (planTimeNull) {
                planTime = cumPlanTime - prevCumTime;
                planTimeEditable = false;
            }
            */
        }
        return cumPlanTime;
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
        else {
            valueEarned = 0;
            // for nonleaves, ask each of our children to recalc.
            for (int i = 0;   i < getNumChildren();   i++) {
                getChild(i).recalcPlanValue();
                valueEarned += getChild(i).valueEarned;
            }
        }
    }

    public Date recalcDateCompleted() {
        if (isLeaf()) return dateCompleted;

        Date d, result = getChild(0).recalcDateCompleted();
        for (int i = 1;   i < getNumChildren();   i++) {
            d = getChild(i).recalcDateCompleted();
            if (d == null)
                result = null;
            else if (result != null && result.compareTo(d) < 0)
                result = d;
        }
        dateCompletedEditable = false;
        return (dateCompleted = result);
    }

    public void recalcPlanDates(EVSchedule schedule) {
        if (isLeaf()) {
            planDate = schedule.getPlannedCompletionDate
                (cumPlanTime, cumPlanTime);
            if (dateCompleted != null)
                schedule.saveCompletedTask(dateCompleted, planTime);
        } else {
            for (int i = getNumChildren();   i-- > 0;   )
                getChild(i).recalcPlanDates(schedule);
            planDate = getChild(getNumChildren()-1).planDate;
        }
    }

    public void recalcMetrics(EVMetrics metrics) {
        if (isLeaf())
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

        if (hasPlanTimeError())
            metrics.addError("The top-down estimate of " + getPlanTime() +
                             " for task \"" + fullName + "\" does not " +
                             "agree with the bottom-up estimate of " +
                             formatTime(bottomUpPlanTime) + ".", this);
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

    /** If this node is the root node of an EVTaskList rollup, this will
     *  recalculate it.
     *
     * <b>Important:</b> the children of this node (which are
     * themselves root nodes of other EVTaskLists) should already be
     * recalculated before calling this method.
     */
    public void recalcRollupNode() {
        EVTask child;

        planTime = cumPlanTime = actualTime = 0.0;
        topDownPlanTime = bottomUpPlanTime = 0.0;
        valueEarned = 0.0;
        planDate = null;
        dateCompleted = EVSchedule.A_LONG_TIME_AGO;

        for (int i = children.size();   i-- > 0;  ) {
            child = getChild(i); // For each child,

            // accumulate numeric task data.
            planTime += child.planTime;
            cumPlanTime += child.cumPlanTime;
            actualTime += child.actualTime;
            valueEarned += child.valueEarned;
            topDownPlanTime += child.topDownPlanTime;
            bottomUpPlanTime += child.bottomUpPlanTime;

            // rollup plan date should be the max of all the plan dates.
            planDate = EVScheduleRollup.maxDate(planDate, child.planDate);

            // rollup completion date should be the max of all the
            // completion dates, unless one or more of them is null -
            // then it should be null.
            if (child.dateCompleted == null)
                dateCompleted = null;
            else if (dateCompleted != null)
                dateCompleted = EVScheduleRollup.maxDate
                    (dateCompleted, child.dateCompleted);
        }
        if (dateCompleted == EVSchedule.A_LONG_TIME_AGO)
            dateCompleted = null;
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
            .append("' pt='").append(planTime)
            .append("' at='").append(actualTime);
        if (planDate != null)
            result.append("' pd='").append(EVSchedule.saveDate(planDate));
        if (dateCompleted != null)
            result.append("' cd='").append(EVSchedule.saveDate(dateCompleted));

        if (isLeaf())
            result.append("'/>");
        else {
            result.append("'>");
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).saveToXML(result);
            result.append("</task>");
        }
    }
}
