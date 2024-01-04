// Copyright (C) 2006-2020 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.LightweightSet;
import net.sourceforge.processdash.util.StringUtils;

/** This class produces a merged view of a rolled-up earned value schedule
 * for a project.
 * 
 * If an EVTaskListRollup rolls up several schedules that either:<ul>
 * <li>All describe a common team project, or common master project</li>
 * <li>All describe work with the same task names</li>
 * </ul>
 * This class can merge the hierarchical task lists to show a single,
 * hierarchical view of the project.  This allows quick access to metrics
 * like percent complete, etc for each hierarchical node of the project.
 */
public class EVTaskListMerger {

    public static final String TASK_LIST_FLAG = "merged";

    protected static final Logger logger = Logger
            .getLogger(EVTaskListMerger.class.getName());

    private EVTaskList taskList;

    private boolean simplify;

    private boolean preserveLeaves;

    private EVTaskFilter filter;

    private Set filteredNodes;

    private EVTask mergedRoot;

    private Map nodesMerged;

    private WBSTaskOrderComparator wbsTaskOrderComparator;

    /** Create and calculate a merged task model for the given task list.
     */
    public EVTaskListMerger(EVTaskList taskList, boolean simplify,
            boolean preserveLeaves, EVTaskFilter filter) {
        this.taskList = taskList;
        this.simplify = simplify;
        this.preserveLeaves = preserveLeaves;
        this.filter = filter;
        this.mergedRoot = new EVTask(taskList.getRootName());
        this.mergedRoot.flag = TASK_LIST_FLAG;
        this.wbsTaskOrderComparator = WBSTaskOrderComparator.getInstance();
        recalculate();
    }

    /** Get the root task representing the merged contents of the target
     * task list.
     */
    public EVTask getMergedTaskRoot() {
        return mergedRoot;
    }

    /** Recalculate the merged contents based on the latest data in the
     * target task list.
     * 
     * The target task list should have already been recalculated before this
     * method is called.  A call to this method will not cause the merged task
     * root to change; its substructure will be replaced, but the top-level
     * {@link EVTask} representing the merged task root will remain constant.
     */
    public void recalculate() {
        clearChildren(mergedRoot);

        // retrieve the list of nodes that pass the filter
        filteredNodes = enumerateFilteredNodes();

        // find all the tasks that we'll want to represent in our merged tree.
        Map allTaskKeys = new HashMap();
        getAllTaskKeys(allTaskKeys, (EVTask) taskList.getRoot());
        if (allTaskKeys.isEmpty())
            // either there are no tasks in this schedule at all, or
            // everything is pruned.  Nothing to merge.
            return;

        maybeSetupDefaultMerge(allTaskKeys, (EVTask) taskList.getRoot());

        // determine the ancestry of our new merged tree, and merge siblings
        // that have similar names.
        defineParentage(allTaskKeys, null, (EVTask) taskList.getRoot());
        mergeSimilarlyNamedChildren(allTaskKeys, null);

        // we're ready - do the work
        createMergedNodes(allTaskKeys);

        if (simplify)
            simplifyNodes(mergedRoot);
    }

    /** Get the set of all EVTask objects from the original EVTaskList that
     * were merged to create the given task and its descendants.
     */
    public Set getTasksMergedBeneath(EVTask t) {
        HashSet result = new HashSet();
        getTasksMergedBeneath(result, t);
        return result;
    }
    private void getTasksMergedBeneath(Set result, EVTask t) {
        Set s = (Set) nodesMerged.get(t);
        if (s == null)
            return;
        result.addAll(s);
        for (int i = t.getNumChildren();  i-- > 0; )
            getTasksMergedBeneath(result, t.getChild(i));
    }

    /**
     * If we are given a task filter, its {@link EVTaskFilter#include(EVTask)}
     * method will only return true for tasks that should be included in
     * calculations. However, those tasks could be scattered throughout the EV
     * task tree. Since the calculations in this class are performed
     * hierarchically, we also need to keep track of the nodes that form the
     * ancestry tree above the filtered nodes. This method calculates the set of
     * nodes that match our task filter, plus their ancestors.
     * 
     * If no task filter is in effect, returns null.
     */
    private Set enumerateFilteredNodes() {
        if (filter == null)
            return null;

        Set result = new HashSet();
        enumerateFilteredNodes(result, taskList.getTaskRoot());
        return result;
    }

    private boolean enumerateFilteredNodes(Set result, EVTask task) {
        boolean childrenWereIncluded = false;
        for (int i = task.getNumChildren();  i-- > 0; )
            if (enumerateFilteredNodes(result, task.getChild(i)))
                childrenWereIncluded = true;

        boolean includeTask = childrenWereIncluded || filter.include(task);
        if (includeTask)
            result.add(task);

        return includeTask;
    }

