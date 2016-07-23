// Copyright (C) 2002-2016 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableModel;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.DefaultTaskLabeler;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.DatabasePluginUtils;
import net.sourceforge.processdash.tool.db.ProjectLocator;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.ui.lib.HTMLTableWriter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FastDateFormat;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



/** CGI script for displaying tasks due in the previous/next week
 */
public class EVWeekReport extends TinyCGIBase {

    private static final String EFF_DATE_PARAM = "eff";
    private static final String ADJ_EFF_DATE_PARAM = "adjustEff";
    private static final String SPLIT_PARAM = "split";

    // Used to indicate if a task is behind, ahead or on schedule
    private static final byte AHEAD_OF_SCHEDULE = 1;
    private static final byte BEHIND_SCHEDULE = 2;

    private static final long MILLIS_PER_WEEK = 7L /*days*/ * 24 /*hours*/
            * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;
    private static final double WEEKS_PER_MONTH = 365.25 / (7 * 12);

    private static Resources resources = Resources.getDashBundle("EV.Week");
    private static Resources monthRes = Resources.getDashBundle("EV.Month");

    /** Generate CGI output. */
    protected void writeContents() throws IOException {
        EVReportSettings settings = new EVReportSettings(getDataRepository(),
                parameters, getPrefix());

        // Get the name of the earned value model to report on.
        String taskListName = settings.getTaskListName();
        if (taskListName == null)
            throw new IOException("No EV task list specified.");


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

        EVTaskFilter taskFilter = settings.getEffectiveFilter(evModel);

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                getDataRepository(), getPSPProperties(), getObjectCache());
        evModel.setDependencyCalculator(depCalc);
        evModel.setTaskLabeler(new DefaultTaskLabeler(getDashboardContext()));

        evModel.recalc();
        EVSchedule schedule = evModel.getSchedule();

        String effDateParam = getParameter(EFF_DATE_PARAM);
        Date effDate = null;
        if (effDateParam != null) try {
            effDate = new Date(Long.parseLong(effDateParam));
        } catch (Exception e) {}
        boolean monthly = isMonthly(settings);

        if (effDate == null || parameters.containsKey(ADJ_EFF_DATE_PARAM)) {
            // if the user hasn't specified an effective date, then use the
            // current time to round the effective date to the nearest week.
            // With a Sunday - Saturday schedule, the following line will show
            // the report for the previous week through Tuesday, and will
            // start showing the next week's report on Wednesday.
            Date now = effDate;
            if (now == null) now = EVCalculator.getFixedEffectiveDate();
            if (now == null) now = new Date();
            int dayOffset = (monthly ? 0 : (effDate == null ? 3 : 7));
            Date effDateTime = new Date(now.getTime()
                    + EVSchedule.WEEK_MILLIS * dayOffset / 7);

            // now, identify the schedule boundary that precedes the effective
            // date and time; use that as the effective date.
            Date scheduleEnd = schedule.getLast().getEndDate();
            Date firstPeriodEnd = schedule.get(1).getEndDate();
            if (effDateTime.compareTo(scheduleEnd) >= 0) {
                if (effDate == null)
                    effDate = maybeRoundToMonthEnd(monthly, scheduleEnd);
                else if (monthly)
                    effDate = roundToMonthEnd(effDate);
                else
                    effDate = extrapolateWeekAfterScheduleEnd(effDateTime,
                        scheduleEnd);
            } else if (monthly) {
                Date scheduleStart = schedule.get(1).getBeginDate();
                if (effDateTime.before(scheduleStart))
                    effDateTime = scheduleStart;
                effDate = roundToMonthEnd(effDateTime);
            } else if (effDateTime.compareTo(firstPeriodEnd) <= 0)
                effDate = firstPeriodEnd;
            else
                effDate = schedule.getPeriodStart(effDateTime);

            // make certain we have an effective date to proceed with.
            if (effDate == null)
                effDate = maybeRoundToMonthEnd(monthly, new Date());
        }

