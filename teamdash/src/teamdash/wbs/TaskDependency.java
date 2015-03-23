// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds information about a task which is a dependency of another task.
 */
public class TaskDependency implements Annotated {

    /** The ID of the task being depended upon */
    public String nodeID;

    /** The full name of the task being depended upon */
    public String displayName;

    /**
     * True if, to the best of our knowledge, the task being depended upon does
     * not exist
     */
    public boolean hasError;

    public TaskDependency(String nodeID, String displayName) {
        init(nodeID, displayName);
    }

    public TaskDependency(String annotatedText) {
        Matcher m = ANNOTATION_PATTERN.matcher(annotatedText);
        if (!m.matches())
            throw new IllegalArgumentException();
        init(m.group(2), m.group(1));
    }

    private void init(String nodeID, String displayName) {
        this.nodeID = nodeID;
        this.displayName = displayName;
        if (displayName != null) {
            this.displayName = displayName;
            this.hasError = false;
        } else {
            this.displayName = TaskDependencySource.UNKNOWN_NODE_DISPLAY_NAME;
            this.hasError = true;
        }
    }

    /**
     * Update this dependency with information from the given source.
     * 
     * @return true if the display name of this node was changed.
     */
    public boolean update(TaskDependencySource source) {
        String newDislayName = source.getDisplayNameForNode(nodeID);
        if (newDislayName == TaskDependencySource.UNKNOWN_NODE_DISPLAY_NAME) {
            hasError = true;
            return false;

        } else {
            hasError = false;
            if (newDislayName.equals(displayName))
                return false;
            else {
                displayName = newDislayName;
                return true;
            }
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof TaskDependency) {
            TaskDependency that = (TaskDependency) obj;
            return this.nodeID.equals(that.nodeID);
        }
        return false;
    }

    public int hashCode() {
        return nodeID.hashCode();
    }

    public String toString() {
        return displayName;
    }

    public String getAnnotation() {
        String name = (displayName != null
                ? displayName
                : TaskDependencySource.UNKNOWN_NODE_DISPLAY_NAME);
        return name + " [" + nodeID + "]";
    }

    private static final Pattern ANNOTATION_PATTERN = Pattern
            .compile("(.*) \\[(.*)\\]");
}