    /** Collect all the nodes that we want to include in our merged model,
     * and place them into a map.
     */
    private void getAllTaskKeys(Map result, EVTask task) {
        if (task.isLevelOfEffortTask() || task.isTotallyPruned())
            // don't include level of effort tasks or pruned tasks in the
            // merged result.
            return;

        if (filteredNodes != null && !filteredNodes.contains(task))
            // if we have a filter, see if it includes this task.
            return;

        // the "imaginary" nodes that are created as the root of an EVTaskList
        // are assigned a flag, indicating whether they are a plain task list
        // or a rollup task list.   These nodes do not include real task data,
        // they just represent a container for real tasks.  Skip down until
        // we find tasks that are not flagged, as these represent real tasks.
        if (task.getFlag() == null)
            addTaskKey(result, task);

        // recurse into children of the given EVTask.
        for (int i = task.getNumChildren(); i-- > 0;)
            getAllTaskKeys(result, task.getChild(i));
    }

    /** Find or create a TaskKey to represent the given task, and save it into
     * the cache.
     */
    private void addTaskKey(Map cache, EVTask task) {
        TaskKey key = null;

        List taskIDs = getTaskIDsIncludingRelaunchSource(task);
        if (taskIDs != null) {
            //  TaskKey objects represent distinct nodes in our merged model.
            // Thus, if we find any distinct EVTask objects which share one
            // or more taskIDs, we need to merge them into a single TaskKey.
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                TaskKey k = (TaskKey) cache.get(i.next());
                if (k != null) {
                    if (key == null)
                        // we found a key that matches one of our task IDs.
                        // Adopt it as our key.
                        key = k;
                    else
                        // We have found more than one previously existing key
                        // which match our task IDs.  This can occur if we
                        // process tasks like this:
                        //  * Task A  { id-1, id-2 }
                        //  * Task B  { id-3, id-4 }
                        //  * Task C  { id-1, id-3 }
                        // Task C will provide new evidence that Tasks A and B
                        // we really the same task.  Thus, we need to merge
                        // the previously distinct TaskKeys together.
                        key.merge(k);
                }
            }
        }

        if (key == null)
            // if we found no existing key that works for us, create one.
            key = new TaskKey();