        int purpose = PLAIN_REPORT;
        if (evModel instanceof EVTaskListRollup
                && parameters.containsKey(SPLIT_PARAM))
            purpose = SPLIT_REPORT;
        writeReport(taskListName, evModel, effDate, settings, taskFilter,
            purpose);
    }

    private Date maybeRoundToMonthEnd(boolean shouldRound, Date d) {
        return (shouldRound ? roundToMonthEnd(d) : d);
    }

    private Date roundToMonthEnd(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    private Date extrapolateWeekAfterScheduleEnd(Date effDate, Date scheduleEnd) {
        long diff = effDate.getTime() - scheduleEnd.getTime();
        long weeks = diff / EVSchedule.WEEK_MILLIS;
        Date result = adjustDate(scheduleEnd, weeks * EVSchedule.WEEK_MILLIS);
        return result;
    }

    private static final int PLAIN_REPORT = 0;
    private static final int SPLIT_REPORT = 1;
    private static final int SUB_REPORT = 2;
    private static final int LEAF_REPORT = 3;

    private boolean isTopLevel(int purpose) {
        return purpose == PLAIN_REPORT || purpose == SPLIT_REPORT;
    }

    private void writeReport(String taskListName, EVTaskList evModel,
            Date effDate, EVReportSettings settings, EVTaskFilter taskFilter,
            int purpose) throws IOException {

        EVSchedule schedule = evModel.getSchedule();
        double totalPlanTime = schedule.getMetrics().totalPlan();
        boolean hideNames = settings.getBool(EVReport.CUSTOMIZE_HIDE_NAMES);
        boolean showAssignedTo = (evModel instanceof EVTaskListRollup)
                && !hideNames;
        boolean showTimingIcons = (evModel instanceof EVTaskListData
                && !isExporting() && purpose == PLAIN_REPORT);
        boolean showMilestones = evModel.showMilestoneColumn();
        boolean showLabels = evModel.showLabelsColumn();
        int numOptionalCols = (showAssignedTo ? 1 : 0) //
                + (showMilestones ? 1 : 0) + (showLabels ? 1 : 0);
        boolean monthly = isMonthly(settings);
        Resources effRes = (monthly ? monthRes : resources);

        // Calculate the dates one week/month before and after the effective date.
        Date lastWeek, nextWeek;
        if (monthly) {
            long eff = effDate.getTime();
            lastWeek = roundToMonthEnd(new Date(eff - 5 * MILLIS_PER_WEEK));
            nextWeek = roundToMonthEnd(new Date(eff + MILLIS_PER_WEEK));
        } else {
            lastWeek = adjustDate(effDate, -EVSchedule.WEEK_MILLIS);
            nextWeek = adjustDate(effDate, EVSchedule.WEEK_MILLIS);
        }
        Date startDate = schedule.getStartDate();
        if (lastWeek.before(startDate)) lastWeek = startDate;
        Date effDateDisplay = new Date(effDate.getTime() - 1000);
        Date nextWeekDisplay = new Date(nextWeek.getTime() - 1000);

        // calculate flags describing whether the actual current date falls
        // within our reporting period
        long now = System.currentTimeMillis();
        boolean reportingPeriodIncludesToday = (lastWeek.getTime() < now
                && now <= effDate.getTime());
        boolean reportingPeriodPrecedesToday = (effDate.getTime() < now
                && now <= nextWeek.getTime());

        // Calculate future cutoff dates for task dependency display
        Date dependDate = getFutureCutoffDate(effDate,
            Settings.getInt("ev.numDependencyWeeks", monthly ? 5 : 3));
        Date revDependDate = getFutureCutoffDate(effDate,
            Settings.getInt("ev.numReverseDependencyWeeks", 6));

        // Get a slice of the schedule representing the previous week.
        EVSchedule filteredSchedule = getEvSchedule(evModel, taskFilter);
        EVSchedule.Period weekSlice = EVScheduleRollup.getSlice(
                filteredSchedule, lastWeek, effDate);

        // Now scan the task list looking for information we need.
        TableModel tasks = evModel.getSimpleTableModel(taskFilter);
        int taskListLen = tasks.getRowCount();

        // keep track of tasks that should be displayed in the three lists.
        boolean[] completedLastWeek = new boolean[taskListLen];
        boolean[] inProgressThisWeek = new boolean[taskListLen];
        boolean[] dueThroughNextWeek = new boolean[taskListLen];
        byte[] progress = new byte[taskListLen];
        Map<String, DependencyForCoord> upcomingDependencies =
            new HashMap<String, DependencyForCoord>();
        List<RevDependencyForCoord> reverseDependencies =
            new ArrayList<RevDependencyForCoord>();
        Arrays.fill(completedLastWeek, false);
        Arrays.fill(inProgressThisWeek, false);
        Arrays.fill(dueThroughNextWeek, false);
        boolean oneCompletedLastWeek = false;
        boolean oneInProgressThisWeek = false;
        boolean oneDueNextWeek = false;

        // keep track of the people assigned to the current schedule
        Set allIndividuals = new HashSet();
        String ignoreIndividual = null;
        if (evModel instanceof EVTaskListData && purpose == PLAIN_REPORT)
            allIndividuals.add(ignoreIndividual = getOwner());

        // retrieve information about the actual time that was logged to tasks
        // during the effective time period
        double[] actualTimeThisWeek = getActualTimeSpent(tasks, lastWeek,
            effDate);
        double completedTasksTimeThisWeek = 0;
        double inProgressTasksTimeThisWeek = 0;

        // keep track of the two total plan/actual time to date for
        // completed tasks.
        double completedTasksTotalPlanTime = 0;
        double completedTasksTotalActualTime = 0;

        // keep track of plan and actual value, this week and to date
        double planValueThisWeek = 0;
        double valueEarnedThisWeek = 0;
        double planValueToDate = 0;
        double valueEarnedToDate = 0;

        for (int i = 0;   i < taskListLen;   i++) {
            double taskValue = parseTime(tasks.getValueAt(i,
                -EVTaskList.PLAN_DTIME_COLUMN));
            Date completed =
                (Date) tasks.getValueAt(i, EVTaskList.DATE_COMPLETE_COLUMN);

            // check to see if the task was due this week, or in the past
            Date due = (Date) tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN);
            if (due != null && due.before(effDate)) {
                planValueToDate += taskValue;
                if (!due.before(lastWeek))
                    planValueThisWeek += taskValue;
            }

            if (completed != null && completed.before(effDate)) {
                    completedTasksTotalPlanTime += taskValue;
                    completedTasksTotalActualTime += parseTime
                        (tasks.getValueAt(i, -EVTaskList.ACT_DTIME_COLUMN));
                    valueEarnedToDate += taskValue;

                    if (!completed.before(lastWeek) &&
                        completed.before(nextWeek)) {
                        completedLastWeek[i] = oneCompletedLastWeek = true;
                        completedTasksTimeThisWeek += actualTimeThisWeek[i];
                        valueEarnedThisWeek += taskValue;

                    } else if (actualTimeThisWeek[i] > 0) {
                        // if the task was marked complete in the past, but
                        // someone logged time against it this week, display
                        // it in the "in progress" table.
                        inProgressThisWeek[i] = oneInProgressThisWeek = true;
                        inProgressTasksTimeThisWeek += actualTimeThisWeek[i];
                    }

            } else {
                Date replannedDue =
                    (Date) tasks.getValueAt(i, EVTaskList.REPLAN_DATE_COLUMN);
                Date taskStarted =
                    (Date) tasks.getValueAt(i, EVTaskList.ACT_START_DATE_COLUMN);

                // Check to see if the task was in progress this week
                if (taskStarted != null && taskStarted.before(effDate)
                     && (completed == null || completed.after(effDate))) {
                    inProgressThisWeek[i] = oneInProgressThisWeek = true;
                    inProgressTasksTimeThisWeek += actualTimeThisWeek[i];
                }

                // Check to see if the task is due next week
                if ((due != null && due.after(startDate) && due.before(nextWeek))
                      || (replannedDue != null && replannedDue.after(startDate)
                              && replannedDue.before(nextWeek))) {
                    dueThroughNextWeek[i] = oneDueNextWeek = true;
                }

                if ((inProgressThisWeek[i] || dueThroughNextWeek[i])
                     && (due != null)) {
                    if (!due.after(effDate))
                        progress[i] = BEHIND_SCHEDULE;
                    else if (due.after(nextWeek))
                        progress[i] = AHEAD_OF_SCHEDULE;
                }

                Date projectedDate = (Date) tasks.getValueAt(i,
                    EVTaskList.PROJ_DATE_COLUMN);
                findUpcomingDependencies(tasks, upcomingDependencies,
                    i, projectedDate, dependDate, ignoreIndividual);
                // don't search for reverse dependencies when we're hiding the
                // names of individuals.  After all, the only thing we display
                // about a reverse dependency is the names of the waiting
                // people.  If we can't display their names, then we won't
                // have any identifying data to show, so there would be no
                // point in collecting reverse dependencies.
                if (!hideNames)
                    findReverseDependencies(tasks, reverseDependencies,
                        revDependDate, i);
            }
            List assignedTo = (List) tasks.getValueAt(i,
                -EVTaskList.ASSIGNED_TO_COLUMN);
            if (assignedTo != null)
                allIndividuals.addAll(assignedTo);
        }

        double cpi = completedTasksTotalPlanTime/completedTasksTotalActualTime;
        boolean showTimeThisWeek = (completedTasksTimeThisWeek > 0
                || inProgressTasksTimeThisWeek > 0);

        // look up planned values from the schedule, if appropriate
        if (!monthly || Settings.getBool("ev.month.extrapolatePV", true)) {
            planValueThisWeek = weekSlice.planValue();
            planValueToDate = weekSlice.getCumPlanValue();
        }
        // look up earned values from the schedule, if not in monthly mode
        if (!monthly) {
            valueEarnedThisWeek = weekSlice.earnedValue();
            valueEarnedToDate = weekSlice.getCumEarnedValue();
        }

        /*
         * Okay, we have all the data we need.  Lets generate the HTML.
         */

        if (isTopLevel(purpose)) {
            String taskListDisplayName = EVTaskList.cleanupName(taskListName);
            String titleHTML = effRes.format("Title_FMT", taskListDisplayName);
            titleHTML = HTMLUtils.escapeEntities(titleHTML);
            StringBuffer header = new StringBuffer(HEADER_HTML);
            StringUtils.findAndReplace(header, TITLE_VAR, titleHTML);
            if (taskFilter != null)
                header.insert(header.indexOf("</head>"), FILTER_HEADER_HTML);
            if (isExportingToExcel())
                StringUtils.findAndReplace(header, "hideIfCollapsed", "ignore");
            out.print(header);

            out.print("<h2>");
            String endDateStr = monthly ? formatMonth(effDateDisplay)
                    : encodeHTML(effDateDisplay);
            out.print(effRes.format("Header_HTML_FMT", endDateStr));
            if (!isExporting() || getParameter(EFF_DATE_PARAM) == null) {
                if (lastWeek.compareTo(startDate) > 0)
                    printNavLink(lastWeek, "Previous", settings, purpose);
                printNavLink(nextWeek, "Next", settings, purpose);
                if (!isExporting())
                    printGoToDateLink(effDate, schedule, settings, purpose);
            }
            out.print("</h2>\n");

            EVReport.printFilterInfo(out, taskFilter, isExportingToExcel());

            EVReport.printScheduleErrors(out, filteredSchedule.getMetrics().getErrors());

        } else {
            out.print("<div class='");
            out.print(purpose == LEAF_REPORT ? "collapsed" : "expanded");
            out.print("'><h2>");
            printExpansionIcon();
            out.print(encodeHTML(taskListName));
            out.print("</h2>\n");
            out.print("<div class='subsection");
            if (purpose != LEAF_REPORT)
                out.print(" hideIfCollapsed");
            out.print("'>");
        }

        String indivDetail = "";
        if (purpose == LEAF_REPORT)
            indivDetail = " class='hideIfCollapsed'";
        String hh = (purpose == SPLIT_REPORT ? "h2" : "h3");

        interpOut("<" + hh + indivDetail + ">${Summary.Header}");
        if (isTopLevel(purpose) && showAssignedTo && !isExporting()) {
            String splitLink = (String) env.get("REQUEST_URI");
            if (purpose == PLAIN_REPORT)
                splitLink = HTMLUtils.appendQuery(splitLink, SPLIT_PARAM, "t");
            else
                splitLink = HTMLUtils.removeParam(splitLink, SPLIT_PARAM);
            out.print("&nbsp;&nbsp;<span class='nav'><a href='");
            out.print(splitLink);
            out.print("'>");
            out.print(resources.getHTML(purpose == PLAIN_REPORT ? "Show_Split"
                    : "Show_Rollup"));
            out.print("</a></span>");
        }
        out.print("</" + hh + ">");
        out.print("<table border=1 name='summary'><tr><td></td><td></td>");
        if (taskFilter == null)
            interpOut("<td class=header colspan=3>${Summary.Direct_Hours}"
                    + "</td><td></td>");
        interpOut("<td class=header colspan=3>${Summary.Earned_Value}"
                + "</td></tr>\n" //
                + "<tr><td></td><td></td>");
        if (taskFilter == null)
            interpOut("<td class=header>${Summary.Plan}</td>"
                    + "<td class=header>${Summary.Actual}</td>"
                    + "<td class=header>${Summary.Ratio}</td><td></td>");
        interpOut("<td class=header>${Summary.Plan}</td>"
                + "<td class=header>${Summary.Actual}</td>"
                + "<td class=header>${Summary.Ratio}</td></tr>\n");

        String thisWeekKey;
        String keySuffix = (monthly ? "_Month" : "_Week");
        if (reportingPeriodIncludesToday) thisWeekKey = "This" + keySuffix;
        else if (reportingPeriodPrecedesToday) thisWeekKey = "Last" + keySuffix;
        else thisWeekKey = "This_Period";
        out.print("<tr><td class=left>"
                + effRes.getHTML("Summary." + thisWeekKey)
                + "</td><td></td>");
        if (taskFilter == null) {
            double directTimeThisWeek;
            if (monthly)
                directTimeThisWeek = sumActualTime(actualTimeThisWeek);
            else
                directTimeThisWeek = weekSlice.getActualDirectTime();
            printTimeData(weekSlice.getPlanDirectTime(), directTimeThisWeek);
            out.print("<td></td>");
        }
        printPctData(planValueThisWeek / totalPlanTime, //
            valueEarnedThisWeek / totalPlanTime);
        out.print("</tr>\n");

        out.print("<tr><td class=left>" + encodeHTML(resources.format(
                "Summary.To_Date_FMT", effDateDisplay)) + "</td><td></td>");
        double directTimeToDate = 0;
        if (taskFilter == null) {
            if (monthly)
                directTimeToDate = schedule.get(0).getActualDirectTime()
                        + sumActualTime(getActualTimeSpent(tasks, startDate,
                            effDate));
            else
                directTimeToDate = weekSlice.getCumActualDirectTime();
            printTimeData(weekSlice.getCumPlanDirectTime(), directTimeToDate);
            out.print("<td></td>");
        }
        printPctData(planValueToDate / totalPlanTime, //
            valueEarnedToDate / totalPlanTime);
        out.print("</tr>\n");

        double numWeeks = Double.NaN;
        if (startDate != null)
            numWeeks = (effDate.getTime() - startDate.getTime() - EVSchedule
                    .dstDifference(startDate.getTime(), effDate.getTime()))
                    / (double) MILLIS_PER_WEEK;
        if (monthly) {
            numWeeks = numWeeks / WEEKS_PER_MONTH;
            interpOut("<tr" + indivDetail + "><td class=left>"
                    + "${Month.Summary.Average_per_Month}</td><td></td>");
        } else {
            interpOut("<tr" + indivDetail
                    + "><td class=left>${Summary.Average_per_Week}</td><td></td>");
        }
        if (taskFilter == null) {
            double planTimePerWeek = weekSlice.getCumPlanDirectTime() / numWeeks;
            double actualTimePerWeek = directTimeToDate / numWeeks;
            printTimeData(planTimePerWeek, actualTimePerWeek);
            out.print("<td></td>");
        }
        double planEVPerWeek = planValueToDate / (totalPlanTime * numWeeks);
        double actualEVPerWeek = valueEarnedToDate / (totalPlanTime * numWeeks);
        printPctData(planEVPerWeek, actualEVPerWeek);
        out.print("</tr>\n");

        if (taskFilter == null) {
            interpOut("<tr" + indivDetail
                    + "><td class=left>${Summary.Completed_Tasks_To_Date}"
                    + "</td><td></td>");
            printData(formatTime(completedTasksTotalPlanTime),
                      formatTime(completedTasksTotalActualTime),
                      1.0 / cpi, "timeFmt");
            out.print("<td></td><td></td><td></td><td></td></tr>\n");
        }
        out.print("</table>\n");

        if (purpose == PLAIN_REPORT || purpose == LEAF_REPORT) {
            out.print("<div class='hideIfCollapsed'>\n");

            // create a table writer with appropriate renderers.
            HTMLTableWriter tableWriter = createTableWriter(evModel, hideNames,
                    showTimingIcons);

            // to draw the completed tasks table, remove the "task with timing
            // icons" renderer if it happens to be in use.
            HTMLTableWriter.CellRenderer taskRenderer = tableWriter
                    .getCellRenderer(EVTaskList.TASK_COLUMN);
            tableWriter.setCellRenderer(EVTaskList.TASK_COLUMN,
                    EVReport.EV_CELL_RENDERER);

            String completedTasksTooltip = encodeHTML(resources.format(
                "Completed_Tasks.Header_Tip_FMT", lastWeek, effDateDisplay));
            String completedTasksHeader;
            if (reportingPeriodIncludesToday)
                completedTasksHeader = effRes
                        .getHTML("Completed_Tasks.Header");
            else if (reportingPeriodPrecedesToday)
                completedTasksHeader = effRes
                        .getHTML("Completed_Tasks.Header_Last");
            else if (monthly)
                completedTasksHeader = effRes.format(
                    "Completed_Tasks.Header_Month_HTML_FMT",
                    formatMonth(effDateDisplay));
            else
                completedTasksHeader = completedTasksTooltip;
            out.print("<h3 title='" + completedTasksTooltip + "'>"
                    + completedTasksHeader + "</h3>\n");
            if (!oneCompletedLastWeek)
                interpOut("<p><i>${None}</i>\n");
            else {
                printCompletedTaskTableHeader(showTimeThisWeek, showAssignedTo,
                    showMilestones, showLabels, monthly);

                double totalPlannedTime = 0;
                double totalActualTime = 0;
                double totalPlannedValue = 0;

                for (int i = 0;   i < taskListLen;   i++) {
                    if (!completedLastWeek[i])
                        continue;

                    double taskPlannedTime =
                        parseTime(tasks.getValueAt(i, -EVTaskList.PLAN_TIME_COLUMN));
                    double taskActualTime =
                        parseTime(tasks.getValueAt(i, -EVTaskList.ACT_TIME_COLUMN));
                    double taskPlannedValue =
                        ((Double)tasks.getValueAt(i,
                                -EVTaskList.PLAN_VALUE_COLUMN)).doubleValue();

                    totalPlannedTime += taskPlannedTime;
                    totalActualTime += taskActualTime;
                    totalPlannedValue += taskPlannedValue;

                    printCompletedLine(tableWriter, tasks, i,
                        showTimeThisWeek ? actualTimeThisWeek : null,
                        showAssignedTo, showMilestones, showLabels);
                }

                interpOut("<tr class='sortbottom'><td><b>${Completed_Tasks.Total}"
                        + "&nbsp;</b></td><td class='timeFmt'>");
                out.print(formatTime(totalPlannedTime) + "</td>");
                out.print("<td class='timeFmt'>");
                out.print(formatTime(totalActualTime) + "</td>");
                if (showTimeThisWeek) {
                    out.print("<td class='timeFmt'>");
                    out.print(formatTime(completedTasksTimeThisWeek) + "</td>");
                }

                if (totalPlannedTime > 0) {
                    double totalPctSpent = totalActualTime/totalPlannedTime;
                    out.print("<td>" + EVSchedule.formatPercent(totalPctSpent)
                            + "</td>");
                } else {
                    out.print("<td>&nbsp;</td>");
                }

                // Empty td for assigned to, planned date, and labels columns
                int noSumCol = 1 + numOptionalCols;
                for (int i = 0; i < noSumCol ; ++i)
                    out.print("<td>&nbsp;</td>");

                out.print("<td>" + EVSchedule.formatPercent(totalPlannedValue)
                    + "</td></tr>\n");

                out.println("</table>");
            }

            // put the "task with timing icons" renderer back in place if necessary
            tableWriter.setCellRenderer(EVTaskList.TASK_COLUMN, taskRenderer);

            String inProgressTooltip = encodeHTML(resources.format(
                "Tasks_In_Progress.Header_Tip_FMT", effDateDisplay));
            String inProgressHeader;
            if (reportingPeriodIncludesToday || reportingPeriodPrecedesToday)
                inProgressHeader = effRes.getHTML("Tasks_In_Progress.Header");
            else
                inProgressHeader = encodeHTML(resources.format(
                    "Tasks_In_Progress.Header_Long_FMT", effDateDisplay));
            out.print("<h3 title='" + inProgressTooltip + "'>"
                    + inProgressHeader + "</h3>\n");
            if (!oneInProgressThisWeek)
                interpOut("<p><i>${None}</i>\n");
            else {
                printUncompletedTaskTableHeader(showTimeThisWeek,
                    showAssignedTo, showMilestones, showLabels, true, monthly);

                double totalPlannedTime = 0;
                double totalActualTime = 0;
                double totalPlannedValue = 0;
                double totalPlannedTimeRemaining = 0;
                double totalUnearnedValue = 0;

                for (int i = 0; i < taskListLen; ++i) {
                    if (!inProgressThisWeek[i])
                        continue;

                    double taskPlannedTime =
                        parseTime(tasks.getValueAt(i, -EVTaskList.PLAN_TIME_COLUMN));
                    double taskActualTime =
                        parseTime(tasks.getValueAt(i, -EVTaskList.ACT_TIME_COLUMN));
                    double taskPlannedValue =
                        ((Double)tasks.getValueAt(i,
                                -EVTaskList.PLAN_VALUE_COLUMN)).doubleValue();
                    double taskPlannedTimeRemaining = taskPlannedTime - taskActualTime;
                    double taskUnearnedValue = computeUnearnedValue(tasks, i);

                    totalPlannedTime += taskPlannedTime;
                    totalActualTime += taskActualTime;
                    totalPlannedValue += taskPlannedValue;
                    totalPlannedTimeRemaining += (taskPlannedTimeRemaining > 0) ?
                                                    taskPlannedTimeRemaining : 0;
                    totalUnearnedValue += taskUnearnedValue;

                    printInProgressLine(tableWriter, tasks, progress[i], i,
                        taskPlannedTimeRemaining,
                        showTimeThisWeek ? actualTimeThisWeek : null,
                        showAssignedTo, showMilestones, showLabels,
                        taskUnearnedValue);
                }

                interpOut("<tr class='sortbottom'><td><b>${Tasks_In_Progress.Total}"
                        + "&nbsp;</b></td><td class='timeFmt'>");
                out.print(formatTime(totalPlannedTime) + "</td>");
                out.print("<td class='timeFmt'>");
                out.print(formatTime(totalActualTime) + "</td>");
                if (showTimeThisWeek) {
                    out.print("<td class='timeFmt'>");
                    out.print(formatTime(inProgressTasksTimeThisWeek) + "</td>");
                }
                out.print("<td>" + EVSchedule.formatPercent(totalPlannedValue)
                        + "</td>");
                if (totalPlannedTime > 0) {
                    double totalPctSpent = totalActualTime/totalPlannedTime;
                    out.print("<td>" + EVSchedule.formatPercent(totalPctSpent)
                            + "</td>");
                } else {
                    out.print("<td>&nbsp;</td>");
                }

                // Empty td because there is no total for Planned Date and Dep.
                int noSumCol = 2 + numOptionalCols;
                for (int i = 0; i < noSumCol ; ++i)
                    out.print("<td>&nbsp;</td>");

                out.print("<td class='timeFmt'>");
                out.print(formatTime(totalPlannedTimeRemaining) + "</td>");

                out.println("<td>" + EVSchedule.formatPercent(totalUnearnedValue)
                        + "</td>");
                out.println("</tr>\n</table>");
            }

            String dueTasksTooltip = encodeHTML(resources.format(
                "Due_Tasks.Header_Tip_FMT", effDateDisplay, nextWeekDisplay));
            String dueTasksHeader;
            if (reportingPeriodIncludesToday)
                dueTasksHeader = effRes.getHTML("Due_Tasks.Header");
            else if (reportingPeriodPrecedesToday)
                dueTasksHeader = effRes.getHTML("Due_Tasks.Header_This");
            else
                dueTasksHeader = encodeHTML(resources.format(
                    "Due_Tasks.Header_Long_FMT", nextWeekDisplay));
            out.print("<h3 title='" + dueTasksTooltip + "'>" + dueTasksHeader
                    + "</h3>\n");
            if (!oneDueNextWeek)
                interpOut("<p><i>${None}</i>\n");
            else {
                printUncompletedTaskTableHeader(false, showAssignedTo,
                    showMilestones, showLabels, false, monthly);

                double timeRemaining = 0;
                for (int i = 0;   i < taskListLen;   i++)
                    if (dueThroughNextWeek[i]) {
                        double forecastTimeRemaining =
                            computeTaskForecastTimeRemaining(tasks, i, cpi);

                        printDueLine(tableWriter, tasks, progress[i], i, cpi,
                            forecastTimeRemaining, showAssignedTo,
                            showMilestones, showLabels);

                        if (progress[i] != AHEAD_OF_SCHEDULE && forecastTimeRemaining > 0)
                            timeRemaining += forecastTimeRemaining;
                    }

                out.print("<tr class='sortbottom'><td align=right colspan=");
                int colspan = 6 + numOptionalCols;
                out.print(Integer.toString(colspan));
                interpOut("><b>${Due_Tasks.Total}"
                        + "&nbsp;</b></td><td class='timeFmt'>");
                out.print(formatTime(timeRemaining));
                out.println("</td></tr>\n</table>");
            }
            out.print("</div>\n");
        } else {
            EVTaskListRollup parentEVModel = (EVTaskListRollup) evModel;
            for (int i = 0;  i < parentEVModel.getSubScheduleCount();  i++) {
                EVTaskList childModel = parentEVModel.getSubSchedule(i);
                String childName = EVTaskList.cleanupName(childModel
                        .getTaskListName());
                int childPurpose = (childModel instanceof EVTaskListRollup
                        ? SUB_REPORT : LEAF_REPORT);
                writeReport(childName, childModel, effDate, settings,
                    taskFilter, childPurpose);
            }
        }

        if (!isTopLevel(purpose)) {
            // end the "subsection" div we started earlier.
            out.print("</div></div>");

        } else {
            interpOut("<" + hh + ">${Dependencies.Header}</" + hh + ">\n");
            discardInternalReverseDependencies(upcomingDependencies,
                reverseDependencies, allIndividuals);
            if (upcomingDependencies.isEmpty() && reverseDependencies.isEmpty())
                interpOut("<p><i>${None}</i>\n");
            else {
                if (!hideNames && !reverseDependencies.isEmpty())
                    printReverseDependencyTable(tasks, reverseDependencies,
                        showAssignedTo);

                List<DependencyForCoord> depsForCoord =
                    new ArrayList<DependencyForCoord>(
                        upcomingDependencies.values());
                Collections.sort(depsForCoord);
                int pos = 0;
                for (DependencyForCoord coord : depsForCoord) {
                    printUpcomingDependencies(coord, tasks, showAssignedTo,
                        hideNames, showMilestones, showLabels, pos++);
                }
            }

            if (isExporting())
                EVReport.writeExportFooter(out);

            if (!isExportingToExcel())
                interpOut(EXPORT_HTML);

            out.print(FOOTER_HTML);
        }
    }

    private double[] getActualTimeSpent(TableModel tasks,
            Date fromDate, Date toDate) {
        Map<String, Map<String, Double>> actualTime = new HashMap();
        if (Settings.isPersonalMode())
            getActualTimeSpentIndiv(actualTime, fromDate, toDate);
        if (Settings.isTeamMode())
            getActualTimeSpentTeam(actualTime, tasks, fromDate, toDate);

        double[] result = new double[tasks.getRowCount()];
        for (int i = 0; i < result.length; i++)
            result[i] = getActualTimeForTask(tasks, i, actualTime);

        return result;
    }

    private void getActualTimeSpentIndiv(Map<String, Map<String, Double>> result,
            Date fromDate, Date toDate) {
        // scan the time log and gather up the actual time spent per task
        Map<String, Double> actualTimes = new HashMap();
        try {
            EnumerIterator timeLogEntries = getDashboardContext().getTimeLog()
                    .filter(null, fromDate, toDate);
            while (timeLogEntries.hasNext()) {
                TimeLogEntry tle = (TimeLogEntry) timeLogEntries.next();
                String path = tle.getPath();
                double time = tle.getElapsedTime();
                sumActualTime(actualTimes, path, time);
            }
        } catch (IOException e) {
        }
        result.put(getOwner(), actualTimes);
    }

    private void getActualTimeSpentTeam(Map<String, Map<String, Double>> result,
            TableModel tasks, Date fromDate, Date toDate) {
        DatabasePlugin db = getDashboardContext().getDatabasePlugin();
        if (db == null)
            return;

        ProjectLocator loc = db.getObject(ProjectLocator.class);
        QueryRunner query = db.getObject(QueryRunner.class);
        if (loc == null || query == null)
            return;

        int fromDateKey = DatabasePluginUtils.getKeyForDate(fromDate, 10000);
        int toDateKey = DatabasePluginUtils.getKeyForDate(toDate, -10000);
        Set<Integer> projectKeys = getProjectKeys(tasks, loc);
        if (projectKeys.isEmpty())
            return;

        List data;
        try {
            data = query.queryHql(TIME_LOG_QUERY, projectKeys, fromDateKey,
                toDateKey);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (Object[] row : (List<Object[]>) data) {
            String dbPlanItemId = (String) row[0];
            String person = (String) row[1];
            double time = ((Number) row[2]).doubleValue();
            if (dbPlanItemId != null && person != null && time > 0) {
                Map<String, Double> personTime = result.get(person);
                if (personTime == null) {
                    personTime = new HashMap();
                    result.put(person, personTime);
                }

                String taskId = DatabasePluginUtils
                        .getTaskIdFromPlanItemId(dbPlanItemId);
                sumActualTime(personTime, taskId, time);
            }
        }
    }

    private void sumActualTime(Map m, String key, double time) {
        if (time > 0) {
            Double oldTime = (Double) m.get(key);
            Double newSum = time + (oldTime == null ? 0 : oldTime);
            m.put(key, newSum);
        }
    }

    private double sumActualTime(double[] actualTime) {
        double result = 0;
        for (double d : actualTime)
            result += d;
        return result;
    }

    private Set<Integer> getProjectKeys(TableModel tasks, ProjectLocator loc) {
        // scan the tasks in the plan and make a list of the IDs for the
        // team projects they come from
        Set<String> projectIDs = new HashSet();
        for (int i = tasks.getRowCount(); i-- > 0;) {
            EVTask task = (EVTask) tasks.getValueAt(i,
                EVTaskList.EVTASK_NODE_COLUMN);
            List<String> taskIds = task.getInheritedTaskIDs();
            if (!taskIds.isEmpty()) {
                // only use the first task ID.  Subsequent IDs will be for
                // master projects, which have no database representation.
                String taskId = taskIds.get(0);
                int colonPos = taskId.indexOf(':');
                if (colonPos != -1)
                    projectIDs.add(taskId.substring(0, colonPos));
            }
        }

        // now look up the keys for those projects in the database. The call
        // to look up a project key will block if the project data hasn't
        // been loaded yet, so this step will ensure that data has been loaded
        // for all the projects in question before we proceed
        Set<Integer> result = new HashSet();
        for (String projectID : projectIDs) {
            Integer key = loc.getKeyForProject(projectID, null);
            if (key != null)
                result.add(key);
        }
        return result;
    }

    private static final String TIME_LOG_QUERY = "select "
            + "f.planItem.identifier, " //
            + "f.dataBlock.person.encryptedName, " //
            + "sum(f.deltaMin) " //
            + "from TimeLogFact f " //
            + "where f.versionInfo.current = 1 "
            + "and f.planItem.project.key in (?) "
            + "and f.startDateDim.key between ? and ? "
            + "group by f.planItem.identifier, f.dataBlock.person.encryptedName";

    private double getActualTimeForTask(TableModel tasks, int i,
            Map<String, Map<String, Double>> actualTime) {
        Object assignedTo = tasks.getValueAt(i, EVTaskList.ASSIGNED_TO_COLUMN);
        Map<String, Double> timeForPerson = actualTime.get(assignedTo);
        if (timeForPerson == null)
            return 0;

        try {
            if (Settings.isPersonalMode()) {
                Object taskPath = tasks.getValueAt(i,
                    EVTaskList.TASK_FULLNAME_COLUMN);
                Double timeFromPath = timeForPerson.remove(taskPath);
                if (timeFromPath != null)
                    return timeFromPath;
            }

            EVTask task = (EVTask) tasks.getValueAt(i,
                EVTaskList.EVTASK_NODE_COLUMN);
            String taskId = task.getFullTaskID();
            Double timeFromId = timeForPerson.remove(taskId);
            return (timeFromId == null ? 0 : timeFromId);

        } finally {
            if (timeForPerson.isEmpty())
                actualTime.remove(assignedTo);
        }
    }


    private void printUncompletedTaskTableHeader(boolean showTimeThisWeek,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels,
            boolean inProgressThisWeek, boolean monthly) {
        String HTMLTableId = (inProgressThisWeek) ? "$$$_progress" : "$$$_due";

        interpOut("<table border=1 name='dueTask' class='sortable' id='" + HTMLTableId+
                  "'><tr>" +
                  "<td></td>"+
                  "<td class=header>${Columns.Planned_Time}</td>"+
                  "<td class=header>${Columns.Actual_Time}</td>");

        if (inProgressThisWeek && showTimeThisWeek)
            interpOut("<td class=header>${Columns.Actual_Time_"
                    + (monthly ? "Month" : "Week") + "}</td>");
        if (inProgressThisWeek)
            interpOut("<td class=header>${Columns.Planned_Value}</td>");

        interpOut("<td class=header>${Columns.Percent_Spent}</td>");

        if (showAssignedTo)
            interpOut("<td class=header>${Columns.Assigned_To}</td>");
        interpOut("<td class=header>${Columns.Planned_Date}</td>");
        if (showMilestones)
            interpOut("<td class=header>${Columns.Milestone}</td>");
        if (showLabels)
            interpOut("<td class=header>${Columns.Labels}</td>");
        interpOut("<td class=header title='${Columns.Depend_Tooltip}'>${Columns.Depend}</td>");

        if (inProgressThisWeek) {
            interpOut("<td class=header>${Columns.Planned_Time_Remaining}</td>" +
                      "<td class=header>${Columns.Unearned_Value}</td>");
        }
        else
            interpOut("<td class=header>${Columns.Forecast_Time_Remaining}</td>");

        interpOut("</tr>\n");
    }

    private void printCompletedTaskTableHeader(boolean showTimeThisWeek,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels,
            boolean monthly) {
        interpOut("<table border=1 name='compTask' class='sortable' " +
                        "id='$$$_comp'><tr>" +
                  "<td></td>"+
                  "<td class=header>${Columns.Planned_Time}</td>"+
                  "<td class=header>${Columns.Actual_Time}</td>");
        if (showTimeThisWeek)
            interpOut("<td class=header>${Columns.Actual_Time_"
                    + (monthly ? "Month" : "Week") + "}</td>");
        interpOut("<td class=header>${Columns.Percent_Spent}</td>");
        if (showAssignedTo)
            interpOut("<td class=header>${Columns.Assigned_To}</td>");
        interpOut("<td class=header>${Columns.Planned_Date}</td>");
        if (showMilestones)
            interpOut("<td class=header>${Columns.Milestone}</td>");
        if (showLabels)
            interpOut("<td class=header>${Columns.Labels}</td>");
        interpOut("<td class=header>${Columns.Earned_Value}</td>"+
                  "</tr>\n");
    }


    private Date getFutureCutoffDate(Date effDate, int numWeeksFromNow) {
        if (numWeeksFromNow < 0)
            return EVSchedule.NEVER;
        else if (numWeeksFromNow == 0)
            return EVSchedule.A_LONG_TIME_AGO;
        else
            return new Date(effDate.getTime() + numWeeksFromNow
                * EVSchedule.WEEK_MILLIS);
    }



    private HTMLTableWriter createTableWriter(EVTaskList evModel,
            boolean hideNames, boolean showTimingIcons) {
        HTMLTableWriter tableWriter = new HTMLTableWriter();
        EVReport.setupTaskTableRenderers(tableWriter, showTimingIcons,
                isExportingToExcel(), hideNames, evModel.getNodeTypeSpecs());
        tableWriter.setExtraColumnAttributes(EVTaskList.TASK_COLUMN,
                "class='left'");
        tableWriter.setExtraColumnAttributes(EVTaskList.ASSIGNED_TO_COLUMN,
                "class='left'");
        return tableWriter;
    }



    private Date adjustDate(Date effDate, long delta) {
        long baseTime = effDate.getTime();
        long adjustedTime = baseTime + delta;
        adjustedTime += EVSchedule.dstDifference(baseTime, adjustedTime);
        return new Date(adjustedTime);
    }



    private void findUpcomingDependencies(TableModel tasks,
            Map<String, DependencyForCoord> upcomingDependencies,
            int taskRowNum, Date projDate, Date cutoffDate,
            String ignoreIndividual) {
        boolean beforeCutoff = compareDates(projDate, cutoffDate) <= 0;
        Collection deps = (Collection) tasks.getValueAt(taskRowNum,
                EVTaskList.DEPENDENCIES_COLUMN);
        if (deps != null) {
            for (Iterator j = deps.iterator(); j.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) j.next();
                // skip unresolvable and reverse dependencies
                if (d.isUnresolvable() || d.isReverse())
                    continue;
                // skip dependencies that have been satisfied
                if (!(d.getPercentComplete() < 1))
                    continue;
                // don't warn an individual when he depends on himself
                if (ignoreIndividual != null
                        && ignoreIndividual.equals(d.getAssignedTo()))
                    continue;

                if (d.isMisordered() || beforeCutoff) {
                    String taskID = d.getTaskID();
                    DependencyForCoord coord = upcomingDependencies.get(taskID);
                    if (coord == null) {
                        coord = new DependencyForCoord(d);
                        upcomingDependencies.put(taskID, coord);
                    }
                    coord.addMatchingTask(new RowNumWithDate(taskRowNum,
                            projDate));
                }
            }
        }
    }


    private void findReverseDependencies(TableModel tasks,
            List<RevDependencyForCoord> reverseDependencies,
            Date cutoffDate, int taskRowNum) {
        Collection deps = (Collection) tasks.getValueAt(taskRowNum,
            EVTaskList.DEPENDENCIES_COLUMN);
        if (deps != null) {
            for (Iterator j = deps.iterator(); j.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) j.next();
                if (!d.isReverse())
                    continue;
                if (d.isMisordered() ||
                        compareDates(d.getProjectedDate(), cutoffDate) <= 0) {
                    RevDependencyForCoord revDep = new RevDependencyForCoord(
                            taskRowNum, d);
                    reverseDependencies.add(revDep);
                }
            }
        }
    }


    private void discardInternalReverseDependencies(
            Map<String, DependencyForCoord> upcomingDependencies,
            List<RevDependencyForCoord> reverseDependencies,
            Set currentTeam) {

        for (Iterator i = reverseDependencies.iterator(); i.hasNext();) {
            RevDependencyForCoord revCoord = (RevDependencyForCoord) i.next();

            revCoord.computeExternalWaitingPeople(currentTeam);
            if (revCoord.externalPeople == null)
                i.remove();
        }
    }


    private class DependencyForCoord implements Comparable<DependencyForCoord> {
        EVTaskDependency d;
        Date sortDate;
        List<RowNumWithDate> matchingTasks;

        public DependencyForCoord(EVTaskDependency d) {
            this.d = d;
            this.matchingTasks = new ArrayList<RowNumWithDate>();
        }

        public void addMatchingTask(RowNumWithDate item) {
            matchingTasks.add(item);
            sortDate = EVCalculator.minStartDate(sortDate, item.date);
        }

        public int compareTo(DependencyForCoord that) {
            return compareDates(this.sortDate, that.sortDate);
        }

    }

    private class RevDependencyForCoord extends RowNumWithDate {
        EVTaskDependency revDep;
        String externalPeople;

        public RevDependencyForCoord(int rowNumber, EVTaskDependency revDep) {
            super(rowNumber, revDep.getProjectedDate());
            this.revDep = revDep;
        }

        public void computeExternalWaitingPeople(Set peopleToIgnore) {
            List waitingIndividuals = new ArrayList(revDep.getAssignedToList());
            waitingIndividuals.removeAll(peopleToIgnore);
            if (waitingIndividuals.isEmpty())
                externalPeople = null;
            else
                externalPeople = StringUtils.join(waitingIndividuals, ", ");
        }

    }

    private class RowNumWithDate implements Comparable<RowNumWithDate> {
        int rowNumber;
        Date date;

        public RowNumWithDate(int rowNumber, Date date) {
            this.rowNumber = rowNumber;
            this.date = date;
        }

        public int compareTo(RowNumWithDate that) {
            return compareDates(this.date, that.date);
        }

    }


    private void printNavLink(Date effDate, String resKey,
            EVReportSettings settings, int purpose) {
        printNavLink(Long.toString(effDate.getTime()), resKey, settings,
            purpose, null, null);
    }

    private void printNavLink(String effDateParam, String resKey,
            EVReportSettings settings, int purpose, String extraHtml,
            String onClick) {
        StringBuffer href = new StringBuffer();
        href.append(isMonthly(settings) ? "month" : "week.class");
        HTMLUtils.appendQuery(href, EFF_DATE_PARAM, effDateParam);
        HTMLUtils.appendQuery(href,
                settings.getQueryString(EVReportSettings.PURPOSE_NAV_LINK));
        if (purpose == SPLIT_REPORT)
            HTMLUtils.appendQuery(href, SPLIT_PARAM, "t");

        out.print("&nbsp;&nbsp;<span class='nav'>");
        if (extraHtml != null)
            out.print(extraHtml);
        out.print("<a href='");
        out.print(href);
        if (onClick != null)
            out.print("' onclick='" + onClick);
        out.print("'>");
        out.print(resources.getHTML(resKey));
        out.print("</a></span>");
    }

    private boolean isMonthly(EVReportSettings settings) {
        return settings.getParameters().containsKey("month");
    }


    private void printGoToDateLink(Date effDate, EVSchedule schedule,
            EVReportSettings settings, int purpose) {
        Date start = schedule.get(0).getEndDate();
        String jacsFields = "<input type='text' id='JacsOut' value='"
                + JACS_DATE_FMT.format(effDate) + "'><input type='hidden' "
                + "id='JacsStart' value='" + start.getTime() + "'>";
        printNavLink("00000000", "Go_To_Date", settings, purpose, jacsFields,
            "return PdashEV.showGoToDateCalendar(this, event)");
    }
    private static final FastDateFormat JACS_DATE_FMT = FastDateFormat
            .getInstance("yyyy-MM-dd");


    private double parseTime(Object time) {
        if (time == null) return 0;
        if (time instanceof Number)
            return ((Number) time).doubleValue();
        long result = FormatUtil.parseTime(time.toString());
        return (result < 0 ? 0 : result);
    }
    protected void printTimeData(double plan, double actual) {
        printData(formatTime(plan), formatTime(actual),
                  actual / plan, "timeFmt");
    }
    protected void printPctData(double plan, double actual) {
        printData(formatPercent(plan), formatPercent(actual),
                  actual / plan, null);
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

    protected void printCompletedLine(HTMLTableWriter tableWriter,
            TableModel tasks, int i, double[] actualTimeThisWeek,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels)
            throws IOException {
        out.print("<tr>");
        tableWriter.writeCell(out, tasks, i, EVTaskList.TASK_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.ACT_TIME_COLUMN);
        writeActualTimeThisWeek(actualTimeThisWeek, i);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PCT_SPENT_COLUMN);
        if (showAssignedTo)
            tableWriter.writeCell(out, tasks, i, EVTaskList.ASSIGNED_TO_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_DATE_COLUMN);
        if (showMilestones)
            tableWriter.writeCell(out, tasks, i, EVTaskList.MILESTONE_COLUMN);
        if (showLabels)
            tableWriter.writeCell(out, tasks, i, EVTaskList.LABELS_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.VALUE_EARNED_COLUMN);
        out.println("</tr>");
    }

    protected void printInProgressLine(HTMLTableWriter tableWriter,
            TableModel tasks, byte progress, int i,
            double plannedTimeRemaining, double[] actualTimeThisWeek,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels,
            double unearnedValue) throws IOException {
        printUncompletedTaskLine(tableWriter, tasks, progress, i, 0,
            plannedTimeRemaining, actualTimeThisWeek, showAssignedTo, showMilestones,
            showLabels, true, unearnedValue);
    }

    protected void printDueLine(HTMLTableWriter tableWriter, TableModel tasks,
            byte progress, int i, double cpi, double forecastTimeRemaining,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels)
            throws IOException {
        printUncompletedTaskLine(tableWriter, tasks, progress, i, cpi,
            forecastTimeRemaining, null, showAssignedTo, showMilestones,
            showLabels, false, 0);
    }

    protected void printUncompletedTaskLine(HTMLTableWriter tableWriter,
            TableModel tasks, byte progress, int i, double cpi,
            double timeRemaining, double[] actualTimeThisWeek,
            boolean showAssignedTo, boolean showMilestones, boolean showLabels,
            boolean inProgressThisWeek, double unearnedValue)
            throws IOException {
        out.print("<tr>");
        tableWriter.writeCell(out, tasks, i, EVTaskList.TASK_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.ACT_TIME_COLUMN);

        if (inProgressThisWeek) {
            writeActualTimeThisWeek(actualTimeThisWeek, i);
            tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_VALUE_COLUMN);
        }

        tableWriter.writeCell(out, tasks, i, EVTaskList.PCT_SPENT_COLUMN);
        if (showAssignedTo)
            tableWriter.writeCell(out, tasks, i, EVTaskList.ASSIGNED_TO_COLUMN);

        if (progress == BEHIND_SCHEDULE)
            tableWriter.setExtraColumnAttributes(EVTaskList.PLAN_DATE_COLUMN,
                               "class='behindSchedule' title='" +
                               getResource("Week.Due_Tasks.Behind_Schedule_Tooltip") + "'");
        else if (progress == AHEAD_OF_SCHEDULE)
            tableWriter.setExtraColumnAttributes(EVTaskList.PLAN_DATE_COLUMN,
                               "class='aheadOfSchedule' title='" +
                               getResource("Week.Due_Tasks.Ahead_Of_Schedule_Tooltip") + "'");

        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_DATE_COLUMN);

        tableWriter.setExtraColumnAttributes(EVTaskList.PLAN_DATE_COLUMN, null);

        if (showMilestones)
            tableWriter.writeCell(out, tasks, i, EVTaskList.MILESTONE_COLUMN);

        if (showLabels)
            tableWriter.writeCell(out, tasks, i, EVTaskList.LABELS_COLUMN);

        tableWriter.writeCell(out, tasks, i, EVTaskList.DEPENDENCIES_COLUMN);

        if (timeRemaining <= 0) {
            out.print("<td class='error' " +
                      EVReport.getSortAttribute(Double.toString(timeRemaining)) +
                      ">" + formatTime(timeRemaining) + "</td>");
        }
        else {
            HTMLTableWriter.writeCell(out, EVReport.TIME_CELL_RENDERER,
                                      formatTime(timeRemaining), 0, 0);
        }

        if (inProgressThisWeek) {
            HTMLTableWriter.writeCell(out, EVReport.EV_CELL_RENDERER,
                    EVSchedule.formatPercent(unearnedValue), 0, 0);
        }

        out.println("</td></tr>");
    }

    private void writeActualTimeThisWeek(double[] actualTimeThisWeek, int i)
            throws IOException {
        if (actualTimeThisWeek != null) {
            double t = actualTimeThisWeek[i];
            HTMLTableWriter.writeCell(out, EVReport.TIME_CELL_RENDERER,
                t > 0 ? formatTime(t) : null, 0, 0);
        }
    }

    private double computeUnearnedValue(TableModel tasks, int i) {
        double unearnedValue = 0;

        double planValue =
            ((Double)tasks.getValueAt(i, -EVTaskList.PLAN_VALUE_COLUMN)).doubleValue();

        double percentSpent =
            ((Double)tasks.getValueAt(i, -EVTaskList.PCT_SPENT_COLUMN)).doubleValue();

        if (!Double.isNaN(planValue) && !Double.isNaN(percentSpent) &&
                !Double.isInfinite(planValue) && !Double.isInfinite(percentSpent))
            unearnedValue = planValue * Math.min(1.0, percentSpent);

        return unearnedValue;
    }

    private double computeTaskForecastTimeRemaining(TableModel tasks,
                                                    int i,
                                                    double cpi) {
        double planTime = parseTime(tasks.getValueAt(i, -EVTaskList.PLAN_TIME_COLUMN));
        double actualTime = parseTime(tasks.getValueAt(i, -EVTaskList.ACT_TIME_COLUMN));
        double forecastTimeRemaining;

        if (cpi > 0 && !Double.isInfinite(cpi))
            forecastTimeRemaining = (planTime / cpi) - actualTime;
        else
            forecastTimeRemaining = planTime - actualTime;

        return forecastTimeRemaining;
    }

    private void printReverseDependencyTable(TableModel tasks,
            List<RevDependencyForCoord> reverseDependencies,
            boolean showAssignedTo) {

        Collections.sort(reverseDependencies);

        out.print("<div class='expanded'>");
        printExpansionIcon();
        interpOut("${Dependencies.Reverse.Header}\n"
                + "<div class='dependDetail hideIfCollapsed'>\n"
                + "<table border=1 name='extCommit' class='sortable' "
                + "id='$$$_extCommit'>\n<tr>"
                + "<td class=header>${Columns.Task_Commitment}</td>");
        if (showAssignedTo)
            interpOut("<td class=header>${Columns.Assigned_To}</td>");
        interpOut("<td class=header>${Columns.Projected_Date}</td>"
                + "<td class=header>${Columns.Needed_By}</td>"
                + "<td class=header>${Columns.Need_Date}</td></tr>\n");

        for (RevDependencyForCoord revCoord : reverseDependencies) {
            int i = revCoord.rowNumber;
            out.print("<td class='left'>");
            out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
            if (showAssignedTo) {
                out.print("</td><td>");
                out.print(encodeHTML(tasks.getValueAt(i,
                    EVTaskList.ASSIGNED_TO_COLUMN)));
            }
            out.print("</td>");
            Date projDate = (Date) tasks.getValueAt(i, EVTaskList.PROJ_DATE_COLUMN);
            printDateCell(projDate, null, false);
            out.print("<td>");
            out.print(encodeHTML(revCoord.externalPeople));
            out.print("</td>");
            printDateCell(revCoord.revDep.getProjectedDate(),
                TaskDependencyAnalyzer.HTML_REVERSE_MISORD_IND,
                revCoord.revDep.isMisordered());
            out.println("</tr>");
        }

        out.println("</table></div></div>");
        out.println("<br>");
    }

    protected void printUpcomingDependencies(DependencyForCoord coord,
            TableModel tasks, boolean showAssignedTo, boolean hideNames,
            boolean showMilestones, boolean showLabels, int pos) {

        boolean isExcel = isExportingToExcel();
        EVTaskDependency d = coord.d.clone();

        out.print("<div class='expanded'>");
        printExpansionIcon();

        out.println(encodeHTML(d.getDisplayName()));

        if (!isExcel) {
            out.print("<span class='hideIfExpanded'>");
            out.print(TaskDependencyAnalyzer.getBriefDetails(d,
                    TaskDependencyAnalyzer.HTML_SEP, hideNames));
            out.println("</span>");
        }

        out.println("<div class='dependDetail hideIfCollapsed'>");
        interpOut("<b>${Columns.Percent_Complete_Tooltip}:</b> ");
        out.println(formatPercent(d.getPercentComplete()));
        interpOut("<br><b>${Columns.Projected_Date}:</b> ");
        out.println(encodeHTML(d.getProjectedDate()));
        if (!hideNames) {
            interpOut("<br><b>${Columns.Assigned_To}:</b> ");
            out.println(encodeHTML(d.getAssignedTo()));
        }

        // Now, print a table of the dependent tasks.
        interpOut("<table border=1 class='sortable' id='$$$_dep_"+pos+"'><tr>"
                + "<td class=header>${Columns.Needed_For}</td>"
                + "<td class=header>${Columns.Projected_Date}</td>");
        if (showMilestones)
            interpOut("<td class=header>${Columns.Milestone}</td>");
        if (showLabels)
            interpOut("<td class=header>${Columns.Labels}</td>");
        if (showAssignedTo)
            interpOut("<td class=header>${Columns.Needed_By}</td>");
        out.println("</tr>");

        Collections.sort(coord.matchingTasks);
        for (RowNumWithDate taskMatch : coord.matchingTasks) {
            int i = taskMatch.rowNumber;
            out.print("<tr><td class='left'>");
            out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
            out.print("</td>");

            d.loadParentDate(tasks.getValueAt(i, EVTaskList.EVTASK_NODE_COLUMN));
            printDateCell(d.getParentDate(),
                TaskDependencyAnalyzer.HTML_INCOMPLETE_MISORD_IND,
                d.isMisordered());

            if (showMilestones) {
                out.print("<td class='left'>");
                out.print(encodeHTML
                          (tasks.getValueAt(i, EVTaskList.MILESTONE_COLUMN)));
                out.print("</td>");
            }

            if (showLabels) {
                out.print("<td class='left'>");
                out.print(encodeHTML
                          (tasks.getValueAt(i, EVTaskList.LABELS_COLUMN)));
                out.print("</td>");
            }

            if (showAssignedTo) {
                out.print("<td class='left'>");
                out.print(encodeHTML(tasks.getValueAt(i,
                    EVTaskList.ASSIGNED_TO_COLUMN)));
                out.print("</td>");
            }
            out.println("</tr>");
        }

        out.println("</table></div></div>");
        out.println("<br>");
    }

    private void printExpansionIcon() {
        if (!isExportingToExcel())
            out.print("<a onclick='PdashEV.toggleExpanded(this); return false;' " +
                        "class='expIcon' href='#'></a>&nbsp;");
    }

    private void printDateCell(Date date, String indicatorHtml,
            boolean showIndicator) {
        out.print("<td ");
        out.print(EVReport.getSortAttribute(EVReport.getDateSortKey(date)));
        out.print(">");
        if (indicatorHtml != null && showIndicator && !isExportingToExcel()) {
            out.print(indicatorHtml);
            out.print("&nbsp;");
        }
        out.print(encodeHTML(date));
        out.println("</td>");
    }

    private EVSchedule getEvSchedule(EVTaskList evModel,
            EVTaskFilter taskFilter) {
        if (taskFilter == null)
            return evModel.getSchedule();
        else
            return new EVScheduleFiltered(evModel, taskFilter);
    }


    static final String TITLE_VAR = "%title%";
    static final String HEADER_HTML =
        "<html><head><title>%title%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<link rel=stylesheet type='text/css' href='/lib/jacs.css'>\n" +
        "<style> td { text-align:right } td.left { text-align:left } "+
        "td.center { text-align: center } " +
        "td.error  { font-style: italic; font-weight: bold; color: red; " +
                           " text-align: left; padding-left: 1ex }\n" +
        "td.header { text-align:center; font-weight:bold; "+
                           " vertical-align:bottom }\n" +
        "td.behindSchedule { background-color: #ffcccc }\n" +
        "td.aheadOfSchedule { background-color: #ddffdd }\n" +
        "table.sortable { empty-cells: show }\n" +
        "span.nav { font-size: medium;  font-style: italic; " +
                           " font-weight: normal }\n" +
        "div.subsection { margin-left: 1cm }\n" +
        "div.dependDetail { margin-left: 1cm }\n" +
        "div.dependDetail table { margin-top: 7px; margin-bottom: 15px }\n" +
        "a.expIcon { width: 10px; padding-left: 10px; " +
                           "background-repeat: no-repeat; " +
                           "background-position: left center; }\n" +
        ".expanded a.expIcon { background-image: url(\"/Images/minus.png\"); }\n" +
        ".collapsed a.expIcon { background-image: url(\"/Images/plus.png\"); }\n" +
        ".collapsed .hideIfCollapsed { display:none }\n" +
        ".expanded .hideIfExpanded { display:none }\n" +
        "h1 { margin-top: 0px }\n" +
        "#JacsOut { border: 0px; padding: 0px; width: 0px; color: #fff }\n" +
        "</style>\n"+
        "<script type='text/javascript' src='/lib/prototype.js'> </script>\n" +
        "<script type='text/javascript' src='/reports/ev.js'> </script>\n" +
        EVReport.REDUNDANT_EXCEL_HEADER +
        EVReport.SORTTABLE_HEADER +
        EVReport.POPUP_HEADER +
        "</head><body>" +
        "<script type='text/javascript' src='/lib/jacs.js'> </script>\n" +
        "<h1>%title%</h1>\n";
    static final String FILTER_HEADER_HTML = EVReport.FILTER_HEADER_HTML;

    static final String EXPORT_HTML =
        "<p class='doNotPrint'>" +
        "<a href=\"excel.iqy?fullPage\">" +
        "<i>${Export_to_Excel}</i></a></p>";
    static final String FOOTER_HTML = "</body></html>";


    private String formatTime(double time) {
        return FormatUtil.formatTime(time);
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
        if (text instanceof Collection)
            text = StringUtils.join((Collection) text, ", ");

        return HTMLUtils.escapeEntities(text.toString());
    }

    static String formatMonth(Date d) {
        return HTMLUtils.escapeEntities(MONTH_FORMAT.format(d));
    }

    private static final FastDateFormat MONTH_FORMAT = FastDateFormat
            .getInstance("MMMM yyyy");

    final static String getResource(String key) {
        return encodeHTML(resources.getString(key)).replace('\n', ' ');
    }

    private void interpOut(String text) {
        out.print(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

    private static int compareDates(Date a, Date b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return +1;
        return a.compareTo(b);
    }
}
