// Copyright (C) 2009 Tuma Solutions, LLC
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.util.StringUtils;

public class WBSTaskOrderComparator {

    private static WBSTaskOrderComparator INSTANCE = null;

    private static DashboardContext DASH_CONTEXT;

    public static void init(DashboardContext ctx) {
        DASH_CONTEXT = ctx;
        INSTANCE = null;
    }

    public static WBSTaskOrderComparator getInstance() {
        if (INSTANCE == null) {
            if (DASH_CONTEXT != null)
                INSTANCE = new WBSTaskOrderComparator(DASH_CONTEXT);
            else
                INSTANCE = new WBSTaskOrderComparator();
        }
        return INSTANCE;
    }



    /**
     * This class keeps track of the relative position of a single task within
     * various projects.
     * 
     * The key of the map is the name of a project which includes a task with
     * this taskID.
     * 
     * The value of the map is the ordinal position of the taskID within the
     * list of IDs for that project.
     * 
     * A single task can have multiple taskIDs. Each of those taskIDs can have
     * a different ordinal within a particular project, but those 'common
     * ordinals' will all be sequential within that project.
     */
    private class TaskIdOrdinalData extends HashMap<String, Integer> {
    }

    private Map<String, TaskIdOrdinalData> taskIdMap;

    private CacheInvalidator cacheInvalidator;

    private WBSTaskOrderComparator() {}

    public WBSTaskOrderComparator(DashboardContext ctx) {
        this(ctx.getHierarchy(), ctx.getData());
    }

    public WBSTaskOrderComparator(PropertyKeyHierarchy hier, DataContext data) {
        taskIdMap = new HashMap<String, TaskIdOrdinalData>();
        cacheInvalidator = new CacheInvalidator(hier, data);
        loadOrdinalData(hier, data, PropertyKey.ROOT);
    }

    private void loadOrdinalData(PropertyKeyHierarchy hier, DataContext data,
            PropertyKey node) {
        if (node != null) {
            loadOrdinalData(data, node.path());
            for (int i = hier.getNumChildren(node); i-- > 0;)
                loadOrdinalData(hier, data, hier.getChildKey(node, i));
        }
    }

    private void loadOrdinalData(DataContext data, String path) {
        String dataName = DataRepository.createDataName(path,
            NODE_ORDER_DATA_NAME);
        ListData list = ListData.asListData(data.getSimpleValue(dataName));
        if (list != null) {
            cacheInvalidator.listenToData(dataName);
            for (int i = 0; i < list.size(); i++) {
                String taskId = StringUtils.asString(list.get(i));
                getOrdinalData(taskId).put(path, i);
            }
        }
    }

    private TaskIdOrdinalData getOrdinalData(String taskId) {
        TaskIdOrdinalData result = taskIdMap.get(taskId);
        if (result == null) {
            result = new TaskIdOrdinalData();
            taskIdMap.put(taskId, result);
        }
        return result;
    }

    /**
     * Consult task order data from any WBSes, and use it to compare the
     * relative order of two tasks.
     * 
     * If the two tasks both appear in a common work breakdown structure,
     * this will return an Integer that is less than 0, equal to 0, or greater
     * than 0 (to indicate that taskA comes first, is identical to, or comes
     * after taskB, respectively).
     * 
     * If the two tasks do not appear together in any WBS, this method will
     * return null to indicate that no WBS ordering can be inferred.
     * 
     * @param aTaskIDs a list of task IDs associated with "task A"
     * @param bTaskIDs a list of task IDs associated with "task B"
     * @return a number less than zero if "task A" appears before "task B"
     *     in some WBS; a number greater than zero if "task A" appears after
     *     "task B" in some WBS; zero if they are the same task; or null if
     *     no task order can be inferred.
     */
    public Integer compare(Collection<String> aTaskIDs,
            Collection<String> bTaskIDs) {
        if (isEmpty(taskIdMap) || isEmpty(aTaskIDs) || isEmpty(bTaskIDs))
            return null;

        TaskIdOrdinalData aOrdinals = new TaskIdOrdinalData();
        for (String taskId : aTaskIDs) {
            // if the two collections include an overlapping element, they
            // must be the same task.  Return 0.
            if (bTaskIDs.contains(taskId))
                return 0;

            // collect all of the task ordinals associated with the task IDs
            // in the first map.  Note that the "putAll" call below could
            // overwrite some ordinals that we've collected so far, but that
            // is OK.  We know that all of the IDs represent the same task, so
            // we can assume that the various IDs would map to sequential
            // ordinal numbers within any particular project.  When compared
            // with *other* tasks, these sequential IDs will sort the same.
            TaskIdOrdinalData oneAOrdinalList = taskIdMap.get(taskId);
            if (oneAOrdinalList != null)
                aOrdinals.putAll(oneAOrdinalList);
        }

        for (String taskId : bTaskIDs) {
            TaskIdOrdinalData oneBOrdinalList = taskIdMap.get(taskId);
            if (oneBOrdinalList != null) {
                for (Entry<String, Integer> e : oneBOrdinalList.entrySet()) {
                    String path = e.getKey();
                    Integer aOrd = aOrdinals.get(path);
                    if (aOrd != null) {
                        Integer bOrd = e.getValue();
                        return aOrd.compareTo(bOrd);
                    }
                }
            }
        }

        // the tasks did not share any common project paths, so we cannot
        // infer their relative order.
        return null;
    }
    private boolean isEmpty(Map m) {
        return m == null || m.isEmpty();
    }
    private boolean isEmpty(Collection c) {
        return c == null || c.isEmpty();
    }


    /**
     * This class watches for changes to the hierarchy or to data, and
     * invalidates the cached set of WBS task order information.
     */
    private class CacheInvalidator implements DashHierarchy.Listener,
            DataListener {

        DashHierarchy hier;

        DataRepository data;

        Set<String> dataNames;

        public CacheInvalidator(PropertyKeyHierarchy hier, DataContext data) {
            if (hier instanceof DashHierarchy) {
                this.hier = (DashHierarchy) hier;
                this.hier.addHierarchyListener(this);
            }
            if (data instanceof DataRepository) {
                this.data = (DataRepository) data;
                this.dataNames = new HashSet();
            }
        }

        public void listenToData(String dataName) {
            if (data != null) {
                dataNames.add(dataName);
                data.addDataListener(dataName, this, false);
            }
        }

        public void hierarchyChanged(Event e) {
            dispose();
        }

        public void dataValueChanged(DataEvent e) {
            dispose();
        }

        public void dataValuesChanged(Vector v) {
            dispose();
        }

        private synchronized void dispose() {
            INSTANCE = null;

            if (hier != null) {
                hier.removeHierarchyListener(this);
                hier = null;
            }

            if (data != null) {
                for (String dataName : dataNames)
                    data.removeDataListener(dataName, this);
                data = null;
            }
        }

    }

    private static final String NODE_ORDER_DATA_NAME = "Task_ID_WBS_Order";

}
