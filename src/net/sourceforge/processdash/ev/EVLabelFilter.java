// Copyright (C) 2006-2013 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.ev.MilestoneDataConstants.MILESTONE_ID_LABEL_PREFIX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.glob.GlobEngine;
import net.sourceforge.processdash.util.glob.TaggedDataListSource;

public class EVLabelFilter implements EVTaskFilter {

    public static final String LABEL_FILTER_ATTR = "labelFilter";

    public static final String LABEL_TAG = "label:";

    public static final String TASK_LABELS_DATANAME = "Task_Labels";

    private String filter;

    private Set knownLabeledTaskIDs;

    private Set matchingTaskIDs;

    private Set matchingTasks;

    public EVLabelFilter(EVTaskList taskList, String filter, DataRepository data) {
        this.filter = filter;

        boolean isLocal = (taskList instanceof EVTaskListData);

        Set labelPaths = new HashSet();
        List labelData = new ArrayList();
        collectLabelData(data, taskList.getTaskRoot(), isLocal, labelPaths,
            labelData);

        if (labelData.isEmpty())
            throw new IllegalArgumentException("No labels found for task list");

        knownLabeledTaskIDs = new HashSet(labelData);
        maybeLabelMilestoneIDs(filter, labelData);

        matchingTaskIDs = GlobEngine.search(filter, LABEL_TAG, labelData,
            new DeferredLabelLookup(data));

        matchingTasks = new HashSet();
        collectMatchingTasks(taskList.getTaskRoot(), false);
    }

    private static void collectLabelData(DataRepository data, EVTask task,
            boolean isLocal, Set labelPaths, List labelData) {
        boolean foundLabels = false;
        List ids = task.getTaskIDs();
        if (ids != null && !ids.isEmpty()) {
            if (isLocal) {
                foundLabels = addLabelData(data, task.getFullName(),
                    labelPaths, labelData);
            } else {
                for (Iterator i = ids.iterator(); i.hasNext();) {
                    String id = (String) i.next();
                    int colonPos = id.indexOf(':');
                    if (colonPos != -1)
                        id = id.substring(0, colonPos) + ":root";
                    String path = EVTaskDependencyResolver.getInstance()
                            .getCanonicalTaskName(id);
                    if (addLabelData(data, path, labelPaths, labelData))
                        foundLabels = true;
                }
            }
        }

        if (!foundLabels) {
            for (int i = task.getNumChildren(); i-- > 0;)
                collectLabelData(data, task.getChild(i), isLocal, labelPaths,
                    labelData);
        }
    }

    private static boolean addLabelData(DataRepository data, String path,
            Set labelPaths, List labelData) {
        if (path == null || path.length() == 0)
            return false;
        if (labelPaths.contains(path))
            return true;

        List labels = getLabelDataForPath(data, path);
        if (labels != null) {
            labelData.addAll(labels);
            labelPaths.add(path);
            return true;
        } else {
            return false;
        }
    }

    private static List getLabelDataForPath(DataRepository data, String path) {
        if (path == null)
            return null;

        SaveableData val = data.getInheritableValue(path, TASK_LABELS_DATANAME);
        if (val != null) {
            SimpleData list = val.getSimpleValue();
            if (list instanceof StringData)
                list = ((StringData) list).asList();
            if (list instanceof ListData)
                return ((ListData) list).asList();
        }

        return null;
    }


    private void maybeLabelMilestoneIDs(String filter, List labelData) {
        // milestones are stored with two different attributes: a label
        // and a full milestone ID.  The DefaultTaskLabeler is smart enough
        // to process both simultaneously, but the GlobEngine only processes
        // the labels.  If we are being asked to filter on milestone IDs,
        // transform those entries into labels so the GlobEngine can find them.
        if (filter.startsWith(MILESTONE_ID_LABEL_PREFIX)) {
            for (int i = labelData.size(); i-- > 0; ) {
                String item = (String) labelData.get(i);
                if (item.startsWith(MILESTONE_ID_LABEL_PREFIX)) {
                    labelData.set(i, LABEL_TAG + item);
                }
            }
        }
    }


    private void collectMatchingTasks(EVTask task, boolean parentMatches) {
        boolean selfMatches;
        List<String> taskIDs = task.getTaskIDs();
        MatchState ms = taskIDsMatch(taskIDs);
        if (ms == MatchState.UNKNOWN_TASK)
            // if this node has no task IDs, then it is either (a) a phase
            // underneath a defined parent task, or (b) a set of nodes that
            // the user defined themselves to hierarchically subdivide their
            // work.  In either case, it should inherit its matching status
            // from its parent.
            selfMatches = parentMatches;
        else
            selfMatches = (ms == MatchState.MATCH);

        for (int i = task.getNumChildren(); i-- > 0;)
            collectMatchingTasks(task.getChild(i), selfMatches);

        if (selfMatches)
            matchingTasks.add(task);
    }

    private enum MatchState { MATCH, NO_MATCH, UNKNOWN_TASK };

    private MatchState taskIDsMatch(List<String> taskIDs) {
        boolean foundKnownTask = false;
        if (taskIDs != null && !taskIDs.isEmpty()) {
            for (String taskID : taskIDs) {
                if (StringUtils.hasValue(taskID)) {
                    if (matchingTaskIDs.contains(taskID))
                        return MatchState.MATCH;
                    else if (knownLabeledTaskIDs.contains(taskID))
                        foundKnownTask = true;
                }
            }
        }
        return (foundKnownTask ? MatchState.NO_MATCH : MatchState.UNKNOWN_TASK);
    }

    public boolean include(EVTask t) {
        return matchingTasks.contains(t);
    }

    public static boolean taskListContainsLabelData(EVTaskList taskList,
            DataRepository data) {
        boolean isLocal = (taskList instanceof EVTaskListData);

        Set labelPaths = new HashSet();
        List labelData = new ArrayList();
        collectLabelData(data, taskList.getTaskRoot(), isLocal, labelPaths,
            labelData);
        return !labelData.isEmpty();
    }

    public String getAttribute(String name) {
        if (LABEL_FILTER_ATTR.equals(name))
            return filter;
        else
            return null;
    }

    private static class DeferredLabelLookup implements TaggedDataListSource {
        DataRepository data;
        DeferredLabelLookup(DataRepository data) {
            this.data = data;
        }
        public List getTaggedData(String dataName) {
            ListData result = ListData.asListData(data.getSimpleValue(dataName));
            return (result == null ? null : result.asList());
        }
    }

}
