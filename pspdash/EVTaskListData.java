// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.event.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pspdash.data.DataRepository;
import pspdash.data.DataComparator;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;

public class EVTaskListData extends EVTaskList
    implements PSPProperties.Listener
{

    public static final String TASK_ORDINAL_PREFIX = "TST_";
    public static final String EST_HOURS_DATA_NAME = "Planned Hours";

    protected DataRepository data;
    protected PSPProperties hierarchy;

    public EVTaskListData(String taskListName,
                          DataRepository data,
                          PSPProperties hierarchy,
                          boolean willNeedChangeNotification) {
        super(taskListName, taskListName, willNeedChangeNotification);
        this.data = data;
        this.hierarchy = hierarchy;

        addTasksFromData(data, taskListName);
        schedule = getSchedule(data, taskListName);
    }
    public boolean isEditable() { return true; }

    private void addTasksFromData(DataRepository data, String taskListName) {
        // search for tasks that belong to the named task list.
        SortedMap tasks = new TreeMap(DataComparator.instance);
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys();
        String dataName, path;
        SimpleData value;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (!dataName.endsWith(ordinalPrefix)) continue;
            value = data.getSimpleValue(dataName);
            path = dataName.substring
                (0, dataName.length() - ordinalPrefix.length());

            // If this is an imported data item not corresponding to any
            // real hierarchy node, ignore it.
            if (hierarchy.findExistingKey(path) == null) continue;
            tasks.put(value, path);
        }

        // now add each task found to the task list.
        i = tasks.values().iterator();
        boolean willNeedChangeNotification = (recalcListeners != null);
        while (i.hasNext())
            addTask((String) i.next(), data, hierarchy, null,
                    willNeedChangeNotification);

        hierarchy.addHierarchyListener(this);
    }
    private EVSchedule getSchedule(DataRepository data, String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            data.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof StringData) d = ((StringData) d).asList();
        if (d instanceof ListData)
            return new EVSchedule((ListData) d);
        else
            return new EVSchedule();
    }

    public void save(String newName) {
        // First, compile a list of all the elements in the datafile that
        // were previously used to save this task list.  (That way we'll
        // know what we need to delete.)
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys();
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
            EVTask r = (EVTask) root;
            for (int j = r.getNumChildren();  j-- > 0;  ) {
                dataName = data.createDataName(r.getChild(j).getFullName(),
                                               ordinalPrefix);
                data.putValue(dataName, new DoubleData(j, false));
                oldNames.remove(dataName);
            }
            dataName = data.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
            data.putValue(dataName, schedule.getSaveList());
            oldNames.remove(dataName);
            taskListName = newName;
        }

        // Finally, delete any old unused data elements.
        i = oldNames.iterator();
        while (i.hasNext())
            data.removeValue((String) i.next());
    }

    public void hierarchyChanged(PSPProperties.Event e) {
        if (someoneCares()) {
            EVTask r = (EVTask) root;

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
                                   PSPProperties hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        EVTask newTask = new EVTask(path, data, hierarchy,
                                    willNeedChangeNotification ? this : null);
        if (((EVTask) root).add(newTask))
            return newTask;
        else
            return null;
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

    public void recalc() {
        TimeLog log = new TimeLog();
        try { log.readDefault(); } catch (IOException ioe) {}
        ((EVTask) root).recalc(schedule, log);
        super.recalc();
    }

    protected void dispose() {
        hierarchy.removeHierarchyListener(this);
        super.dispose();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.length() > 0 &&
                taskListName.indexOf('/') == -1);
    }

    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                              EST_HOURS_DATA_NAME);
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

}
