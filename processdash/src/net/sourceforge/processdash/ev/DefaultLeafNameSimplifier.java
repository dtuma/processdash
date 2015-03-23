// Copyright (C) 2011 Tuma Solutions, LLC
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
import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.HierarchyNameSimplifier;

/**
 * In many reports and dialogs, it is necessary to display the names of leaf
 * tasks. Unfortunately, the full paths to these tasks can be long and unwieldy.
 * Fortunately, many tasks have common redundant prefixes. This class accepts a
 * list of tasks, and identifies the final portion of each task path name that
 * must be displayed to ensure unique names for all of the tasks.
 * 
 * @author Tuma
 */
public class DefaultLeafNameSimplifier implements HierarchyNameSimplifier {

    private Collection tasks;

    private String shortNamePrefix;

    private Map<String, String> shortenedNames;


    public DefaultLeafNameSimplifier(Collection tasks, String shortNamePrefix) {
        this.tasks = tasks;
        this.shortNamePrefix = shortNamePrefix;
        recalc();
    }

    public void setTasks(Collection tasks) {
        this.tasks = tasks;
        recalc();
    }

    public String getShortenedName(String path) {
        return shortenedNames.get(path);
    }

    public void recalc() {
        // create empty working map.
        Map<String, TerminalName> working = new HashMap();
        int minimumNumberOfPathSegments = Settings.getInt(SETTING_NAME, 2);

        // add all names to the working map
        for (Object oneTask : tasks) {
            String onePath = getPath(oneTask);
            TerminalName oneName = new TerminalName(onePath,
                    minimumNumberOfPathSegments);
            oneName.save(working);
        }

        // pull the resolved names from the working map to build the results.
        Map<String, String> results = new HashMap();
        for (TerminalName oneName : working.values()) {
            if (oneName != CONFLICT)
                results.put(oneName.getFullPath(),
                    oneName.getShortenedPath(shortNamePrefix));
        }
        this.shortenedNames = results;
    }

    private String getPath(Object t) {
        if (t instanceof String) {
            return (String) t;
        } else if (t instanceof EVTask) {
            return ((EVTask) t).getFullName();
        } else {
            throw new IllegalArgumentException("Unrecognized item type");
        }
    }

    private static class TerminalName {

        private String fullPath;

        private String[] nameComponents;

        private int uniqueTerminalCount;

        public TerminalName(String fullPath, int minPathComponents) {
            this.fullPath = fullPath;

            // trim initial slash, split name components
            String path = fullPath;
            if (path.startsWith("/"))
                path = path.substring(1);
            this.nameComponents = path.split("/");

            // initialize the number of final path segments
            this.uniqueTerminalCount = Math.min(this.nameComponents.length,
                minPathComponents);
        }

        public String getFullPath() {
            return fullPath;
        }

        public String getShortenedPath(String shortPrefix) {
            if (uniqueTerminalCount >= nameComponents.length)
                return getFullPath();
            else if (shortPrefix == null)
                return getPathString();
            else
                return shortPrefix + getPathString();
        }

        private String getPathString() {
            StringBuilder result = new StringBuilder();
            int len = nameComponents.length;
            for (int pos = len - uniqueTerminalCount; pos < len; pos++) {
                result.append("/").append(nameComponents[pos]);
            }
            return result.substring(1);
        }

        public void save(Map<String, TerminalName> dest) {
            String thisPathString = null;
            for (; thisPathString == null || this.lengthen();) {

                // get the current path string of the task.
                thisPathString = getPathString();

                // look up this name to see if it already maps to something.
                TerminalName other = dest.get(thisPathString);
                if (other == null) {
                    // if the current name does not conflict with anything
                    // else, break out of the loop.
                    break;

                } else if (other.fullPath.equals(this.fullPath)) {
                    // in some situations (e.g. rollups), it could be common
                    // for two paths to have the exact same full name (because
                    // the task was assigned to the more than one person). Such
                    // collisions are harmless and only need to be mapped once.
                    // The presence of the other task indicates that this
                    // full path has already been mapped, so there is nothing
                    // for this routine to do.
                    return;

                } else {
                    // otherwise, lengthen the other name, and continue on to
                    // another potential iteration of this loop.
                    other.lengthenForConflict(dest);

                    // even in the face of a conflict, the loop will terminate
                    // if we cannot lengthen "this" object. That would occur if
                    // the current task path is a subset of an existing task.
                }
            }

            // when the loop terminates, place this item in the map.
            dest.put(thisPathString, this);
        }

        private void lengthenForConflict(Map<String, TerminalName> dest) {
            // try to increment the terminal count.
            String conflictName = getPathString();
            if (lengthen()) {
                // if we were able to lengthen the name, put this object in the
                // map with our new name, and put a CONFLICT token under the
                // original key.
                String lengthenedName = getPathString();
                dest.put(lengthenedName, this);
                dest.put(conflictName, CONFLICT);
            }
        }

        /**
         * Increment the number of terminal path components, if it is possible
         * without overrunning the actual number of path segments.
         * 
         * @return true if the name could be lengthened.
         */
        private boolean lengthen() {
            if (uniqueTerminalCount < nameComponents.length) {
                uniqueTerminalCount++;
                return true;
            } else {
                return false;
            }
        }
    }

    private static final TerminalName CONFLICT = new TerminalName("", 1);

    private static final String SETTING_NAME = "ev.flatView.minPathSegmentDisplayCount";

}
