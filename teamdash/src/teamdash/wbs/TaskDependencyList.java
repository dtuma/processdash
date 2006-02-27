package teamdash.wbs;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for holding a list of {@link teamdash.wbs.TaskDependency} objects.
 */
public class TaskDependencyList extends ArrayList {

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
        if (isEmpty())
            return "";

        StringBuffer result = new StringBuffer();
        for (Iterator i = this.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            result.append(TASK_SEPARATOR).append(d.displayName);
        }
        return result.substring(TASK_SEPARATOR.length());
    }

    private static final String TASK_SEPARATOR = "  \u25AA  ";
}
