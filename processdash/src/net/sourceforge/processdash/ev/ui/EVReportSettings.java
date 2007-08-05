// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVLabelFilter;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class EVReportSettings {

    static final String TASKLIST_PARAM = "tl";
    static final String LABEL_FILTER_PARAM = "labelFilter";
    static final String LABEL_FILTER_AUTO_PARAM = "labelFilterAuto";

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
        } else if (StringUtils.hasValue(getParameter(LABEL_FILTER_PARAM)))
            HTMLUtils.appendQuery(query, LABEL_FILTER_PARAM,
                    getParameter(LABEL_FILTER_PARAM));

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

    /** Build a task filter object that should be used to display the report.
     */
    public EVTaskFilter getEffectiveFilter(EVTaskList evModel) {
        String filter = null;
        if (parameters.containsKey(LABEL_FILTER_PARAM))
            filter = getParameter(LABEL_FILTER_PARAM);
        else if (usingCustomizationSettings) {
            SimpleData val = getValue("settings//" + LABEL_FILTER_PARAM);
            filter = (val == null ? null : val.format());
        }

        if (StringUtils.hasValue(filter)) {
            try {
                EVTaskFilter result = new EVLabelFilter(evModel, filter,
                        data);
                return result;
            } catch (Exception e) {
            }
        }

        return null;
    }


    /*
     * Routines for internal use
     */


    private void loadCustomizationSettings() {
        taskListName = determineTaskListName();

        if (parameters.containsKey(LABEL_FILTER_AUTO_PARAM))
            lookupLabelFilter();

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

    private SimpleData getValue(String name) {
        String dataName = getSettingDataName(name);
        return data.getSimpleValue(dataName);
    }

    private void setValue(String name, SimpleData val) {
        String dataName = getSettingDataName(name);
        data.putValue(dataName, val);
    }

    private String getSettingDataName(String name) {
        String prefix;
        if (parameters.containsKey(TASKLIST_PARAM))
            prefix = "/" + taskListName;
        else
            prefix = this.prefix;

        return DataRepository.createDataName(prefix, name);
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
