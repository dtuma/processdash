// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ev.ui;


import java.io.IOException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.swing.table.TableModel;

import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.util.StringUtils;

import pspdash.TimeLogEditor;
import pspdash.TinyCGIBase;
import pspdash.TinyCGIException;
import pspdash.TinyWebServer;
import pspdash.data.DoubleData;


/** CGI script for displaying tasks due in the previous/next week
 */
public class EVWeekReport extends TinyCGIBase {

    private static final String EFF_DATE_PARAM = "eff";

    private double totalPlanTime;


    /** Generate CGI output. */
    protected void writeContents() throws IOException {

        // Get the name of the earned value model to report on.
        String taskListName = getPrefix();
        if (taskListName == null || taskListName.length() < 2)
            throw new IOException("No EV task list specified.");
        taskListName = taskListName.substring(1);

        // strip the "publishing prefix" if it is present.
        if (taskListName.startsWith("ev /"))
            taskListName = taskListName.substring(4);

        // Load and recalculate the named earned value model.
        EVTaskList evModel = EVTaskList.openExisting
            (taskListName,
             getDataRepository(),
             getPSPProperties(),
             getObjectCache(),
             false); // change notification not required
        if (evModel == null)
            throw new TinyCGIException(404, "Not Found",
                                       "No such task/schedule");
        evModel.recalc();
        EVSchedule schedule = evModel.getSchedule();
        EVMetrics  metrics = schedule.getMetrics();
        totalPlanTime = metrics.totalPlan();

        //FIXME: allow the user to change the effective date somehow.
        String effDateParam = getParameter(EFF_DATE_PARAM);
        Date effDateTime = null;
        if (effDateParam != null) try {
            effDateTime = new Date(Long.parseLong(effDateParam));
        } catch (Exception e) {}
        if (effDateTime == null) effDateTime = new Date();

        // by default, look at the EV model and find the start of the current
        // period; use that as the effective date.
        Date effDate = schedule.getPeriodStart(effDateTime);
        if (effDate == null)
            effDate = new Date();

        // Calculate the dates one week before and after the effective date.
        Date lastWeek = new Date(effDate.getTime() - EVSchedule.WEEK_MILLIS);
        Date nextWeek = new Date(effDate.getTime() + EVSchedule.WEEK_MILLIS);
        Date startDate = schedule.getStartDate();
        if (lastWeek.before(startDate)) lastWeek = startDate;

        // Get a slice of the schedule representing the previous week.
        EVSchedule.Period weekSlice =
            EVScheduleRollup.getSlice(schedule, lastWeek, effDate);

        // Now scan the task list looking for information we need.
        TableModel tasks = evModel.getSimpleTableModel();
        int taskListLen = tasks.getRowCount();

        // keep track of tasks that should be displayed in the two lists.
        boolean[] completedLastWeek = new boolean[taskListLen];
        boolean[] dueThroughNextWeek = new boolean[taskListLen];
        for (int i = taskListLen;  i-- > 0; )
            completedLastWeek[i] = dueThroughNextWeek[i] = false;
        boolean oneCompletedLastWeek = false;
        boolean oneDueNextWeek = false;

        // keep track of the two total plan/actual time to date for
        // completed tasks.
        double completedTasksTotalPlanTime = 0;
        double completedTasksTotalActualTime = 0;

        for (int i = 0;   i < taskListLen;   i++) {
            Date completed =
                (Date) tasks.getValueAt(i, EVTaskList.DATE_COMPLETE_COLUMN);
            if (completed != null && completed.before(effDate)) {
                    completedTasksTotalPlanTime += parseTime
                        (tasks.getValueAt(i, EVTaskList.PLAN_DTIME_COLUMN));
                    completedTasksTotalActualTime += parseTime
                        (tasks.getValueAt(i, EVTaskList.ACT_DTIME_COLUMN));

                    if (completed.after(lastWeek) &&
                        completed.before(nextWeek))
                        completedLastWeek[i] = oneCompletedLastWeek = true;

            } else {
                Date due =
                    (Date) tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN);
                if (due != null &&
                    due.after(startDate) &&
                    due.before(nextWeek))
                    dueThroughNextWeek[i] = oneDueNextWeek = true;
            }
        }

        double cpi = completedTasksTotalPlanTime/completedTasksTotalActualTime;

        /*
         * Okay, we have all the data we need.  Lets generate the HTML.
         */

