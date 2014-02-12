// Copyright (C) 2001-2014 Tuma Solutions, LLC
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.ui.chart.CategoryChartData;
import net.sourceforge.processdash.ev.ui.chart.CategoryChartSeries;
import net.sourceforge.processdash.ev.ui.chart.ChartEventAdapter;
import net.sourceforge.processdash.ev.ui.chart.EVTaskKey;
import net.sourceforge.processdash.ev.ui.chart.XYChartData;
import net.sourceforge.processdash.ev.ui.chart.XYChartSeries;
import net.sourceforge.processdash.ev.ui.chart.XYNameDataset;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.HierarchyNoteEvent;
import net.sourceforge.processdash.hier.HierarchyNoteListener;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.ui.lib.AbstractTreeTableModel;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.util.DateAdjuster;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TimeZoneDateAdjuster;
import net.sourceforge.processdash.util.TimeZoneUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class EVTaskList extends AbstractTreeTableModel
    implements EVTask.Listener, ActionListener
{

    public static final String EV_TASK_LIST_ELEMENT_NAME = "EVModel";
    public static final String MAIN_DATA_PREFIX = "/Task-Schedule/";
    static Resources resources = Resources.getDashBundle("EV");
    static Logger logger = Logger.getLogger(EVTaskList.class.getName());


    protected String taskListName;
    protected String taskListID = null;
    protected EVSchedule schedule;
    protected EVCalculator calculator;
    protected EVDependencyCalculator dependencyCalculator = null;
    protected TaskLabeler taskLabeler = null;
    protected MilestoneProvider milestoneProvider = null;
    protected Set nodeTypeSpecs = null;
    protected Properties metaData = null;
    protected boolean isBrandNewTaskList = false;

    /** timer for triggering recalculations */
    protected Timer recalcTimer = null;

    protected double totalPlanValue;
    protected double totalActualTime;
    protected boolean showDirectTimeColumns;
    protected boolean showBaselineColumns;
    protected boolean showNodeTypeColumn;
    protected boolean showReplanColumn = Settings.getBool(
            "ev.showReplanColumn", true);
    protected boolean showNotesColumn;
    protected boolean showMilestoneColumn;
    protected boolean showLabelsColumn;


    protected EVTaskList(String taskListName,
                         String displayName,
                         boolean willNeedChangeNotification) {
        super(null);

        this.taskListName = taskListName;

        if (willNeedChangeNotification) {
            recalcListeners = Collections.synchronizedSet(new HashSet());

            recalcTimer = new Timer(Integer.MAX_VALUE, this);
            recalcTimer.setInitialDelay(1000);
            recalcTimer.setRepeats(false);
        }

        if (displayName != null)
            root = new EVTask(taskListName);

        this.showNotesColumn = false;
    }

    public EVTaskList(String taskListName,
                      String displayName,
                      String errorMessage) {
        this(taskListName, null, false);
        createErrorRootNode(displayName, errorMessage);
    }


    protected void createErrorRootNode(String displayName,
                                       String errorMessage) {
        if (displayName == null || displayName.length() == 0)
            displayName = resources.getString
                ("TaskList.Default_Error_Root_Node_Name");
        root = new EVTask(displayName);
        schedule = new EVSchedule(0.0);
        ((EVTask) root).setTaskError(errorMessage);
        schedule.getMetrics().addError(errorMessage, (EVTask) root);
    }


    public static String[] findTaskLists(DataRepository data) {
        return findTaskLists(data, false, false);
    }
    public static String[] findTaskLists(DataRepository data,
                                         boolean excludeRollups,
                                         boolean includeImports) {
        /*
            let findTaskLists return a full path to imported schedules.
            save that full path in rollup task list names.
            make the name pretty when displaying it to the user.

         */
        TreeSet result = new TreeSet();

        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        String dataName, taskListName;
        while (i.hasNext()) {
            dataName = (String) i.next();

            // see if this data name defines a regular task list.
            taskListName =
                EVTaskListData.taskListNameFromDataElement(dataName);

            // if that failed, maybe see if this defines a rollup.
            if (taskListName == null && !excludeRollups)
                taskListName =
                    EVTaskListRollup.taskListNameFromDataElement(dataName);

            // if any of the tests succeeded, add the name to the list.
            if (taskListName != null)
                result.add(cleanupName(taskListName) + "\n" + taskListName);
        }

        if (includeImports) {
            for (String importedName : ImportedEVManager.getInstance()
                    .getImportedTaskListNames()) {
                result.add(cleanupName(importedName) + "\n" + importedName);
            }
        }

        String[] ret = new String[result.size()];
        i = result.iterator();
        int j = 0;
        while (i.hasNext()) {
            String oneName = (String) i.next();
            int pos = oneName.indexOf('\n');
            oneName = oneName.substring(pos+1);
            ret[j++] = oneName;
        }
        return ret;
    }

    /** Finds and opens an existing task list.
     *  @param taskListName the symbolic name of the task list.
     *  @param data a reference to the data repository.
     *  @param hierarchy a reference to the user's hierarchy.
     *  @param willNeedChangeNotification should the returned task list
     *     support change notifications?
     *  @return the named task list, or null if no list by that name is found.
     */
    public static EVTaskList openExisting(String taskListName,
                                          DataRepository data,
                                          DashHierarchy hierarchy,
                                          ObjectCache cache,
                                          boolean willNeedChangeNotification)
    {
        // most common case: open a regular task list
        if (EVTaskListData.validName(taskListName) &&
            EVTaskListData.exists(data, taskListName))
            return new EVTaskListData(taskListName, data, hierarchy,
                                      willNeedChangeNotification);

        // next most common case: open a rollup task list
        if (EVTaskListRollup.validName(taskListName) &&
            EVTaskListRollup.exists(data, taskListName))
            return new EVTaskListRollup(taskListName, data, hierarchy, cache,
                                        willNeedChangeNotification);

        // open an cached imported XML task list.
        if (EVTaskListCached.validName(taskListName) &&
            EVTaskListCached.exists(taskListName, cache))
            return new EVTaskListCached(taskListName, cache);

        // open an imported XML task list
        if (EVTaskListXML.validName(taskListName) &&
            EVTaskListXML.exists(taskListName))
            return new EVTaskListXML(taskListName);

        // for testing purposes, return a cached list
        if (taskListName != null && taskListName.equals(TESTING_TASK_LIST_NAME))
            return TESTING_TASK_LIST;

        // no task list was found.
        return null;
    }

    public static EVTaskList open(String taskListName,
                                  DataRepository data,
                                  DashHierarchy hierarchy,
                                  ObjectCache cache,
                                  boolean willNeedChangeNotification)
    {
        EVTaskList result = openExisting
            (taskListName, data, hierarchy, cache, willNeedChangeNotification);

        if (result == null && EVTaskListData.validName(taskListName))
            result = new EVTaskListData
                (taskListName, data, hierarchy, willNeedChangeNotification);

        if (result == null && EVTaskListXML.validName(taskListName))
            result = new EVTaskListXML(taskListName);

        if (result == null)
            result = new EVTaskList
                (taskListName, getDisplayName(taskListName),
                 resources.getString
                 ("TaskList.Invalid_Schedule_Error_Message"));

        return result;
    }

    /** Find task lists which contain a given hierarchy task.
     * 
     * @param data the data repository
     * @param path the path to a project in the hierarchy
     * @return a list of the names of task lists that contain the given task.
     */
    public static List getTaskListNamesForPath(DataRepository data, String path) {
        String origPath = path;

        // chop trailing "/" if it is present.
        if (path.endsWith("/")) path = path.substring(0, path.length()-1);

        // Make a list of data name prefixes that could indicate the
        // name of the task list for this path.
        ArrayList prefixList = new ArrayList();
        while (path != null) {
            prefixList.add(path);
            path = DataRepository.chopPath(path);
        }
        String[] prefixes = (String[]) prefixList.toArray(new String[0]);

        // Search the data repository for elements that begin with any of
        // the prefixes we just contructed.
        String dataName, prefix, ord_pref = "/"+EVTaskListData.TASK_ORDINAL_PREFIX;
        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        ArrayList taskLists = new ArrayList();

    DATA_ELEMENT_SEARCH:
        while (i.hasNext()) {
            dataName = (String) i.next();
            for (int j = prefixes.length;  j-- > 0; ) {
                prefix = prefixes[j];

                if (!dataName.startsWith(prefix))
                    // if the dataname doesn't start with this prefix, it
                    // won't start with any of the others either.  Go to the
                    // next data element.
                    continue DATA_ELEMENT_SEARCH;

                if (!dataName.regionMatches(prefix.length(), ord_pref,
                                            0, ord_pref.length()))
                    // If the prefix isn't followed by the ordinal tag
                    // "/TST_", try the next prefix.
                    continue;

                // we've found a match! Compute the resulting task list
                // name and add it to our list.
                dataName = dataName.substring
                    (prefix.length() + ord_pref.length());
                taskLists.add(dataName);
            }
        }

        // Discard any task lists which have prune the given task
        i = taskLists.iterator();
        while (i.hasNext()) {
            String taskListName = (String) i.next();
            if (EVTask.taskIsPruned(data, taskListName, origPath))
                i.remove();
        }
        return taskLists;
    }

    /** Find the best task list (or lists) that represent the given project.
     * 
     *  If the project (or one of its ancestors) explicitly names a preferred
     *  task list (via the "Project_Schedule_Name" data element), this method
     *  will return a list of length 1, containing that task list name.
     *  Otherwise, this method will return the same data as the
     *  {@link #getTaskListNamesForPath(DataRepository, String)} method.
     */
    public static List getPreferredTaskListsForPath(DataRepository data,
            String path) {
        String result = getRegisteredTaskListForPath(data, path);
        if (result != null)
            return Collections.singletonList(result);
        else
            return getTaskListNamesForPath(data, path);
    }

    /** This method checks to see if a project schedule has been registered
     * for path (or one of its parents) via the "Project_Schedule_Name" or
     * "Project_Schedule_ID" data elements.  If so, it returns the name of
     * the registered schedule (which will be guaranteed to exist).
     * 
     * In the process of making this inquiry, this method will update /
     * synchronize the "Name" and "ID" attributes above if they have gotten
     * out of sync with each other.
     * 
     * @param data the data repository
     * @param projectPath the path to a hierarchy node
     * @return the name of a schedule
     */
    private static String getRegisteredTaskListForPath(DataRepository data,
            String projectPath) {
        // check to see if one of our parents names a registered schedule.
        StringBuffer path = new StringBuffer(projectPath);
        SaveableData sd = data.getInheritableValue(path, PROJECT_SCHEDULE_NAME);
        if (sd == null) return null;
        SimpleData val = sd.getSimpleValue();
        if (val == null || !val.test()) return null;

        // We found a named schedule. Remember the prefix of the project that
        // registered this schedule name.  Also, save the data name for the
        // corresponding schedule ID attribute.
        String prefix = path.toString();
        String projSchedIdDataName = DataRepository.createDataName(
            prefix, PROJECT_SCHEDULE_ID);

        // Next, check to see if the named schedule actually exists.
        String taskListName = val.format();
        if (EVTaskListData.exists(data, taskListName)
                || EVTaskListRollup.exists(data, taskListName)) {
            // The named schedule exists!  Retrieve its actual task list ID,
            // and record this for posterity.  Note that in the most common
            // case, we will be recording the exact same ID value that is
            // already present - but the data repository will be able to
            // efficiently figure out whether a real change was made.
            String taskListIdDataName = DataRepository.createDataName(
                    MAIN_DATA_PREFIX + taskListName, ID_DATA_NAME);
            SimpleData taskListID = data.getSimpleValue(taskListIdDataName);
            data.putValue(projSchedIdDataName, taskListID);

            // Finally, return the name we found.
            return taskListName;
        }

        // We found a registered name, but it does not point to an existing
        // schedule.  Check to see if we have a schedule ID to fall back to.
        val = data.getSimpleValue(projSchedIdDataName);
        if (val == null || !val.test())
            return null;    // no fall-back schedule ID was provided.

        String taskListID = val.format();
        taskListName = getTaskListNameForID(data, taskListID);
        if (taskListName == null)
            return null;   // the fall-back ID doesn't name a real schedule.

        // The fall-back ID named a real schedule.  Save that schedule name
        // for posterity, then return it to our caller.
        String projSchedNameDataName = DataRepository.createDataName(
            prefix, PROJECT_SCHEDULE_NAME);
        data.putValue(projSchedNameDataName, StringData.create(taskListName));
        return taskListName;
    }

    /**
     * Finds a regular or roll-up task list with the current ID, and returns
     * its name.  If there is no regular or roll-up task list with the given
     * ID, returns null.
     */
    public static String getTaskListNameForID(DataRepository data,
            String taskListID) {
        if (!StringUtils.hasValue(taskListID))
            return null;

        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        while (i.hasNext()) {
            String dataName = (String) i.next();
            if (!dataName.endsWith(ID_DATA_NAME))
                continue;

            SimpleData sd = data.getSimpleValue(dataName);
            if (sd != null && taskListID.equals(sd.format())) {
                String taskListName = dataName.substring(
                    MAIN_DATA_PREFIX.length(),
                    dataName.length() - ID_DATA_NAME.length() - 1);
                if (EVTaskListData.exists(data, taskListName)
                        || EVTaskListRollup.exists(data, taskListName))
                    return taskListName;
            }
        }

        return null;
    }

    /**
     * Finds a task list with the current ID, and returns its name.  If there
     * is no task list with the given ID, returns null.
     * 
     * @param data the data repository
     * @param taskListID the ID to look for
     * @param includeImports true if imported XML schedules should be searched;
     *         false if we should only consider plain and rollup schedules.
     * @return the name of the task list with this ID
     */
    public static String getTaskListNameForID(DataRepository data,
            String taskListID, boolean includeImports) {
        String result = getTaskListNameForID(data, taskListID);
        if (result == null && includeImports) {
            result = ImportedEVManager.getInstance().getTaskListNameForID(
                taskListID);
        }
        return result;
    }

    private static String TESTING_TASK_LIST_NAME = null;
    private static EVTaskList TESTING_TASK_LIST = null;
    public static void setTestingTaskList(String name, EVTaskList taskList) {
        TESTING_TASK_LIST_NAME = name;
        TESTING_TASK_LIST = taskList;
    }

    /** Add a listener who wishes to receive notifications about task list
     * save events. */
    public static void addTaskListSaveListener(ActionListener l) {
        TASK_LIST_SAVE_LISTENERS.add(new WeakReference(l));
    }

    /** Remove a listener who no longer cares to receive notifications about
     * task list save events. */
    public static void removeTaskListSaveListener(ActionListener l) {
        synchronized (TASK_LIST_SAVE_LISTENERS) {
            for (Iterator i = TASK_LIST_SAVE_LISTENERS.iterator(); i.hasNext();) {
                WeakReference ref = (WeakReference) i.next();
                Object o = ref.get();
                if (o == null || o == l)
                    i.remove();
            }
        }
    }

    protected static void fireTaskListSaved(String taskListName) {
        synchronized (TASK_LIST_SAVE_LISTENERS) {
            ActionEvent e = null;
            for (Iterator i = TASK_LIST_SAVE_LISTENERS.iterator(); i.hasNext();) {
                WeakReference ref = (WeakReference) i.next();
                ActionListener l = (ActionListener) ref.get();
                if (l != null) {
                    if (e == null)
                        e = new ActionEvent(EVTaskList.class, 0, taskListName);
                    l.actionPerformed(e);
                }
            }
        }
    }
    private static List TASK_LIST_SAVE_LISTENERS = new Vector();

    public String getTaskListName() {
        return taskListName;
    }

    public String getDisplayName() {
        return getDisplayName(taskListName);
    }

    public static String getDisplayName(String taskListName) {
        int slashPos = taskListName.lastIndexOf('/');
        if (slashPos == -1)
            return taskListName;
        else
            return taskListName.substring(slashPos+1);
    }

    public static String cleanupName(String taskListDataName) {
        // Strip all initial text up to and including the "main data prefix."
        int pos = taskListDataName.indexOf(MAIN_DATA_PREFIX);
        if (pos != -1)
            taskListDataName = taskListDataName.substring
                (pos + MAIN_DATA_PREFIX.length());

        // Strip all final text following the "/" character.
        pos = taskListDataName.indexOf('/');
        if (pos != -1)
            taskListDataName = taskListDataName.substring(0, pos);

        return taskListDataName;
    }

    public static String[] getDisplayNames(String[] taskListNames) {
        String[] result = new String[taskListNames.length];
        for (int i = result.length;   i-- > 0;  )
            result[i] = cleanupName(taskListNames[i]);
        return result;
    }

    public boolean isEmpty() { return ((EVTask) root).isLeaf(); }
    public boolean isEditable() { return false; }
    public EVTask getTaskRoot() { return (EVTask) root; }
    public String getRootName() { return ((EVTask) root).name; }
    public String getID() { return taskListID; }

    public String getMetadata(String key) {
        return (metaData == null ? null : metaData.getProperty(key));
    }

    public String setMetadata(String key, String value) {
        if (metaData == null)
            metaData = new Properties();

        String result;
        if (StringUtils.hasValue(value))
            result = (String) metaData.put(key, value);
        else
            result = (String) metaData.remove(key);

        if (EVMetadata.Baseline.SNAPSHOT_ID.equals(key))
            setBaselineDataSource(getBaselineSnapshot());

        if (RECALC_METDATADATA.contains(key))
            recalcTimer.restart();

        return result;
    }
    private static final Set<String> RECALC_METDATADATA =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            EVMetadata.REZERO_ON_START_DATE,
            EVMetadata.Forecast.Ranges.USE_CURRENT_PLAN,
            EVMetadata.Forecast.Ranges.USE_HIST_DATA,
            EVMetadata.Forecast.Ranges.SAVED_HIST_DATA
            )));

    public String getTimezoneID() {
        return getMetadata(EVMetadata.TimeZone.ID);
    }

    public void setTimezoneID(String id) {
        setMetadata(EVMetadata.TimeZone.ID, id);
    }


    public void save() { save(taskListName); }
    public void save(String newName) {
        fireTaskListSaved(newName);
    }

    public String getAsXML() {
        return getAsXML(false);
    }
    public String getAsXML(boolean whitespace) {
        String newline = (whitespace ? "\n" : "");
        StringBuffer result = new StringBuffer();
        result.append("<").append(EV_TASK_LIST_ELEMENT_NAME).append(" rct='")
            .append(calculator.reorderCompletedTasks);
        if (getID() != null)
            result.append("' tlid='").append(getID());
        String timezone = getTimezoneID();
        if (timezone != null)
            result.append("' tz='").append(XMLUtils.escapeAttribute(timezone));
        result.append("'>").append(newline);
        ((EVTask) root).saveToXML(result, whitespace);
        schedule.saveToXML(result, whitespace);
        result.append("</").append(EV_TASK_LIST_ELEMENT_NAME).append(">");

        return result.toString();
    }


    public boolean addTask(String path,
                           DataRepository data,
                           DashHierarchy hierarchy,
                           ObjectCache cache,
                           boolean willNeedChangeNotification) {
        if (path == null || path.length() == 0 || !isEditable()) return false;

        EVTask newTask = createAndAddTask(path, data, hierarchy, cache,
                                          willNeedChangeNotification);
        if (newTask == null)
            return false;

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { ((EVTask)root).getNumChildren() - 1 };
        Object[] children = new Object[] { newTask };
        fireTreeNodesInserted
            (this, ((EVTask) root).getPath(), childIndices, children);
        return true;
    }
    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   DashHierarchy hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        return null;
    }

    public boolean removeTask(TreePath path) {
        if (!isEditable()) return false;

        if (checkRemovable(path) == false) return false;

        int pathLen = path.getPathCount();
        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        int pos = doRemoveTask(parent, child);
        if (pos == -1) return true;

        finishRemovingTask(pos);
        child.destroy();

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos };
        Object[] children = new Object[] { child };
        fireTreeNodesRemoved
            (this, ((EVTask) parent).getPath(), childIndices, children);
        return true;
    }
    protected boolean checkRemovable(TreePath path) {
        // default behavior: only remove tasks which are children of the root.
        return path.getPathCount() == 2;
    }
    protected int doRemoveTask(EVTask parent, EVTask child) {
        return parent.remove(child);
    }
    protected void finishRemovingTask(int pos) {}

    public boolean explodeTask(TreePath path) { return false; }

    public boolean moveTaskUp(int pos) {
        if (!isEditable()) return false;

        EVTask r = (EVTask) root;
        if (pos < 1 || pos >= r.getNumChildren()) return false;

        // make the change
        r.moveUp(pos);
        finishMovingTaskUp(pos);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos-1, pos };
        Object[] children = new Object[]{ r.getChild(pos-1), r.getChild(pos) };
        fireTreeStructureChanged(this, r.getPath(), childIndices, children);
        return true;
    }
    protected void finishMovingTaskUp(int pos) {}


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    /// Change notification support
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////


    /*
     * Pass along evNodeChanged events to up to one additional EVTask.Listener
     */

    EVTask.Listener evNodeListener = null;
    public void setNodeListener(EVTask.Listener l) { evNodeListener = l; }
    public void evNodeChanged(EVTask node, boolean needsRecalc) {
        if (evNodeListener != null) evNodeListener.evNodeChanged(node, needsRecalc);
        if (recalcTimer != null && needsRecalc) recalcTimer.restart();
    }


    /** Defines the interface for an object that listens for recalculations
     *  that occur in an EVTaskList.
     *
     * Note: this only allows for notification of recalculation
     * events - not to the structure of the task list.  To be
     * notified of such changes, register as a TreeModelListener.
     */
    public interface RecalcListener {
        /** The task list has been recalculated. */
        public void evRecalculated(EventObject e);
    }
    Set recalcListeners = null;
    public synchronized void addRecalcListener(RecalcListener l) {
        if (recalcListeners != null)
            recalcListeners.add(l);
    }
    public void removeRecalcListener(RecalcListener l) {
        if (recalcListeners != null) {
            recalcListeners.remove(l);
            maybeDispose();
        }
    }
    protected boolean someoneCares() {
        return (recalcListeners != null && !recalcListeners.isEmpty());
    }
    protected void fireEvRecalculated() {
        if (someoneCares()) {
            EventObject e = new EventObject(this);
            Iterator i = new ArrayList(recalcListeners).iterator();
            while (i.hasNext())
                ((RecalcListener) i.next()).evRecalculated(e);
        }
    }
    public void actionPerformed(ActionEvent e) {
        if (recalcTimer != null && e.getSource() == recalcTimer &&
            someoneCares())
            recalc();
    }

    private void maybeDispose() {
        if (recalcListeners == null) return;
        if (recalcListeners.isEmpty()) {
        }
    }
    public void dispose() {
        //System.out.println("disposing!");
        ((EVTask) root).destroy();
    }

    public void finalize() throws Throwable {
        //System.out.println("finalizing EVTaskList " + taskListName);
        super.finalize();
    }


    public void recalc() {
        if (calculator != null)
            calculator.recalculate();
        if (dependencyCalculator != null)
            dependencyCalculator.recalculate(this);
        totalPlanValue = schedule.getMetrics().totalPlan();
        EVTask taskRoot = (EVTask) root;
        totalActualTime = taskRoot.actualCurrentTime;

        double directTimeDelta = taskRoot.planTime - taskRoot.planValue;
        showDirectTimeColumns = Math.abs(directTimeDelta) > 0.99;
        showBaselineColumns = (calculator != null
                && calculator.getBaselineDataSource() != null);
        showNodeTypeColumn = taskRoot.isUsingNodeTypes();
        showMilestoneColumn = showLabelsColumn = false;
        scanForLabelsAndMilestones(taskRoot);
        nodeTypeSpecs = null;

        fireEvRecalculated();
    }

    public EVSchedule getSchedule() { return schedule; }

    public void setDependencyCalculator(EVDependencyCalculator d) {
        this.dependencyCalculator = d;
    }

    public void disableBaselineData() {
        setBaselineDataSource(null);
        showBaselineColumns = false;
        schedule.getMetrics().discardMetrics(
            new PatternList().addRegexp("Baseline"));
    }

    public void setTaskLabeler(TaskLabeler l) {
        this.taskLabeler = l;
        if (l instanceof MilestoneProvider)
            setMilestoneProvider((MilestoneProvider) l);
    }

    /** @since 2.0.2 */
    public TaskLabeler getTaskLabeler() {
        return taskLabeler;
    }

    /** @since 2.0.1 */
    public void setMilestoneProvider(MilestoneProvider m) {
        this.milestoneProvider = m;
    }

    public List getLabelsForTask(EVTask n) {
        if (taskLabeler == null)
            return null;
        return taskLabeler.getLabelsForTask(n);
    }

    private Object getTaskLabelsText(EVTask n) {
        List<String> labels = getLabelsForTask(n);
        if (labels == null || labels.isEmpty())
            return "";

        if (labels.size() == 1) {
            String label = labels.get(0);
            return (shouldHide(label) ? "" : label);
        }

        StringBuilder result = new StringBuilder();
        for (String label : labels) {
            if (!shouldHide(label))
                result.append(", ").append(label);
        }
        if (result.length() > 0)
            return result.substring(2);
        else
            return "";
    }

    private boolean shouldHide(String label) {
        return taskLabeler.getHiddenLabels().contains(label);
    }

    private boolean scanForLabelsAndMilestones(EVTask task) {
        if (taskLabeler == null)
            return false;

        List<String> labels = taskLabeler.getLabelsForTask(task);
        if (labels != null) {
            for (String l : labels) {
                if (!showMilestoneColumn && l.startsWith(
                        MilestoneDataConstants.MILESTONE_ID_LABEL_PREFIX))
                    showMilestoneColumn = true;
                else if (!showLabelsColumn && !shouldHide(l))
                    showLabelsColumn = true;
            }
        }
        if (showLabelsColumn && showMilestoneColumn)
            return true;

        for (int i = task.getNumChildren();  i-- > 0; )
            if (scanForLabelsAndMilestones(task.getChild(i)))
                return true;

        return false;
    }

    private String getMilestoneTaskError(EVTask t) {
        MilestoneList m = getMilestonesForTask(t);
        return (m == null ? null : m.getMissedMilestoneMessage());
    }

    protected void scanForMilestoneErrors(List<EVTask> evLeaves) {
        if (!"errors".equalsIgnoreCase(Settings
                .getVal("ev.showMissedMilestones")))
            return;
        if (milestoneProvider == null)
            return;

        Set<Milestone> missedMilestones = new HashSet();
        for (EVTask task : evLeaves) {
            MilestoneList milestones = getMilestonesForTask(task);
            if (milestones != null)
                missedMilestones.add(milestones.getMissedMilestone());
        }
        missedMilestones.remove(null);
        for (Milestone m : missedMilestones)
            schedule.getMetrics().addError(resources.format(
                "Task.Milestone_Date.Multiple_Error_Msg_FMT",
                m.getCommitDate(), m.getName()) + " ", //
                getTaskRoot());
    }

    public MilestoneList getMilestonesForTask(EVTask task) {
        List<Milestone> milestones = null;
        if (milestoneProvider != null)
            milestones = milestoneProvider.getMilestonesForTask(task);

        if (milestones == null || milestones.isEmpty())
            return null;
        else
            return new MilestoneList(milestones, getMissedMilestone(task,
                milestones));
    }

    private Milestone getMissedMilestone(EVTask t, List<Milestone> milestones) {
        if (t.getDateCompleted() != null || t.isValuePruned() || !t.isLeaf())
            return null;

        Date projected = EVTaskDependency.getDependencyComparisonDate(t);
        if (projected == null)
            return null;

        if (milestones == null || milestones.size() != 1)
            return null;

        Milestone m = milestones.get(0);
        while (m != null) {
            Date commitDate = m.getCommitDate();
            if (commitDate != null) {
                long delta = projected.getTime() - commitDate.getTime();
                if (delta > EVCalculator.DAY_MILLIS)
                    return m;
            }
            m = m.getNextMilestone();
        }

        return null;
    }

    public Set getNodeTypeSpecs() {
        if (nodeTypeSpecs == null)
            recalcNodeTypeSpecs();
        return nodeTypeSpecs;
    }

    protected void recalcNodeTypeSpecs() {
        Set result = new LinkedHashSet();
        recalcNodeTypeSpecs((EVTask) root, result);
        nodeTypeSpecs = result;
    }

    private void recalcNodeTypeSpecs(EVTask task, Set result) {
        ListData l = task.nodeTypeSpec;
        if (l != null && l.size() > 0)
            result.add(l.asList());
        for (int i = task.getNumChildren();  i-- > 0; )
            recalcNodeTypeSpecs(task.getChild(i), result);
    }

    public List findTasksByFullName(String fullName) {
        return ((EVTask) root).findByFullName(fullName);
    }

    public String saveSnapshot(String snapshotId, String snapshotName,
            String snapshotComment) {
        return null;
    }

    public EVSnapshot getSnapshotById(String snapshotId) {
        return null;
    }

    public EVSnapshot getBaselineSnapshot() {
        String snapshotId = getMetadata(EVMetadata.Baseline.SNAPSHOT_ID);
        if (!StringUtils.hasValue(snapshotId))
            return null;
        else
            return getSnapshotById(snapshotId);
    }

    protected void setBaselineDataSource(EVSnapshot snapshot) {
        if (calculator != null)
            calculator.setBaselineDataSource(snapshot);
        schedule.setBaseline(snapshot);
    }

    /** Possibly shift this task list from its original time zone into the
     * default local time zone.
     */
    protected void maybeRetargetTimezone() {
        // retrieve the user preference for time zone alignment.
        String userSetting = Settings.getVal("ev."
                + EVMetadata.TimeZone.RollupStrategy.SETTING,
            EVMetadata.TimeZone.RollupStrategy.REALIGN_TO_CALENDAR);

        // if the user wants no changes to be made, return.
        if (EVMetadata.TimeZone.RollupStrategy.NO_CHANGE.equals(userSetting))
            return;

        // look up the time zone specified for this task list.
        TimeZone timezone = TimeZoneUtils.getTimeZone(getTimezoneID());
        boolean isTimeZonePrecise = (timezone != null);

        // by default, the dashboard creates schedules with periods that are
        // a week long, and that start/end at midnight. However, individuals can
        // alter the From/To dates to create alternative schedules (such as
        // daily or hourly schedules).  Perform a quick check to see if this
        // schedule appears to be "weekly."
        boolean isWeeklySchedule = (schedule.getAverageDaysPerPeriod() > 5);

        // if the schedule has odd periods (not a week long), then we can't
        // assume its periods start/end at midnight.  So if no time zone info
        // was provided and the schedule isn't "weekly," do nothing.
        if (!isTimeZonePrecise && !isWeeklySchedule)
            return;

        // if no time zone information was associated with this "weekly"
        // schedule, infer the time zone from the start time of the EV schedule.
        if (timezone == null)
            timezone = schedule.guessTimeZone();

        realignScheduleFrom(timezone, isTimeZonePrecise, isWeeklySchedule);
    }

    private void realignScheduleFrom(TimeZone timezone,
            boolean isTimeZonePrecise, boolean normalizePeriods) {

        // if the current time zone has the same rules as the original time
        // zone, no changes need to be made.
        TimeZone current = TimeZone.getDefault();
        if (current.hasSameRules(timezone))
            return;

        DateAdjuster timeZoneAdjustment = new TimeZoneDateAdjuster(timezone,
                current);
        DateAdjuster normalizationAdjustment = null;

        if (normalizePeriods) {
            // how many milliseconds different is the current time zone from
            // the source time zone?
            int offset = timezone.getRawOffset() - current.getRawOffset();
            // normalize the dates in the schedule, using that offset as the
            // nominal adjustment.
            normalizationAdjustment = schedule.normalizeDates(offset);
        } else {
            schedule.adjustDates(timeZoneAdjustment);
        }

        // now, apply timestamp adjustments to the task list as well.
        if (isTimeZonePrecise || normalizationAdjustment == null)
            // if the source timezone is exact, or if normalization was not
            // feasible, perform exact time zone math on the dates in the
            // task list.
            getTaskRoot().adjustDates(timeZoneAdjustment);
        else
            // if the source timezone was inferred, just apply the same
            // adjustments that were made to the schedule.
            getTaskRoot().adjustDates(normalizationAdjustment);
    }


    //////////////////////////////////////////////////////////////////////
    /// TreeTableModel support
    //////////////////////////////////////////////////////////////////////


    public static final String[] COLUMN_KEYS = {
        "Task", "NodeType", "PT", "PDT", "BT", "Time", "DTime", "PV", "CPT",
        "CPV", "Who", "Baseline_Date", "Plan_Date", "Replan_Date",
        "Forecast_Date", "Date", "Milestone", "Labels", "Notes", "Depn",
        "PctC", "PctS", "EV" };

    /** Names of the columns in the TreeTableModel. */
    protected static String[] colNames =
        resources.getStrings("TaskList.Columns.", COLUMN_KEYS, ".Name");
    public static int[] colWidths =
        resources.getInts("TaskList.Columns.", COLUMN_KEYS, ".Width_");
    public static String[] toolTips =
        resources.getStrings("TaskList.Columns.", COLUMN_KEYS, ".Tooltip");

    public static final int TASK_COLUMN           = 0;
    public static final int NODE_TYPE_COLUMN      = TASK_COLUMN+1;
    public static final int PLAN_TIME_COLUMN      = NODE_TYPE_COLUMN+1;
    public static final int PLAN_DTIME_COLUMN     = PLAN_TIME_COLUMN+1;
    public static final int BASELINE_TIME_COLUMN  = PLAN_DTIME_COLUMN+1;
    public static final int ACT_TIME_COLUMN       = BASELINE_TIME_COLUMN+1;
    public static final int ACT_DTIME_COLUMN      = ACT_TIME_COLUMN+1;
    public static final int PLAN_VALUE_COLUMN     = ACT_DTIME_COLUMN+1;
    public static final int PLAN_CUM_TIME_COLUMN  = PLAN_VALUE_COLUMN+1;
    public static final int PLAN_CUM_VALUE_COLUMN = PLAN_CUM_TIME_COLUMN+1;
    public static final int ASSIGNED_TO_COLUMN    = PLAN_CUM_VALUE_COLUMN+1;
    public static final int BASELINE_DATE_COLUMN  = ASSIGNED_TO_COLUMN+1;
    public static final int PLAN_DATE_COLUMN      = BASELINE_DATE_COLUMN+1;
    public static final int REPLAN_DATE_COLUMN    = PLAN_DATE_COLUMN+1;
    public static final int FORECAST_DATE_COLUMN  = REPLAN_DATE_COLUMN+1;
    public static final int DATE_COMPLETE_COLUMN  = FORECAST_DATE_COLUMN+1;
    public static final int MILESTONE_COLUMN      = DATE_COMPLETE_COLUMN+1;
    public static final int LABELS_COLUMN         = MILESTONE_COLUMN+1;
    public static final int NOTES_COLUMN          = LABELS_COLUMN+1;
    public static final int DEPENDENCIES_COLUMN   = NOTES_COLUMN+1;
    public static final int PCT_COMPLETE_COLUMN   = DEPENDENCIES_COLUMN+1;
    public static final int PCT_SPENT_COLUMN      = PCT_COMPLETE_COLUMN+1;
    public static final int VALUE_EARNED_COLUMN   = PCT_SPENT_COLUMN+1;

    // pseudo column numbers for retrieving other extended task data
    public static final int TASK_FULLNAME_COLUMN  = -88888;
    public static final int EVTASK_NODE_COLUMN    = -99999;
    public static final int PROJ_DATE_COLUMN      = -1000;
    public static final int ACT_START_DATE_COLUMN = -123;

    public static final int[] HIDABLE_COLUMN_LIST = { NODE_TYPE_COLUMN,
            PLAN_DTIME_COLUMN, ACT_DTIME_COLUMN, BASELINE_TIME_COLUMN,
            BASELINE_DATE_COLUMN, REPLAN_DATE_COLUMN, MILESTONE_COLUMN,
            LABELS_COLUMN, NOTES_COLUMN };

    public static final String ID_DATA_NAME = "Task List ID";
    private static final String METADATA_DATA_NAME = "Task_List_Metadata";
    private static final String PROJECT_SCHEDULE_ID = "Project_Schedule_ID";
    private static final String PROJECT_SCHEDULE_NAME = "Project_Schedule_Name";
    private static final String SNAPSHOT_DATA_PREFIX = "Snapshot";

    /** Types of the columns in the TreeTableModel. */
    static protected Class[]  colTypes = {
        TreeTableModel.class,   // project/task
        String.class,           // node type
        String.class,           // planned time
        String.class,           // planned direct time
        String.class,           // baseline time
        String.class,           // actual time
        String.class,           // actual direct time
        String.class,           // planned value
        String.class,           // planned cumulative time
        String.class,           // planned cumulative value
        String.class,           // assigned to
        Date.class,             // baseline date
        Date.class,             // planned date
        Date.class,             // replanned date
        Date.class,             // forecast date
        Date.class,             // date
        MilestoneList.class,    // milestone
        String.class,           // labels
        Map.class,              // notes
        Collection.class,       // task dependencies
        String.class,           // percent complete
        String.class,           // percent spent
        String.class };         // earned value


    public static final Object COLUMN_FMT_OTHER = "other";
    public static final Object COLUMN_FMT_TIME = "time";
    public static final Object COLUMN_FMT_DATE = "date";
    public static final Object COLUMN_FMT_PERCENT = "percent";

    public static final Object[] COLUMN_FORMATS = {
        COLUMN_FMT_OTHER,     // project/task
        COLUMN_FMT_OTHER,     // node type
        COLUMN_FMT_TIME,      // planned time
        COLUMN_FMT_TIME,      // planned direct time
        COLUMN_FMT_TIME,      // baseline time
        COLUMN_FMT_TIME,      // actual time
        COLUMN_FMT_TIME,      // actual direct time
        COLUMN_FMT_PERCENT,   // planned value
        COLUMN_FMT_TIME,      // planned cumulative time
        COLUMN_FMT_PERCENT,   // planned cumulative value
        COLUMN_FMT_OTHER,     // assigned to
        COLUMN_FMT_DATE,      // baseline date
        COLUMN_FMT_DATE,      // planned date
        COLUMN_FMT_DATE,      // replanned date
        COLUMN_FMT_DATE,      // forecast date
        COLUMN_FMT_DATE,      // date
        COLUMN_FMT_OTHER,     // milestone
        COLUMN_FMT_OTHER,     // labels
        COLUMN_FMT_OTHER,     // notes
        COLUMN_FMT_OTHER,     // task dependencies
        COLUMN_FMT_PERCENT,   // percent complete
        COLUMN_FMT_PERCENT,   // percent spent
        COLUMN_FMT_PERCENT,   // earned value
    };


    //
    // The TreeModel interface
    //

    /** Returns the number of children of <code>node</code>. */
    public int getChildCount(Object node) {
        return ((EVTask) node).getNumChildren();
    }

    /** Returns the child of <code>node</code> at index <code>i</code>. */
    public Object getChild(Object node, int i) {
        return ((EVTask) node).getChild(i);
    }

    /** Returns true if the passed in object represents a leaf, false
     *  otherwise. */
    public boolean isLeaf(Object node) {
        return ((EVTask) node).isLeaf();
    }

    /** Returns true if the direct time columns in this schedule should be
     * displayed.
     */
    public boolean showDirectTimeColumns() {
        return showDirectTimeColumns;
    }

    /** Returns true if the milestone column in this schedule should be displayed */
    public boolean showMilestoneColumn() {
        return showMilestoneColumn;
    }

    /** Returns true if the label column in this schedule should be displayed */
    public boolean showLabelsColumn() {
        return showLabelsColumn;
    }

    /** Returns true if the notes column in this schedule should be displayed */
    public boolean showNotesColumn() {
        return showNotesColumn;
    }

    /** Returns true if the value in column <code>column</code> of object
     *  <code>node</code> is editable. */
    public boolean isCellEditable(Object node, int column) {
        if (node == null) return false;
        switch (column) {
        case TASK_COLUMN:
            // The column with the tree in it should be editable; this
            // causes the JTable to forward mouse and keyboard events
            // in the Tree column to the underlying JTree.
            return true;

        case NODE_TYPE_COLUMN:
            return Settings.isReadWrite()
                    && ((EVTask) node).isNodeTypeEditable();

        case PLAN_TIME_COLUMN:
            return Settings.isReadWrite()
                    && ((EVTask) node).isPlannedTimeEditable();

        case DATE_COMPLETE_COLUMN:
            return Settings.isReadWrite()
                    && ((EVTask) node).isCompletionDateEditable();

        case NOTES_COLUMN:
        case DEPENDENCIES_COLUMN:
            return (Settings.isReadWrite()
                    && this instanceof EVTaskListData && node != root);
        }
        return false;
    }


    //
    //  The TreeTableNode interface.
    //

    /** Returns the number of columns. */
    public int getColumnCount() { return colNames.length; }

    /** Returns the name for a particular column. */
    public String getColumnName(int column) {
        boolean showColumn = true;
        if (column == PLAN_DTIME_COLUMN || column == ACT_DTIME_COLUMN)
            showColumn = showDirectTimeColumns;
        else if (column == BASELINE_TIME_COLUMN
                || column == BASELINE_DATE_COLUMN)
            showColumn = showBaselineColumns;
        else if (column == NODE_TYPE_COLUMN)
            showColumn = showNodeTypeColumn;
        else if (column == REPLAN_DATE_COLUMN)
            showColumn = showReplanColumn;
        else if (column == MILESTONE_COLUMN)
            showColumn = showMilestoneColumn;
        else if (column == LABELS_COLUMN)
            showColumn = showLabelsColumn;
        else if (column == NOTES_COLUMN)
            showColumn = showNotesColumn;

        return getColumnName(column, showColumn);
    }

    private String getColumnName(int column, boolean showColumn) {
        if (showColumn)
            return colNames[column];
        else
            return " " + colNames[column] + " ";
    }

    /** Returns the class for the particular column. */
    public Class getColumnClass(int column) { return colTypes[column]; }

    /** Returns the value of the particular column. */
    public Object getValueAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN:           return n.getName();
        case TASK_FULLNAME_COLUMN:  return n.getFullName();
        case NODE_TYPE_COLUMN:      return n.getNodeType();
        case PLAN_TIME_COLUMN:      return n.getPlanTimeText();
        case -PLAN_TIME_COLUMN:     return n.getPlanTime();
        case PLAN_DTIME_COLUMN:     return n.getPlanDirectTimeText();
        case -PLAN_DTIME_COLUMN:    return n.getPlanDirectTime();
        case ACT_TIME_COLUMN:       return n.getActualTimeText(totalActualTime);
        case -ACT_TIME_COLUMN:      return n.getActualTime();
        case ACT_DTIME_COLUMN:      return n.getActualDirectTimeText();
        case -ACT_DTIME_COLUMN:     return n.getActualDirectTime();
        case PLAN_VALUE_COLUMN:     return n.getPlanValueText(totalPlanValue);
        case -PLAN_VALUE_COLUMN:    return n.getPlanValuePercent(totalPlanValue);
        case PLAN_CUM_TIME_COLUMN:  return n.getCumPlanTimeText();
        case PLAN_CUM_VALUE_COLUMN: return n.getCumPlanValueText(totalPlanValue);
        case ASSIGNED_TO_COLUMN:    return n.getAssignedToText();
        case -ASSIGNED_TO_COLUMN:   return n.getAssignedTo();
        case BASELINE_TIME_COLUMN:  return n.getBaselineTimeText();
        case -BASELINE_TIME_COLUMN: return n.getBaselineTime();
        case BASELINE_DATE_COLUMN:  return n.getBaselineDate();
        case PLAN_DATE_COLUMN:      return n.getPlanDate();
        case REPLAN_DATE_COLUMN:    return n.getReplanDate();
        case FORECAST_DATE_COLUMN:  return n.getForecastDate();
        case DATE_COMPLETE_COLUMN:  return n.getActualDate();
        case MILESTONE_COLUMN:      return getMilestonesForTask(n);
        case LABELS_COLUMN:         return getTaskLabelsText(n);
        case NOTES_COLUMN:          return n.getNoteData();
        case DEPENDENCIES_COLUMN:   return n.getDependencies();
        case PCT_COMPLETE_COLUMN:   return n.getPercentCompleteText();
        case PCT_SPENT_COLUMN:      return n.getPercentSpentText();
        case -PCT_SPENT_COLUMN:     return n.getPercentSpent();
        case VALUE_EARNED_COLUMN:   return n.getValueEarnedText(totalPlanValue);
        case ACT_START_DATE_COLUMN: return n.getActualStartDate();

        case EVTASK_NODE_COLUMN:    return n;
        case PROJ_DATE_COLUMN:
            return EVTaskDependency.getDependencyComparisonDate(n);
        }
        return null;
    }

    /** Set the value at a particular row/column */
    public void setValueAt(Object value, Object node, int column) {
        //System.out.println("setValueAt("+aValue+","+node+","+column+")");
        if (node == null) return;
        EVTask n = (EVTask) node;
        switch (column) {
        case NODE_TYPE_COLUMN:     n.userSetNodeType(value);              break;
        case PLAN_TIME_COLUMN:     n.userSetPlanTime(value);              break;
        case DATE_COMPLETE_COLUMN: n.userSetActualDate(value);            break;
        case DEPENDENCIES_COLUMN:  n.setDependencies((Collection) value); break;
        }
    }

    /** If the given cell has an error, return it.  Otherwise return null */
    public String getErrorStringAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN: return n.getTaskError();
        case PLAN_TIME_COLUMN: return n.getPlanTimeError();
        case NODE_TYPE_COLUMN: return n.getNodeTypeError();
        case DATE_COMPLETE_COLUMN: return n.getDateCompleteError();
        case MILESTONE_COLUMN: return getMilestoneTaskError(n);
        default: return null;
        }
    }


    /** Load the unique ID for this task list.
     * @param taskListName the name of this task list.
     * @param data the DataRepository
     * @param savedDataName the name of a data element that is used for the
     *   persistence of this task list. (It will be used to check and see if
     *   this task list is new, or is an existing task list.)
     */
    protected void loadID(String taskListName, DataRepository data,
            String savedDataName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName = DataRepository.createDataName
            (globalPrefix, ID_DATA_NAME);

        SimpleData d = data.getSimpleValue(dataName);
        if (d != null) {
            taskListID = d.format();
        } else {
            // This task list doesn't have a unique ID yet.  Generate one.
            // It should be a value that needs no special handling to
            // appear as an XML attribute.
            int i = Math.abs((new Random()).nextInt());
            taskListID =
                Integer.toString(i, Character.MAX_RADIX) + "." +
                Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);

            // Since unique task list IDs were introduced after version 1.5
            // of the dashboard, we may be in this branch of code not
            // because this is a new task list, but because this is the
            // first time the list has been opened.  In the latter case,
            // we need to save the unique ID.
            String savedDataFullName = DataRepository.createDataName
                (globalPrefix, savedDataName);
            if (data.getValue(savedDataFullName) != null) {
                // getting to this point indicates that this task list is
                // not new - it has been previously saved to the repository.
                data.putValue(dataName, StringData.create(taskListID));
            } else {
                isBrandNewTaskList = true;
            }
        }

        setPseudoTaskIdForRoot();
    }

    // assign a pseudo task ID to the task root.
    protected void setPseudoTaskIdForRoot() {
        String rootTaskID = EVTaskDependencyResolver
                .getPseudoTaskIdForTaskList(taskListID);
        getTaskRoot().taskIDs = Collections.singletonList(rootTaskID);
    }

    /** Save the unique ID for this task list.
     * @param newName the name to use when saving this task list.
     * @param data the DataRepository
     */
    protected String saveID(String newName, DataRepository data) {
        StringData value = StringData.create(taskListID);
        return persistDataValue(newName, data, ID_DATA_NAME, value);
    }

    /** Load the metadata for this task list.
     * @param taskListName the name of this task list.
     * @param data the DataRepository
    */
    protected void loadMetadata(String taskListName, DataRepository data) {
        metaData = new Properties();

        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName = DataRepository.createDataName
            (globalPrefix, METADATA_DATA_NAME);

        SimpleData d = data.getSimpleValue(dataName);
        String val = (d == null ? null : d.format());
        if (StringUtils.hasValue(val)) {
            try {
                metaData.load(new ByteArrayInputStream(val.getBytes("ISO-8859-1")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Save the metadata for this task list.
     * @param newName the name to use when saving this task list.
     * @param data the DataRepository
     */
    protected String saveMetadata(String newName, DataRepository data) {
        SimpleData md = null;
        if (newName != null && metaData != null && !metaData.isEmpty()) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                metaData.store(out, "task list metadata");
                md = StringData.create(out.toString("ISO-8859-1"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return persistDataValue(newName, data, METADATA_DATA_NAME, md);
    }

    /**
     * Retrieve a single snapshot of this task list.
     * 
     * @param data the DataRepository
     * @param snapshotId the ID of the snapshot to retrieve
     * @return the {@link EVSnapshot} of this schedule with the given ID, or
     *     null if no such snapshot exists.
     */
    protected EVSnapshot getSnapshotFromData(DataRepository data,
            String snapshotId) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName = globalPrefix + "/" + SNAPSHOT_DATA_PREFIX + "/"
                + snapshotId;
        SimpleData d = data.getSimpleValue(dataName);
        if (d != null && d.test()) {
            try {
                return new EVSnapshot(snapshotId, d.format());
            } catch (Exception e) {
                logger.warning("Couldn't open snapshot '" + snapshotId
                        + "' for task list '" + taskListName + "' - aborting");
            }
        }
        return null;
    }

    /**
     * Save a snapshot of this task list to the data repository.
     * 
     * Note: it is the responsibility of the caller to recalculate this
     * task list <b>before</b> calling this method.
     * 
     * @param data the {@link DataRepository}
     * @param snapshotId the preferred id to use for the snapshot.  Can be
     *     null to indicate that an id should be autogenerated.  If a snapshot
     *     with the given ID already exists, it will be overwritten with a new
     *     snapshot
     * @param snapshotName the user-displayable name to give the snapshot.
     * @return the ID that was assigned to the snapshot
     */
    protected String saveSnapshotToData(DataRepository data, String snapshotId,
            String snapshotName, String snapshotComment) {
        if (snapshotId == null) {
            snapshotId = Long.toString(System.currentTimeMillis(),
                Character.MAX_RADIX);
        }

        EVSnapshot snap = new EVSnapshot(snapshotId, snapshotName,
                snapshotComment, new Date(), this);
        String xml = snap.getAsXML();
        String dataName = SNAPSHOT_DATA_PREFIX + "/" + snapshotId;
        persistDataValue(taskListName, data, dataName, StringData.create(xml));

        return snapshotId;
    }

    /** Rename the snapshots for this task list.
     * 
     * @param oldNames the names of old data elements associated with this
     *      task list
     * @param newName the new name of the task list
     * @param data the data repository
     * @return the names of the new data elements used to store snapshots
     */
    protected void renameSnapshots(Set oldNames, String newName,
            DataRepository data) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName + "/";
        String oldSnapshotPrefix = globalPrefix + SNAPSHOT_DATA_PREFIX + "/";
        int oldPrefixLen = globalPrefix.length();
        boolean nameIsChanging = !taskListName.equals(newName);

        for (Iterator i = oldNames.iterator(); i.hasNext();) {
            String oldDataName = (String) i.next();
            if (oldDataName.startsWith(oldSnapshotPrefix)) {
                if (nameIsChanging) {
                    SimpleData value = data.getSimpleValue(oldDataName);
                    String dataName = oldDataName.substring(oldPrefixLen);
                    persistDataValue(newName, data, dataName, value);
                }
                i.remove();
            }
        }
    }

    public List<EVSnapshot.Metadata> getSnapshots() {
        return null;
    }

    /**
     * Retrieve a list of the snapshots that have been saved for this
     * task list.
     */
    protected List<EVSnapshot.Metadata> getSnapshots(DataRepository data) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName + "/";
        String snapshotPrefix = globalPrefix + SNAPSHOT_DATA_PREFIX + "/";
        int snapshotPrefixLen = snapshotPrefix.length();
        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        List<EVSnapshot.Metadata> result = new ArrayList();
        while (i.hasNext()) {
            String dataName = (String) i.next();
            if (dataName.startsWith(snapshotPrefix)) {
                try {
                    String snapshotId = dataName.substring(snapshotPrefixLen);
                    EVSnapshot.Metadata m = new EVSnapshot.Metadata(dataName,
                            snapshotId, data.getSimpleValue(dataName).format());
                    result.add(m);
                } catch (Exception e) {}
            }
        }
        Collections.sort(result);
        return result;
    }


    /**
     * Help a task list to persist data to the repository.
     * 
     * @param newName the name of the task list that we're saving.  May differ
     *    from the current taskListName if the task list is being renamed.
     *    Can be null to indicate that the task list is being deleted.
     * @param data the data repository
     * @param dataName the terminal part of the name that should be used to
     *    construct a data name for persistence
     * @param value the value to persist
     * @return the full dataname of the element that was saved to the repository
     */
    protected String persistDataValue(String newName, DataRepository data,
            String dataName, SimpleData value) {
        // delete the old data value if necessary.
        if (!taskListName.equals(newName)) {
            String oldDataName = DataRepository.createDataName
                (MAIN_DATA_PREFIX + taskListName, dataName);
            data.putValue(oldDataName, null);
        }

        // save the new value into the repository.
        String newDataName = null;
        if (newName != null) {
            newDataName = DataRepository.createDataName(MAIN_DATA_PREFIX
                    + newName, dataName);
            data.putValue(newDataName, value);
        }

        return newDataName;
    }

    public Object getDeepestNoteFor(EVTask node) {
        while (node != null && node != root) {
            Object result = node.getNoteData();
            if (result != null)
                return result;
            else
                node = node.getParent();
        }
        return null;
    }

    public List<EVTask> getFilteredLeaves(EVTaskFilter filter) {
        List allLeaves = null;
        if (calculator != null)
            allLeaves = calculator.getEVLeaves();
        if (allLeaves == null || allLeaves.isEmpty())
            allLeaves = ((EVTask) root).getLeafTasks();

        List<EVTask> result = new ArrayList<EVTask>();
        for (Iterator i = allLeaves.iterator(); i.hasNext();) {
            EVTask task = (EVTask) i.next();
            if (task.isChronologicallyPruned() &&
                task.actualDirectTime == 0)
                continue;
            else if (filter != null && !filter.include(task))
                continue;
            else
                result.add(task);
        }
        return result;
    }

    public TableModel getSimpleTableModel() {
        return getSimpleTableModel(null);
    }
    public TableModel getSimpleTableModel(EVTaskFilter filter) {
        return new SimpleTableModel(filter);
    }

    private class SimpleTableModel implements TableModel {
        private List rowList = null;

        public SimpleTableModel(EVTaskFilter filter) {
            rowList = getFilteredLeaves(filter);
        }

        public int getRowCount() { return rowList.size(); }
        public int getColumnCount() {
            return EVTaskList.this.getColumnCount(); }
        public String getColumnName(int columnIndex) {
            return EVTaskList.this.getColumnName(columnIndex); }
        public Class getColumnClass(int columnIndex) {
            return EVTaskList.this.getColumnClass(columnIndex); }
        public boolean isCellEditable(int row, int columnIndex) {
            return EVTaskList.this.isCellEditable(getRow(row), columnIndex); }
        public Object getValueAt(int row, int columnIndex) {
            if (columnIndex == 0)
                return getRow(row).getFullName();
            else if (columnIndex == NOTES_COLUMN)
                return getDeepestNoteFor(getRow(row));
            else if (columnIndex == DEPENDENCIES_COLUMN)
                return getRow(row).getAllDependencies();
            else
                return EVTaskList.this.getValueAt(getRow(row), columnIndex);
        }
        public void setValueAt(Object aValue, int row, int columnIndex) {
            EVTaskList.this.setValueAt(aValue, getRow(row), columnIndex); }
        public void addTableModelListener(TableModelListener l) { }
        public void removeTableModelListener(TableModelListener l) { }

        private EVTask getRow(int row) {
            return (EVTask) rowList.get(row);
        }
    }

    public FlatTreeModel getFlatModel() { return new FlatTreeModel(); }

    public class FlatTreeModel extends AbstractTreeTableModel
        implements TreeModelListener, RecalcListener, HierarchyNoteListener
    {

        private List evLeaves;

        private String commonPathPrefix;

        public FlatTreeModel() {
            super(EVTaskList.this.root);
            evLeaves = getEVLeaves();
            commonPathPrefix = getCommonPathPrefix();
            EVTaskList.this.addTreeModelListener(this);
            EVTaskList.this.addRecalcListener(this);
        }

        private List getEVLeaves() {
            if (calculator instanceof EVCalculatorData)
                return new ArrayList(((EVCalculatorData) calculator).getReorderableEVLeaves());
            else
                return new ArrayList(calculator.getEVLeaves());
        }

        private String getCommonPathPrefix() {
            EVTask taskRoot = EVTaskList.this.getTaskRoot();
            EVTask node = taskRoot;
            // see if the root node only has one child (and drill down into
            // the task hierarchy as long as only one child is present)
            while (node.getNumChildren() == 1) {
                node = node.getChild(0);
            }
            // if we found a single, regular task that is the ancestor of all
            // the other tasks, then we know that all the tasks in the plan
            // will share its full name as their path prefix.
            if (node != taskRoot && node.getNumChildren() > 1) {
                return node.getFullName();
            }
            return null;
        }

        public Class getColumnClass(int column) {
            return EVTaskList.this.getColumnClass(column);
        }

        public boolean isCellEditable(Object node, int column) {
            return EVTaskList.this.isCellEditable(node, column);
        }

        public void setValueAt(Object aValue, Object node, int column) {
            EVTaskList.this.setValueAt(aValue, node, column);
        }

        public int getColumnCount() {
            return EVTaskList.this.getColumnCount();
        }

        public String getColumnName(int column) {
            return EVTaskList.this.getColumnName(column);
        }

        public Object getValueAt(Object node, int column) {
            if (column == TASK_COLUMN)
                return getFlatNodeDisplayName((EVTask) node);
            else if (column == NOTES_COLUMN)
                return getDeepestNoteFor((EVTask) node);
            else if (column == DEPENDENCIES_COLUMN)
                return ((EVTask) node).getAllDependencies();
            else
                return EVTaskList.this.getValueAt(node, column);
        }

        private String getFlatNodeDisplayName(EVTask node) {
            String result;
            if (node == root) {
                result = node.getName();
                if (commonPathPrefix != null)
                    // display the shared prefix on the root node.
                    result = result + " - " + commonPathPrefix;
            } else {
                result = node.getFullName();
                if (commonPathPrefix != null)
                    // remove the shared prefix, along with the slash
                    // character that follows it.
                    result = result.substring(commonPathPrefix.length() + 1);
            }
            return result;
        }

        public int getChildCount(Object parent) {
            if (parent == root)
                return evLeaves.size();
            return 0;
        }

        public Object getChild(Object parent, int index) {
            if (parent == root && index >= 0 && index < evLeaves.size())
                return evLeaves.get(index);
            return null;
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == root && child instanceof EVTask)
                return EVTask.indexOfNode(evLeaves, (EVTask) child);
            return -1;
        }

        public void treeNodesChanged(TreeModelEvent e) { recalcLeafList(); }
        public void treeNodesInserted(TreeModelEvent e) { recalcLeafList(); }
        public void treeNodesRemoved(TreeModelEvent e) { recalcLeafList(); }
        public void treeStructureChanged(TreeModelEvent e) { recalcLeafList(); }
        public void evRecalculated(EventObject e) { recalcLeafList(); }

        private synchronized void recalcLeafList() {
            // get the new leaf list.
            List oldLeaves = evLeaves;
            List newLeaves = getEVLeaves();

            // compare the new list to the existing list.  If they are the
            // same, nothing needs to be done.
            if (newLeaves.equals(evLeaves)) return;

            // fire appropriate events based upon the changes to the list.
            int oldLen = oldLeaves.size();
            int newLen = newLeaves.size();
            int changedRows = Math.min(newLen, oldLen);

            evLeaves = newLeaves;
            commonPathPrefix = getCommonPathPrefix();

            int[] childIndices = new int[changedRows];
            Object[] children = new Object[changedRows];
            for (int i = 0;   i < changedRows;   i++) {
                childIndices[i] = i;
                children[i] = newLeaves.get(i);
            }
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                                 childIndices, children);

            if (oldLen < newLen) {
                int insertedRows = newLen-oldLen;
                childIndices = new int[insertedRows];
                children = new Object[insertedRows];
                for (int i = 0;   i < insertedRows;   i++) {
                    childIndices[i] = i+changedRows;
                    children[i] = newLeaves.get(i+changedRows);
                }
                fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                                      childIndices, children);
                // the next line shouldn't be necessary - but the JTreeTable
                // seems to have a bug.  When it handles the treeNodesInserted
                // event above, it seems to call the toString() method on
                // each child, and allocate screen real estate for each tree
                // node based on an assumption that the toString() value is
                // what will be displayed.  That assumption is not correct -
                // we display a full path instead.  Somehow, the line below
                // helps the JTreeTable to recover and paint correctly.
                fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                                     childIndices, children);

            } else if (oldLen > newLen) {
                int deletedRows = oldLen-newLen;
                childIndices = new int[deletedRows];
                children = new Object[deletedRows];
                for (int i = 0;   i < deletedRows;   i++) {
                    childIndices[i] = i+changedRows;
                    children[i] = oldLeaves.get(i+changedRows);
                }
                fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                                     childIndices, children);
            }
        }

        private void enumerateOrdinals() {
            Iterator allLeaves = evLeaves.iterator();
            int pos = 1;
            while (allLeaves.hasNext()) {
                EVTask task = (EVTask) allLeaves.next();
                task.taskOrdinal = pos++;
            }
        }

        public boolean moveTasksUp(int firstTaskPos, int lastTaskPos) {
            if (lastTaskPos < firstTaskPos
                    || firstTaskPos < 1
                    || lastTaskPos >= evLeaves.size())
                return false;

            EVTask predecessor = (EVTask) evLeaves.remove(firstTaskPos - 1);
            evLeaves.add(lastTaskPos, predecessor);
            enumerateOrdinals();

            fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos - 1 },
                    new Object[] { predecessor });
            fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos },
                    new Object[] { predecessor });
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos },
                    new Object[] { predecessor });

            if (recalcTimer != null) recalcTimer.restart();

            return true;
        }

        public boolean moveTasksDown(int firstTaskPos, int lastTaskPos) {
            if (lastTaskPos < firstTaskPos
                    || firstTaskPos < 0
                    || lastTaskPos >= evLeaves.size() - 1)
                return false;

            EVTask successor = (EVTask) evLeaves.remove(lastTaskPos + 1);
            evLeaves.add(firstTaskPos, successor);
            enumerateOrdinals();

            fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos + 1 },
                    new Object[] { successor });
            fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos },
                    new Object[] { successor });
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos },
                    new Object[] { successor });

            if (recalcTimer != null) recalcTimer.restart();

            return true;
        }

        /** Extract and reorder one or more tasks from the list, and reinsert
         * them at a particular location.
         * 
         * @param tasks a list of task full names, separated by tabs/newlines.
         *      If a sequence of non-whitespace characters does not name any
         *      ev leaf in this flat model, it will be ignored.
         * @param beforePos the position in the task list where the tasks
         *      should be reinserted
         * @return if the <tt>tasks</tt> parameter did not name any tasks in
         *      this flat model, returns null.  Otherwise, returns an array
         *      of two integers, indicating the first and last indices of the
         *      reinserted nodes.
         */
        public int[] insertTasks(String tasks, int beforePos) {
            if (tasks == null || tasks.trim().length() == 0)
                return null;

            List newLeaves = new LinkedList(evLeaves);
            Object insertionMarker = new Object();
            if (beforePos >= newLeaves.size()) {
                newLeaves.add(insertionMarker);
            } else {
                newLeaves.add(beforePos, insertionMarker);
            }

            String[] nodeNames = tasks.split("[\t\f\r\n]+",0);
            List tasksToInsert = new ArrayList(nodeNames.length);
            for (int i = 0; i < nodeNames.length; i++)
                extractNamedTask(newLeaves, tasksToInsert, nodeNames[i]);
            if (tasksToInsert.isEmpty())
                return null;

            int insertionPos = newLeaves.indexOf(insertionMarker);
            newLeaves.remove(insertionPos);
            newLeaves.addAll(insertionPos, tasksToInsert);

            evLeaves = new ArrayList(newLeaves);
            enumerateOrdinals();

            int[] changedNodes = new int[evLeaves.size()];
            for (int i = 0; i < changedNodes.length; i++)
                changedNodes[i] = i;
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                    changedNodes, evLeaves.toArray());

            if (recalcTimer != null) recalcTimer.restart();

            return new int[] { insertionPos,
                    insertionPos + tasksToInsert.size() - 1 };
        }

        private void extractNamedTask(List src, List dest,
                String flatNameOfTaskToExtract) {
            int extractNameLen = flatNameOfTaskToExtract.length();
            int prefixLen = (commonPathPrefix == null
                    ? 0 : commonPathPrefix.length() + 1);
            for (Iterator i = src.iterator(); i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof EVTask) {
                    EVTask task = (EVTask) obj;
                    String oneFullName = task.getFullName();
                    if (oneFullName.length() == extractNameLen + prefixLen
                            && flatNameOfTaskToExtract.regionMatches(true, 0,
                                oneFullName, prefixLen, extractNameLen)) {
                        i.remove();
                        dest.add(task);
                        break;
                    }
                }
            }
        }

        public void notesChanged(HierarchyNoteEvent e) {
            List<Integer> changedPositions = new ArrayList();
            String path = e.getPath();
            for (int i = evLeaves.size(); i-- > 0; ) {
                EVTask t = (EVTask) evLeaves.get(i);
                if (Filter.pathMatches(t.fullName, path, true))
                    changedPositions.add(i);
            }
            if (changedPositions.isEmpty())
                return;
            int[] changedIndexes = new int[changedPositions.size()];
            Object[] changedNodes = new Object[changedPositions.size()];
            for (int i = 0;  i < changedIndexes.length; i++) {
                Integer pos = changedPositions.get(i);
                changedIndexes[i] = pos;
                changedNodes[i] = evLeaves.get(pos);
            }

            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                changedIndexes, changedNodes);
        }

        public int getIndexOfFirstTaskFinishingAfter(Date commitDate) {
            if (commitDate == null)
                return -1;

            for (int i = 0;  i < evLeaves.size();  i++) {
                EVTask t = (EVTask) evLeaves.get(i);
                Date proj = EVTaskDependency.getDependencyComparisonDate(t);
                if (proj == null)
                    continue;

                long delta = proj.getTime() - commitDate.getTime();
                if (delta > EVCalculator.DAY_MILLIS)
                    return i;
            }

            return evLeaves.size();
        }
    }

    public TreeTableModel getMergedModel() {
        boolean simple = Settings.getBool("ev.simplifiedMerge", true);
        boolean leaves = Settings.getBool("ev.mergePreservesLeaves", true);
        return getMergedModel(simple, leaves, null);
    }

    public TreeTableModel getMergedModel(boolean simple,
            boolean preserveLeaves, EVTaskFilter filter) {
        EVTaskListMerger merger = new EVTaskListMerger(EVTaskList.this, simple,
                preserveLeaves, filter);
        MergedTreeModel result = new MergedTreeModel(merger);
        addRecalcListener(result);
        return result;
    }


    public class MergedTreeModel extends AbstractTreeTableModel implements
            RecalcListener {

        private EVTaskListMerger merger;

        public MergedTreeModel(EVTaskListMerger merger) {
            super(merger.getMergedTaskRoot());
            this.merger = merger;
        }

        public Object getChild(Object parent, int index) {
            return EVTaskList.this.getChild(parent, index);
        }

        public int getChildCount(Object parent) {
            return EVTaskList.this.getChildCount(parent);
        }

        public Class getColumnClass(int column) {
            return EVTaskList.this.getColumnClass(column);
        }

        public int getColumnCount() {
            return EVTaskList.this.getColumnCount();
        }

        public String getColumnName(int column) {
            return EVTaskList.this.getColumnName(column);
        }

        public int getIndexOfChild(Object parent, Object child) {
            return EVTaskList.this.getIndexOfChild(parent, child);
        }

        public Object getValueAt(Object node, int column) {
            if (column == MERGED_DESCENDANT_NODES)
                return merger.getTasksMergedBeneath((EVTask) node);
            else
                return EVTaskList.this.getValueAt(node, column);
        }

        public boolean isCellEditable(Object node, int column) {
            return EVTaskList.this.isCellEditable(node, column);
        }

        public boolean isLeaf(Object node) {
            return EVTaskList.this.isLeaf(node);
        }

        public void evRecalculated(EventObject e) {
            merger.recalculate();
            fireTreeStructureChanged(this, new Object[] { root }, new int[0],
                    null);
        }

    }
    public static final int MERGED_DESCENDANT_NODES = -100;


    ///////////////////////////////////////////////////////////////////////
    // The methods/classes below assist in the generation of JFreeCharts
    // based on EVTaskList.
    //////////////////////////////////////////////////////////////////////


    protected class EVTaskChartEventAdapter extends ChartEventAdapter
            implements RecalcListener {

        @Override
        public void registerForUnderlyingDataEvents() { addRecalcListener(this); }
        @Override
        public void deregisterForUnderlyingDataEvents() { removeRecalcListener(this); }

        public void evRecalculated(EventObject e) { chartDataRecalcHelper.dataChanged(); }

    }

    /** A chart series where each EV leaf provides one data point */
    public abstract class EVLeavesChartSeries {

        private String seriesKey;

        private EVTaskFilter filter;

        protected List<EVTask> evLeaves;

        public EVLeavesChartSeries(String seriesKey, EVTaskFilter filter) {
            this.seriesKey = seriesKey;
            this.filter = filter;
        }

        public void recalc() {
            evLeaves = getFilteredLeaves(filter);
        }

        public String getSeriesKey() {
            return seriesKey;
        }

        public int getItemCount() {
            return evLeaves.size();
        }

        public EVTask get(int itemIndex) {
            return evLeaves.get(itemIndex);
        }

        public String getItemName(int itemIndex) {
            return get(itemIndex).getFullName();
        }

    }

    private abstract class EVLeavesXYChartSeries extends EVLeavesChartSeries
            implements XYChartSeries {

        public EVLeavesXYChartSeries(String seriesKey, EVTaskFilter filter) {
            super(seriesKey, filter);
        }

    }

    private class EVLeavesXYChartData extends XYChartData implements
            XYNameDataset {

        private EVLeavesXYChartSeries[] allSeries;

        public EVLeavesXYChartData(ChartEventAdapter eventAdapter,
                EVLeavesXYChartSeries... series) {
            super(eventAdapter);
            this.allSeries = series;
        }

        @Override
        public void recalc() {
            clearSeries();
            for (EVLeavesXYChartSeries s : allSeries) {
                s.recalc();
                maybeAddSeries(s);
            }
        }

        public String getName(int seriesIndex, int itemIndex) {
            EVLeavesXYChartSeries s = (EVLeavesXYChartSeries) getSeries()
                    .get(seriesIndex);
            return s.getItemName(itemIndex);
        }

    }


    private class PlanVsActualXYChartSeries extends EVLeavesXYChartSeries {

        public PlanVsActualXYChartSeries(EVTaskFilter filter) {
            super("Completed_Task", filter);
        }

        public Number getX(int itemIndex) {
            return get(itemIndex).planTime / 60.0;
        }

        public Number getY(int itemIndex) {
            return get(itemIndex).actualTime / 60.0;
        }

    }

    private class CPIXYChartSeries extends EVLeavesXYChartSeries {

        public CPIXYChartSeries(EVTaskFilter filter) {
            super("Completed_Task", filter);
        }

        public Number getX(int itemIndex) {
            return get(itemIndex).getDateCompleted().getTime();
        }

        public Number getY(int itemIndex) {
            return get(itemIndex).planTime / get(itemIndex).actualTime;
        }

    }

    public class GenericCategoryChartSeries extends EVLeavesCategoryChartSeries {

        protected int[] columnIndexes;
        protected List<Comparable> columnKeys;

        public GenericCategoryChartSeries(String seriesKey,
                EVTaskFilter filter, int[] columnIndexes,
                Comparable[] columnKeys) {
            super(seriesKey, filter);
            this.columnIndexes = columnIndexes;
            this.columnKeys = Arrays.asList(columnKeys);;
        }

        public Number getValue(int rowNum, int columnNum) {
            Number value = null;

            Object objValue = getValueAt(get(rowNum), columnIndexes[columnNum]);

            if (objValue instanceof Number)
                value = (Number) objValue;

            return value;
        }

        public List<Comparable> getColumnsKeys() {
            return columnKeys;
        }

    }

    public class PlanVsActualCategoryChartSeries extends GenericCategoryChartSeries {
        /** The column positions */
        public static final int ACTUAL_DIRECT_TIME_COLUMN_POS = 0;
        public static final int PLANNED_TIME_COLUMN_POS = 1;

        public PlanVsActualCategoryChartSeries(EVTaskFilter filter) {
            super("Tasks_In_Progress", filter, new int[] { -ACT_DTIME_COLUMN, -PLAN_DTIME_COLUMN },
                    new String[] { toolTips[ACT_DTIME_COLUMN], toolTips[PLAN_DTIME_COLUMN] });
        }

    }

    private abstract class EVLeavesCategoryChartSeries extends EVLeavesChartSeries
            implements CategoryChartSeries {

        private List<EVTaskKey> taskKeys;

        public EVLeavesCategoryChartSeries(String seriesKey, EVTaskFilter filter) {
            super(seriesKey, filter);
        }

        @Override
        public void recalc() {
            super.recalc();
            taskKeys = new ArrayList<EVTaskKey>();

            EVTaskKey taskKey;

            int i = 0;
            for (EVTask task : evLeaves) {
                taskKey = new EVTaskKey(i++, task);
                taskKeys.add(taskKey);
            }
        }

        public List<? extends Comparable> getRowKeys() {
            return taskKeys;
        }

    }


    public XYDataset getPlanVsActualTimeData(EVTaskFilter filter) {
        return new EVLeavesXYChartData(new EVTaskChartEventAdapter(),
                new PlanVsActualXYChartSeries(filter));
    }

    public CategoryDataset getPlanVsActualDirectTimeData(EVTaskFilter filter) {
        EVLeavesCategoryChartSeries series = new PlanVsActualCategoryChartSeries(filter);
        return new CategoryChartData(new EVTaskChartEventAdapter(), series);
    }

    public XYDataset getCPIData(EVTaskFilter filter) {
        return new EVLeavesXYChartData(new EVTaskChartEventAdapter(),
                new CPIXYChartSeries(filter));
    }


    ///////////////////////////////////////////////////////////////////////
    // The methods/classes below assist in the generation of chart's tooltip
    ///////////////////////////////////////////////////////////////////////

    public EVTaskTooltipGenerator getTooltipGenerator(String format, int[] columns) {
        return new EVTaskTooltipGenerator(format, columns);
    }

    /**
     * Class used to create a formatted tooltip for an EVTask
     */
    public class EVTaskTooltipGenerator {

        /** The tooltip format */
        private String format;

        /** The data columns that will be extracted from the task to generate the tooltip */
        private int[] columns;

        /** True if we are generating an HTML formatted tooltip */
        private boolean html;

        public EVTaskTooltipGenerator(String format, int[] columns) {
            this.format = format;
            this.columns = columns;
            this.html = format.regionMatches(true, 0, "<html>", 0, 6);
        }

        /**
         * Generates and returns a formatted tooltip for a specific task
         */
        public String getTooltip(EVTask task) {
            Object[] values = new Object[columns.length];

            for (int i = 0; i < columns.length; ++i) {
                Object v = EVTaskList.this.getValueAt(task, columns[i]);
                if (html && v instanceof String)
                    v = HTMLUtils.escapeEntities((String) v);
                values[i] = v;
            }

            return MessageFormat.format(format, values);
        }
    }
}
