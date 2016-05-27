// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui.scanner;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.util.NullSafeObjectUtils;

public class SkippedWorkflowSteps extends GenericScanItemList {

    @Override
    protected List<Object[]> getItems() throws IOException {
        List<Object[]> allTasks = super.getItems();
        List<Object[]> skippedTasks = new LinkedList();

        int pos = 0;
        while (pos < allTasks.size()) {
            List<Object[]> enactmentTasks = getTasksForEnactment(allTasks, pos);
            if (shouldProcessEnactment(enactmentTasks)) {
                sortTasksWithinEnactment(enactmentTasks);
                findSkippedTasksInEnactment(skippedTasks, enactmentTasks);
            }
            pos += enactmentTasks.size();
        }

        return skippedTasks;
    }

    /**
     * Find a series of adjacent tasks, starting with the given postion, that
     * all come from the same process enactment.
     */
    private List<Object[]> getTasksForEnactment(List<Object[]> allTasks,
            int firstPos) {
        Object[] firstTask = allTasks.get(firstPos);
        int pos = firstPos;
        while (++pos < allTasks.size()) {
            Object[] oneTask = allTasks.get(pos);
            if (mismatch(firstTask, oneTask, Field.Root, Field.Process))
                break;
        }

        return allTasks.subList(firstPos, pos);
    }

    /**
     * If an enactment is 0% complete or 100% complete, then we know there
     * cannot be any skipped steps. This method returns true if the enactment is
     * partially complete.
     */
    private boolean shouldProcessEnactment(List<Object[]> enactmentTasks) {
        boolean sawCompletedTask = false, sawIncompleteTask = false;
        for (Object[] task : enactmentTasks) {
            if (incomplete(task))
                sawIncompleteTask = true;
            else
                sawCompletedTask = true;
        }

        return sawCompletedTask && sawIncompleteTask;
    }

    /**
     * Although process phases have an implicit ordering, people are free to
     * tailor the process - for example, by iterating over some set of tasks
     * that would otherwise be too large. When such tailoring occurs, we must
     * examine the tasks in the order they appear within the WBS.
     * 
     * Our main query will place tasks in WBS order, if the list of tasks is
     * flat. But if tasks are nested hierarchically, the query will return the
     * nested tasks in the wrong order. This method corrects that problem.
     */
    private void sortTasksWithinEnactment(List<Object[]> enactmentTasks) {
        // before we begin, we must know:
        // (a) if any nested tasks are present
        // (b) if nesting occurs to more than one level
        // This loop makes those determinations.
        int[] nestedTaskDepths = new int[enactmentTasks.size()];
        int maxDepth = 0;
        for (int i = enactmentTasks.size(); i-- > 0;) {
            Object[] task = enactmentTasks.get(i);

            // see if this item has a different parent than the enactment root.
            // if so, it is a nested item.
            Object rootKey = task[Field.Root.ordinal()];
            Object parentKey = task[Field.Parent.ordinal()];
            if (!rootKey.equals(parentKey)) {
                // nesting could occur to any depth. Our sorting algorithm below
                // needs to process the deepest tasks first, so we calculate the
                // total WBS depth of this nested task, and record it.
                int depth = numSegments(task[Field.WBSName.ordinal()])
                        + numSegments(task[Field.TaskName.ordinal()]);
                nestedTaskDepths[i] = depth;
                maxDepth = Math.max(maxDepth, depth);
            }
        }

        // sort nested tasks in order, starting with the deepest ones.
        while (maxDepth > 0) {
            Object lastParentKey = null;
            Object lastGrandparentKey = null, lastParentOrdinal = null;

            for (int i = nestedTaskDepths.length; i-- > 0;) {
                // if this task is part of the deepest nesting level,
                if (nestedTaskDepths[i] == maxDepth) {
                    // get the plan item key of this task's parent.
                    Object[] task = enactmentTasks.get(i);
                    Object parentKey = task[Field.Parent.ordinal()];
                    if (!parentKey.equals(lastParentKey)) {
                        // if this is the first time we've encountered this
                        // particular parent key, find that parent in the
                        // database, and retrieve (a) the key of *its* parent,
                        // and (b) its ordinal within that grandparent.
                        List parentQuery = getPdash().getQuery().query(
                            PARENT_QUERY, parentKey);
                        if (parentQuery.isEmpty())
                            continue;

                        Object[] parentData = (Object[]) parentQuery.get(0);
                        lastGrandparentKey = parentData[0];
                        lastParentOrdinal = parentData[1];
                        lastParentKey = parentKey;
                    }

                    // rewrite the data for the nested task, replacing its
                    // parent and ordinal values with the corresponding values
                    // from its parent.
                    task[Field.Parent.ordinal()] = lastGrandparentKey;
                    task[Field.Ordinal.ordinal()] = lastParentOrdinal;
                }
            }

            // if we reparented any tasks at this nesting level, sort the list
            // of tasks. The sort is stable, so reparented tasks won't change
            // location relative to each other. Instead, they will just be moved
            // to the right order next to other items with the same parent.
            if (lastParentKey != null)
                Collections.sort(enactmentTasks, TASK_SORTER);

            // step up a nesting level and repeat.
            maxDepth--;
        }
    }

