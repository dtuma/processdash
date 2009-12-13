// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlSerializer;


public class MSProjectXmlWriter {

    private DataContext metadata;

    private EVTask root;

    private Date statusDate;

    private GanttDateStyle dateStyle;

    public MSProjectXmlWriter() {
        statusDate = new Date();
        dateStyle = GanttDateStyle.REPLAN;
    }

    public void setMetadata(DataContext metadata) {
        this.metadata = metadata;
    }

    public EVTask getRoot() {
        return root;
    }

    public void setRoot(EVTask root) {
        this.root = root;
    }

    public Date getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }

    public GanttDateStyle getDateStyle() {
        return dateStyle;
    }

    public void setDateStyle(GanttDateStyle dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setDateStyle(String dateStyle) {
        try {
            setDateStyle(GanttDateStyle.valueOf(dateStyle.toUpperCase()));
        } catch (Exception e) {}
    }

    public void setTaskList(EVTaskList tl) {
        setRoot(tl.getTaskRoot());
        setStatusDate(tl.getSchedule().getEffectiveDate());
    }



    private XmlSerializer xml;

    private Date startDate;

    private UidMapper taskUidMapper;

    private UidMapper personUidMapper;

    private Map<EVTask, Integer> tasksByObject;

    private Map<String, EVTask> tasksByKey;

    private Map<String, Integer> tasksByTid;

    public void write(OutputStream out) throws IOException {
        xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, ENCODING);
        xml.startDocument(ENCODING, null);

        xml.startTag(null, PROJECT_TAG);
        xml.attribute(null, "xmlns", MS_PROJECT_NAMESPACE);

        prepareData();

        writeProjectLevelData();
        writeCalendars();
        writeTaskData();
        writeResourceData();
        writeAssignmentData();

        xml.endTag(null, PROJECT_TAG);
        xml.endDocument();
        xml = null;
    }


    private void prepareData() {
        startDate = new Date();
        taskUidMapper = new UidMapper(TASK_MAP_KEY);
        personUidMapper = new UidMapper(PERSON_MAP_KEY);
        tasksByObject = new HashMap();
        tasksByKey = new HashMap();
        tasksByTid = new HashMap();
        scanForGlobalData(root);
        taskUidMapper.save();
        personUidMapper.save();
        startDate = new Date(startDate.getTime() - DAY_MILLIS);
    }

    private void scanForGlobalData(EVTask task) {
        addResources(task.getAssignedTo());
        assignTaskUid(task);

        checkStartDate(task.getBaselineStartDate());
        checkStartDate(task.getPlanStartDate());
        checkStartDate(task.getReplanStartDate());
        checkStartDate(task.getForecastStartDate());
        checkStartDate(task.getActualStartDate());

        for (int i = 0;  i < task.getNumChildren(); i++)
            scanForGlobalData(task.getChild(i));
    }

    private void assignTaskUid(EVTask task) {
        if (!tasksByObject.containsKey(task)) {
            boolean shouldRegisterTaskIDs = false;

            String taskKey;
            List<String> taskIDs = task.getTaskIDs();
            if (taskIDs != null && !taskIDs.isEmpty()) {
                taskKey = taskIDs.get(0);
                shouldRegisterTaskIDs = true;
            } else {
                taskKey = task.getFullName();
            }
            String keyToUse = taskKey;

            EVTask other = tasksByKey.get(taskKey);
            if (other != null) {
                shouldRegisterTaskIDs = false;
                if (other == task.getParent()) {
                    String who = task.getAssignedToText();
                    int personId = personUidMapper.getIdNoCreate(who);
                    if (personId > 0) {
                        keyToUse = taskKey = taskKey + ";" + personId;
                        other = tasksByKey.get(taskKey);
                    }
                }
                int num = 2;
                while (other != null) {
                    keyToUse = taskKey + "#" + num++;
                    other = tasksByKey.get(keyToUse);
                }
            }

            int id = taskUidMapper.getId(keyToUse);
            tasksByObject.put(task, id);
            tasksByKey.put(keyToUse, task);
            if (shouldRegisterTaskIDs)
                registerTaskIds(task, id);
        }
    }

    private void registerTaskIds(EVTask task, int uid) {
        List<String> taskIDs = task.getTaskIDs();
        if (taskIDs != null) {
            for (String oneTaskId : taskIDs) {
                Integer oldVal = tasksByTid.put(oneTaskId, uid);
                // don't overwrite task ID mappings! They probably belong to
                // a parent, whose mapping takes priority.  If we detect that
                // we just overwrote a mapping (shouldn't happen), restore
                // the original value
                if (oldVal != null)
                    tasksByTid.put(oneTaskId, oldVal);
            }
        }
    }

    private void checkStartDate(Date d) {
        if (d != null)
            this.startDate = EVCalculator.minStartDate(this.startDate, d);
    }

    private void addResources(List<String> assignedTo) {
        if (assignedTo != null) {
            for (String oneName : assignedTo) {
                personUidMapper.getId(oneName);
            }
        }
    }

    private void writeProjectLevelData() throws IOException {
        writeBool("ScheduleFromStart", true);
        writeTruncDate("StartDate", startDate);
        writeInt("CurrencyDigits", 0);
        writeString("CurrencySymbol", "Hours");
        writeInt("CurrencySymbolPosition", CURRENCY_POS_AFTER_WITH_SPACE);
        writeInt("CalendarUID", 1);
        writeString("DefaultStartTime", "00:00:00");
        writeString("DefaultFinishTime", "23:59:59");
        writeInt("MinutesPerDay", DAY_MINUTES);
        writeInt("MinutesPerWeek", DAY_MINUTES * 7);
        writeInt("DaysPerMonth", 30);
        writeBool("HonorConstraints", true);
        writeDate("StatusDate", statusDate);
        writeDate("CurrentDate", statusDate);
        writeInt("NewTaskStartDate", NEW_TASKS_START_ON_CURRENT_DATE);
    }

    private void writeCalendars() throws IOException {
        xml.startTag(null, CALENDARS_TAG);

        // write the base calendar
        xml.startTag(null, CALENDAR_TAG);
        writeInt("UID", 1);
        writeString("Name", "24 Hours");
        writeBool("IsBaseCalendar", true);
        writeInt("BaseCalendarUID", -1);
        xml.startTag(null, "WeekDays");
        for (int i = 1;  i <= 7;  i++) {
            xml.startTag(null, "WeekDay");
            writeInt("DayType", i);
            writeBool("DayWorking", true);
            xml.startTag(null, "WorkingTimes");
            xml.startTag(null, "WorkingTime");
            writeString("FromTime", "00:00:00");
            writeString("ToTime", "00:00:00");
            xml.endTag(null, "WorkingTime");
            xml.endTag(null, "WorkingTimes");
            xml.endTag(null, "WeekDay");
        }
        xml.endTag(null, "WeekDays");
        xml.endTag(null, CALENDAR_TAG);

        // write a calendar for each individual
        for (Entry<String, Integer> e : personUidMapper.cache.entrySet()) {
            String resourceName = e.getKey();
            int calId = getCalendarUid(e.getValue());
            xml.startTag(null, CALENDAR_TAG);
            writeInt("UID", calId);
            writeString("Name", resourceName);
            writeBool("IsBaseCalendar", false);
            writeInt("BaseCalendarUID", 1);
            xml.endTag(null, CALENDAR_TAG);
        }

        xml.endTag(null, CALENDARS_TAG);
    }

    private void writeTaskData() throws IOException {
        xml.startTag(null, TASKS_TAG);

        // write a "task" element for an imaginary top-level node. (This
        // top-level node is not displayed by MS Project.)
        xml.startTag(null, TASK_TAG);
        writeInt("UID", 0);
        writeString("Name", "Process Dashboard Exported Schedule");
        writeInt("OutlineLevel", 0);
        writeBool("Summary", true);
        writeInt("CalendarUID", 1);
        xml.endTag(null, TASK_TAG);

        // write task elements for all of the tasks in this schedule.
        writeTaskData(root, 1);

        xml.endTag(null, TASKS_TAG);
    }

    private void writeTaskData(EVTask task, int depth) throws IOException {
        xml.startTag(null, TASK_TAG);
        writeInt("UID", getTaskUid(task));
        writeString("Name", task.getName());
        writeInt("OutlineLevel", depth);

        if (task.isLeaf()) {
            Date startDate = filterDate(dateStyle.getStartDate(task));
            Date finishDate = filterDate(dateStyle.getFinishDate(task));

            if (finishDate != null) {
                writeDate("Start", startDate);
                writeDate("Finish", finishDate);
            }
//            writeDuration("Duration", calculateDuration(startDate, finishDate));
//            writeDuration("Work", task.getPlanTime());
            writeBool("EffortDriven", true);
            writeBool("Summary", false);
            writeCost("Cost", task.getPlanDirectTime());
            writeDate("ActualStart", task.getActualStartDate());
            writeDate("ActualFinish", task.getDateCompleted());
            writeCost("ActualCost", task.getActualDirectTime());
            writeInt("FixedCostAccrual", 3);
//            writeDuration("ActualWork", task.getActualTime());
            if (finishDate != null) {
                writeInt("ConstraintType", CONSTRAINT_FINISH_NO_EARLIER_THAN);
                writeDate("ConstraintDate", finishDate);
            }

        } else {
            writeBool("Summary", true);
        }
        writeTaskPredecessors(task);
        if (shouldWriteBaseline(task)) {
            xml.startTag(null, BASELINE_TAG);
            writeInt("Number", 0);
            writeTruncDate("Start", task.getBaselineStartDate());
            writeTruncDate("Finish", task.getBaselineDate());
            writeDuration("Work", task.getPlanDirectTime());
            writeCost("Cost", task.getPlanDirectTime());
            xml.endTag(null, BASELINE_TAG);
        }

        xml.endTag(null, TASK_TAG);

        for (int i = 0; i < task.getNumChildren(); i++)
            writeTaskData(task.getChild(i), depth + 1);
    }

    private void writeTaskPredecessors(EVTask task) throws IOException {
        List<String> dependentTids = task.getDependentTaskIDs();
        if (dependentTids == null || dependentTids.isEmpty())
            return;

        Set<Integer> dependentUids = new HashSet();
        for (String tid : dependentTids)
            dependentUids.add(tasksByTid.get(tid));

        for (Integer uid : dependentUids)
            if (uid != null)
                writeTaskPredecessor(uid);
    }


    private void writeTaskPredecessor(int uid) throws IOException {
        xml.startTag(null, "PredecessorLink");
        writeInt("PredecessorUID", uid);
        writeInt("Type", PREDECESSOR_FINISH_TO_START);
        xml.endTag(null, "PredecessorLink");
    }

    private void writeResourceData() throws IOException {
        xml.startTag(null, RESOURCES_TAG);
        for (Entry<String, Integer> s : personUidMapper.cache.entrySet())
            writeResourceData(s.getKey(), s.getValue());
        xml.endTag(null, RESOURCES_TAG);
    }

    private void writeResourceData(String name, Integer uid) throws IOException {
        xml.startTag(null, RESOURCE_TAG);
        writeInt("UID", uid);
        writeString("Name", name);
        writeInt("CalendarUID", getCalendarUid(uid));
        xml.endTag(null, RESOURCE_TAG);
    }

    private void writeAssignmentData() throws IOException {
        xml.startTag(null, ASSIGNMENTS_TAG);
        writeAssignmentData(root);
        xml.endTag(null, ASSIGNMENTS_TAG);
    }

    private void writeAssignmentData(EVTask task) throws IOException {
        if (task.isLeaf()) {
            int taskId = getTaskUid(task);
            int personId = getPersonUid(task.getAssignedToText());
            if (personId > 0) {
                xml.startTag(null, ASSIGNMENT_TAG);
                writeInt("UID", taskId);
                writeInt("TaskUID", taskId);
                writeInt("ResourceUID", personId);
                Date actualFinish = task.getDateCompleted();
                writeDate("ActualFinish", actualFinish);
                writeDate("ActualStart", task.getActualStartDate());
//                writeDuration("ActualWork", task.getActualDirectTime()); // causes crash!
//                if (actualFinish != null)
//                    writeCost("ACWP", task.getActualDirectTime());
                writeDate("Finish", filterDate(dateStyle.getFinishDate(task)));
                writeDate("Start", filterDate(dateStyle.getStartDate(task)));
//                writeDuration("Work", task.getPlanDirectTime()); // causes crash!
//                if (before(task.getPlanDate(), statusDate))
//                    writeCost("BCWS", task.getPlanDirectTime());
//                if (actualFinish != null)
//                    writeCost("BCWP", task.getPlanDirectTime());

                if (shouldWriteBaseline(task)) {
                    xml.startTag(null, BASELINE_TAG);
                    writeInt("Number", 0);
                    writeTruncDate("Start", task.getBaselineStartDate());
                    writeTruncDate("Finish", task.getBaselineDate());
                    writeCost("Cost", task.getPlanDirectTime());
                    xml.endTag(null, BASELINE_TAG);
                }

                xml.endTag(null, ASSIGNMENT_TAG);
            }
        } else {
            for (int i = 0; i < task.getNumChildren(); i++)
                writeAssignmentData(task.getChild(i));
        }
    }

    private int getTaskUid(EVTask task) {
        return tasksByObject.get(task);
    }
    private int getPersonUid(String name) {
        return personUidMapper.getIdNoCreate(name);
    }
    private int getCalendarUid(int personUid) {
        return personUid + 1;
    }

    /*
    private double calculateDuration(Date start, Date finish) {
        if (start == null || finish == null)
            return DAY_MINUTES;
        else
            return Math.abs(finish.getTime() - start.getTime()) / MINUTE_MILLIS;
    }
     */

    private boolean shouldWriteBaseline(EVTask task) {
        if (dateStyle == GanttDateStyle.BASELINE)
            return false;
        else
            return task.getBaselineDate() != null;
    }


    private Date filterDate(Date d) {
        if (d == EVSchedule.NEVER)
            return null;
        else
            return d;
    }

    private void writeTruncDate(String tag, Date d) throws IOException {
        if (d != null)
            writeString(tag, DATE_FMT.format(d) + "T00:00:00");
    }

    private void writeDate(String tag, Date d) throws IOException {
        if (d != null) {
            String date = DATE_TIME_FMT.format(d);
            writeString(tag, date);
        }
    }

    private static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");
    private static final DateFormat DATE_TIME_FMT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");

    private void writeCost(String tag, double minutes) throws IOException {
        // MS Project appears to express cost as an integer with two implied
        // decimal points.  In our case, the units of cost are hours, so one
        // hour is represented as "100".
        writeInt(tag, (int) (minutes * 100 / 60.0));
    }

    /*
    private void writeDouble(String tag, double d) throws IOException {
        if (!Double.isInfinite(d) && !Double.isNaN(d))
            writeString(tag, Double.toString(d));
    }
    */

    private void writeInt(String tag, int i) throws IOException {
        writeString(tag, Integer.toString(i));
    }

    private void writeBool(String tag, boolean b) throws IOException {
        writeString(tag, b ? "1" : "0");
    }

    void writeDuration(String tag, double time) throws IOException {
        int hours = (int) (time / 60);
        time = time - 60 * hours;
        int mins = (int) (time);
        writeString(tag, "PT" + hours + "H" + mins + "M0S");
    }

    private void writeString(String tag, String str) throws IOException {
        xml.startTag(null, tag);
        xml.text(str);
        xml.endTag(null, tag);
    }

    private class UidMapper {

        String key;

        private int initialVal;

        ListData strings;

        Map<String, Integer> cache;

        boolean dirty;

        public UidMapper(String key) {
            this.key = key;
            this.initialVal = 1;
            if (metadata != null)
                this.strings = ListData.asListData(metadata.getValue(key));
            if (this.strings == null)
                this.strings = new ListData();

            this.cache = new HashMap<String, Integer>();
            for (int i = 0;  i < strings.size();  i++)
                cache.put((String) strings.get(i), i + initialVal);
            this.dirty = false;
        }

        public int getId(String s) {
            Integer result = cache.get(s);
            if (result == null) {
                result = strings.size() + initialVal;
                strings.add(s);
                cache.put(s, result);
                dirty = true;
            }
            return result;
        }

        public int getIdNoCreate(String s) {
            Integer result = cache.get(s);
            return (result == null ? -1 : result);
        }

        public void save() {
            if (dirty && metadata != null) {
                metadata.putValue(key, strings);
                dirty = false;
            }
        }

    }

    private static final String ENCODING = "UTF-8";

    private static final String MS_PROJECT_NAMESPACE = "http://schemas.microsoft.com/project";

    private static final String PROJECT_TAG = "Project";

    private static final String CALENDARS_TAG = "Calendars";

    private static final String CALENDAR_TAG = "Calendar";

    private static final String TASKS_TAG = "Tasks";

    private static final String TASK_TAG = "Task";

    private static final String BASELINE_TAG = "Baseline";

    private static final String RESOURCES_TAG = "Resources";

    private static final String RESOURCE_TAG = "Resource";

    private static final String ASSIGNMENTS_TAG = "Assignments";

    private static final String ASSIGNMENT_TAG = "Assignment";

    private static final int DAY_MINUTES = 24 /*hours*/ * 60 /*minutes*/;

    private static final long MINUTE_MILLIS = 60 /*seconds*/ * 1000 /*millis*/;

    private static final long DAY_MILLIS = DAY_MINUTES * MINUTE_MILLIS;

    /** predecessor type indicating a "finish to start" task dependency */
    private static final int PREDECESSOR_FINISH_TO_START = 1;

    /** project attribute indicating that new tasks should begin on the
     * current date (rather than the project start date) */
    private static final int NEW_TASKS_START_ON_CURRENT_DATE = 1;

    /** project attribute indicating that the currency symbol should be
     * displayed after a cost value, with a space between */
    private static final int CURRENCY_POS_AFTER_WITH_SPACE = 3;

    /** constraint type indicating that a task should finish no earlier
     * that the constraint date */
    private static final int CONSTRAINT_FINISH_NO_EARLIER_THAN = 6;

    private static final String TASK_MAP_KEY = "Task_ID_Map";

    private static final String PERSON_MAP_KEY = "Person_ID_Map";

}
