// Copyright (C) 2006-2016 Tuma Solutions, LLC
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

import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVLabelFilter;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.ui.web.reports.ExcelReport;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class EVReportSettings {

    static final String TASKLIST_PARAM = "tl";
    static final String LABEL_FILTER_PARAM = "labelFilter";
    static final String LABEL_FILTER_AUTO_PARAM = "labelFilterAuto";
    static final String PATH_FILTER_PARAM = "pathFilter";
    static final String MERGED_PATH_FILTER_PARAM = "mergedPathFilter";
    static final String PATH_FILTER_AUTO_PARAM = "pathFilterAuto";
    static final String GROUP_FILTER_PARAM = "groupFilter";
    static final String GROUP_FILTER_AUTO_PARAM = "groupFilterAuto";
    static final String PRESERVE_LEAVES_PARAM = "preserveLeaves";
    public static final String CUSTOMIZE_HIDE_NAMES = "hideAssignedTo";

    private DataRepository data;
    private Map parameters;
    private String prefix;

    private String taskListName;
    private boolean usingCustomizationSettings;



    public EVReportSettings(DataRepository data, Map params, String prefix) {
        this.data = data;
        this.parameters = params;
        this.prefix = prefix;

        loadCustomizationSettings();
    }


    /** Return the name of the task list that should be used for the report. */
    public String getTaskListName() {
        return taskListName;
    }


    /** Return the value of a boolean parameter or setting. */
    public boolean getBool(String name) {
        boolean defaultVal = Settings.getBool("ev."+name, false);
        if (!usingCustomizationSettings)
            return defaultVal;
        SimpleData val = getValue("settings//" + name);
        return (val != null ? val.test() : defaultVal);
    }

    /** Return the value of a string parameter or setting */
    public String getStr(String name) {
        String defaultVal = Settings.getVal("ev."+name);
        if (!usingCustomizationSettings)
            return defaultVal;
        SimpleData val = getValue("settings//" + name);
        return (val != null ? val.format() : defaultVal);
    }


    /** Save the value of a setting to the data repository.
     * 
     * @param settingName the name of the setting.  (The value of the setting
     *    will be read from the parameters used to create this object.)
     * @param isBool true if this is a boolean setting, false if it is a string.
     */
    public void store(String settingName, boolean isBool) {
        SimpleData val = null;
        if (isBool)
            val = (parameters.containsKey(settingName) ? ImmutableDoubleData.TRUE
                    : ImmutableDoubleData.FALSE);
        else if (parameters.get(settingName) instanceof String)
            val = StringData.create(getParameter(settingName));

        setValue("settings//" + settingName, val);
        touchSettingsTimestamp();
    }

    /** Save the default value of a setting to the user settings.
     * 
     * @param settingName the name of the setting.  (The value of the setting
     *    will be read from the parameters used to create this object.)
     * @param isBool true if this is a boolean setting, false if it is a string.
     */
    public void storeDefault(String settingName, boolean isBool) {
        String val = null;
        if (isBool)
            val = (parameters.containsKey(settingName) ? "true" : "false");
        else if (parameters.get(settingName) instanceof String)
            val = getParameter(settingName);
        InternalSettings.set("ev." + settingName, val);
    }


    /** Return the query string that should be appended to the URIs of related
     * resources
     * 
     * @param purpose the type of URI to be constructed, one of
     *     {@link #PURPOSE_WEEK} or {@link #PURPOSE_TASK_STYLE}
     * @return a query string to use (could be the empty string)
     */
    public String getQueryString(int purpose) {
        StringBuffer query = new StringBuffer();

        if (purpose != PURPOSE_TASK_STYLE
                && parameters.containsKey(TASKLIST_PARAM))
            HTMLUtils.appendQuery(query, TASKLIST_PARAM,
                    getParameter(TASKLIST_PARAM));

        if (parameters.containsKey(LABEL_FILTER_AUTO_PARAM)
                && purpose != PURPOSE_IMAGE) {
            if (purpose == PURPOSE_WEEK)
                HTMLUtils.appendQuery(query, LABEL_FILTER_AUTO_PARAM, "t");
        } else if (StringUtils.hasValue(getParameter(LABEL_FILTER_PARAM))) {
            HTMLUtils.appendQuery(query, LABEL_FILTER_PARAM,
                    getParameter(LABEL_FILTER_PARAM));
        }

        if (parameters.containsKey(PATH_FILTER_AUTO_PARAM)
                && purpose != PURPOSE_IMAGE) {
            if (purpose == PURPOSE_WEEK)
                HTMLUtils.appendQuery(query, PATH_FILTER_AUTO_PARAM, "t");
        } else if (StringUtils.hasValue(getParameter(PATH_FILTER_PARAM))) {
            HTMLUtils.appendQuery(query, PATH_FILTER_PARAM,
                    getParameter(PATH_FILTER_PARAM));
        } else if (StringUtils.hasValue(getParameter(MERGED_PATH_FILTER_PARAM))) {
            HTMLUtils.appendQuery(query, MERGED_PATH_FILTER_PARAM,
                    getParameter(MERGED_PATH_FILTER_PARAM));
        }

        if (parameters.containsKey(GROUP_FILTER_AUTO_PARAM)
                && purpose != PURPOSE_IMAGE) {
            if (purpose == PURPOSE_WEEK)
                HTMLUtils.appendQuery(query, GROUP_FILTER_AUTO_PARAM, "t");
        } else if (StringUtils.hasValue(getParameter(GROUP_FILTER_PARAM))) {
            HTMLUtils.appendQuery(query, GROUP_FILTER_PARAM,
                    getParameter(GROUP_FILTER_PARAM));
        }

        return query.toString();
    }
    public static final int PURPOSE_WEEK = 0;
    public static final int PURPOSE_NAV_LINK = PURPOSE_WEEK;
    public static final int PURPOSE_TASK_STYLE = 1;
    public static final int PURPOSE_IMAGE = 2;
    public static final int PURPOSE_OTHER = PURPOSE_WEEK;


    /** Return the prefix that could be prepended to a URI to ...?
     * 
     * @return
     */
    public String getEffectivePrefix() {
        if (parameters.containsKey(TASKLIST_PARAM))
            return "/" + HTMLUtils.urlEncode(taskListName) + "//reports/";
        else
            return "";
    }

    /**
     * Get the user group filter that should be used to display the report.
     */
    public UserFilter getUserGroupFilter() {
        if (!UserGroupManager.getInstance().isEnabled())
            return null;

        String filterID = getParameter(GROUP_FILTER_PARAM);
        UserFilter f = UserGroupManager.getInstance().getFilterById(filterID);
        if (f == null)
            f = UserGroupManager.getEveryonePseudoGroup();
        return f;
    }

    /** Build a task filter object that should be used to display the report.
     */
    public EVTaskFilter getEffectiveFilter(EVTaskList evModel) {
        EVTaskFilter result = null;

        // first, look up any applicable label filter
        String labelFilter = null;
        if (parameters.containsKey(LABEL_FILTER_PARAM))
            labelFilter = getParameter(LABEL_FILTER_PARAM);
        else if (usingCustomizationSettings) {
            SimpleData val = getValue("settings//" + LABEL_FILTER_PARAM);
            labelFilter = (val == null ? null : val.format());
        }

        // if we found a label filter, apply it.
        if (StringUtils.hasValue(labelFilter)) {
            try {
                result = new EVLabelFilter(evModel, labelFilter, data);
            } catch (Exception e) {
            }
        }

        // next, look up any applicable path filter
        String pathFilter = null;
        if (parameters.containsKey(PATH_FILTER_PARAM))
            pathFilter = getParameter(PATH_FILTER_PARAM);
        else if (usingCustomizationSettings) {
            SimpleData val = getValue("settings//" + PATH_FILTER_PARAM);
            pathFilter = (val == null ? null : val.format());
        }

        // if we found a path filter, apply it
        if (StringUtils.hasValue(pathFilter)) {
            EVHierarchicalFilter hf = EVHierarchicalFilter.getFilter(evModel,
                pathFilter);
            if (hf != null)
                result = hf.appendFilter(result);
        } else {

            // next, look for a merged path filter
            String mergedPathFilter = null;
            if (parameters.containsKey(MERGED_PATH_FILTER_PARAM))
                mergedPathFilter = getParameter(MERGED_PATH_FILTER_PARAM);
            else if (usingCustomizationSettings) {
                SimpleData val = getValue("settings//" + MERGED_PATH_FILTER_PARAM);
                mergedPathFilter = (val == null ? null : val.format());
            }

            // if we found a merged path filter, apply it
            if (StringUtils.hasValue(mergedPathFilter)) {
                EVHierarchicalFilter hf = EVHierarchicalFilter
                        .getFilterForMerged(evModel, mergedPathFilter);
                if (hf != null)
                    result = hf.appendFilter(result);
            }
        }

        if (result != null)
            evModel.disableBaselineData();

        return result;
    }

    /** Determine whether leaves should be preserved in a merged task list. */
    public boolean shouldMergePreserveLeaves() {
        // In a merged model, we preserve leaves if we want to see the task
        // breakdowns by individual.  Of course, if we're hiding names in this
        // report, there is no reason to preserve those anonymous leaves.
        if (getBool(CUSTOMIZE_HIDE_NAMES))
            return false;

        // if the user has explicitly provided a query parameter with
        // instructions on leaf preservation, honor it.
        if (parameters.containsKey(PRESERVE_LEAVES_PARAM)) {
            if ("false".equals(parameters.get(PRESERVE_LEAVES_PARAM)))
                return false;
            else
                return true;
        }

        // otherwise, look for a global default setting.
        return Settings.getBool("ev.mergePreservesLeaves", true);
    }

    public Map getParameters() {
        return parameters;
    }

    public boolean isExporting() {
        return parameters.get("EXPORT") != null;
    }

    public boolean exportingToExcel() {
        return ExcelReport.EXPORT_TAG.equals(getParameter("EXPORT"));
    }


    /*
     * Routines for internal use
     */


    private void loadCustomizationSettings() {
        taskListName = determineTaskListName();

        if (parameters.containsKey(LABEL_FILTER_AUTO_PARAM))
            lookupLabelFilter();
        if (parameters.containsKey(PATH_FILTER_AUTO_PARAM))
            lookupPathFilter();
        if (parameters.containsKey(GROUP_FILTER_AUTO_PARAM))
            lookupGroupFilter();

        usingCustomizationSettings = isTimestampRecent();
    }


    private String determineTaskListName() {
        String result = getParameter(TASKLIST_PARAM);
        if ("auto".equals(result))
            return getAutoTaskList();
        else if (result != null && result.length() > 0)
            return result;
        else {
            result = prefix;
            if (result == null || result.length() < 2)
                return null;

            // strip the initial slash
            result = result.substring(1);

            // strip the "publishing prefix" if it is present.
            if (result.startsWith("ev /"))
                result = result.substring(4);
            else if (result.startsWith("evr /"))
                result = result.substring(5);
            return result;
        }
    }


    /** Attempt to automatically determine the task list to use for a
     * particular project.
     * 
     * @return the name of a task list, if a suitable one was found; otherwise,
     *     null.
     */
    private String getAutoTaskList() {
        // check and see whether there is exactly one preferred task list for
        // the prefix in question.
        List taskLists = EVTaskList.getPreferredTaskListsForPath(data, prefix);
        if (taskLists != null && taskLists.size() == 1)
            return (String) taskLists.get(0);

        // we can't guess.  Give up.
        return null;
    }

    private void lookupLabelFilter() {
        SaveableData sval = data.getInheritableValue(prefix, "Label//Filter");
        SimpleData val = (sval == null ? null : sval.getSimpleValue());
        parameters.put(LABEL_FILTER_PARAM, val == null ? "" : val.format());
    }

    private void lookupPathFilter() {
        SaveableData sval = data.getInheritableValue(prefix, "Earned_Value//Path_Filter");
        if (sval == null) sval = data.getInheritableValue(prefix, "Earned_Value_Path_Filter");
        SimpleData val = (sval == null ? null : sval.getSimpleValue());
        parameters.put(PATH_FILTER_PARAM, val == null ? "" : val.format());

        sval = data.getInheritableValue(prefix, "Earned_Value//Merged_Path_Filter");
        if (sval == null) sval = data.getInheritableValue(prefix, "Earned_Value_Merged_Path_Filter");
        val = (sval == null ? null : sval.getSimpleValue());
        parameters.put(MERGED_PATH_FILTER_PARAM, val == null ? "" : val.format());
    }

    private void lookupGroupFilter() {
        SaveableData sval = data.getInheritableValue(prefix, UserGroupManager.FILTER_DATANAME);
        SimpleData val = (sval == null ? null : sval.getSimpleValue());
        parameters.put(GROUP_FILTER_PARAM, val == null ? "" : val.format());
    }

    private SimpleData getValue(String name) {
        String dataName = getSettingDataName(name);
        return data.getSimpleValue(dataName);
    }

    private void setValue(String name, SimpleData val) {
        String dataName = getSettingDataName(name);
        data.putValue(dataName, val);
    }

    private String getSettingDataName(String name) {
        return DataRepository.createDataName(getSettingsPrefix(), name);
    }

    /** Return the prefix that was used to create this settings object */
    public String getSettingsPrefix() {
        if (parameters.containsKey(TASKLIST_PARAM))
            return "/" + taskListName;
        else
            return this.prefix;
    }

    private boolean isTimestampRecent() {
        DateData settingsTimestamp = (DateData) getValue("settings//timestamp");
        if (settingsTimestamp == null)
            return false;
        long when = settingsTimestamp.getValue().getTime();
        long delta = System.currentTimeMillis() - when;
        if (10000 < delta && delta < MAX_SETTINGS_AGE)
            touchSettingsTimestamp();
        return (delta < MAX_SETTINGS_AGE);
    }

    private void touchSettingsTimestamp() {
        setValue("settings//timestamp", new DateData());
    }

    private static final long MAX_SETTINGS_AGE =
            60 /*mins*/* 60 /*sec*/* 1000 /*millis*/;


    private String getParameter(String name) {
        return (String) parameters.get(name);
    }
}
