// Copyright (C) 2013-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.XMLUtils;

public class ImportedEVManager {

    public interface CachedDataCalculator {

        public Object calculateCachedData(String taskListName, Element xml);

    }

    private static final Logger logger = Logger
            .getLogger(ImportedEVManager.class.getName());

    private static final ImportedEVManager INSTANCE = new ImportedEVManager();

    public static ImportedEVManager getInstance() {
        return INSTANCE;
    }


    private Map<String, ImportedTaskList> importedTaskLists;

    private Map<Object, CachedDataCalculator> calculators;

    private ImportedEVManager() {
        importedTaskLists = Collections.synchronizedMap(new HashMap());
        calculators = Collections.synchronizedMap(new LinkedHashMap());
    }



    /**
     * Discard any cached information held by this class, along with all
     * registered calculators.
     */
    public void dispose() {
        importedTaskLists.clear();
        calculators.clear();
    }


    /**
     * Add information for an imported task list.
     * 
     * @param uniqueKey
     *            the String key for this imported task list. This value should
     *            begin with a prefix that is specific to the import file. Then
     *            it should contain a name formatted by the
     *            {@link ExportManager#exportedScheduleDataPrefix(String, String)}
     *            method.
     * @param xml
     *            the parsed XML Element corresponding to this EV schedule.
     * @param srcFile 
     *            the PDASH file the EV data was imported from
     * @param srcDatasetID
     *            the ID of the dataset that this schedule originated from
     */
    public void importTaskList(String uniqueKey, Element xml,
            File srcFile, String srcDatasetID) {
        if (EVTaskList.EV_TASK_LIST_ELEMENT_NAME.equals(xml.getTagName())) {
            ImportedTaskList taskList = new ImportedTaskList(uniqueKey, xml,
                    srcFile, srcDatasetID);
            importedTaskLists.put(uniqueKey, taskList);
        } else {
            logger.warning("Attempt to import invalid EV XML "
                    + "document; ignoring");
        }
    }


    /**
     * Remove all of the imported task lists whose unique key began with a
     * particular prefix.
     * 
     * @param prefix
     *            a uniqueKey prefix, which identifies the file that the task
     *            list was imported from.
     */
    public void closeTaskLists(String prefix) {
        synchronized (importedTaskLists) {
            for (Iterator<String> i = importedTaskLists.keySet().iterator(); i
                    .hasNext();) {
                String uniqueKey = i.next();
                if (uniqueKey.startsWith(prefix))
                    i.remove();
            }
        }
    }


    /**
     * Get the taskListNames of all imported schedules.
     * 
     * @return a Set containing the task list names of all imported schedules.
     *         Task list names are generally of the form
     *         <tt>[task list ID]#XMLID[unique key]</tt>
     */
    public Set<String> getImportedTaskListNames() {
        Set<String> result = new HashSet<String>();
        synchronized (importedTaskLists) {
            for (ImportedTaskList tl : importedTaskLists.values()) {
                result.add(tl.taskListName);
            }
        }
        return result;
    }


    /**
     * Retrieve the XML fragment associated with a particular imported task
     * list.
     * 
     * @param taskListName
     *            the task list name, which can either be a uniqueKey, or (more
     *            commonly) a string of the form
     *            <tt>[task list ID]#XMLID[unique key]</tt>
     * @return the XML fragment associated with that imported task list, or null
     *         if no task list could be found with that name.
     */
    public Element getImportedTaskListXml(String taskListName) {
        ImportedTaskList tl = getImportedTaskListByName(taskListName);
        return (tl == null ? null : tl.xml);
    }


    /**
     * Find an imported task list with the given ID, and return its full task
     * list name.
     * 
     * @param taskListID
     *            a task list ID
     * @return the full name for the imported task list with that ID, or null if
     *         no such task list could be found.
     */
    public String getTaskListNameForID(String taskListID) {
        ImportedTaskList tl = getImportedTaskListByID(taskListID);
        return (tl == null ? null : tl.taskListName);
    }


    /**
     * Find an imported task list with the given ID, and return the ID of the
     * dataset from which it originated.
     * 
     * @param taskListID
     *            a task list ID
     * @return the ID of the dataset that owns the given task list, or null if
     *         no such task list could be found.
     */
    public String getSrcDatasetID(String taskListID) {
        ImportedTaskList tl = getImportedTaskListByID(taskListID);
        return (tl == null ? null : tl.srcDatasetID);
    }


    /**
     * Find an imported task list with the given ID, and return the PDASH file
     * the the task list was imported from.
     * 
     * @param taskListID
     *            a task list ID
     * @return the PDASH file the task list was imported from, or null if no
     *         such task list could be found.
     */
    public File getSrcFile(String taskListID) {
        ImportedTaskList tl = getImportedTaskListByID(taskListID);
        return (tl == null ? null : tl.srcFile);
    }



    /**
     * Register an object to perform cached data calculations.
     */
    public void addCalculator(Object calculatorKey, CachedDataCalculator calc) {
        calculators.put(calculatorKey, calc);
    }


    /**
     * Find an imported task list with the given name, and retrieve the cached
     * data object that was calculated by the calculator with the given key
     * 
     * @param taskListName
     *            the name of a task list
     * @param calculatorKey
     *            the key that was previously used to register a
     *            {@link CachedDataCalculator}
     * @return the data object calculated by that calculator for this task list.
     */
    public <T> T getCachedData(String taskListName, Object calculatorKey) {
        ImportedTaskList tl = getImportedTaskListByName(taskListName);
        return (T) (tl == null ? null : tl.getCachedData(calculatorKey));
    }


