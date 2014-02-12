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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.swing.tree.TreePath;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataComparator;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.TopDownBottomUpJanitor;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyNoteEvent;
import net.sourceforge.processdash.hier.HierarchyNoteListener;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.util.StringUtils;


public class EVTaskListData extends EVTaskList
    implements DashHierarchy.Listener
{

    public static final String TASK_LIST_FLAG = "plain";
    public static final String TASK_ORDINAL_PREFIX = "TST_";
    public static final String EST_HOURS_DATA_NAME = "Planned Hours";
    public static final String DATES_LOCKED_DATA_NAME = "Schedule_Dates_Locked";
    protected DataRepository data;
    protected DashHierarchy hierarchy;
    protected TaskNoteListener taskNoteListener;

    public EVTaskListData(String taskListName,
                          DataRepository data,
                          DashHierarchy hierarchy,
                          boolean willNeedChangeNotification) {
        super(taskListName, taskListName, willNeedChangeNotification);
        this.data = data;
        this.hierarchy = hierarchy;

        EST_TIME_JANITOR.cleanup(data, hierarchy);
        addTasksFromData(data, taskListName);
        schedule = getSchedule(data, taskListName);
        loadID(taskListName, data, EST_HOURS_DATA_NAME);
        loadMetadata(taskListName, data);
        setupTimeZone();
        assignToOwner();
        loadScheduleNotes();
        setDefaultOptions();
        calculator = new EVCalculatorData(this);
        setBaselineDataSource(getBaselineSnapshot());
        ((EVTask) root).flag = TASK_LIST_FLAG;
        this.showNotesColumn = true;
        if (willNeedChangeNotification) {
            hierarchy.addHierarchyListener(this);

            taskNoteListener = new TaskNoteListener();
            HierarchyNoteManager.addHierarchyNoteListener(taskNoteListener);
        }
    }
    public boolean isEditable() { return true; }

    private void addTasksFromData(DataRepository data, String taskListName) {
        // search for tasks that belong to the named task list.
        SortedMap tasks = new TreeMap(DataComparator.getInstance());
        String ordinalDataName = TASK_ORDINAL_PREFIX + taskListName;
        findTasksInHierarchy(tasks, ordinalDataName, PropertyKey.ROOT);

        // now add each task found to the task list.
        Iterator i = tasks.values().iterator();
        boolean willNeedChangeNotification = (recalcListeners != null);
        while (i.hasNext())
            addTask((String) i.next(), data, hierarchy, null,
                    willNeedChangeNotification);
    }

    private void findTasksInHierarchy(SortedMap tasks, String ordinalDataName,
            PropertyKey key) {
        String path = key.path();
        String dataName = DataRepository.createDataName(path, ordinalDataName);
        SimpleData value = data.getSimpleValue(dataName);
        if (value != null)
            tasks.put(value, path);

        for (int i = 0; i < hierarchy.getNumChildren(key); i++)
            findTasksInHierarchy(tasks, ordinalDataName, hierarchy.getChildKey(
                    key, i));
    }

    private EVSchedule getSchedule(DataRepository data, String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            DataRepository.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof StringData) d = ((StringData) d).asList();
        if (d instanceof ListData) {
            String lockedName = DataRepository.createDataName(globalPrefix,
                DATES_LOCKED_DATA_NAME);
            SimpleData l = data.getSimpleValue(lockedName);
            boolean locked = (l != null && l.test());
            return new EVSchedule((ListData) d, locked);
        } else
            return new EVSchedule();
    }
    protected void setupTimeZone() {
        // check and see whether this schedule has a time zone set.
        // If not, set it to the default local time zone.
        if (getTimezoneID() == null)
            setTimezoneID(TimeZone.getDefault().getID());
    }
    protected void assignToOwner() {
        String owner = ProcessDashboard.getOwnerName(data);
        if (owner != null) {
            EVTask r = (EVTask) root;
            owner = StringUtils.findAndReplace(owner, ",", " ");
            r.assignedTo = Collections.singletonList(owner);
        }
    }
    protected void loadScheduleNotes() {
        String noteData = metaData.getProperty(EVMetadata.SCHEDULE_NOTES);
        schedule.setPeriodNoteData(noteData);
        schedule.showNotesColumn = true;
    }
    protected void saveScheduleNotes() {
        String noteData = schedule.getPeriodNoteData();
        if (noteData == null)
            metaData.remove(EVMetadata.SCHEDULE_NOTES);
        else
            metaData.put(EVMetadata.SCHEDULE_NOTES, noteData);
    }
    private void setDefaultOptions() {
        if (metaData.containsKey(EVMetadata.REZERO_ON_START_DATE) == false) {
            if (isBrandNewTaskList)
                metaData.put(EVMetadata.REZERO_ON_START_DATE, "false");
            else
                metaData.put(EVMetadata.REZERO_ON_START_DATE, "true");
        }
    }


    public void save(String newName) {
        EVTask r = (EVTask) root;
        boolean nameIsChanging = (!taskListName.equals(newName));
        saveScheduleNotes();

        // First, compile a list of all the elements in the datafile that
        // were previously used to save this task list.  (That way we'll
        // know what we need to delete.)
        String globalPrefix = MAIN_DATA_PREFIX + taskListName + "/";
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        Set oldNames = new HashSet();
        String dataName;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (dataName.startsWith(globalPrefix) ||
                dataName.endsWith(ordinalPrefix))
                oldNames.add(dataName);
        }

        // Now, save the data to the repository.
        if (newName != null) {
            globalPrefix = MAIN_DATA_PREFIX + newName;
            ordinalPrefix = TASK_ORDINAL_PREFIX + newName;
            for (int j = r.getNumChildren();  j-- > 0;  ) {
                dataName = DataRepository.createDataName
                    (r.getChild(j).getFullName(), ordinalPrefix);
                data.putValue(dataName, new DoubleData(j, false));
                oldNames.remove(dataName);
            }
            // save the schedule
            dataName = DataRepository.createDataName
                (globalPrefix, EST_HOURS_DATA_NAME);
            data.putValue(dataName, schedule.getSaveList());
            oldNames.remove(dataName);
            if (schedule.areDatesLocked()) {
                dataName = DataRepository.createDataName(globalPrefix,
                    DATES_LOCKED_DATA_NAME);
                data.putValue(dataName, ImmutableDoubleData.TRUE);
                oldNames.remove(dataName);
            }
            // save the task list unique ID and the metadata
            oldNames.remove(saveID(newName, data));
            oldNames.remove(saveMetadata(newName, data));
            renameSnapshots(oldNames, newName, data);

            taskListName = newName;
        }

        // Finally, delete any old unused data elements.
        i = oldNames.iterator();
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (!nameIsChanging && dataName.startsWith(MAIN_DATA_PREFIX))
                // when the task list name isn't changing, and the data element
                // in question starts with the main data prefix, give it the
                // benefit of the doubt and leave it alone.  It most likely
                // represents some bit of schedule metadata we don't know about
                // (introduced in a later version of the dashboard)
                ;
            else
                data.removeValue(dataName);
        }

        // allow our tasks to do the same thing.
        r.saveStructuralData(newName);
        r.saveDependencyInformation();

        super.save(newName);
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        if (someoneCares()) {
            EVTask r = (EVTask) root;

            fireTreeStructureWillChange(this, r.getPath(), null, null);

            // delete all the previous children.
            int n = r.getNumChildren();
            int[] childIndices = new int[n];
            Object[] children = new Object[n];
            while (n-- > 0)
                children[(childIndices[n] = n)] = r.getChild(n);
            r.destroy();
            fireTreeNodesRemoved
                (this, ((EVTask) r).getPath(), childIndices, children);

            // add the new kids.
            addTasksFromData(data, taskListName);
            fireTreeStructureChanged(this, r.getPath(), null, null);
            recalc();
        }
    }

    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   DashHierarchy hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        EVTask newTask = new EVTask(taskListName, path, data, hierarchy,
                                    willNeedChangeNotification ? this : null);
        if (((EVTask) root).add(newTask))
            return newTask;
        else
            return null;
    }

    protected boolean checkRemovable(TreePath path) {
        return path.getPathCount() > 1;
    }

    protected int doRemoveTask(EVTask parent, EVTask child) {
        if (parent == (EVTask) root)
            return super.doRemoveTask(parent, child);
        else {
            child.setUserPruned(!child.isUserPruned());
            return -1;
        }
    }

    public boolean explodeTask(TreePath path) { // move to the data version
        // for now, only remove tasks which are children of the root.
        int pathLen = path.getPathCount();
        if (pathLen != 2) return false;

        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        if (child.getNumChildren() == 0) return false;
        int pos = parent.remove(child);

        List leafTasks = child.getLeafTasks();
        int[] insertedIndicies = new int[leafTasks.size()];
        Object[] insertedChildren = new Object[leafTasks.size()];
        Iterator i = leafTasks.iterator();
        EVTask leaf;
        int leafNum = 0;
        while (i.hasNext()) {
            leaf = (EVTask) i.next();
            leaf.getParent().remove(leaf);
            leaf.name = leaf.fullName.substring(1);
            parent.add(pos+leafNum, leaf);

            insertedIndicies[leafNum] = leafNum + pos;
            insertedChildren[leafNum] = leaf;
            leafNum++;
        }
        child.destroy();

        // send the appropriate TreeModel events.
        int[] removedIndices = new int[] { pos };
        Object[] removedChildren = new Object[] { child };
        EVTask[] parentPath = ((EVTask) parent).getPath();
        fireTreeNodesRemoved
            (this, parentPath, removedIndices, removedChildren);
        fireTreeNodesInserted
            (this, parentPath, insertedIndicies, insertedChildren);
        return true;
    }

    public void recalcLeavesOnly() {
        this.calculator = new EVCalculatorLeavesOnly((EVTask) root);
        if (this.recalcTimer != null)
            this.recalcTimer.setInitialDelay(10);
    }

    public void setValueAt(Object value, Object node, int column) {
        super.setValueAt(value, node, column);
        if (column == PLAN_TIME_COLUMN)
            EST_TIME_JANITOR.cleanup(data, hierarchy);
    }

    @Override
    public String saveSnapshot(String snapshotId, String snapshotName,
            String snapshotComment) {
        return saveSnapshotToData(data, snapshotId, snapshotName,
            snapshotComment);
    }

    @Override
    public EVSnapshot getSnapshotById(String snapshotId) {
        return getSnapshotFromData(data, snapshotId);
    }

    @Override
    public List<EVSnapshot.Metadata> getSnapshots() {
        return super.getSnapshots(data);
    }

    public void dispose() {
        hierarchy.removeHierarchyListener(this);
        if (taskNoteListener != null)
            HierarchyNoteManager.removeHierarchyNoteListener(taskNoteListener);
        super.dispose();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.length() > 0 &&
                taskListName.indexOf('/') == -1);
    }

    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = DataRepository.createDataName
            (MAIN_DATA_PREFIX + taskListName, EST_HOURS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }

    public static String taskListNameFromDataElement(String dataName) {
        if (dataName == null ||
            !dataName.startsWith(MAIN_DATA_PREFIX) ||
            !dataName.endsWith("/" + EST_HOURS_DATA_NAME))
            return null;

        return dataName.substring
            (MAIN_DATA_PREFIX.length(),
             dataName.length() - EST_HOURS_DATA_NAME.length() - 1);
    }

    static final TopDownBottomUpJanitor EST_TIME_JANITOR =
        new TopDownBottomUpJanitor("Estimated Time");

    protected class TaskNoteListener implements HierarchyNoteListener {

        public void notesChanged(HierarchyNoteEvent e) {
            List<EVTask> tasks = getTaskRoot().findByFullName(e.getPath());
            if (tasks != null && !tasks.isEmpty()) {
                for (EVTask task : tasks) {
                    task.loadTaskNote();
                    evNodeChanged(task, false);
                }
            }
        }
    }

}
