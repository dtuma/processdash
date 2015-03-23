// Copyright (C) 2011 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.SimpleDataContext;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.TaskScheduleChartSettings.PersistenceException;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.util.StringUtils;

public class TaskScheduleChartUtil {

    private static Logger logger = Logger
            .getLogger(TaskScheduleChartSettings.class.getName());



    public static class ChartItem implements Comparable<ChartItem> {
        /** A snippet describing a chart */
        public SnippetDefinition snip;
        /** The settings to use for the chart.  (May be null.) */
        public TaskScheduleChartSettings settings;
        /** The name to display for the chart */
        public String name;

        public ChartItem(SnippetDefinition snip,
                TaskScheduleChartSettings settings) {
            this.snip = snip;
            this.settings = settings;
            if (settings != null)
                name = settings.getCustomName();
            if (!StringUtils.hasValue(name))
                name = snip.getName();
        }

        public int compareTo(ChartItem that) {
            return this.name.compareTo(that.name);
        }
    }


    public enum ChartListPurpose { ChartWindow, ReportMain, ReportAll };

    /**
     * Retrieve a list of charts that are applicable for a particular task list.

     * @param taskListID the ID of the task list
     * @param data the data repository
     * @param filterInEffect true if a filter is in effect
     * @param isRollup true if the task list is a rollup
     * @param hideNames true if the identities of individuals should be
     *      protected
     *
     * @return a collection of {@link ChartItem} objects describing the
     *      snippets for the relevant charts, and the settings that should
     *      be used to display those snippets.
     */
    public static List<ChartItem> getChartsForTaskList(String taskListID,
            DataRepository data, boolean filterInEffect, boolean isRollup,
            boolean hideNames, ChartListPurpose purpose) {
        Map<String, TaskScheduleChartSettings> chartSettings =
            TaskScheduleChartSettings.getSettingsForTaskList(taskListID, data);

        Map<String, ChartItem> result = new HashMap<String, ChartItem>();

        SimpleDataContext ctx = getContextTags(filterInEffect, isRollup,
            hideNames);

        SnippetDefinitionManager.initialize();
        for (SnippetDefinition snip : SnippetDefinitionManager
                .getSnippetsInCategory("ev")) {
            if (snip.matchesContext(ctx)) {
                try {
                    ChartItem item = new ChartItem(snip, chartSettings
                            .remove(snip.getId()));
                    result.put(snip.getId(), item);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Problem with EV Chart Snippet '"
                            + snip.getId() + "'", e);
                }
            }
        }

        for (TaskScheduleChartSettings customChart : chartSettings.values()) {
            ChartItem base = result.get(customChart.getChartID());
            if (base != null) {
                ChartItem custom = new ChartItem(base.snip, customChart);
                result.put(customChart.getSettingsIdentifier(), custom);
            }
        }

        List preferredCharts = new ArrayList<ChartItem>();
        if (purpose == ChartListPurpose.ReportMain
                || purpose == ChartListPurpose.ReportAll) {
            List<String> preferredChartOrdering = TaskScheduleChartSettings
                    .getPreferredChartOrdering(taskListID, data);
            for (String oneId : preferredChartOrdering) {
                if (TaskScheduleChartSettings.SECONDARY_CHART_MARKER
                        .equals(oneId)) {
                    if (purpose == ChartListPurpose.ReportMain)
                        break;
                    else
                        preferredCharts.add(null);
                } else {
                    ChartItem chart = result.remove(oneId);
                    if (chart != null)
                        preferredCharts.add(chart);
                }
            }
            if (purpose == ChartListPurpose.ReportMain)
                return preferredCharts;
            else if (!preferredCharts.contains(null))
                preferredCharts.add(null);
        }

        List finalResult = new ArrayList<ChartItem>(result.values());
        Collections.sort(finalResult);
        finalResult.addAll(0, preferredCharts);
        return finalResult;
    }

    private static SimpleDataContext getContextTags(boolean filterInEffect,
            boolean isRollup, boolean hideNames) {
        SimpleDataContext ctx = new SimpleDataContext();
        TagData tag = TagData.getInstance();

        ctx.put(EVSnippetEnvironment.EV_CONTEXT_KEY, tag);
        if (isRollup)
            ctx.put(EVSnippetEnvironment.ROLLUP_EV_CONTEXT_KEY, tag);
        if (filterInEffect)
            ctx.put(EVSnippetEnvironment.FILTERED_EV_CONTEXT_KEY, tag);
        if (hideNames)
            ctx.put(EVSnippetEnvironment.ANON_EV_CONTEXT_KEY, tag);

        return ctx;
    }


    /**
     * Construct the environment that should be passed to an EV Chart snippet
     * widget.
     */
    public static Map getEnvironment(EVTaskList taskList, EVSchedule schedule,
            EVTaskFilter filter, SnippetDefinition snip, DashboardContext ctx) {
        Map environment = new HashMap();
        environment.put(EVSnippetEnvironment.TASK_LIST_KEY, taskList);
        environment.put(EVSnippetEnvironment.SCHEDULE_KEY, schedule);
        environment.put(EVSnippetEnvironment.TASK_FILTER_KEY, filter);
        environment.put(EVSnippetEnvironment.RESOURCES, snip
                .getResources());
        environment.put(TinyCGI.DASHBOARD_CONTEXT, ctx);
        environment.put(TinyCGI.DATA_REPOSITORY, ctx.getData());
        environment.put(TinyCGI.PSP_PROPERTIES, ctx.getHierarchy());
        return environment;
    }


    /**
     * Get the parameters that should be passed to an EV Chart snippet widget.
     * 
     * @throws PersistenceException
     *             if the parameters for this settings object cannot be
     *             read/parsed
     */
    public static Map getParameters(TaskScheduleChartSettings settings)
            throws PersistenceException {

        Map result = new HashMap();
        if (settings != null) {
            try {
                result.putAll(settings.getParameters());
                result.put(EVSnippetEnvironment.SNIPPET_VERSION,
                    settings.getChartVersion());
                result.put(EVSnippetEnvironment.EV_CUSTOM_SNIPPET_NAME_KEY,
                    settings.getCustomName());
            } catch (PersistenceException pe) {
                // if this is a custom chart built by a user, we can't
                // guess how the chart should be drawn.  Rethrow
                // the exception.
                if (settings.getCustomName() != null)
                    throw pe;

                // otherwise, if this is a standard chart and the
                // saved settings can't be read, revert back to the
                // default settings provided by that chart.
                logger.log(Level.SEVERE, "Unexpected problem reading "
                        + "settings for chart with id '" + settings.getChartID()
                        + "' - reverting to defaults", pe);
                result = new HashMap();
            }
        }
        return result;
    }
}
