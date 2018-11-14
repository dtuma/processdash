// Copyright (C) 2018 Tuma Solutions, LLC
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.WorkflowInfo;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.process.WorkflowInfo.Workflow;
import net.sourceforge.processdash.process.WorkflowInfoFactory;
import net.sourceforge.processdash.tool.db.PersonFilter;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;
import net.sourceforge.processdash.util.HTMLUtils;

public class SkippedComponentSteps extends SkippedWorkflowSteps {

    private Set<String> finalPhases;

    private String currentFinalTaskName;

    @Override
    protected List<Object[]> getItems() throws IOException {
        try {
            loadWorkflowInfo();

            if (finalPhases == null || finalPhases.isEmpty())
                return Collections.EMPTY_LIST;
            else
                return super.getItems();
        } finally {
            finalPhases = null;
            currentFinalTaskName = null;
        }
    }

    private void loadWorkflowInfo() {
        WorkflowInfo workflowInfo = WorkflowInfoFactory.get(getDataContext());
        if (workflowInfo == null)
            return;

        finalPhases = new HashSet();
        for (Workflow workflow : workflowInfo.getWorkflows()) {
            List<Phase> phases = workflow.getPhases();
            int numPhases = phases.size();
            if (numPhases > 1) {
                Phase finalPhase = phases.get(numPhases - 1);
                finalPhases.add("WF:" + finalPhase.getPhaseId());
            }
        }
    }

    @Override
    protected String getQuery() throws IOException {
        return readHqlFile("/dash/snippets/skippedWorkflowSteps");
    }

    @Override
    protected boolean shouldProcessEnactment(List<Object[]> enactmentTasks) {
        // look for enactments that have a completed final phase
        for (Object[] task : enactmentTasks) {
            Object phaseID = task[Field.PhaseID.ordinal()];
            if (finalPhases.contains(phaseID) && !incomplete(task)) {
                currentFinalTaskName = task[Field.WBSName.ordinal()] + "/"
                        + task[Field.TaskName.ordinal()];
                return true;
            }
        }

        // this enactment did not include a completed final phase
        return false;
    }

    @Override
    protected boolean notDone(Object[] task) {
        // if this task represents the final phase in the process, consider it
        // "done." This will cause earlier phases to trigger the "skipped" logic
        // if any data is missing. And:
        //
        // 1) We already know the final phase was marked complete by at least
        // one person, because we checked that in shouldProcessEnactment()
        //
        // 2) Omitting this check would allow earlier phases to slip through the
        // cracks if the final phase had no time logged.
        Object taskPhaseID = task[Field.PhaseID.ordinal()];
        if (finalPhases.contains(taskPhaseID))
            return false;

        // if a task has not been marked complete, it was not done
        if (incomplete(task))
            return true;

        // if no time was logged against a task, it was not done
        Number time = (Number) task[Field.ActTime.ordinal()];
        return (time == null || time.doubleValue() == 0);
    }

    @Override
    protected void findSkippedTasksInEnactment(List<Object[]> badComponents,
            List<Object[]> enactmentTasks, PersonFilter privacyFilt) {

        // ask the parent class to find the tasks in this enactment that were
        // skipped. We know that the final phase was marked complete, so this
        // should return any prior tasks that were not done. We place those into
        // a temporary list so we see the items for this component only
        List<Object[]> skippedTasks = new LinkedList<Object[]>();
        super.findSkippedTasksInEnactment(skippedTasks, enactmentTasks,
            privacyFilt);
        if (skippedTasks.isEmpty())
            return;

        // get the first task from the list, and extract information about the
        // enactment root
        Object[] firstTask = skippedTasks.get(0);
        Object badComponentID = firstTask[Field.RootID.ordinal()];
        String badComponentName = (String) firstTask[Field.RootName.ordinal()];
        int badComponentNameLen = badComponentName.length() + 1;

        // format a detailed message describing the problem
        if (currentFinalTaskName.startsWith(badComponentName))
            currentFinalTaskName = currentFinalTaskName
                    .substring(badComponentNameLen);
        Resources res = (Resources) env.get(SnippetEnvironment.RESOURCES);
        String badComponentMsg = res.format("Item_FMT", currentFinalTaskName);

        // build an HTML block describing this bad component
        StringBuilder html = new StringBuilder();
        html.append(HTMLUtils.escapeEntities(badComponentName));
        html.append("&nbsp;&mdash;&nbsp;");
        html.append(HTMLUtils.escapeEntities(badComponentMsg));

        // add list items to describe the skipped tasks in this component
        html.append("\n<ul style='margin-bottom:0.5em'>\n");
        for (Object[] oneTask : skippedTasks) {
            String oneTaskName = oneTask[Field.WBSName.ordinal()] + "/"
                    + oneTask[Field.TaskName.ordinal()];
            if (oneTaskName.startsWith(badComponentName))
                oneTaskName = oneTaskName.substring(badComponentNameLen);
            String who = (String) oneTask[Field.Who.ordinal()];

            html.append("    <li>");
            html.append(HTMLUtils.escapeEntities(oneTaskName));
            html.append("&nbsp;&mdash;&nbsp;");
            html.append(HTMLUtils.escapeEntities(who));
            html.append("</li>\n");
        }
        html.append("</ul>\n");

        // construct an array with the information we've collected
        Object[] oneBadComponent = new Object[] { badComponentID,
                html.toString() };
        badComponents.add(oneBadComponent);
    }

    @Override
    protected String getItemHtml(Object[] oneItem) {
        return (String) oneItem[1];
    }

}
