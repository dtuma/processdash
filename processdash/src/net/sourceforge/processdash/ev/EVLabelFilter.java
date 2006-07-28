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

package net.sourceforge.processdash.ev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.glob.GlobEngine;

public class EVLabelFilter implements EVTaskFilter {

    public static final String LABEL_TAG = "label:";

    public static final String TASK_LABELS_DATANAME = "Task_Labels";

    private Set matchingTaskIDs;

    private Set matchingTasks;

    public EVLabelFilter(EVTaskList taskList, String filter, DataRepository data) {
        boolean isLocal = (taskList instanceof EVTaskListData);

        Set labelPaths = new HashSet();
        collectLabelPaths((EVTask) taskList.getRoot(), isLocal, labelPaths);

        List labelData = new ArrayList();
        for (Iterator i = labelPaths.iterator(); i.hasNext();)
            append(labelData, getLabelDataForPath(data, (String) i.next()));

        if (labelData.isEmpty())
            throw new IllegalArgumentException("No labels found for task list");

        matchingTaskIDs = GlobEngine.search(filter, LABEL_TAG, labelData);

        matchingTasks = new HashSet();
        collectMatchingTasks((EVTask) taskList.getRoot(), false);
    }

    private void collectLabelPaths(EVTask task, boolean isLocal, Set paths) {
        List ids = task.getTaskIDs();
        if (ids != null && !ids.isEmpty()) {
            if (isLocal) {
                paths.add(task.getFullName());
            } else {
                for (Iterator i = ids.iterator(); i.hasNext();) {
                    String id = (String) i.next();
                    String path = EVTaskDependencyResolver.getInstance()
                            .getCanonicalTaskName(id);
                    paths.add(path);
                }
            }

        } else {
            for (int i = task.getNumChildren(); i-- > 0;)
                collectLabelPaths(task.getChild(i), isLocal, paths);
        }
    }

    private List getLabelDataForPath(DataRepository data, String path) {
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

    private void append(List dest, List src) {
        if (src != null)
            dest.addAll(src);
    }


    private boolean collectMatchingTasks(EVTask task, boolean parentMatches) {

        boolean selfMatches = taskIDsMatch(task.getTaskIDs());

        boolean childMatches = false;
        for (int i = task.getNumChildren(); i-- > 0;)
            if (collectMatchingTasks(task.getChild(i), selfMatches))
                childMatches = true;

        boolean isMatchingLeaf = (parentMatches && task.isLeaf()
                && task.getTaskIDs() == null);

        boolean foundMatch = selfMatches || childMatches || isMatchingLeaf;

        if (foundMatch)
            matchingTasks.add(task);

        return foundMatch;
    }

    private boolean taskIDsMatch(Collection taskIDs) {
        if (taskIDs != null) {
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                if (matchingTaskIDs.contains(i.next()))
                    return true;
            }
        }
        return false;
    }

    public boolean include(EVTask t) {
        return matchingTasks.contains(t);
    }
}
