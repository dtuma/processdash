// Copyright (C) 2008-2009 Tuma Solutions, LLC
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
import java.util.Set;

import net.sourceforge.processdash.hier.Filter;

public class EVHierarchicalFilter implements EVTaskFilter {

    public static final String HIER_FILTER_ATTR = "hierFilter";

    public static final String MERGED_ROOT_ID = "ROOT";


    protected String displayName;

    protected Set includedTasks;

    public Set<String> includedTaskIds = new HashSet<String>();

    protected EVTaskFilter nextFilter;

    public EVHierarchicalFilter(String displayName, Set includedTasks) {
        this.displayName = displayName;
        this.includedTasks = includedTasks;

        //FIXME - 2023-03-12 - RBentall
        //Build a set which contains the fulltaskIds. 
        //If we have duplicate task IDs, ignore the second.
        //May be better to refactor so that we use a map instead of set for includedTasks.       
        Iterator it = includedTasks.iterator();

        while(it.hasNext()){
            EVTask e = (EVTask)it.next();
            includedTaskIds.add(e.getFullTaskID());           
        }
    }

    public EVTaskFilter appendFilter(EVTaskFilter filter) {
        this.nextFilter = filter;
        return this;
    }

    public boolean include(EVTask t) {
        if (!includedTasks.contains(t))
            return false;
        else if (nextFilter != null)
            return nextFilter.include(t);
        else
            return true;
    }

    public String getAttribute(String name) {
        if (HIER_FILTER_ATTR.equals(name))
            return displayName;
        else if (nextFilter != null)
            return nextFilter.getAttribute(name);
        else
            return null;
    }

    public static EVHierarchicalFilter getFilterForMerged(EVTaskList tl,
            String path) {
        String taskID;
        String subpath;
        int slashPos = path.indexOf('/');
        if (slashPos == -1) {
            taskID = path;
            subpath = null;
        } else {
            taskID = path.substring(0, slashPos);
            subpath = path.substring(slashPos + 1);
        }

        EVTaskListMerger m = new EVTaskListMerger(tl, false, false, null);
        EVTask matchingNode;
        if (MERGED_ROOT_ID.equals(taskID))
            matchingNode = m.getMergedTaskRoot();
        else
            matchingNode = m.getMergedTaskRoot().findByTaskIDs(
                Collections.singleton(taskID));
        matchingNode = findNodeWithSubpath(matchingNode, subpath);

        Set includedTasks;
        String displayName;
        if (matchingNode == null) {
            includedTasks = Collections.EMPTY_SET;
            displayName = "";
        } else {
            includedTasks = m.getTasksMergedBeneath(matchingNode);
            displayName = matchingNode.getFullName();
        }

        return new EVHierarchicalFilter(displayName, includedTasks);
    }

    public static EVHierarchicalFilter getFilter(EVTaskList tl, String path) {
        if (path == null || path.length() == 0 || path.equals("/"))
            return null;

        if (path.startsWith("/"))
            path = path.substring(1);

        EVTask matchingNode = findNodeWithSubpath(tl.getTaskRoot(), path);

        Set includedTasks;
        if (matchingNode == null)
            includedTasks = Collections.EMPTY_SET;
        else {
            includedTasks = new HashSet();
            includedTasks.add(matchingNode);
            includedTasks.addAll(matchingNode.getDescendants());
        }

        return new EVHierarchicalFilter(path, includedTasks);
    }

    private static EVTask findNodeWithSubpath(EVTask task, String path) {
        if (path == null || path.length() == 0)
            return task;

        // look over our children for a matching
        for (int i = 0; i < task.getNumChildren(); i++) {
            EVTask child = task.getChild(i);
            String childName = child.getName();
            if (childName.startsWith("/"))
                childName = childName.substring(1);
            if (childName.equals(path))
                return child;
            if (Filter.pathMatches(path, childName)) {
                String subpath = path.substring(childName.length() + 1);
                EVTask result = findNodeWithSubpath(child, subpath);
                if (result != null)
                    return result;
            }
        }

        return null;
    }

}