        String taskListHTML = TinyWebServer.encodeHtmlEntities(taskListName);
        out.print(StringUtils.findAndReplace
                  (HEADER_HTML, TASK_LIST_VAR, taskListHTML));

        out.print("<h2>For the week ending: ");
        out.println(EVSchedule.formatDate(new Date(effDate.getTime() - 1000)));
        out.print("</h2>\n");

        Map errors = metrics.getErrors();
        if (errors != null && errors.size() > 0) {
            out.print("<table border><tr>" +
                      "<td style='text-align:left' bgcolor='#ff5050'>" +
                      "<h3>Errors</h3><b>There are problems with this " +
                      "earned value schedule:<ul>");
            Iterator i = errors.keySet().iterator();
            while (i.hasNext())
                out.print("\n<li>" +
                          TinyWebServer.encodeHtmlEntities((String) i.next()));
            out.print("\n</ul>Until you correct these problems, calculations" +
                      " may be incorrect.</b></td></tr></table>\n");
        }

        out.print("<h3>Weekly Data</h3>" +
                  "<table border=1 name='summary'><tr>" +
                  "<td></td><td></td>"+
                  "<td class=header colspan=3>Time</td><td></td>" +
                  "<td class=header colspan=3>Earned Value</td></tr>\n" +
                  "<tr><td></td><td></td>" +
                  "<td class=header>Plan</td>"+
                  "<td class=header>Actual</td>"+
                  "<td class=header>Plan/Act</td><td></td>" +
                  "<td class=header>Plan</td>"+
                  "<td class=header>Actual</td>"+
                  "<td class=header>Plan/Act</td></tr>\n");

        out.print("<tr><td class=left>This Week</td><td></td>");
        printTimeData(weekSlice.getPlanTime(), weekSlice.getActualTime());
        out.print("<td></td>");
        printPctData(weekSlice.getPlanValue(totalPlanTime),
                     weekSlice.getEarnedValue(totalPlanTime));
        out.print("</tr>\n");

        out.print("<tr><td class=left>To Date</td><td></td>");
        printTimeData(weekSlice.getCumPlanTime(),
                      weekSlice.getCumActualTime());
        out.print("<td></td>");
        printPctData(weekSlice.getCumPlanValue(totalPlanTime),
                     weekSlice.getCumEarnedValue(totalPlanTime));
        out.print("</tr>\n");

        out.print("<tr><td class=left>Completed tasks to date</td><td></td>");
        printData(formatTime(completedTasksTotalPlanTime),
                  formatTime(completedTasksTotalActualTime),
                  cpi);
        out.print("<td></td><td></td><td></td><td></td></tr>\n");

        out.println("</table>\n"+
                    "<h3>Tasks Completed This Week</h3>");
        if (!oneCompletedLastWeek)
            out.println("<p><i>(None)</i>");
        else {
            out.print("<table border=1 name='compTask'><tr>" +
                      "<td></td>"+
                      "<td class=header>Plan<br>Time</td>"+
                      "<td class=header>Actual<br>Time</td>"+
                      "<td class=header>Earned<br>Value</td>"+
                      "<td class=header>Planned<br>Date</td>"+
                      "<td class=header>Plan Time/<br>Actual Time</td>"+
                      "</tr>\n");

            for (int i = 0;   i < taskListLen;   i++)
                if (completedLastWeek[i])
                    printCompletedLine(tasks, i);

            out.println("</table>");
        }

        out.println("<h3>Tasks Due Through Next Week</h3>\n");
        if (!oneDueNextWeek)
            out.println("<p><i>(None)</i>");
        else {
            out.print("<table border=1 name='dueTask'><tr>" +
                      "<td></td>"+
                      "<td class=header>Plan<br>Time</td>"+
                      "<td class=header>Actual<br>Time</td>"+
                      "<td class=header>Planned<br>Date</td>"+
                      "<td class=header>Percent<br>Spent</td>"+
                      "<td class=header>Forecast Time<br>Remaining</td>"+
                      "</tr>\n");

            double timeRemaining = 0;
            for (int i = 0;   i < taskListLen;   i++)
                if (dueThroughNextWeek[i])
                    timeRemaining += printDueLine(tasks, i, cpi);

            out.print("<tr><td align=right colspan=5><b>Total:&nbsp;</b></td><td>");
            out.print(formatTime(timeRemaining));
            out.println("</td></tr>\n</table>");
        }

