// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
import java.util.ArrayList;
import java.util.Arrays;
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
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.lib.HTMLTableWriter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



/** CGI script for displaying tasks due in the previous/next week
 */
public class EVWeekReport extends TinyCGIBase {

    private static final String EFF_DATE_PARAM = "eff";
    private static final String SPLIT_PARAM = "split";



    private static final long MILLIS_PER_WEEK = 7L /*days*/ * 24 /*hours*/
            * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;

    private static Resources resources = Resources.getDashBundle("EV.Week");

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

        if (effDate == null) {
            // if the user hasn't specified an effective date, then use the
            // current time to round the effective date to the nearest week.
            // With a Sunday - Saturday schedule, the following line will show
            // the report for the previous week through Tuesday, and will
            // start showing the next week's report on Wednesday.
            Date effDateTime = new Date(System.currentTimeMillis()
                    + EVSchedule.WEEK_MILLIS * 3 / 7);

            // now, identify the schedule boundary that precedes the effective
            // date and time; use that as the effective date.
            Date scheduleEnd = schedule.getLast().getEndDate();
            if (effDateTime.compareTo(scheduleEnd) >= 0)
                effDate = scheduleEnd;
            else
                effDate = schedule.getPeriodStart(effDateTime);

            // make certain we have an effective date to proceed with.
            if (effDate == null)
                effDate = new Date();
        }

        int purpose = PLAIN_REPORT;
        if (evModel instanceof EVTaskListRollup
                && parameters.containsKey(SPLIT_PARAM))
            purpose = SPLIT_REPORT;
        writeReport(taskListName, evModel, effDate, settings, taskFilter,
            purpose);
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
        EVMetrics  metrics = schedule.getMetrics();
        double totalPlanTime = metrics.totalPlan();
        boolean hideNames = settings.getBool(EVReport.CUSTOMIZE_HIDE_NAMES);
        boolean showAssignedTo = (evModel instanceof EVTaskListRollup)
                && !hideNames;
        boolean showTimingIcons = (evModel instanceof EVTaskListData
                && !isExporting() && purpose == PLAIN_REPORT);
        boolean showLabels = evModel.showLabelsColumn();

        // Calculate the dates one week before and after the effective date.
        Date lastWeek = adjustDate(effDate, -EVSchedule.WEEK_MILLIS);
        Date nextWeek = adjustDate(effDate, EVSchedule.WEEK_MILLIS);
        Date startDate = schedule.getStartDate();
        if (lastWeek.before(startDate)) lastWeek = startDate;

        // Calculate future cutoff dates for task dependency display
        Date dependDate = getFutureCutoffDate(effDate,
            Settings.getInt("ev.numDependencyWeeks", 3));
        Date revDependDate = getFutureCutoffDate(effDate,
            Settings.getInt("ev.numReverseDependencyWeeks", 6));

        // Get a slice of the schedule representing the previous week.
        EVSchedule.Period weekSlice = EVScheduleRollup.getSlice(
                getEvSchedule(evModel, taskFilter), lastWeek, effDate);

        // Now scan the task list looking for information we need.
        TableModel tasks = evModel.getSimpleTableModel(taskFilter);
        int taskListLen = tasks.getRowCount();

        // keep track of tasks that should be displayed in the three lists.
        boolean[] completedLastWeek = new boolean[taskListLen];
        boolean[] dueThroughNextWeek = new boolean[taskListLen];
        Map<String, DependencyForCoord> upcomingDependencies =
            new HashMap<String, DependencyForCoord>();
        List<RevDependencyForCoord> reverseDependencies =
            new ArrayList<RevDependencyForCoord>();
        Arrays.fill(completedLastWeek, false);
        Arrays.fill(dueThroughNextWeek, false);
        boolean oneCompletedLastWeek = false;
        boolean oneDueNextWeek = false;

        // keep track of the people assigned to the current schedule
        Set allIndividuals = new HashSet();
        String ignoreIndividual = null;
        if (evModel instanceof EVTaskListData && purpose == PLAIN_REPORT)
            allIndividuals.add(ignoreIndividual = getOwner());

        // keep track of the two total plan/actual time to date for
        // completed tasks.
        double completedTasksTotalPlanTime = 0;
        double completedTasksTotalActualTime = 0;