        // add our own information to the given TaskKey.
        key.addTask(task);
        // reregister the key in the cache to reflect merging operations above
        registerTaskKey(cache, key);
    }

    private List getTaskIDsIncludingRelaunchSource(EVTask task) {
        List taskIDs = task.getTaskIDs();
        String relaunchSourceID = task.getRelaunchSourceID();
        if (!StringUtils.hasValue(relaunchSourceID)) {
            return taskIDs;
        } else if (!hasValue(taskIDs)) {
            return Collections.singletonList(relaunchSourceID);
        } else {
            List<String> result = new ArrayList<String>(taskIDs);
            result.add(relaunchSourceID);
            return result;
        }
    }


    /** Some task lists have no task ID information.  This includes EV schedules
     * for historical projects, as well as EV schedules for informal projects.
     * For schedules that fall into this category, try to find a way to merge
     * them in a useful way.
     */
    private void maybeSetupDefaultMerge(Map allTaskKeys, EVTask task) {
        if (isDefaultMergeAppropriate(task) == false)
            return;

        // create a key to represent all the top-level tasks in the task list.
        TaskKey key = new TaskKey();
        addTopLevelTasks(key, task);
        registerTaskKey(allTaskKeys, key);
    }

    /** Check to see whether a default merge is appropriate for a given task
     * list.
     * 
     * @param task the root of the task list.  The task list is presumed not
     *    to contain any task identifiers.
     * @return true if a default merge is appropriate.
     */
    private boolean isDefaultMergeAppropriate(EVTask task) {
        // if this is not a rollup schedule, a default merge isn't appropriate.
        if (!EVTaskListRollup.TASK_LIST_FLAG.equals(task.getFlag()))
            return false;

        // add other potential checks here.  Think about this further before
        // enabling this feature.

        return false;
        //return true;
    }

    /** Find all of the top-level tasks in a task list, and add them to a single
     * task key.
     * 
     * For an individual schedule this return, the tasks that they added to
     * their task list directly (i.e., as children of the task list root).  For
     * rollup schedules, this recurses and finds the the top-level tasks for
     * all subschedules.
     * 
     * @param key the destination key to add tasks to
     * @param task a node in an earned value task list.  (Clients should call
     *    this with the root node of the task list for correct results.)
     */
    private void addTopLevelTasks(TaskKey key, EVTask task) {
        if (task.isLevelOfEffortTask() || task.isTotallyPruned())
            // don't include level of effort tasks or pruned tasks in the
            // merged result.
            return;

        if (filteredNodes != null && !filteredNodes.contains(task))
            // if we have a filter, see if it includes this task.
            return;

        // if we find a top-level task (indicated by the fact that it isn't
        // flagged as an "imaginary" nodes at the root of an EVTaskList),
        // add it to the key.
        if (task.getFlag() == null)
            key.addTask(task);

        else {
            // recurse into children of the given EVTask.
            for (int i = task.getNumChildren(); i-- > 0;)
                addTopLevelTasks(key, task.getChild(i));
        }
    }

    /** Register this key in the cache, so it can be looked up by any of its
     * task IDs or ev nodes. */
    private void registerTaskKey(Map cache, TaskKey key) {
        for (Iterator i = key.getTaskIDs().iterator(); i.hasNext();)
            cache.put(i.next(), key);
        for (EVTask task : key.getEvNodes()) {
            cache.put(task, key);
            if (task.getRelaunchSourceID() != null)
                cache.put(task.getRelaunchSourceID(), key);
        }
    }

    /** Set the "parent" attributes of all the TaskKeys in the map
     * 
     * We perform this step on a second pass through the target task list,
     * because the first pass will have experienced a lot of churn of creating
     * distinct TaskKey objects, then immediately merging/discarding some.
     * Once we have a fairly stable list of TaskKeys, we can set the parentage
     * more simply and efficiently.
     * 
     * @param allTaskKeys the map of TaskKeys to process
     * @param task an EVTask in the target task list
     * @param parent the TaskKey representing that EVTask.
     */
    private void defineParentage(Map allTaskKeys, TaskKey parent, EVTask task) {
        if (task.isLevelOfEffortTask() || task.isTotallyPruned())
            // we don't do level of effort tasks or pruned tasks, remember?
            return;

        if (filteredNodes != null && !filteredNodes.contains(task))
            // skip filtered tasks, remember?
            return;

        // find the TaskKey for this EVTask, and set its parent.
        TaskKey myKey = (TaskKey) allTaskKeys.get(task);
        if (myKey != null && parent != null)
            // note that we do not perform this step if parent is null.  This
            // is because you could have one task list with root child A, whose
            // children are B and C.  Then you could have a second task list
            // whose root child is C.  When you get around to processing the
            // second task list, it will appear that C has no parent.  But
            // we know better.  Don't clobber real parents (discovered earlier)
            // with null values.
            myKey.setParent(parent);

        // recurse into children of the given EVTask.
        for (int i = task.getNumChildren(); i-- > 0;)
            defineParentage(allTaskKeys, myKey, task.getChild(i));
    }

    /** Merge TaskKeys that share a common parent and at least one overlapping
     * name.
     */
    private void mergeSimilarlyNamedChildren(Map allTaskKeys, TaskKey parent) {
        List mergedChildren = new LinkedList();

        List childKeys = new LinkedList(findChildrenOfKey(allTaskKeys, parent));
        while (!childKeys.isEmpty()) {
            // select a child from the list.
            TaskKey oneKey = (TaskKey) childKeys.remove(0);
            for (Iterator i = childKeys.iterator(); i.hasNext();) {
                // find each remaining sibling.  If it shares a common name
                // with the child we just selected, merge the two nodes.
                TaskKey sibling = (TaskKey) i.next();
                if (intersects(oneKey.getNamesUsed(), sibling.getNamesUsed())) {
                    mergeSiblings(allTaskKeys, oneKey, sibling);
                    i.remove();
                }
            }
            mergedChildren.add(oneKey);
        }

        // recurse down into the merged children.
        for (Iterator i = mergedChildren.iterator(); i.hasNext();) {
            TaskKey k = (TaskKey) i.next();
            mergeSimilarlyNamedChildren(allTaskKeys, k);
        }
    }

    /** Merge two TaskKeys that are siblings of each other
     */
    private void mergeSiblings(Map allTaskKeys, TaskKey key, TaskKey sibling) {
        key.merge(sibling);
        // reregister the merged TaskKey.
        registerTaskKey(allTaskKeys, key);
    }

    /** Find all of the TaskKeys that have a given parent */
    private List findChildrenOfKey(Map allTaskKeys, TaskKey parent) {
        if (parent != null)
            return parent.getChildren();
        else {
            Set result = new HashSet();
            for (Iterator i = allTaskKeys.values().iterator(); i.hasNext();) {
                TaskKey key = (TaskKey) i.next();
                if (key.getParent() == null)
                    result.add(key);
            }
            return new ArrayList(result);
        }
    }

    /** Filter a collection of EVTask objects to find only the leaf nodes. */
    private List getLeafNodes(Set evNodes) {
        List result = new ArrayList(evNodes.size());
        for (Iterator iter = evNodes.iterator(); iter.hasNext();) {
            EVTask t = (EVTask) iter.next();
            if (t.isLeaf())
                result.add(t);
        }
        return result;
    }

    /** Filter a list of EVTask objects to find those that match our filter */
    private List getFilteredNodes(List evNodes) {
        if (filter == null)
            return evNodes;

        List result = new ArrayList(evNodes.size());
        for (Iterator iter = evNodes.iterator(); iter.hasNext();) {
            EVTask t = (EVTask) iter.next();
            if (filter.include(t))
                result.add(t);
        }
        return result;
    }


    /** Create the root children of the merged node, and all descendants.
     * Calculate all rollup data along the way.
     */
    private void createMergedNodes(Map allTaskKeys) {
        nodesMerged = new HashMap();
        nodesMerged.put(mergedRoot, new HashSet());

        // find the TaskKeys which should be direct children of our merged
        // root.  These are the TaskKeys whose parent is null.
        List rootChildren = findChildrenOfKey(allTaskKeys, null);

        // compute the names of each root child, and use a TreeMap to put
        // them in the appropriate order.
        Map<String, TaskKey> rootChildNameMap = new HashMap();
        SortedMap<SortableTaskName, TaskKey> rootChildMap = new TreeMap();
        for (Iterator i = rootChildren.iterator(); i.hasNext();) {
            TaskKey rootChild = (TaskKey) i.next();
            SortableTaskName rootChildSorter = getNameForRootTask(rootChild);
            TaskKey existingChild = rootChildNameMap.get(rootChildSorter.name);
            if (existingChild == null) {
                rootChildNameMap.put(rootChildSorter.name, rootChild);
                rootChildMap.put(rootChildSorter, rootChild);
            } else
                // we seem to have found two root children that want to share
                // the same root name!  This should be an extremely uncommon
                // scenario, but for consistency with the other merging
                // philosophy, we'll just merge the two root children. (We
                // couldn't catch this earlier, because we use a different
                // algorithm for computing the names of root children than we
                // do for the rest of the tree.)
                //     For now, we will not merge the new task IDs into the
                // existing SortableTaskName, because that could produce
                // issues with circular sorting
                mergeSiblings(allTaskKeys, existingChild, rootChild);
        }

        // now, actually create the root children.  Grandchildren and all
        // other descendents will be created recursively.
        for (Entry<SortableTaskName, TaskKey> e : rootChildMap.entrySet()) {
            SortableTaskName rootChildSorter = e.getKey();
            TaskKey rootChild = (TaskKey) e.getValue();
            addChild(mergedRoot, rootChild, rootChildSorter.name, allTaskKeys);
        }

        // recalculate the metrics on the merged root node.
        EVCalculatorRollup.recalcRollupNode(mergedRoot);

        // lookup baseline data for all tasks in the merged task list
        recalcBaselineData(mergedRoot);
    }


    /** Add a node (and all its descendants) to the merged task tree.
     * 
     * @param parent the parent EVTask where the new node should be created.
     * @param key the TaskKey representing data for this new node.
     * @param name the name that should be given to the new node.
     * @param allTaskKeys the collection of all known TaskKeys.
     */
    private void addChild(EVTask parent, TaskKey key, String name,
            Map allTaskKeys) {
        // create an empty EVTask to represent the new node.
        EVTask newNode = new EVTask(name);
        newNode.fullName = pathConcat(parent.fullName, name);
        parent.add(newNode);
        nodesMerged.put(newNode, key.getEvNodes());

        // collect information about the descendants of this node.
        List childKeys = findChildrenOfKey(allTaskKeys, key);
        List leafNodes = getFilteredNodes(getLeafNodes(key.getEvNodes()));

        populateChild(newNode, key, childKeys, allTaskKeys, leafNodes);
    }

    /** Create the descendants for a newly created, merged node.
     */
    private void populateChild(EVTask newNode, TaskKey key,
            List childKeys, Map allTaskKeys, List leavesToMerge) {
        // compute the names of each child.  Use the SortableTaskName class
        // and a TreeMap to put them in the best order relative to each other.
        SortedMap childMap = new TreeMap();
        for (Iterator i = childKeys.iterator(); i.hasNext();) {
            TaskKey childKey = (TaskKey) i.next();
            SortableTaskName childName = getNameForSubtask(childKey);
            childMap.put(childName, childKey);
        }

        // now, actually create the children.  Grandchildren and all
        // other descendents will be created recursively.
        for (Iterator i = childMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            SortableTaskName childName = (SortableTaskName) e.getKey();
            TaskKey childKey = (TaskKey) e.getValue();
            addChild(newNode, childKey, childName.name, allTaskKeys);
        }
        int numberOfTrueChildren = newNode.getNumChildren();

        // temporarily add all of the leaf nodes whose data should be merged
        // with the target node.
        if (preserveLeaves)
            Collections.sort(leavesToMerge, ASSIGNED_TO_COMPARATOR);
        for (Iterator i = leavesToMerge.iterator(); i.hasNext();) {
            EVTask node = (EVTask) i.next();
            EVTask tempChild = (EVTask) node.clone();
            tempChild.assignedTo = node.getAssignedTo();
            tempChild.nodeTypeSpec = node.getAcceptableNodeTypes();
            tempChild.maybeFallbackToOverspentDates();
            // EVTasks will ignore requests to add nodes that are already
            // in their list of children.  Short-circuit that logic by
            // choosing a new, random name for this temporary node.
            tempChild.name = tempChild.fullName = uniqueNodeName();
            newNode.add(tempChild);
        }

        // recalculate the merged metrics on the newly created node.
        List allMergedNodes = getFilteredNodes(new ArrayList(key.getEvNodes()));
        allMergedNodes.addAll(leavesToMerge);
        rollupNode(newNode, allMergedNodes, leavesToMerge);

        // accumulate "node time" from the nodes we are merging.  (This is
        // time that was logged against a non-leaf node in a subschedule.)
        for (Iterator i = key.evNodes.iterator(); i.hasNext();) {
            EVTask subTask = (EVTask) i.next();
            if (!containsIdentity(leavesToMerge, subTask)
                    && (filter == null || filter.include(subTask))
                    && !subTask.isUserPruned()) {
                double subNodeTime = getActualNodeTime(subTask);
                if (subNodeTime > 0) {
                    newNode.actualNodeTime += subNodeTime;
                    newNode.actualTime += subNodeTime;
                    newNode.actualCurrentTime += subNodeTime;
                    newNode.actualDirectTime += subNodeTime;
                }
            }
        }

        // finally, revisit the leaf nodes.  If we're preserving them, we
        // need to make them presentable to the user.  If not, we need to
        // delete them.
        for (int i = newNode.getNumChildren(); i-- > numberOfTrueChildren;) {
            EVTask tempChild = newNode.getChild(i);
            if (preserveLeaves && isLeafTaskWithData(tempChild)) {
                // replace the temporary/unique child name with something that
                // we can display to the user
                tempChild.name = newNode.name;
                tempChild.fullName = newNode.fullName;
            } else {
                newNode.remove(tempChild);
            }
        }
        // If we're preserving leaves but we only have one leaf that contains
        // data, we should delete that singleton leaf to simplify the tree.
        if (numberOfTrueChildren == 0 && newNode.getNumChildren() == 1) {
            EVTask tempChild = newNode.getChild(0);
            newNode.nodeTypeSpec = tempChild.nodeTypeSpec;
            newNode.remove(tempChild);
        }
    }

    private double getActualNodeTime(EVTask task) {
        double result = task.actualCurrentTime;
        for (int i = task.getNumChildren(); i-- > 0;)
            result -= task.getChild(i).actualCurrentTime;
        return result;
    }

    private boolean isLeafTaskWithData(EVTask task) {
        // if the task isn't a leaf, return false.
        if (task.isLeaf() == false) return false;
        // check for various types of actual data; if present, return true
        if (task.getPlanTime() > 0) return true;
        if (task.getActualTime() > 0) return true;
        if (StringUtils.hasValue(task.getNodeType())) return true;
        // if we got here, return false.
        return false;
    }

    /** Merge metrics and data structures for a node in the result tree.
     * 
     * @param task the node in result tree that will hold the merged data.
     * @param mergedNodes the nodes from the original task list that we are
     *    merging to create this node.
     * @param doResources true if resource assignments should be rolled up.
     */
    private void rollupNode(EVTask task, Collection mergedNodes,
            Collection doResourcesFor) {
        // begin by computing the simple rollup of metrics data from the
        // children of the merged node.
        EVCalculatorRollup.recalcRollupNode(task);

        // now, compute the set union of resource assignments, dependencies,
        // task IDs, and node types from the nodes we are merging.
        Set assignedTo = new TreeSet();
        Set dependencies = new HashSet();
        List taskIDLists = new LinkedList();
        Set nodeTypes = new TreeSet();
        for (Iterator i = mergedNodes.iterator(); i.hasNext();) {
            EVTask node = (EVTask) i.next();
            if (containsIdentity(doResourcesFor, node))
                addAll(assignedTo, node.getAssignedTo());
            addAll(dependencies, node.getDependencies());
            taskIDLists.add(node.getTaskIDs());
            if (StringUtils.hasValue(node.getNodeType()))
                nodeTypes.add(node.getNodeType());
        }

        // assign these merged lists of data to the merged EVTask.
        task.assignedTo = new LinkedList(assignedTo);
        task.dependencies = new LinkedList(dependencies);
        task.taskIDs = new LinkedList(mergeTaskIDLists(taskIDLists));
        task.nodeType = StringUtils.join(nodeTypes, ", ");
    }

    /** Merge several lists of taskIDs, preserving order as best as possible */
    private List mergeTaskIDLists(List taskIDLists) {
        Map taskIDPositions = new LinkedHashMap();
        Map taskIDCounts = new LinkedHashMap();
        for (Iterator i = taskIDLists.iterator(); i.hasNext();) {
            List idList = (List) i.next();
            if (idList != null) {
                int pos = 0;
                for (Iterator j = idList.iterator(); j.hasNext();) {
                    String id = (String) j.next();
                    increment(taskIDPositions, id, pos++);
                    increment(taskIDCounts, id, 1);
                }
            }
        }
        Set averages = new TreeSet();
        for (Iterator i = taskIDPositions.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            Object id = e.getKey();
            Integer val = (Integer) e.getValue();
            Integer count = (Integer) taskIDCounts.get(id);
            Double avg = new Double(val.doubleValue() / count.doubleValue());
            e.setValue(avg);
            averages.add(avg);
        }
        List result = new LinkedList();
        for (Iterator i = averages.iterator(); i.hasNext();) {
            Object avg = i.next();
            result.addAll(findKeysWithValue(taskIDPositions, avg));
        }
        return result;
    }

    /** Compute a SortableTaskName to represent a non-root child in the
     * merged tree.
     */
    private SortableTaskName getNameForSubtask(TaskKey key) {
        // choose a name for this subtask
        String name = getNodeName(key);
        Set taskIDs = key.getTaskIDs();
        SortableTaskName result = new SortableTaskName(name, taskIDs);

        // calculate an "ordinal" for this subtask.  Generally, we want nodes
        // that always appear early in EV schedules to appear early in our
        // result as well.  Our simplified algorithm is to:
        //   * look at all the EVTasks that this TaskKey represents.
        //   * find their numerical position within their parent task
        //   * add up those numbers.
        //   * finally, normalize by the number of EVTasks we just merged.
        //     (Otherwise, tasks would be placed later simply because more
        //     people were assigned to them.)
        // This is NOT a perfect algorithm.  But hopefully it will produce
        // good-enough results. At any rate, it will be deterministic.
        for (Iterator i = key.evNodes.iterator(); i.hasNext();) {
            EVTask t = (EVTask) i.next();
            EVTask p = t.getParent();
            if (p != null)
                result.ordinal += p.getChildIndex(t);
        }
        result.ordinal = result.ordinal * 1000 / key.evNodes.size();

        return result;
    }

    /** Select a name to represent a given node in the merged tree */
    private String getNodeName(TaskKey key) {
        // look through all the evNodes that were merged to create this
        // TaskKey.  Find the distinct names that were used, and count how many
        // times each one appears.
        Map nameCounts = new HashMap();
        for (Iterator i = key.getEvNodes().iterator(); i.hasNext();) {
            EVTask t = (EVTask) i.next();
            String name = t.getName();
            increment(nameCounts, name, 1);
        }
        // find the list of names that appeared most often.
        Integer bestCount = (Integer) new TreeSet(nameCounts.values()).last();
        List bestNames = findKeysWithValue(nameCounts, bestCount);
        // if several names are tied for most common, sort them to ensure that
        // our results are deterministic.
        if (bestNames.size() > 1)
            Collections.sort(bestNames);
        // return a name to our caller
        return (String) bestNames.get(0);
    }

    private SortableTaskName getNameForRootTask(TaskKey key) {
        String name = getRootChildName(key);
        Set taskIDs = key.getTaskIDs();
        SortableTaskName result = new SortableTaskName(name, taskIDs);

        // Calculate an "ordinal" for this root task.  The ordinal will
        // be based upon its relative position in the original task list
        // (the one that we are merging). If the task appears early in that
        // task list, it will receive a low number; if it appears later, it
        // will receive a higher number.
        //    This ordinal will only be used when two root tasks do not
        // appear in the same WBS together.  (For example, if someone created
        // an EV rollup of unrelated projects that did not belong to a common
        // master project.)  In that scenario, we try to put the nodes in the
        // same order that the user did when they created their rollup.
        result.ordinal = getDepthFirstOrdinal(taskIDs, taskList.getTaskRoot());

        return result;
    }

    /** Determine the name that should be used to represent a root child.
     */
    private String getRootChildName(TaskKey rootChild) {
        String rootChildName;
        if (taskList instanceof EVTaskListData) {
            // if we're just creating a merged view of a plain task list,
            // there is no need to calculate a canonical name for the root.
            rootChildName = null;

        } else {
            // first, use heuristics to determine the best task IDs, and
            // use those to lookup a canonical name.
            List bestTaskIDs = getBestTaskIDs(rootChild.getEvNodes());
            rootChildName = EVTaskDependencyResolver.getInstance()
                    .getCanonicalTaskName(bestTaskIDs);

            // if that fails, just try to look up any canonical name.
            if (rootChildName == null)
                rootChildName = EVTaskDependencyResolver.getInstance()
                        .getCanonicalTaskName(rootChild.getTaskIDs());
        }

        // If we don't have a canonical name, just return any name at all.
        if (rootChildName == null) {
            EVTask firstEVNode = (EVTask) rootChild.getEvNodes().iterator()
                    .next();
            rootChildName = firstEVNode.getFullName();
            if (rootChildName == null || rootChildName.length() == 0)
                rootChildName = firstEVNode.getName();
        }

        return rootChildName;
    }

    /** Which task IDs represent a collection of nodes best?
     * 
     * If a given task ID appears more often than any other, it will be
     * returned in a List of length 1.  This would occur, for example, when
     * several team projects are rolling up into a master project. Each
     * subschedule would name one team project task ID, plus the master project
     * task ID.  The master project task ID would thus be the most common, and
     * would be returned.
     * 
     * If more than one node is in the given collection, but no task ID appears
     * more than once, then none of them can be best.  This would occur if the
     * nodes in question have no task IDs, or if all of their task IDs are
     * different.  In that scenario, null is returned.
     * 
     * If several task IDs share the "most common" designation, then the ID
     * which appears first most often will be returned.  This would occur, for
     * example, when we are merging the schedules belonging to a single team
     * project.  Every schedule will name the team project task ID followed by
     * the master project task ID.  Both IDs appear the same number of times.
     * Since the team project task ID always appears first, it will be returned.
     * Note that if there is a tie for this honor, all the tied IDs will be
     * returned, in no particular order.
     * 
     * @param nodes a collection of {@link EVTask} objects
     * @return a list of taskIDs that describe those nodes best.  If the nodes
     *    in question do not have any task IDs, returns null.
     */
    protected static List getBestTaskIDs(Collection nodes) {
        // find all the task IDs named by the given nodes, and count the
        // number of times each task ID appears
        Map taskIDCounts = new HashMap();
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            List taskIDs = ((EVTask) i.next()).getTaskIDs();
            if (hasValue(taskIDs)) {
                for (Iterator j = taskIDs.iterator(); j.hasNext();) {
                    String taskID = (String) j.next();
                    increment(taskIDCounts, taskID, 1);
                }
            }
        }
        if (taskIDCounts.isEmpty())
            // no task IDs were found.
            return null;

        // find the IDs which appeared most often
        Integer bestCount = (Integer) new TreeSet(taskIDCounts.values()).last();
        if (bestCount.intValue() < 2 && nodes.size() > 1)
            // no ID appeared more than once.
            return null;
        List bestIDs = findKeysWithValue(taskIDCounts, bestCount);
        if (bestIDs.size() < 2)
            // only one task ID in the "best" slot?  return it.
            return bestIDs;

        // look only at the taskIDs under consideration.  Look at their
        // ordinal positions and sum them.
        Map taskIDPositions = new HashMap();
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            List taskIDs = ((EVTask) i.next()).getTaskIDs();
            if (intersects(taskIDs, bestIDs)) {
                List l = new LinkedList(taskIDs);
                l.retainAll(bestIDs);
                int pos = 0;
                for (Iterator j = l.iterator(); j.hasNext();) {
                    String taskID = (String) j.next();
                    increment(taskIDPositions, taskID, pos++);
                }
            }
        }
        // find and return the taskID(s) with the lowest summed ordinal value.
        Integer bestPos = (Integer) new TreeSet(taskIDPositions.values())
                .first();
        bestIDs = findKeysWithValue(taskIDPositions, bestPos);
        return bestIDs;
    }

    private int getDepthFirstOrdinal(Set<String> lookForTaskIDs, EVTask root) {
        int result = -1;
        if (lookForTaskIDs != null && !lookForTaskIDs.isEmpty() && root != null)
            result = getDepthFirstOrdinal(lookForTaskIDs, root, -1);
        return (result < 0 ? Integer.MAX_VALUE : result);
    }

    private int getDepthFirstOrdinal(Set<String> lookForTaskIDs, EVTask t,
            int ordinal) {
        List<String> taskIDs = t.getTaskIDs();
        if (taskIDs != null) {
            for (String id : taskIDs)
                if (lookForTaskIDs.contains(id))
                    return 0 - ordinal;
        }
        ordinal--;
        for (int i = 0;  i < t.getNumChildren();  i++) {
            ordinal = getDepthFirstOrdinal(lookForTaskIDs, t.getChild(i),
                ordinal);
            if (ordinal > 0)
                return ordinal;
        }
        return ordinal;
    }


    /** Find baseline data and load it into the merged schedule.
     */
    private void recalcBaselineData(EVTask mergedRoot) {
        // at the moment, we refuse to calculate baselines for filtered data
        if (filter != null)
            return;

        EVSnapshot baselineSnapshot = null;
        if (taskList.calculator != null)
            baselineSnapshot = taskList.calculator.getBaselineDataSource();
        if (baselineSnapshot == null)
            return;

        EVTaskList mergedBaseline;

        //If a tasklist filter has been applied after the merged baseline has already been calculated,
        //we need to recalculate the baseline.
        if (baselineSnapshot == lastSnapshot && !taskList.evTaskListFilterApplied()) {
            mergedBaseline = lastMergedBaseline;
        } else {
            EVTaskList baselineTaskList = baselineSnapshot.getTaskList();
            lastMergedBaseline = mergedBaseline = new EVTaskListMerged(
                    baselineTaskList, false, preserveLeaves, null);
            lastSnapshot = baselineSnapshot;
        }

        EVTask baselineRoot = mergedBaseline.getTaskRoot();
        EVCalculator.calcBaselineTaskData(mergedRoot, baselineRoot);
    }
    private Object lastSnapshot = null;
    private EVTaskList lastMergedBaseline = null;


    /** Collapse parent nodes that have only one child.
     */
    private void simplifyNodes(EVTask task) {
        int numChildren = task.getNumChildren();

        for (int i = 0;  i < numChildren;  i++)
            simplifyNodes(task.getChild(i));

        if (numChildren == 1) {
            EVTask child = task.getChild(0);
            task.assignedTo = mergeLists(task.assignedTo, child.assignedTo);
            task.dependencies = mergeLists(task.dependencies, child.dependencies);
            task.taskIDs = mergeLists(task.taskIDs, child.taskIDs);
            task.name = pathConcat(task.name, child.name);

            if (task.baselineStartDate == null)
                task.baselineStartDate = child.baselineStartDate;
            if (task.baselineDate == null)
                task.baselineDate = child.baselineDate;
            if (!(task.baselineTime > 0))
                task.baselineTime = child.baselineTime;

            Set nm = new HashSet((Set) nodesMerged.get(task));
            nm.addAll((Set) nodesMerged.get(child));
            nodesMerged.put(task, nm);

            task.remove(child);
            for (int i = 0;  i < child.getNumChildren();  i++)
                task.forceAdd(child.getChild(i));
        }
    }

    /** Convenience routine for instance counting */
    private static List mergeLists(List a, List b) {
        LinkedHashSet result = new LinkedHashSet();
        addAll(result, a);
        addAll(result, b);
        return new LinkedList(result);
    }

    /** Convenience routine for merging path names */
    private static String pathConcat(String a, String b) {
        if (b == null || b.length() == 0)
            return a;
        else if (a == null || a.length() == 0)
            return b;
        else if (b.startsWith("/"))
            return a + b;
        else
            return a + "/" + b;
    }

    /** Convenience method.  Is a no-op if the second arg is null or empty. */
    private static void addAll(Collection a, Collection b) {
        if (hasValue(b))
            a.addAll(b);
    }

    /** Test for the presence of an object in a collection, using identity
     * comparisons instead of equals()
     */
    private static boolean containsIdentity(Collection a, Object b) {
        for (Iterator i = a.iterator(); i.hasNext();) {
            if (i.next() == b)
                return true;
        }
        return false;
    }

    /** Convenience routine for instance counting */
    private static void increment(Map numbers, Object key, int incr) {
        Integer i = (Integer) numbers.get(key);
        if (i == null)
            i = new Integer(incr);
        else
            i = new Integer(i.intValue() + incr);
        numbers.put(key, i);
    }

    /** Find all the keys in the map whose values equal the given value.
     */
    private static List findKeysWithValue(Map map, Object value) {
        LinkedList result = new LinkedList();
        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (value.equals(e.getValue()))
                result.add(e.getKey());
        }
        return result;
    }


    /** Delete all of the children of an EVTask */
    private static void clearChildren(EVTask task) {
        while (task.getNumChildren() > 0)
            task.remove(task.getChild(0));
    }

    /** Return true if the two collections have any common members */
    private static boolean intersects(Collection a, Collection b) {
        if (hasValue(a) && hasValue(b)) {
            for (Iterator i = a.iterator(); i.hasNext();) {
                if (b.contains(i.next()))
                    return true;
            }
        }
        return false;
    }

    /** Return true if the collection is non-null and is not empty */
    private static boolean hasValue(Collection c) {
        return c != null && !c.isEmpty();
    }

    /** Generate a fairly unique node name.
     * 
     * A different value will be returned on each call to this method.
     * No guarantees are made that the name is unique within the final,
     * merged tree (if some user created an item called "node1" in their
     * EV schedule, for example).
     */
    private String uniqueNodeName() {
        return "node" + uniqueNumber++;
    }
    int uniqueNumber = 1;



    /** Object to hold data on behalf of a node which should appear in the
     * merged result tree.
     */
    private class TaskKey {

        /** The EVTask nodes that should be merged to create this task */
        private Set evNodes = new HashSet();

        /** The task IDs used by all of the {@link evNodes} */
        private Set taskIDs = new HashSet();

        /** The names of all the {@link evNodes} */
        private Set namesUsed = new HashSet();

        /** The TaskKey which represents the parent of this node */
        private TaskKey parent;

        private LightweightSet children = new LightweightSet();

        public Set<EVTask> getEvNodes() {
            return evNodes;
        }

        public Set getTaskIDs() {
            return taskIDs;
        }

        public Set getNamesUsed() {
            return namesUsed;
        }

        public void setParent(TaskKey newParent) {
            if (parent != null)
                parent.children.remove(this);
            parent = newParent;
            if (newParent != null)
                newParent.children.add(this);
        }

        public TaskKey getParent() {
            return parent;
        }

        public List getChildren() {
            return children;
        }

        /** Add data from the given EVTask to this object */
        public void addTask(EVTask task) {
            evNodes.add(task);
            if (task.getTaskIDs() != null)
                taskIDs.addAll(task.getTaskIDs());
            namesUsed.add(task.getName());
        }

        /** Merge data from another TaskKey into this object */
        public void merge(TaskKey that) {
            this.evNodes.addAll(that.evNodes);
            this.taskIDs.addAll(that.taskIDs);
            this.namesUsed.addAll(that.namesUsed);
            this.children.addAll(that.children);
            for (Iterator i = that.children.iterator(); i.hasNext();)
                ((TaskKey) i.next()).parent = this;
            that.children.clear();
            that.setParent(null);
        }

    }

    /** A simple class for holding a string, and sorting it according to
     * an ordinal value.
     */
    private class SortableTaskName implements Comparable {

        public String name;

        public Set<String> taskIDs;

        public int ordinal;

        public SortableTaskName(String name, Set<String> taskIDs) {
            this.name = name;
            this.taskIDs = taskIDs;
        }

        public int compareTo(Object o) {
            SortableTaskName that = (SortableTaskName) o;

            if (wbsTaskOrderComparator != null) {
                Integer wbsCompare = wbsTaskOrderComparator.compare(
                    this.taskIDs, that.taskIDs);
                if (wbsCompare != null)
                    return wbsCompare;
            }

            if (this.ordinal == that.ordinal)
                return this.name.compareTo(that.name);
            else
                return this.ordinal - that.ordinal;
        }

    }

    private static class AssignedToComparator implements Comparator<EVTask> {

        public int compare(EVTask t1, EVTask t2) {
            String s1 = t1.getAssignedToText();
            String s2 = t2.getAssignedToText();
            return s1.compareTo(s2);
        }

    }
    private static final Comparator<EVTask> ASSIGNED_TO_COMPARATOR =
        new AssignedToComparator();
}