    private int numSegments(Object o) {
        return ((String) o).split("/").length;
    }

    private static final String PARENT_QUERY = "select p.parent.key, p.ordinal "
            + "from PlanItem as p where p.key = ?";


    /**
     * Look through the tasks in an enactment, and find incomplete tasks
     * followed by completed tasks.
     */
    private void findSkippedTasksInEnactment(List<Object[]> skippedTasks,
            List<Object[]> enactmentTasks) {

        Object[] previousTask = null;
        Object[] workingIncompleteTask = null;
        List<Object[]> currentIncompleteTasks = new LinkedList();

        for (Object[] task : enactmentTasks) {

            // if this is a new task, save the working incomplete task.
            boolean isNewTask = mismatch(task, previousTask, Field.TaskID);
            if (isNewTask && workingIncompleteTask != null) {
                currentIncompleteTasks.add(workingIncompleteTask);
                workingIncompleteTask = null;
            }

            if (incomplete(task)) {
                // if this task is incomplete, make a note of it.
                if (workingIncompleteTask == null) {
                    workingIncompleteTask = task;
                } else {
                    appendResourceNames(workingIncompleteTask, task);
                }

            } else if (isNewTask && !currentIncompleteTasks.isEmpty()) {
                // if this task represents a new process step, add any
                // accumulated incomplete tasks to our result list.
                skippedTasks.addAll(currentIncompleteTasks);
                currentIncompleteTasks.clear();
            }

            previousTask = task;
        }
    }

    private boolean mismatch(Object[] taskA, Object[] taskB, Field... fields) {
        if (taskA == null || taskB == null)
            return true;
        for (Field f : fields) {
            int pos = f.ordinal();
            if (!NullSafeObjectUtils.EQ(taskA[pos], taskB[pos]))
                return true;
        }
        return false;
    }

    private boolean incomplete(Object[] task) {
        Integer date = (Integer) task[Field.Completed.ordinal()];
        return (date == null || date > 99990000);
    }

    private void appendResourceNames(Object[] workingIncompleteTask,
            Object[] task) {
        String prevWho = (String) workingIncompleteTask[Field.Who.ordinal()];
        String newPerson = (String) task[Field.Who.ordinal()];
        String combinedWho = prevWho + ", " + newPerson;
        workingIncompleteTask[Field.Who.ordinal()] = combinedWho;
    }

    private enum Field {
        TaskID, Root, Process, Parent, Ordinal, Completed, Who, WBSName, TaskName
    };

    private static class TaskSorter implements Comparator<Object[]> {

        @Override
        public int compare(Object[] task1, Object[] task2) {
            Comparable parent1 = (Comparable) task1[Field.Parent.ordinal()];
            Comparable parent2 = (Comparable) task2[Field.Parent.ordinal()];
            int result = parent1.compareTo(parent2);
            if (result != 0)
                return result;

            Comparable ordinal1 = (Comparable) task1[Field.Ordinal.ordinal()];
            Comparable ordinal2 = (Comparable) task2[Field.Ordinal.ordinal()];
            return ordinal1.compareTo(ordinal2);
        }
    }

    private static final Comparator TASK_SORTER = new TaskSorter();

}
