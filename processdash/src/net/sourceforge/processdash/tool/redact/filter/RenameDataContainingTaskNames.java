// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyInfo;
import net.sourceforge.processdash.tool.redact.HierarchyPathMapper;
import net.sourceforge.processdash.tool.redact.TaskListMapper;

@EnabledFor(RedactFilterIDs.TASK_NAMES)
public class RenameDataContainingTaskNames implements DataFileEntryFilter {

    private HierarchyInfo hierarchyInfo;

    private HierarchyPathMapper pathMapper;

    private TaskListMapper taskListMapper;

    public void filter(DataFileEntry e) {
        // try each of the following naming approaches, in order.  If any one
        // succeeds, don't try the following ones.
        if (maybeRenameGlobalTSElement(e) //
                || maybeRenameLocalTsElement(e) //
                || maybeRenameCommonTaskNameBasedElements(e) //
                || maybeRenameRelativeHierarchyPaths(e))
            ;
    }


    /**
     * The global.dat file contains data elements that hold metadata for locally
     * defined task lists. These data element names contain task list names and
     * must be renamed.
     */
    private boolean maybeRenameGlobalTSElement(DataFileEntry e) {
        // check to see if we are in the global data file
        if (!"global.dat".equals(e.getFilename()))
            return false;

        // look for the presence of the task & schedule dataname prefix
        String dataName = e.getKey();
        if (!dataName.startsWith(GLOBAL_TS_PREFIX))
            return false;

        // extract the task list name out of the data name
        int end = dataName.indexOf('/', beg + 1);
        if (end == -1)
            return false;
        String taskListName = dataName.substring(beg, end);

        // construct a new name for the data element
        String newName = taskListMapper.hashTaskListName(taskListName);
        e.setKey(GLOBAL_TS_PREFIX + newName + dataName.substring(end));
        return true;
    }

    private static final String GLOBAL_TS_PREFIX = "Task-Schedule/";

    private static final int beg = GLOBAL_TS_PREFIX.length();


    /**
     * Throughout the hierarchy, certain data elements are used to tag the
     * inclusion/exclusion/ordering of tasks within task lists. These data names
     * contain task list names and could contain task names, so they must be
     * renamed.
     */
    private boolean maybeRenameLocalTsElement(DataFileEntry e) {
        String dataName = e.getKey();
        int[] tsPrefixPos = getTsPrefixPos(dataName);
        if (tsPrefixPos == null)
            return false;

        String path = dataName.substring(0, tsPrefixPos[0]);
        String tsPrefix = dataName.substring(tsPrefixPos[0], tsPrefixPos[1]);
        String taskListName = dataName.substring(tsPrefixPos[1]);
        String newName = pathMapper.getString(path) + tsPrefix
                + taskListMapper.hashTaskListName(taskListName);
        e.setKey(newName);
        return true;
    }

    private static final int[] getTsPrefixPos(String dataName) {
        for (String prefix : TS_DATA_PREFIXES) {
            if (dataName.startsWith(prefix))
                return new int[] { 0, prefix.length() };
        }
        for (String delim : TS_DATA_DELIMS) {
            int pos = dataName.indexOf(delim);
            if (pos != -1)
                return new int[] { pos, pos + delim.length() };
        }
        return null;
    }

    private static final String[] TS_DATA_PREFIXES = { "TST_", "TST-LOE_",
            "TST-TSK#_", "TST-PRUNED_" };

    private static final String[] TS_DATA_DELIMS = { "/TST_", "/TST-LOE_",
            "/TST-TSK#_", "/TST-PRUNED_" };



    /**
     * Certain elements are commonly written by dashboard logic relative to
     * hierarchy paths. If this entry represents one of those well-known items,
     * remap the name.
     */
    private boolean maybeRenameCommonTaskNameBasedElements(DataFileEntry e) {
        String dataName = e.getKey();
        int suffixPos = getTaskNameSuffixPos(dataName);
        if (suffixPos == -1)
            return false;

        String origPath = dataName.substring(0, suffixPos);
        if (origPath.contains(" /") || origPath.contains("/ "))
            // the presence of a space next to a slash indicates that this is a
            // process-namespaced data element.
            return false;

        String suffix = dataName.substring(suffixPos);
        String newPath = pathMapper.getString(origPath);
        String newName = newPath + suffix;
        e.setKey(newName);
        return true;
    }

    private int getTaskNameSuffixPos(String dataName) {
        for (String suffix : TASK_NAME_BASED_ELEMS) {
            if (dataName.endsWith(suffix))
                return dataName.length() - suffix.length();
        }
        return -1;
    }

    private static final String[] TASK_NAME_BASED_ELEMS = { "/leaf", "/node",
            "/Time", "/Estimated Time", "/Defects Injected",
            "/Defects Removed", "/Started", "/Completed" };


    /**
     * Use our knowledge of the hierarchy and the relative path of the current
     * data file to determine if certain elements represent paths to our
     * hierarchy children. If so, rename them appropriately.
     */
    private boolean maybeRenameRelativeHierarchyPaths(DataFileEntry e) {
        setFilename(e);
        if (currentNode != null) {
            String dataName = e.getKey();
            String remappedName = currentNode.remapRelativePath(dataName);
            if (remappedName != null && !remappedName.equals(dataName)) {
                e.setKey(remappedName);
                return true;
            }
        }
        return false;
    }

    private String currentDatafile;

    private HierarchyInfo.Node currentNode;

    private void setFilename(DataFileEntry e) {
        String datafile = e.getFilename();
        if (!datafile.equals(currentDatafile)) {
            this.currentDatafile = datafile;
            this.currentNode = hierarchyInfo.findNodeForDataFile(datafile);
        }
    }

}