    /**
     * Scan all of the imported task lists, and build a map of the cached data
     * object that was calculated for each one by the calculator with the given
     * key
     * 
     * @param calculatorKey
     *            the key that was previously used to register a
     *            {@link CachedDataCalculator}
     * @return a map whose keys are task list names, and whose value is the data
     *         object calculated by that calculator for that task list.
     */
    public <T> Map<String, T> getCachedData(Object calculatorKey) {
        ArrayList<ImportedTaskList> taskLists;
        synchronized (importedTaskLists) {
            taskLists = new ArrayList(importedTaskLists.values());
        }

        Map result = new HashMap();
        for (ImportedTaskList tl : taskLists) {
            String name = tl.taskListName;
            Object data = tl.getCachedData(calculatorKey);
            result.put(name, data);
        }
        return result;
    }


    private ImportedTaskList getImportedTaskListByName(String taskListName) {
        if (taskListName == null)
            return null;

        // parse out the task list ID value, if it is present
        String uniqueKey, taskListID;
        int pos = taskListName.indexOf(EVTaskListXML.XMLID_FLAG);
        if (pos == -1) {
            uniqueKey = taskListName;
            taskListID = null;
        } else {
            taskListID = taskListName.substring(0, pos);
            uniqueKey = taskListName.substring(pos
                    + EVTaskListXML.XMLID_FLAG.length());
        }

        // try locating the imported task list in a variety of ways
        ImportedTaskList result = importedTaskLists.get(uniqueKey);
        if (result == null)
            result = getImportedTaskListByID(taskListID);
        if (result == null && Settings.getBool("ev.imports.matchByName", true))
            result = getImportedTaskListByUniqueDisplayName(taskListName);

        return result;
    }


    private ImportedTaskList getImportedTaskListByID(String taskListID) {
        ImportedTaskList result = null;
        if (taskListID != null) {
            synchronized (importedTaskLists) {
                for (ImportedTaskList taskList : importedTaskLists.values()) {
                    if (taskListID.equals(taskList.taskListID)) {
                        if (result != null)
                            logger.fine("Two imported task lists share the "
                                    + "same ID: '" + taskList.taskListName
                                    + "' and '" + result.taskListName + "'");

                        if (taskList.compareTo(result) > 0)
                            result = taskList;
                    }
                }
            }
        }
        return result;
    }


    /**
     * "Missing task list" errors are a common problem in team dashboards. See
     * if we can mitigate these errors by following a simple heuristic: if the
     * rollup is calling for a imported plain task list with a name like
     * "Some Task List (Owner name)", and we have exactly one imported schedule
     * with that name, return it.
     */
    private ImportedTaskList getImportedTaskListByUniqueDisplayName(
            String taskListName) {
        // Retrieve the display name, and make certain it includes the embedded
        // name of a schedule owner.
        String displayName = EVTaskList.cleanupName(taskListName);
        if (displayName.lastIndexOf(')') == -1)
            return null;

        ImportedTaskList result = null;
        synchronized (importedTaskLists) {
            for (ImportedTaskList taskList : importedTaskLists.values()) {
                if (displayName.equals(taskList.displayName)) {
                    if (result == null)
                        // if this is the first schedule we've found with this
                        // display name, save it.
                        result = taskList;
                    else
                        // if we've now seen two schedules with this name,
                        // abort and return null.
                        return null;
                }
            }
        }

        return result;
    }


    private class ImportedTaskList implements Comparable<ImportedTaskList> {

        private File srcFile;

        private String srcDatasetID;

        private Element xml;

        private String taskListID;

        private String taskListName;

        private String displayName;

        private Date effDate;

        private Map cachedData;

        protected ImportedTaskList(String uniqueKey, Element xml,
                File srcFile, String srcDatasetID) {
            this.xml = xml;
            this.srcFile = srcFile;
            this.srcDatasetID = srcDatasetID;
            this.taskListID = xml.getAttribute(EVTaskListXML.XMLID_ATTR);
            if (XMLUtils.hasValue(taskListID)) {
                this.taskListName = taskListID + EVTaskListXML.XMLID_FLAG
                        + uniqueKey;
            } else {
                this.taskListName = uniqueKey;
                this.taskListID = null;
            }
            this.displayName = EVTaskList.cleanupName(taskListName);

            Element sched = XMLUtils.getChildElements(xml).get(1);
            this.effDate = XMLUtils.getXMLDate(sched, "eff");

            this.cachedData = new HashMap();
        }

        private synchronized Object getCachedData(Object calculatorKey) {
            Object result = cachedData.get(calculatorKey);
            if (result == null) {
                CachedDataCalculator calc = calculators.get(calculatorKey);
                if (calc != null)
                    result = calc.calculateCachedData(taskListName, xml);
                cachedData.put(calculatorKey, result);
            }
            return result;
        }

        public int compareTo(ImportedTaskList that) {
            if (that == null || that.effDate == null)
                return +1;
            else if (this.effDate == null)
                return -1;
            else
                return this.effDate.compareTo(that.effDate);
        }

    }

}
