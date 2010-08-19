// Copyright (C) 2010 Tuma Solutions, LLC
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

import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.ui.TaskScheduleChooser;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.msg.MessageDispatcher;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;

public class TaskListAltererMessageHandler implements MessageHandler {

    private static final String MESSAGE_TYPE = "pdash.alterTaskList";

    private static final String[] MESSAGE_TYPES = { MESSAGE_TYPE };

    private DashboardContext ctx;

    private static final Logger logger = Logger
            .getLogger(TaskListAltererMessageHandler.class.getName());


    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public void handle(MessageEvent message) {
        try {
            logger.finer("Handling task list alteration message "
                    + message.getMessageId());
            Element xml = message.getMessageXml();
            for (Element instr : XMLUtils.getChildElements(xml))
                handleInstr(instr);
            logger.finer("Done handling task list alteration message");

        } catch (IllegalArgumentException iae) {
            MessageDispatcher.getInstance().setMessageHandled(message, false);
            // Print a warning about the invalid XML message we received.
            logError(iae, message.getMessageXml());
        }
    }

    private void logError(Exception ex, Element xml) {
        StringBuilder errMsg = new StringBuilder();
        errMsg.append(ex.getMessage()).append(" while handling message:\n")
                .append(XMLUtils.getAsText(xml));
        logger.warning(errMsg.toString());
    }

    private void handleInstr(Element instr) {
        String type = instr.getTagName();
        if (ADD_TO_ROLLUP.equals(type))
            handleAddToRollup(instr);
        else
            throw new IllegalArgumentException("Unrecognized instruction "
                    + type);

    }

    private void handleAddToRollup(Element instr) {
        EVTaskListRollup rollup = openRollupTaskList(instr);

        boolean madeChange = false;
        for (Element e : XMLUtils.getChildElements(instr)) {
            if (!ADD_TASK_LIST.equals(e.getTagName()))
                throw new IllegalArgumentException("Under " + ADD_TO_ROLLUP
                        + ", unrecognized instruction " + e.getTagName());

            if (handleAddTaskListToRollup(rollup, e))
                madeChange = true;
        }

        if (madeChange)
            rollup.save();
    }

    /**
     * Attempt to find the task list specified by the given XML element, and add
     * it to the rollup if it isn't already there. Return true if any change was
     * made.
     */
    private boolean handleAddTaskListToRollup(EVTaskListRollup rollup,
            Element instr) {
        // if this instruction names a particular task list by ID,
        // check to see if the rollup already has a child schedule
        // with that ID. If so, ignore this instruction and quietly
        // do nothing. (This optimization saves computation time.)
        if (rollupContainsScheduleWithID(rollup, instr))
            return false;

        // open the child task list.
        EVTaskList child = openTaskList(instr);
        if (child == null)
            throw new IllegalArgumentException(
                    "The child task list was not found");

        // now that we have the task list open, check to see if its ID matches
        // a schedule we already know about. If so, no changes are needed.
        if (rollupContainsScheduleWithID(rollup, child))
            return false;

        // try to add the child schedule to the rollup.
        if (!rollup.addTask(child.getTaskListName(), ctx.getData(), ctx
                .getHierarchy(), ctx.getCache(), false))
            throw new IllegalArgumentException(
                    "Unable to add child task list to rollup");

        // If the user currently has this rollup open in a Task & Schedule
        // window, perform a courtesy to them and automatically add the
        // schedule to their active view (without marking their view as
        // dirty). It is helpful for them to receive the changes that occur
        // asynchronously. In addition, if they later save changes to this
        // rollup, they won't overwrite the addition we just made.
        TaskScheduleChooser.dispatchAddMessage(rollup.getTaskListName(), child
                .getTaskListName());

        logger.info("Added task list '" + child.getDisplayName()
                + "' to rollup '" + rollup.getDisplayName() + "'");
        return true;
    }

    private boolean rollupContainsScheduleWithID(EVTaskListRollup rollup,
            Element instr) {
        return rollupContainsScheduleWithID(rollup, instr
                .getAttribute(TASK_LIST_ID_ATTR));
    }

    private boolean rollupContainsScheduleWithID(EVTaskListRollup rollup,
            EVTaskList child) {
        return rollupContainsScheduleWithID(rollup, child.getID());
    }

