// Copyright (C) 2009-2011 Tuma Solutions, LLC
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.net.cms.ParamDataPersister;
import net.sourceforge.processdash.net.cms.XmlParamDataPersisterV1;
import net.sourceforge.processdash.util.HttpQueryParser;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class TaskScheduleChartSettings {

    public class PersistenceException extends Exception {
        private PersistenceException(String message) {
            super(message); }
        private PersistenceException(String message, Throwable cause) {
            super(message, cause); }
    }

    private String taskListID;

    private String chartID;

    private String customName;

    private String xml;

    private String chartVersion;

    private Map parameters;


    public TaskScheduleChartSettings() {}

    private TaskScheduleChartSettings(String dataName, String value) {
        Matcher m = DATA_NAME_PATTERN.matcher(dataName);
        if (!m.matches())
            throw new IllegalArgumentException("Invalid data name");

        String qualifier = m.group(1);
        if (GLOBAL_QUALIFIER.equals(qualifier))
            this.taskListID = null;
        else
            this.taskListID = qualifier;

        this.chartID = m.group(2);

        if (m.group(3) == null)
            this.customName = null;
        else
            this.customName = m.group(3).substring(1);

        this.xml = value;
    }

    /**
     * @return the ID of the task list that these settings are associated with,
     *         or null if they are global settings.
     */
    public String getTaskListID() {
        return taskListID;
    }

    public void setTaskListID(String taskListID) {
        this.taskListID = taskListID;
    }

    /**
     * @return the unique ID of the snippet/widget that these settings apply to
     */
    public String getChartID() {
        return chartID;
    }

    public void setChartID(String chartID) {
        this.chartID = chartID;
    }

    /**
     * @return the custom chart name selected by the user for these settings, or
     *         null if these settings apply to the built-in chart.
     */
    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * @return the version of the snippet/widget that created these settings
     */
    public String getChartVersion() throws PersistenceException {
        if (chartVersion == null)
            parseXML();
        return chartVersion;
    }

    public void setChartVersion(String chartVersion) {
        this.chartVersion = chartVersion;
        this.xml = null;
    }

    /**
     * Returns a string that describes this settings object.
     * 
     * If this settings object has a custom name, the string will be of the
     * form "chartID/customName".  Otherwise, it will be the same as the
     * chartID.
     */
    public String getSettingsIdentifier() {
        if (StringUtils.hasValue(customName))
            return chartID + "/" + customName;
        else
            return chartID;
    }

    /**
     * @return true if this object represents a global setting, false if it
     *       represents a task list specific setting
     */
    public boolean isGlobal() {
        return taskListID == null;
    }

    /**
     * @param that another settings object for comparison
     * @return true if this settings object has the same "custom name" as
     *     the given settings object.
     */
    public boolean hasSameNameAs(TaskScheduleChartSettings that) {
        String thatName = (that == null ? null : that.customName);
        return eq(customName, thatName);
    }

    /**
     * @param that another settings object for comparison
     * @return true if this settings object has the same scope (global vs
     *     a particular task-list) as the given settings object.
     */
    public boolean hasSameScopeAs(TaskScheduleChartSettings that) {
        return (that != null && eq(this.taskListID, that.taskListID));
    }

    private boolean eq(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * @return parameters that this snippet/widget should use.  An empty map
     *         implies "use default settings".
     * @throws PersistenceException
     */
    public Map getParameters() throws PersistenceException {
        if (parameters == null)
            parseXML();
        return parameters;
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
        this.xml = null;
    }

    private void parseXML() throws PersistenceException {
        if (!StringUtils.hasValue(xml)) {
            this.chartVersion = "";
            this.parameters = new HashMap();
            return;
        }

        Element doc;
        try {
            doc = XMLUtils.parse(xml).getDocumentElement();
        } catch (Exception ex) {
            throw new PersistenceException("unparseable chart settings", ex);
        }

        // for now, only support the XML persister.  If the attribute states
        // that a different persister was used, abort.
        if (!XML_PERSISTER_ID.equals(doc.getAttribute(PERSISTER_ATTR)))
            throw new PersistenceException(
                    "unexpected data persistence mechanism");

        this.chartVersion = doc.getAttribute(VERSION_ATTR);

        Map params = new HashMap();
        String persistedText = XMLUtils.getTextContents(doc);
        if (persistedText != null && persistedText.trim().length() > 0) {
            try {
                String query = PERSISTER.getQueryString(persistedText);
                QUERY_PARSER.parse(params, query);
            } catch (Exception e) {
                throw new PersistenceException(
                        "could not parse chart parameters", e);
            }
        }
        this.parameters = params;
    }

    /**
     * Save this chart setting into the data repository.
     */
    public void save(DataRepository data) {
        String dataName = getDataName();
        if (this.xml == null)
            this.xml = buildXML();
        data.putValue(dataName, StringData.create(this.xml));
    }

    /**
     * @return true if a value is already present in the data repository for
     *     this chart setting (and would be overwritten by a save).
     */
    public boolean exists(DataRepository data) {
        String dataName = getDataName();
        return data.getValue(dataName) != null;
    }

    /**
     * Delete this chart setting from the data repository.
     */
    public void delete(DataRepository data) {
        String dataName = getDataName();
        data.putValue(dataName, null);
    }

    private String getDataName() {
        StringBuilder result = new StringBuilder();
        result.append(SETTINGS_PREFIX);
        result.append(taskListID == null ? GLOBAL_QUALIFIER : taskListID);
        result.append("/").append(chartID);
        if (StringUtils.hasValue(customName))
            result.append("/").append(customName);
        return result.toString();
    }

    private String buildXML() {
        if (parameters == null || parameters.isEmpty())
            return "";

        StringBuilder result = new StringBuilder();
        result.append("<snippet");
        appendAttr(result, CHART_TYPE_ATTR, chartID);
        appendAttr(result, VERSION_ATTR, chartVersion);
        appendAttr(result, PERSISTER_ATTR, XML_PERSISTER_ID);
        result.append("><![CDATA[");
        result.append(PERSISTER.getTextToPersist(parameters));
        result.append("]]></snippet>");
        return result.toString();
    }

    private void appendAttr(StringBuilder result, String attr, String val) {
        result.append(" ").append(attr).append("=\"").append(
            XMLUtils.escapeAttribute(val)).append("\"");
    }

    /**
     * Find a list of chart setting objects that apply to a particular task list.
     * 
     * @param taskListID the ID of the task list.
     * @param data the data repository
     * @return a list of
     */
    public static Map<String, TaskScheduleChartSettings> getSettingsForTaskList(
            String taskListID, DataRepository data) {
        Map<String, TaskScheduleChartSettings> globalSettings = new HashMap();
        Map<String, TaskScheduleChartSettings> taskListSettings = new HashMap();
        String taskListPrefix = getTaskListPrefix(taskListID);

        Iterator i = data.getKeys(SETTINGS_PREFIX, DataNameFilter.EXPLICIT_ONLY);
        while (i.hasNext()) {
            String dataName = (String) i.next();
            Map dest;
            if (dataName.startsWith(GLOBAL_SETTINGS_PREFIX))
                dest = globalSettings;
            else if (dataName.startsWith(taskListPrefix))
                dest = taskListSettings;
            else
                continue;

            SimpleData sd = data.getSimpleValue(dataName);
            if (!(sd instanceof StringData))
                continue;

            try {
                StringData str = (StringData) sd;
                TaskScheduleChartSettings s = new TaskScheduleChartSettings(
                        dataName, str.format());
                dest.put(s.getSettingsIdentifier(), s);
            } catch (Exception e) {
                TaskScheduleChart.logger.warning(
                    "Could not load chart settings for " + dataName);
            }
        }

        Map result = globalSettings;
        result.putAll(taskListSettings);
        return result;
    }

    /**
     * In some settings, the user is given an option of selecting a subset of
     * charts and arranging them in a preferred ordering.  This method will
     * retrieve that preferred ordering for a given task list.
     */
    public static List<String> getPreferredChartOrdering(String taskListID,
            DataRepository data) {
        SimpleData sd = data.getSimpleValue(SETTINGS_PREFIX + taskListID
                + CHART_ORDERING_SETTING);
        if (sd == null)
            sd = data.getSimpleValue(SETTINGS_PREFIX + GLOBAL_QUALIFIER
                    + CHART_ORDERING_SETTING);
        ListData l = ListData.asListData(sd);
        return (l == null ? Collections.EMPTY_LIST : l.asList());
    }

    /**
     * In some settings, the user is given an option of selecting a subset of
     * charts and arranging them in a preferred ordering.  This method will
     * save that preferred ordering for a given task list.
     */
    public static void savePreferredChartOrdering(String taskListID,
            List<String> chartIds, DataRepository data) {

        // When more than one person opens a particular dataset, someone may
        // have a older version of the charts (or may not have the charting
        // add-on installed at all).  In that case, the person with fewer
        // charts may save a list that is incomplete (does not contain all of
        // the available chart IDs).  Look through the previous version of our
        // saved setting, find any items that are missing from the new list,
        // and insert them into the new list in our best guess of the right
        // order.  Note that we can't reliably infer the ordering intent of a
        // user who isn't working with a full set of charts.  But fortunately,
        // if the user with missing charts makes no changes to the order, the
        // algorithm below will effectively retain the previous ordering
        // without changes.
        List<String> newChartIds = new ArrayList<String>(chartIds);
        List<String> oldOrder = getPreferredChartOrdering(taskListID, data);
        int insertPos = 0;
        for (String oneId : oldOrder) {
            int pos = newChartIds.indexOf(oneId);
            if (pos == -1) {
                newChartIds.add(insertPos++, oneId);
            } else {
                insertPos = pos+1;
            }
        }

        // Now that we have constructed the list, save it to the repository.
        ListData l = new ListData();
        for (String id : newChartIds)
            l.add(id);
        data.putValue(SETTINGS_PREFIX + taskListID + CHART_ORDERING_SETTING, l);
    }

    private static String getTaskListPrefix(String taskListID) {
        return SETTINGS_PREFIX + taskListID + "/";
    }

    private static final String SETTINGS_PREFIX = "/Task-Schedule-Chart/";
    private static final String GLOBAL_QUALIFIER = "global";
    private static final String GLOBAL_SETTINGS_PREFIX = SETTINGS_PREFIX
            + GLOBAL_QUALIFIER + "/";
    private static final String CHART_ORDERING_SETTING = ":Chart Order";
    public static final String SECONDARY_CHART_MARKER = "*More*";

    private static final String PERSISTER_ATTR = "persister";
    private static final String XML_PERSISTER_ID = "xml.v1";
    private static final String CHART_TYPE_ATTR = "type";
    private static final String VERSION_ATTR = "version";

    private static final String DATA_NAME_SEGMENT = "([^/]+)";
    private static final Pattern DATA_NAME_PATTERN = Pattern.compile(
            SETTINGS_PREFIX
            + DATA_NAME_SEGMENT           // global-vs-task list qualifier
            + "/" + DATA_NAME_SEGMENT     // chart ID
            + "(/.+)?");                  // custom chart name

    private static final ParamDataPersister PERSISTER = new XmlParamDataPersisterV1(
            true, false);
    private static final HttpQueryParser QUERY_PARSER = new HttpQueryParser();

}