        for (int i = 0;   i < taskListLen;   i++) {
            Date completed =
                (Date) tasks.getValueAt(i, EVTaskList.DATE_COMPLETE_COLUMN);
            if (completed != null && completed.before(effDate)) {
                    completedTasksTotalPlanTime += parseTime
                        (tasks.getValueAt(i, -EVTaskList.PLAN_DTIME_COLUMN));
                    completedTasksTotalActualTime += parseTime
                        (tasks.getValueAt(i, -EVTaskList.ACT_DTIME_COLUMN));

                    if (!completed.before(lastWeek) &&
                        completed.before(nextWeek))
                        completedLastWeek[i] = oneCompletedLastWeek = true;

            } else {
                Date due =
                    (Date) tasks.getValueAt(i, EVTaskList.PLAN_DATE_COLUMN);
                if (due != null && due.after(startDate)) {
                    if (due.before(nextWeek))
                        dueThroughNextWeek[i] = oneDueNextWeek = true;
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

        /*
         * Okay, we have all the data we need.  Lets generate the HTML.
         */

        if (isTopLevel(purpose)) {
            String titleHTML = resources.format("Title_FMT", taskListName);
            titleHTML = HTMLUtils.escapeEntities(titleHTML);
            StringBuffer header = new StringBuffer(HEADER_HTML);
            StringUtils.findAndReplace(header, TITLE_VAR, titleHTML);
            if (taskFilter != null)
                header.insert(header.indexOf("</head>"), FILTER_HEADER_HTML);
            if (isExportingToExcel())
                StringUtils.findAndReplace(header, "hideIfCollapsed", "ignore");
            out.print(header);

            out.print("<h2>");
            String endDateStr = encodeHTML(new Date(effDate.getTime() - 1000));
            out.print(resources.format("Header_HTML_FMT", endDateStr));
            if (!isExporting()) {
                if (lastWeek.compareTo(startDate) > 0)
                    printNavLink(lastWeek, "Previous", settings, purpose);
                printNavLink(nextWeek, "Next", settings, purpose);
            }
            out.print("</h2>\n");

            EVReport.printFilterInfo(out, taskFilter, isExportingToExcel());

            Map errors = metrics.getErrors();
            if (errors != null && errors.size() > 0) {
                out.print("<table border><tr><td class='modelErrors'><h2>");
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

        interpOut("<tr><td class=left>${Summary.This_Week}</td><td></td>");
        if (taskFilter == null) {
            printTimeData(weekSlice.planDirectTime(),
                    weekSlice.actualDirectTime());
            out.print("<td></td>");
        }
        printPctData(weekSlice.planValue()/totalPlanTime,
                     weekSlice.earnedValue()/totalPlanTime);
        out.print("</tr>\n");

        interpOut("<tr" + indivDetail
                + "><td class=left>${Summary.To_Date}</td><td></td>");
        if (taskFilter == null) {
            printTimeData(weekSlice.cumPlanDirectTime(),
                          weekSlice.cumActualDirectTime());
            out.print("<td></td>");
        }
        printPctData(weekSlice.cumPlanValue()/totalPlanTime,
                     weekSlice.cumEarnedValue()/totalPlanTime);
        out.print("</tr>\n");

        double numWeeks = Double.NaN;
        if (startDate != null)
            numWeeks = (effDate.getTime() - startDate.getTime() - EVSchedule
                    .dstDifference(startDate.getTime(), effDate.getTime()))
                    / (double) MILLIS_PER_WEEK;
        interpOut("<tr" + indivDetail
                + "><td class=left>${Summary.Average_per_Week}</td><td></td>");
        if (taskFilter == null) {
            double planTimePerWeek = weekSlice.cumPlanDirectTime() / numWeeks;
            double actualTimePerWeek =
                weekSlice.cumActualDirectTime() / numWeeks;
            printTimeData(planTimePerWeek, actualTimePerWeek);
            out.print("<td></td>");
        }
        double planEVPerWeek =
            weekSlice.cumPlanValue() / (totalPlanTime * numWeeks);
        double actualEVPerWeek =
            weekSlice.cumEarnedValue() / (totalPlanTime * numWeeks);
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

            interpOut("<h3>${Completed_Tasks.Header}</h3>\n");
            if (!oneCompletedLastWeek)
                interpOut("<p><i>${None}</i>\n");
            else {
                interpOut("<table border=1 name='compTask' class='sortable' " +
                                "id='$$$_comp'><tr>" +
                          "<td></td>"+
                          "<td class=header>${Columns.Planned_Time}</td>"+
                          "<td class=header>${Columns.Actual_Time}</td>"+
                          "<td class=header>${Columns.Percent_Spent}</td>");
                if (showAssignedTo)
                    interpOut("<td class=header>${Columns.Assigned_To}</td>");
                interpOut("<td class=header>${Columns.Planned_Date}</td>");
                if (showLabels)
                    interpOut("<td class=header>${Columns.Labels}</td>");
                interpOut("<td class=header>${Columns.Earned_Value}</td>"+
                          "</tr>\n");

                for (int i = 0;   i < taskListLen;   i++)
                    if (completedLastWeek[i])
                        printCompletedLine(tableWriter, tasks, i,
                            showAssignedTo, showLabels);

                out.println("</table>");
            }

            // put the "task with timing icons" renderer back in place if necessary
            tableWriter.setCellRenderer(EVTaskList.TASK_COLUMN, taskRenderer);
            interpOut("<h3>${Due_Tasks.Header}</h3>\n");
            if (!oneDueNextWeek)
                interpOut("<p><i>${None}</i>\n");
            else {
                interpOut("<table border=1 name='dueTask' class='sortable' id='$$$_due'><tr>" +
                          "<td></td>"+
                          "<td class=header>${Columns.Planned_Time}</td>"+
                          "<td class=header>${Columns.Actual_Time}</td>"+
                          "<td class=header>${Columns.Percent_Spent}</td>");
                if (showAssignedTo)
                    interpOut("<td class=header>${Columns.Assigned_To}</td>");
                interpOut("<td class=header>${Columns.Planned_Date}</td>");
                if (showLabels)
                    interpOut("<td class=header>${Columns.Labels}</td>");
                interpOut("<td class=header title='${Columns.Depend_Tooltip}'>${Columns.Depend}</td>"+
                          "<td class=header>${Columns.Forecast_Time_Remaining}</td>"+
                          "</tr>\n");

                double timeRemaining = 0;
                for (int i = 0;   i < taskListLen;   i++)
                    if (dueThroughNextWeek[i])
                        timeRemaining += printDueLine(tableWriter, tasks, i, cpi,
                                showAssignedTo, showLabels);

                out.print("<tr class='sortbottom'><td align=right colspan=");
                int colspan = 6 + (showAssignedTo ? 1:0) + (showLabels ? 1:0);
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
                    printReverseDependencyTable(tasks, reverseDependencies);

                List<DependencyForCoord> depsForCoord =
                    new ArrayList<DependencyForCoord>(
                        upcomingDependencies.values());
                Collections.sort(depsForCoord);
                int pos = 0;
                for (DependencyForCoord coord : depsForCoord) {
                    printUpcomingDependencies(coord, tasks, showAssignedTo,
                        hideNames, showLabels, pos++);
                }
            }

            if (!isExportingToExcel())
                interpOut(EXPORT_HTML);

            out.print(FOOTER_HTML);
        }
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
        StringBuffer href = new StringBuffer("week.class");
        HTMLUtils.appendQuery(href, EFF_DATE_PARAM,
                Long.toString(effDate.getTime()));
        HTMLUtils.appendQuery(href,
                settings.getQueryString(EVReportSettings.PURPOSE_NAV_LINK));
        if (purpose == SPLIT_REPORT)
            HTMLUtils.appendQuery(href, SPLIT_PARAM, "t");

        out.print("&nbsp;&nbsp;<span class='nav'><a href='");
        out.print(href);
        out.print("'>");
        out.print(resources.getHTML(resKey));
        out.print("</a></span>");
    }


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

    protected void printCompletedLine(HTMLTableWriter tableWriter, TableModel tasks, int i,
            boolean showAssignedTo, boolean showLabels) throws IOException {
        out.print("<tr>");
        tableWriter.writeCell(out, tasks, i, EVTaskList.TASK_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.ACT_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PCT_SPENT_COLUMN);
        if (showAssignedTo)
            tableWriter.writeCell(out, tasks, i, EVTaskList.ASSIGNED_TO_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_DATE_COLUMN);
        if (showLabels)
            tableWriter.writeCell(out, tasks, i, EVTaskList.LABELS_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.VALUE_EARNED_COLUMN);
        out.println("</tr>");
    }

    protected double printDueLine(HTMLTableWriter tableWriter,
            TableModel tasks, int i, double cpi, boolean showAssignedTo,
            boolean showLabels) throws IOException {
        out.print("<tr>");
        tableWriter.writeCell(out, tasks, i, EVTaskList.TASK_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.ACT_TIME_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PCT_SPENT_COLUMN);
        if (showAssignedTo)
            tableWriter.writeCell(out, tasks, i, EVTaskList.ASSIGNED_TO_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.PLAN_DATE_COLUMN);
        if (showLabels)
            tableWriter.writeCell(out, tasks, i, EVTaskList.LABELS_COLUMN);
        tableWriter.writeCell(out, tasks, i, EVTaskList.DEPENDENCIES_COLUMN);

        double planTime = parseTime(tasks.getValueAt(i, -EVTaskList.PLAN_TIME_COLUMN));
        double actualTime = parseTime(tasks.getValueAt(i, -EVTaskList.ACT_TIME_COLUMN));
        double forecastTimeRemaining;
        if (cpi > 0 && !Double.isInfinite(cpi))
            forecastTimeRemaining = (planTime / cpi) - actualTime;
        else
            forecastTimeRemaining = planTime - actualTime;
        if (forecastTimeRemaining > 0)
            HTMLTableWriter.writeCell(out, EVReport.TIME_CELL_RENDERER,
                    formatTime(forecastTimeRemaining), 0, 0);
        else
            out.print("<td class='error' "
                    + EVReport.getSortAttribute("-1")
                    + ">0:00&nbsp;&nbsp;???");
        out.println("</td></tr>");
        return forecastTimeRemaining > 0 ? forecastTimeRemaining : 0;
    }

    private void printReverseDependencyTable(TableModel tasks,
            List<RevDependencyForCoord> reverseDependencies) {

        Collections.sort(reverseDependencies);

        out.print("<div class='expanded'>");
        printExpansionIcon();
        interpOut("${Dependencies.Reverse.Header}\n"
                + "<div class='dependDetail hideIfCollapsed'>\n"
                + "<table border=1 name='extCommit' class='sortable' "
                + "id='$$$_extCommit'>\n<tr>"
                + "<td class=header>${Columns.Task_Commitment}</td>"
                + "<td class=header>${Columns.Projected_Date}</td>"
                + "<td class=header>${Columns.Needed_By}</td>"
                + "<td class=header>${Columns.Need_Date}</td></tr>\n");

        for (RevDependencyForCoord revCoord : reverseDependencies) {
            int i = revCoord.rowNumber;
            out.print("<td class='left'>");
            out.print(encodeHTML(tasks.getValueAt(i, EVTaskList.TASK_COLUMN)));
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
            TableModel tasks, boolean showAssignedTo,
            boolean hideNames, boolean showLabels, int pos) {

        boolean isExcel = isExportingToExcel();
        EVTaskDependency d = coord.d;
        Date origParentDate = d.getParentDate();

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

            Date projDate = (Date) tasks.getValueAt(i,
                EVTaskList.PROJ_DATE_COLUMN);
            d.setParentDate(projDate);
            printDateCell(projDate,
                TaskDependencyAnalyzer.HTML_INCOMPLETE_MISORD_IND,
                d.isMisordered());

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

        d.setParentDate(origParentDate);
    }

    private void printExpansionIcon() {
        if (!isExportingToExcel())
            out.print("<a onclick='toggleExpanded(this); return false;' " +
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
        "<style> td { text-align:right } td.left { text-align:left } "+
        "td.center { text-align: center } " +
        "td.error  { font-style: italic;  color: red }\n" +
        "td.header { text-align:center; font-weight:bold; "+
                           " vertical-align:bottom }\n" +
        "td.modelErrors { text-align: left; background-color: #ff5050 }\n" +
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
        "</style>\n"+
        "<script type='text/javascript' src='/lib/prototype.js'> </script>\n" +
        "<script>\n" +
        "function toggleExpanded(elem) {\n" +
        "    elem = findExpandableElem($(elem));\n" +
        "    if (elem) {\n" +
        "        if (Element.hasClassName(elem, \"expanded\")) {\n" +
        "           Element.removeClassName(elem, \"expanded\");\n" +
        "           Element.addClassName(elem, \"collapsed\");\n" +
        "       } else {\n" +
        "           Element.removeClassName(elem, \"collapsed\");\n" +
        "           Element.addClassName(elem, \"expanded\");\n" +
        "       }\n" +
        "    }\n" +
        "}\n" +
        "function findExpandableElem(elem) {\n" +
        "    if (!elem) return elem;\n" +
        "    if (Element.hasClassName(elem, \"expanded\")) { return elem; }\n" +
        "    if (Element.hasClassName(elem, \"collapsed\")) { return elem; }\n" +
        "    return findExpandableElem(elem.parentNode);\n" +
        "}\n" +
        "</script>\n" +
        EVReport.REDUNDANT_EXCEL_HEADER +
        EVReport.SORTTABLE_HEADER +
        EVReport.POPUP_HEADER +
        "</head><body><h1>%title%</h1>\n";
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

        return WebServer.encodeHtmlEntities(text.toString());
    }

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