    private boolean rollupContainsScheduleWithID(EVTaskListRollup rollup,
            String lookForID) {
        // check for a child schedule with the given ID. Note that we do NOT
        // check for matches by name, because we commonly need to replace
        // a "missing" schedule with a new one. Of course, we don't care
        // whether the new schedule has the same name as the old, missing one.
        if (hasValue(lookForID)) {
            for (int i = rollup.getSubScheduleCount(); i-- > 0;) {
                EVTaskList tl = rollup.getSubSchedule(i);
                if (lookForID.equals(tl.getID())) {
                    logger.finer("Rollup schedule '" + rollup.getDisplayName()
                            + "' already contains a task list with ID '"
                            + lookForID + "'");
                    return true;
                }
            }
        }

        return false;
    }

    private EVTaskListRollup openRollupTaskList(Element instr) {
        EVTaskList result = openTaskList(instr);
        if (result == null)
            throw new IllegalArgumentException(
                    "The rollup task list was not found");
        else if (result instanceof EVTaskListRollup)
            return (EVTaskListRollup) result;
        else
            throw new IllegalArgumentException("The EV task list '"
                    + result.getDisplayName() + "' is not a rollup task list");
    }

    private EVTaskList openTaskList(Element instr) {
        EVTaskList result = openNamedTaskList(instr);
        if (result == null)
            result = openTaskListForProject(instr);
        return result;
    }

    private EVTaskList openNamedTaskList(Element instr) {
        EVTaskList result = null;
        String taskListName = getTaskListName(instr);
        if (hasValue(taskListName))
            result = EVTaskList.openExisting(taskListName, ctx.getData(), ctx
                    .getHierarchy(), ctx.getCache(), false);

        return result;
    }

    private String getTaskListName(Element instr) {
        String result = null;

        String taskListID = instr.getAttribute(TASK_LIST_ID_ATTR);
        if (hasValue(taskListID))
            result = EVTaskList.getTaskListNameForID(ctx.getData(), taskListID,
                true);

        if (!hasValue(result))
            result = instr.getAttribute(TASK_LIST_NAME_ATTR);

        return result;
    }


    private EVTaskList openTaskListForProject(Element instr) {
        String taskListName = getTaskListNameForProject(instr);
        if (hasValue(taskListName))
            return EVTaskList.openExisting(taskListName, ctx.getData(), ctx
                    .getHierarchy(), ctx.getCache(), false);
        else
            return null;
    }

    private String getTaskListNameForProject(Element instr) {
        String projectName = getProjectName(instr);
        if (hasValue(projectName)) {
            List taskListNames = EVTaskList.getPreferredTaskListsForPath(ctx
                    .getData(), projectName);
            if (taskListNames != null && taskListNames.size() == 1)
                return (String) taskListNames.get(0);
        }
        return null;
    }

    private String getProjectName(Element instr) {
        String result = null;

        String projectID = instr.getAttribute(FOR_PROJECT_ID_ATTR);
        if (hasValue(projectID))
            result = findProjectWithID(PropertyKey.ROOT, projectID);

        if (!hasValue(result))
            result = instr.getAttribute(FOR_PROJECT_NAME_ATTR);

        return result;
    }

    private String findProjectWithID(PropertyKey parent, String projectID) {
        for (int i = ctx.getHierarchy().getNumChildren(parent); i-- > 0;) {
            PropertyKey node = ctx.getHierarchy().getChildKey(parent, i);

            // check to see if THIS node is the project we're looking for.
            String path = node.path();
            if (pathMatchesProjectID(path, projectID))
                return path;

            // Otherwise, recurse into children to look for the project.
            String childResult = findProjectWithID(node, projectID);
            if (childResult != null)
                return childResult;
        }

        // the project was not found in this portion of the tree.
        return null;
    }

    private boolean pathMatchesProjectID(String path, String projectID) {
        String dataName = DataRepository.createDataName(path,
            PROJECT_ID_DATA_NAME);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd != null && projectID.equals(sd.format()));
    }

    private static final boolean hasValue(String s) {
        return XMLUtils.hasValue(s);
    }

    private static final String ADD_TO_ROLLUP = "addToRollup";

    private static final String ADD_TASK_LIST = "addTaskList";

    private static final String TASK_LIST_ID_ATTR = "taskListID";

    private static final String TASK_LIST_NAME_ATTR = "taskListName";

    private static final String FOR_PROJECT_ID_ATTR = "forProjectID";

    private static final String FOR_PROJECT_NAME_ATTR = "forProject";

    private static final String PROJECT_ID_DATA_NAME = "Project_ID";

}
