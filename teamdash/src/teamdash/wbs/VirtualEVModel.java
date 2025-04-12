// Copyright (C) 2025 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMember;
import teamdash.team.WeekData;
import teamdash.team.WeeklySchedule;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class VirtualEVModel {

    /** The team project */
    private TeamProject teamProject;

    /** The WBS model for the project */
    private WBSModel wbs;

    /** The team member we are calculating EV for */
    private TeamMember m;


    public VirtualEVModel(TeamProject teamProject, TeamMember m) {
        this.teamProject = teamProject;
        this.wbs = teamProject.getWBS();
        this.m = m;
    }



    private TaskData taskRoot;

    private double totalPlanTime;

    private Date planFinishDate;

    public void recalc() {
        buildTaskTree();
    }



    private String timeAttr, assignedAttr;

    private void buildTaskTree() {
        // prepare frequently used values
        timeAttr = TeamMemberTimeColumn.getMemberNodeDataAttrName(m);
        assignedAttr = TeamTimeColumn.getMemberAssignedZeroAttrName(m);

        // scan the WBS and build tasks for the items assigned to this member
        TaskData tree = buildTaskTree(wbs.getRoot());

        // if nothing is assigned to this member, create an empty root task
        if (tree == null) {
            tree = new TaskData(wbs.getRoot());
            tree.planTime = 0;
            tree.children = Collections.EMPTY_LIST;
        }

        // read the total plan time from the root node
        totalPlanTime = tree.planTime;

        // calculate the date when that effort could be completed
        planFinishDate = m.getSchedule().getDateForEffort(totalPlanTime);
        if (planFinishDate == null)
            planFinishDate = NEVER;
        else
            planFinishDate = truncDate(planFinishDate);

        // save the tree we built
        taskRoot = tree;
    }

    private TaskData buildTaskTree(WBSNode node) {
        WBSNode[] children = wbs.getChildren(node);
        boolean isLeaf = (children.length == 0);
        if (isLeaf) {
            // create a task if this leaf is assigned to the given team member
            double nodePlanTime = node.getNumericAttribute(timeAttr);
            boolean isAssigned = node.getAttribute(assignedAttr) != null;
            if (nodePlanTime > 0 || isAssigned) {
                TaskData result = new TaskData(node);
                result.planTime = nodePlanTime > 0 ? nodePlanTime : 0;
                result.children = Collections.EMPTY_LIST;
                return result;
            } else {
                // if this leaf is not assigned to the given team member,
                // exclude it from the tree we build
                return null;
            }

        } else {
            // recurse over children to see if any are assigned to this person
            TaskData parent = null;
            for (WBSNode child : children) {
                TaskData childTask = buildTaskTree(child);
                if (childTask != null) {
                    // if we find at least one assigned child, build a parent
                    // and add the children to it
                    if (parent == null)
                        parent = new TaskData(node);
                    parent.addChild(childTask);
                }
            }

            return parent;
        }
    }



    public void writeEV(OutputStream out) throws IOException {
        // start the XML document and write root tag
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, ENCODING);
        xml.startDocument(ENCODING, Boolean.TRUE);
        xml.startTag(null, "evSchedules");

        // write <evSchedule> tag
        xml.startTag(null, "evSchedule");
        xml.attribute(null, "name", teamProject.getProjectName());

        // write <EVModel> tag
        String taskListID = getTaskListID(m);
        xml.startTag(null, "EVModel");
        xml.attribute(null, "rct", "false");
        xml.attribute(null, "tlid", taskListID);
        xml.attribute(null, "tz", TimeZone.getDefault().getID());

        // write top-level <task> tag representing the task list root
        startTaskTag(xml, taskRoot, "TL-" + taskListID);
        xml.attribute(null, "who", m.getName());
        xml.attribute(null, "flag", "plain");

        // recursively write tasks for project
        writeTaskTree(xml, taskRoot);

        // finish top-level task tag
        xml.endTag(null, "task");

        // write the schedule block
        writeSchedulePeriods(xml);

        // finish the document
        xml.endTag(null, "EVModel");
        xml.endTag(null, "evSchedule");
        xml.endTag(null, "evSchedules");
        xml.endDocument();
    }

    private String getTaskListID(TeamMember m) {
        return teamProject.getProjectID() + ".00"
                + Long.toString(m.getName().hashCode(), Character.MAX_RADIX);
    }

    private void startTaskTag(XmlSerializer xml, TaskData task, String tid)
            throws IOException {
        xml.startTag(null, "task");
        xml.attribute(null, "name", task.node.getName());
        xml.attribute(null, "pt", xmlTime(task.planTime));
        xml.attribute(null, "at", ZERO_TIME);

        if (tid == null) {
            int nid = task.node.getTreeNodeID();
            tid = teamProject.getProjectID() + ":" //
                    + (nid > 0 ? nid : "root");
        }
        xml.attribute(null, "tid", tid);

        xml.attribute(null, "nt", task.getNodeType());
    }

    private void writeTaskTree(XmlSerializer xml, TaskData task)
            throws IOException {
        startTaskTag(xml, task, null);
        for (TaskData child : task.children)
            writeTaskTree(xml, child);
        xml.endTag(null, "task");
    }



    private void writeSchedulePeriods(XmlSerializer xml) throws IOException {
        // write the opening <schedule> tag
        Date startDate = truncDate(m.getStartDate());
        xml.startTag(null, "schedule");
        xml.attribute(null, "tpt", xmlTime(totalPlanTime));
        xml.attribute(null, "evt", ZERO_TIME);
        xml.attribute(null, "at", ZERO_TIME);
        xml.attribute(null, "pt", ZERO_TIME);
        xml.attribute(null, "it", ZERO_TIME);
        xml.attribute(null, "start", xmlDate(startDate));
        xml.attribute(null, "eff", xmlDate(startDate));

        // print the periods in the schedule
        WeeklySchedule schedule = m.getSchedule();
        int startWeek = schedule.getStartWeek();
        int stopWeek = getStopWeek(schedule);
        double cumPlanTime = 0;
        boolean weekNeeded = false;
        for (int week = startWeek - 1; week <= stopWeek || weekNeeded; week++) {
            WeekData wd = schedule.getWeekData(week);
            if (wd.getType() == WeekData.TYPE_OUTSIDE_SCHEDULE)
                break;

            double weekHours = wd.getHours();
            Date weekEnd = truncDate(schedule.weekValueToDate(week + 1));

            if (cumPlanTime + weekHours > totalPlanTime)
                weekHours = Math.max(0, totalPlanTime - cumPlanTime);
            cumPlanTime += weekHours;

            // write XML for this weekly period
            xml.startTag(null, "period");
            xml.attribute(null, "end", xmlDate(weekEnd));
            xml.attribute(null, "pt", xmlTime(weekHours));
            xml.attribute(null, "at", ZERO_TIME);
            xml.attribute(null, "cpv", xmlTime(cumPlanTime));
            xml.attribute(null, "cev", ZERO_TIME);
            xml.attribute(null, "cac", ZERO_TIME);
            xml.endTag(null, "period");

            // our schedule must end with a final zero row to prevent EV logic
            // from using it as a work balancing target during optimized plan
            // calculations. So if the week we just wrote had nonzero hours, set
            // a flag telling the for() loop to write at least one more period
            weekNeeded = (weekHours > 0);
        }

        // close the <schedule> tag
        xml.endTag(null, "schedule");
    }

    private int getStopWeek(WeeklySchedule schedule) {
        if (planFinishDate != NEVER)
            return ((int) schedule.dateToDoubleWeekValue(planFinishDate));
        else
            return schedule.getMaintenanceStartWeek();
    }



    private static String xmlTime(double time) {
        if (time > 0)
            return Double.toString(time * 60);
        else
            return ZERO_TIME;
    }

    private static String xmlDate(Date d) {
        if (d != null)
            return "@" + d.getTime();
        else
            return "";
    }

    private static Date truncDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date min(Date a, Date b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return (a.before(b) ? a : b);
    }

    private static Date max(Date a, Date b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return (a.after(b) ? a : b);
    }



    private static class TaskData {

        WBSNode node;

        double planTime;

        TaskData parent;

        List<TaskData> children;

        TaskData(WBSNode node) {
            this.node = node;
        }

        void addChild(TaskData child) {
            this.planTime += child.planTime;

            child.parent = this;

            if (children == null)
                children = new ArrayList();
            children.add(child);
        }

        String getNodeType() {
            String type = null;
            if (children == null || children.isEmpty())
                type = node.getType();

            if (type != null && type.endsWith(TeamProcess.TASK_SUFFIX))
                return type.substring(0,
                    type.length() - TeamProcess.TASK_SUFFIX.length());
            else
                return "";
        }
    }



    private static final String ENCODING = "UTF-8";

    private static final String ZERO_TIME = "0.0";

    private static final Date NEVER = new Date(Long.MAX_VALUE);

}