        out.print(EXPORT_HTML);
        out.print(FOOTER_HTML);
    }


    private long parseTime(Object time) {
        if (time == null) return 0;
        long result = TimeLogEditor.parseTime(time.toString());
        return (result < 0 ? 0 : result);
    }
    protected static NumberFormat percentFormatter =
        NumberFormat.getPercentInstance();
    private double parsePercent(String pct) {
        try {
            return percentFormatter.parse(pct).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }
    protected void printTimeData(String plan, String actual) {
        printData(plan, actual,
                  (double) parseTime(plan) / (double) parseTime(actual));
    }
    protected void printPctData(String plan, String actual) {
        printData(plan, actual,
                  (double) parsePercent(plan) /
                  (double) parsePercent(actual));
    }
    protected void printData(String plan, String actual, double fraction) {
        out.print("<td>");
        out.print(plan);
        out.print("</td><td>");
        out.print(actual);
        out.print("</td><td>");
        if (!Double.isInfinite(fraction) && !Double.isNaN(fraction))
            out.print(DoubleData.formatNumber(fraction));
        else
            out.print("&nbsp;");
        out.println("</td>");
    }

    protected void printCompletedLine(TableModel tasks, int i) {
        double planTime, actualTime;
        String time;
        out.print("<tr><td class=left>");
        out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
        out.print("</td><td>");
        time = (String) tasks.getValueAt(i, EVTaskList.PLAN_TIME_COLUMN);
        planTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        time = (String) tasks.getValueAt(i, EVTaskList.ACT_TIME_COLUMN);
        actualTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        out.print(tasks.getValueAt(i, EVTaskList.VALUE_EARNED_COLUMN));
        out.print("</td><td>");
        out.print(encodeHTML
                  (tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN)));
        out.print("</td><td>");
        if (actualTime > 0)
            out.print(DoubleData.formatNumber(planTime/actualTime));
        else
            out.print("&nbsp;");
        out.println("</td></tr>");
    }

    protected double printDueLine(TableModel tasks, int i, double cpi) {
        double planTime, actualTime, percentSpent, forecastTimeRemaining;
        String time;
        out.print("<tr><td class=left>");
        out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
        out.print("</td><td>");
        time = (String) tasks.getValueAt(i, EVTaskList.PLAN_TIME_COLUMN);
        planTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        time = (String) tasks.getValueAt(i, EVTaskList.ACT_TIME_COLUMN);
        actualTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        out.print(encodeHTML
                  (tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN)));
        out.print("</td><td>");
        percentSpent = actualTime / planTime;
        out.print(percentFormatter.format(percentSpent));
        out.print("</td><td>");
        if (cpi > 0)
            forecastTimeRemaining = (planTime / cpi) - actualTime;
        else
            forecastTimeRemaining = planTime - actualTime;

        if (forecastTimeRemaining > 0)
            out.print(formatTime(forecastTimeRemaining));
        else
            out.print("<i>0:00&nbsp;&nbsp;???</i>");
        out.println("</td></tr>");
        return forecastTimeRemaining > 0 ? forecastTimeRemaining : 0;
    }



    static final String TASK_LIST_VAR = "%taskListName%";
    static final String HEADER_HTML =
        "<html><head><title>Weekly Tasks - %taskListName%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<style> td { text-align:right } td.left { text-align:left } "+
        "td.header { text-align:center; font-weight:bold; "+
                           " vertical-align:bottom } </style>\n"+
        "</head><body><h1>Weekly Tasks - %taskListName%</h1>\n";

    static final String EXPORT_HTML =
        "<p class='doNotPrint'>" +
        "<a href=\"excel.iqy?uri=week.class&fullPage\">" +
        "<i>Export to Excel</i></a></p>";
    static final String FOOTER_HTML = "</body></html>";


    private String formatTime(double time) {
        return EVSchedule.formatTime(time);
    }
    private String formatEV(double ev) {
        return EVSchedule.formatPercent(ev / totalPlanTime);
    }

    /** translate an object to appropriate HTML */
    final static String encodeHTML(Object text) {
        if (text == null)
            return "";
        if (text instanceof Date)
            text = EVSchedule.formatDate((Date) text);

        return TinyWebServer.encodeHtmlEntities(text.toString());
    }
}
