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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.EVReport.DependencyCellRenderer;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.ui.TimeLogEditor;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.reports.ExcelReport;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



/** CGI script for displaying tasks due in the previous/next week
 */
public class EVWeekReport extends TinyCGIBase {

    private static final String EFF_DATE_PARAM = "eff";

        private static final int MINUTES_PER_WEEK =
                7 /*days*/ * 24 /*hours*/ * 60 /*minutes*/;

    private double totalPlanTime;

    private static Resources resources = Resources.getDashBundle("EV.Week");

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

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                evModel, getDataRepository(), getPSPProperties(),
                getObjectCache());
        evModel.setDependencyCalculator(depCalc);

        evModel.recalc();
        EVSchedule schedule = evModel.getSchedule();
        EVMetrics  metrics = schedule.getMetrics();
        totalPlanTime = metrics.totalPlan();

        String effDateParam = getParameter(EFF_DATE_PARAM);
        Date effDateTime = null;
        if (effDateParam != null) try {
            effDateTime = new Date(Long.parseLong(effDateParam));
        } catch (Exception e) {}
        if (effDateTime == null)
            // if the user hasn't specified an effective date, then use the
            // current time to round the effective date to the nearest week.
            // With a Sunday - Saturday schedule, the following line will show
            // the report for the previous week through Tuesday, and will
            // start showing the next week's report on Wednesday.
            effDateTime = new Date(System.currentTimeMillis()
                    + EVSchedule.WEEK_MILLIS * 3 / 7);

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

        // Calculate a future date cutoff for task dependency display
        int numDependWeeks = Settings.getInt("ev.numDependencyWeeks", 3);
        Date dependDate = new Date(effDate.getTime() + numDependWeeks
                * EVSchedule.WEEK_MILLIS);

        // Get a slice of the schedule representing the previous week.
        EVSchedule.Period weekSlice =
            EVScheduleRollup.getSlice(schedule, lastWeek, effDate);

        // Now scan the task list looking for information we need.
        TableModel tasks = evModel.getSimpleTableModel();
        int taskListLen = tasks.getRowCount();

        // keep track of tasks that should be displayed in the three lists.
        boolean[] completedLastWeek = new boolean[taskListLen];
        boolean[] dueThroughNextWeek = new boolean[taskListLen];
        Map upcomingDependencies = new LinkedHashMap();
        Arrays.fill(completedLastWeek, false);
        Arrays.fill(dueThroughNextWeek, false);
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
                if (due != null && due.after(startDate)) {
                    if (due.before(nextWeek))
                        dueThroughNextWeek[i] = oneDueNextWeek = true;
                    if (due.before(dependDate)) {
                        findUpcomingDependencies(tasks, upcomingDependencies, i);
                    }
                }
            }
        }

        double cpi = completedTasksTotalPlanTime/completedTasksTotalActualTime;

        /*
         * Okay, we have all the data we need.  Lets generate the HTML.
         */

        String titleHTML = resources.format("Title_FMT", taskListName);
        titleHTML = HTMLUtils.escapeEntities(titleHTML);
        out.print(StringUtils.findAndReplace
                  (HEADER_HTML, TITLE_VAR, titleHTML));

        out.print("<h2>");
        String endDateStr = encodeHTML(new Date(effDate.getTime() - 1000));
        out.print(resources.format("Header_HTML_FMT", endDateStr));
        if (!parameters.containsKey("EXPORT")) {
            printNavLink(lastWeek, effDate, schedule, "Previous");
            printNavLink(nextWeek, effDate, schedule, "Next");
        }
        out.print("</h2>\n");

        Map errors = metrics.getErrors();
        if (errors != null && errors.size() > 0) {
            out.print("<table border><tr><td bgcolor='#ff5050'><h2>");
            out.print(getResource("Report.Errors_Heading"));
            out.print("</h2><b>");
            out.print(getResource("Error_Dialog.Head"));
            out.print("<ul>");
            Iterator i = errors.keySet().iterator();
            while (i.hasNext())
                out.print("\n<li>" +
                          WebServer.encodeHtmlEntities((String) i.next()));
            out.print("\n</ul>");
            out.print(getResource("Error_Dialog.Foot"));
            out.print("</b></td></tr></table>\n");
        }

        interpOut("<h3>${Summary.Header}</h3>" +
                        "<table border=1 name='summary'><tr>" +
                "<td></td><td></td>"+
                "<td class=header colspan=3>${Summary.Direct_Hours}</td><td></td>" +
                "<td class=header colspan=3>${Summary.Earned_Value}</td></tr>\n" +
                "<tr><td></td><td></td>" +
                "<td class=header>${Summary.Plan}</td>"+
                "<td class=header>${Summary.Actual}</td>"+
                "<td class=header>${Summary.Ratio}</td><td></td>" +
                "<td class=header>${Summary.Plan}</td>"+
                "<td class=header>${Summary.Actual}</td>"+
                "<td class=header>${Summary.Ratio}</td></tr>\n");

        interpOut("<tr><td class=left>${Summary.This_Week}</td><td></td>");
        printTimeData(weekSlice.getPlanDirectTime(),
                              weekSlice.getActualDirectTime());
        out.print("<td></td>");
        printPctData(weekSlice.getPlanValue(totalPlanTime),
                     weekSlice.getEarnedValue(totalPlanTime));
        out.print("</tr>\n");

        interpOut("<tr><td class=left>${Summary.To_Date}</td><td></td>");
        printTimeData(weekSlice.getCumPlanTime(),
                      weekSlice.getCumActualTime());
        out.print("<td></td>");
        printPctData(weekSlice.getCumPlanValue(totalPlanTime),
                     weekSlice.getCumEarnedValue(totalPlanTime));
        out.print("</tr>\n");

        double numWeeks = metrics.elapsed() / MINUTES_PER_WEEK;
        interpOut("<tr><td class=left>${Summary.Average_per_Week}" +
                        "</td><td></td>");
        double planTimePerWeek =
                parseTime(weekSlice.getCumPlanTime()) / numWeeks;
        double actualTimePerWeek =
                parseTime(weekSlice.getCumActualTime()) / numWeeks;
        printTimeData(formatTime(planTimePerWeek),
                      formatTime(actualTimePerWeek));
        out.print("<td></td>");
        double planEVPerWeek =
                parsePercent(weekSlice.getCumPlanValue(totalPlanTime)) / numWeeks;
        double actualEVPerWeek =
                parsePercent(weekSlice.getCumEarnedValue(totalPlanTime)) / numWeeks;
        printPctData(formatPercent(planEVPerWeek),
                     formatPercent(actualEVPerWeek));
        out.print("</tr>\n");

        interpOut("<tr><td class=left>${Summary.Completed_Tasks_To_Date}" +
                        "</td><td></td>");
        printData(formatTime(completedTasksTotalPlanTime),
                  formatTime(completedTasksTotalActualTime),
                  1.0 / cpi, "timefmt");
        out.print("<td></td><td></td><td></td><td></td></tr>\n");

        interpOut("</table>\n<h3>${Completed_Tasks.Header}</h3>\n");
        if (!oneCompletedLastWeek)
            interpOut("<p><i>${None}</i>\n");
        else {
            interpOut("<table border=1 name='compTask'><tr>" +
                      "<td></td>"+
                      "<td class=header>${Columns.Planned_Time}</td>"+
                      "<td class=header>${Columns.Actual_Time}</td>"+
                      "<td class=header>${Columns.Percent_Spent}</td>"+
                      "<td class=header>${Columns.Planned_Date}</td>"+
                      "<td class=header>${Columns.Earned_Value}</td>"+
                      "</tr>\n");

            for (int i = 0;   i < taskListLen;   i++)
                if (completedLastWeek[i])
                    printCompletedLine(tasks, i);

            out.println("</table>");
        }

        interpOut("<h3>${Due_Tasks.Header}</h3>\n");
        if (!oneDueNextWeek)
            interpOut("<p><i>${None}</i>\n");
        else {
            interpOut("<table border=1 name='dueTask'><tr>" +
                      "<td></td>"+
                      "<td class=header>${Columns.Planned_Time}</td>"+
                      "<td class=header>${Columns.Actual_Time}</td>"+
                      "<td class=header>${Columns.Percent_Spent}</td>"+
                      "<td class=header>${Columns.Planned_Date}</td>"+
                      "<td class=header>${Columns.Depend}</td>"+
                      "<td class=header>${Columns.Forecast_Time_Remaining}</td>"+
                      "</tr>\n");

            EVReport.DependencyCellRenderer rend =
                new EVReport.DependencyCellRenderer(exportingToExcel());
            double timeRemaining = 0;
            for (int i = 0;   i < taskListLen;   i++)
                if (dueThroughNextWeek[i])
                    timeRemaining += printDueLine(tasks, i, cpi, rend);

            interpOut("<tr><td align=right colspan=6><b>${Due_Tasks.Total}" +
                        "&nbsp;</b></td><td class='timefmt'>");
            out.print(formatTime(timeRemaining));
            out.println("</td></tr>\n</table>");
        }


        interpOut("<h3>${Dependencies.Header}</h3>\n");
        if (upcomingDependencies.isEmpty())
            interpOut("<p><i>${None}</i>\n");
        else {
            interpOut("<table border=1 name='dependTask'><tr>" +
                      "<td></td>"+
                      "<td class=header>${Columns.Assigned_To}</td>"+
                      "<td class=header " +
                          "title='${Columns.Percent_Complete_Tooltip}'>" +
                          "${Columns.Percent_Complete}</td>"+
                      "<td class=header>${Columns.Needed_For}</td>"+
                      "<td class=header>${Columns.Planned_Date}</td>"+
                      "</tr>\n");

            for (Iterator i = upcomingDependencies.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                EVTaskDependency d = (EVTaskDependency) e.getKey();
                List dependentTasks = (List) e.getValue();
                printUpcomingDependencyLines(d, dependentTasks, tasks);
            }

            out.println("</table>");
        }


        interpOut(EXPORT_HTML);
        out.print(FOOTER_HTML);
    }


    private void findUpcomingDependencies(TableModel tasks,
            Map upcomingDependencies, int i) {
        Collection deps = (Collection) tasks.getValueAt(i,
                EVTaskList.DEPENDENCIES_COLUMN);
        if (deps != null) {
            for (Iterator j = deps.iterator(); j.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) j.next();
                if (!d.isUnresolvable() && d.getPercentComplete() < 1) {
                    List l = (List) upcomingDependencies.get(d);
                    if (l == null) {
                        l = new LinkedList();
                        upcomingDependencies.put(d, l);
                    }
                    l.add(new Integer(i));
                }
            }
        }
    }


    private void printNavLink(Date weekStart, Date effDate, EVSchedule schedule,
            String resKey) {
        long effTime = weekStart.getTime() + 1000;
        Date newEffDate = schedule.getPeriodStart(new Date(effTime));
        if (newEffDate.equals(effDate))
            return;

        out.print("&nbsp;&nbsp;<span class='nav'><a href='week.class?"
                + EFF_DATE_PARAM + "=");
        out.print(effTime);
        out.print("'>");
        out.print(resources.getHTML(resKey));
        out.print("</a></span>");
    }


    private boolean exportingToExcel() {
        return ExcelReport.EXPORT_TAG.equals(getParameter("EXPORT"));
    }
    private long parseTime(Object time) {
        if (time == null) return 0;
        long result = FormatUtil.parseTime(time.toString());
        return (result < 0 ? 0 : result);
    }
    private double parsePercent(String pct) {
        try {
                return FormatUtil.parsePercent(pct);
        } catch (Exception e) {
            return 0;
        }
    }
    protected void printTimeData(String plan, String actual) {
        printData(plan, actual,
                  (double) parseTime(actual) / (double) parseTime(plan),
                  "timefmt");
    }
    protected void printPctData(String plan, String actual) {
        printData(plan, actual,
                  (double) parsePercent(actual) / (double) parsePercent(plan),
                  null);
    }
    protected void printData(String plan, String actual, double fraction,
                String className) {
        String td;
        if (className == null)
                td = "<td>";
        else
                td = "<td class='" + className + "'>";
        out.print(td);
        out.print(plan);
        out.print("</td>");
        out.print(td);
        out.print(actual);
        out.print("</td><td>");
        if (!Double.isInfinite(fraction) && !Double.isNaN(fraction))
            out.print(FormatUtil.formatNumber(fraction));
        else
            out.print("&nbsp;");
        out.println("</td>");
    }

    protected void printCompletedLine(TableModel tasks, int i) {
        double planTime, actualTime;
        String time;
        out.print("<tr><td class=left>");
        out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
        out.print("</td><td class='timefmt'>");
        time = (String) tasks.getValueAt(i, EVTaskList.PLAN_TIME_COLUMN);
        planTime = parseTime(time);
        out.print(time);
        out.print("</td><td class='timefmt'>");
        time = (String) tasks.getValueAt(i, EVTaskList.ACT_TIME_COLUMN);
        actualTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        if (planTime > 0)
            out.print(formatPercent(actualTime/planTime));
        else
            out.print("&nbsp;");
        out.print("</td><td>");
        out.print(encodeHTML
                  (tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN)));
        out.print("</td><td>");
        out.print(tasks.getValueAt(i, EVTaskList.VALUE_EARNED_COLUMN));
        out.println("</td></tr>");
    }

    protected double printDueLine(TableModel tasks, int i, double cpi,
            EVReport.DependencyCellRenderer rend) {
        double planTime, actualTime, percentSpent, forecastTimeRemaining;
        String time;
        out.print("<tr><td class=left>");
        out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
        out.print("</td><td class='timefmt'>");
        time = (String) tasks.getValueAt(i, EVTaskList.PLAN_TIME_COLUMN);
        planTime = parseTime(time);
        out.print(time);
        out.print("</td><td class='timefmt'>");
        time = (String) tasks.getValueAt(i, EVTaskList.ACT_TIME_COLUMN);
        actualTime = parseTime(time);
        out.print(time);
        out.print("</td><td>");
        percentSpent = actualTime / planTime;
        out.print(formatPercent(percentSpent));
        out.print("</td><td>");
        out.print(encodeHTML
                  (tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN)));
        out.print("</td><td class=center>");
        Object depns = tasks.getValueAt(i, EVTaskList.DEPENDENCIES_COLUMN);
        String depHtml = rend.getInnerHtml(depns, i,
                EVTaskList.DEPENDENCIES_COLUMN);
        out.print(depHtml == null ? "&nbsp;" : depHtml);
        out.print("</td>");
        if (cpi > 0)
            forecastTimeRemaining = (planTime / cpi) - actualTime;
        else
            forecastTimeRemaining = planTime - actualTime;

        if (forecastTimeRemaining > 0)
            out.print("<td class='timefmt'>" + formatTime(forecastTimeRemaining));
        else
            out.print("<td class='error'>0:00&nbsp;&nbsp;???");
        out.println("</td></tr>");
        return forecastTimeRemaining > 0 ? forecastTimeRemaining : 0;
    }

    protected void printUpcomingDependencyLines(EVTaskDependency d,
            List dependentTasks, TableModel tasks) {

        out.print("<tr><td class=left>");
        out.print(encodeHTML(d.getDisplayName()));
        out.print("</td><td class='left'>");
        out.print(encodeHTML(d.getAssignedTo()));
        out.print("</td><td>");
        out.print(formatPercent(d.getPercentComplete()));
        out.print("</td>");

        boolean firstRow = true;
        for (Iterator j = dependentTasks.iterator(); j.hasNext();) {
            int i = ((Integer) j.next()).intValue();
            if (!firstRow)
                out.print("<tr><td colspan='3'></td>");
            out.print("<td class='left'>");
            out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
            out.print("</td><td>");
            out.print(encodeHTML
                    (tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN)));
            out.println("</td></tr>");
            firstRow = false;
        }
    }



    static final String TITLE_VAR = "%title%";
    static final String HEADER_HTML =
        "<html><head><title>%title%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<style> td { text-align:right } td.left { text-align:left } "+
        "td.center { text-align: center } " +
        "td.error  { font-style: italic;  color: red }\n" +
        "td.timefmt { vnd.ms-excel.numberformat: [h]\\:mm }\n" +
        "td.header { text-align:center; font-weight:bold; "+
                           " vertical-align:bottom }\n" +
        "span.nav { font-size: medium;  font-style: italic; " +
                           " font-weight: normal }\n" +
        "</style>\n"+
        EVReport.POPUP_HEADER +
        "</head><body><h1>%title%</h1>\n";

    static final String EXPORT_HTML =
        "<p class='doNotPrint'>" +
        "<a href=\"excel.iqy?uri=week.class&fullPage\">" +
        "<i>${Export_to_Excel}</i></a></p>";
    static final String FOOTER_HTML = "</body></html>";


    private String formatTime(double time) {
        return FormatUtil.formatTime(time);
    }
    private String formatEV(double ev) {
        return formatPercent(ev / totalPlanTime);
    }
    private String formatPercent(double pct) {
        return FormatUtil.formatPercent(pct);
    }

    /** translate an object to appropriate HTML */
    final static String encodeHTML(Object text) {
        if (text == null)
            return "";
        if (text instanceof Date)
            text = EVSchedule.formatDate((Date) text);

        return WebServer.encodeHtmlEntities(text.toString());
    }

    final static String getResource(String key) {
        return encodeHTML(resources.getString(key)).replace('\n', ' ');
    }

    private void interpOut(String text) {
        out.print(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }
}
