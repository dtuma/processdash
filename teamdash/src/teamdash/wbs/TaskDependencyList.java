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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for holding a list of {@link teamdash.wbs.TaskDependency} objects.
 */
public class TaskDependencyList extends ArrayList implements Annotated {

    /**
     * Update all the tasks in our list using the given dependency source.
     * 
     * @return true if the name of any task changed.
     */
    public boolean update(TaskDependencySource source) {
        boolean result = false;
        for (Iterator i = this.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            if (d.update(source))
                result = true;
        }
        return result;
    }

    /** Return true if any of our tasks have an error status. */
    public boolean hasError() {
        for (Iterator i = this.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            if (d.hasError)
                return true;
        }
        return false;
    }

    /**
     * Create a pretty representation of this list of tasks, for display in a
     * table
     */
    public String toString() {
        return toString(false);
    }

    public String getAnnotation() {
        return toString(true);
    }

    public String toString(boolean annotated) {
        if (isEmpty())
            return "";

        StringBuffer result = new StringBuffer();
        for (Iterator i = this.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            result.append(TASK_SEPARATOR);
            result.append(annotated ? d.getAnnotation() : d.displayName);
        }
        return result.substring(TASK_SEPARATOR.length());
    }

    public static TaskDependencyList valueOf(String text) {
        if (text == null || text.length() == 0)
            return null;

        TaskDependencyList result = new TaskDependencyList();
        String[] items = text.split(TASK_SEPARATOR);
        for (int i = 0; i < items.length; i++) {
            try {
                result.add(new TaskDependency(items[i]));
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
        }
        return result;
    }

    private static final String TASK_SEPARATOR = "  \u25AA  ";
}
