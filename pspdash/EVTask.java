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
import java.util.Vector;

import pspdash.data.DataRepository;
import pspdash.data.DataListener;
import pspdash.data.DataEvent;
import pspdash.data.SimpleData;
import pspdash.data.NumberData;
import pspdash.data.DoubleData;
import pspdash.data.DateData;

public class EVTask implements DataListener {

    public static final String PLAN_TIME_DATA_NAME      = "Estimated Time";
    public static final String ACT_TIME_DATA_NAME       = "Time";
    public static final String DATE_COMPLETED_DATA_NAME = "Completed";

    public interface Listener {
        public void fireEVNodeChanged(EVTask node);
    }

    EVTask parent = null;
    ArrayList children = new ArrayList();

    String name, fullName;
    double planTime, planValue, cumPlanTime, cumPlanValue,
        actualTime, valueEarned;
    Date planDate, dateCompleted;
    boolean planTimeEditable, planTimeNull, dateCompletedEditable;
    Listener listener;

    DataRepository data;

    /** Creates an EVTask suitable for the root of an EVTaskList. */
    public EVTask(String rootName) {
        this.name = rootName;
        this.fullName = "";
        planTime = planValue = cumPlanTime = cumPlanValue =
            actualTime = valueEarned = 0;
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

        setPlanTime(getValue(PLAN_TIME_DATA_NAME));

        addChildrenFromHierarchy(fullName, key, data, hierarchy, listener);

        if (isLeaf()) {
            setActualDate(getValue(DATE_COMPLETED_DATA_NAME));
            setActualTime(getValue(ACT_TIME_DATA_NAME));
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
            planTime = ((NumberData) time).getDouble();
            if (Double.isNaN(planTime) || Double.isInfinite(planTime))
                planTime = 0.0;
            planTimeEditable = time.isEditable();
        } else {
            planTimeNull = (time == null);
            planTime = 0;
            planTimeEditable = true;
        }
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
            actualTime = ((NumberData) time).getDouble();
            if (Double.isNaN(actualTime) || Double.isInfinite(actualTime))
                actualTime = 0.0;
        } else {
            actualTime = 0;
        }
    }

    public void setPlanTime(Object aValue) {
        if (plannedTimeIsEditable() && aValue instanceof String) {
            // parse the value to obtain a number of minutes
            long planTime = TimeLogEditor.parseTime((String) aValue);
            if (planTime != -1) {
                this.planTime = planTime;
                // save those minutes to the data repository
                data.putValue(data.createDataName(fullName,
                                                  PLAN_TIME_DATA_NAME),
                              new DoubleData(planTime, true));
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
    static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent))
            percent = 0;
        return percentFormatter.format(percent);
    }


    static String formatTime(double time) {
        // time is in minutes.
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
    public String getActualTime() { return formatTime(actualTime); }
    public String getPlanValue(double totalPlanTime) {
        return formatPercent(planValue/totalPlanTime); }
    public String getCumPlanTime() { return formatTime(cumPlanTime); }
    public String getCumPlanValue(double totalPlanTime) {
        return formatPercent(cumPlanValue/totalPlanTime); }
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




    public void recalc(EVSchedule schedule, TimeLog log) {
        resetRootValues();
        recalcPlanCumTime(0.0);
        recalcActualTimes();
        recalcDateCompleted();
        recalcPlanValue();
        schedule.prepForEvents();
        schedule.cleanUp();
        Date now = new Date();
        recalcPlanDates(schedule);
        for (int i = log.v.size();   i-- > 0;   )
            saveTimeLogInfo(schedule, (TimeLogEntry) log.v.get(i));
        schedule.setEffectiveDate(now);
        schedule.getMetrics().reset(schedule.getStartDate(), now,
                                    schedule.getPeriodStart(now),
                                    schedule.getPeriodEnd(now));
        recalcMetrics(schedule.getMetrics());
        schedule.getMetrics().recalcComplete();
        schedule.firePreparedEvents();
    }

    protected void resetRootValues() {
        planTime = planValue = cumPlanTime = cumPlanValue =
            actualTime = valueEarned = 0;
        planDate = dateCompleted = null;
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

            // If we didn't really have a value for planTime, compute it
            // from our children.
            if (planTimeNull) {
                planTime = cumPlanTime - prevCumTime;
                planTimeEditable = false;
            }
        }
        return cumPlanTime;
    }

    public double recalcActualTimes() {
        if (!isLeaf()) {
            // for nonleaves, ask each of our children to recalc.
            actualTime = 0;
            for (int i = 0;   i < getNumChildren();   i++)
                actualTime += getChild(i).recalcActualTimes();
        }
        return actualTime;
    }

    public void recalcPlanValue() {
        planValue = planTime;
        cumPlanValue = cumPlanTime;

        if (isLeaf())
            valueEarned = (dateCompleted == null ? 0 : planValue);
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
                (cumPlanTime, cumPlanValue);
            if (dateCompleted != null)
                schedule.saveCompletedTask(dateCompleted, planValue);
        } else {
            for (int i = getNumChildren();   i-- > 0;   )
                getChild(i).recalcPlanDates(schedule);
            planDate = getChild(getNumChildren()-1).planDate;
        }
    }

    public void recalcMetrics(EVMetrics metrics) {
        if (isLeaf())
            metrics.addTask(planTime, actualTime, planDate, dateCompleted);
        else
            for (int i = getNumChildren();   i-- > 0;   )
                getChild(i).recalcMetrics(metrics);
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

    public void recalcLocal() {
        EVTask child;

        planTime = cumPlanTime = actualTime = 0.0;
        planValue = cumPlanValue = valueEarned = 0.0;
        planDate = null;
        dateCompleted = EVSchedule.A_LONG_TIME_AGO;

        for (int i = children.size();   i-- > 0;  ) {
            child = getChild(i); // For each child,

            // accumulate numeric task data.
            planTime += child.planTime;
            cumPlanTime += child.cumPlanTime;
            actualTime += child.actualTime;
            planValue += child.planValue;
            cumPlanValue += child.cumPlanValue;
            valueEarned += child.valueEarned;
            /*
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
            */
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
        if (listener != null) listener.fireEVNodeChanged(this);
    }
}
