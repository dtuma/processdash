// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.DatabasePluginUtils;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskMapType;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskNodeType;

public class ProbeDatabaseUtil {

    private DataRepository data;

    private String prefix;

    private Set onlyInclude;

    private DatabasePlugin plugin;

    private QueryRunner query;

    protected ProbeDatabaseUtil(DataRepository data, String prefix) {
        this.data = data;
        this.prefix = prefix;
    }

    public ProbeDatabaseResultSet loadData(String[] columnHeaders,
            ListData onlyInclude) {
        if (isEnabled() == false)
            return null;

        if (onlyInclude != null)
            this.onlyInclude = new HashSet(onlyInclude.asList());

        String workflowName = "";
        try {
            getQueryRunner();
            workflowName = getWorkflowName();
            List enactmentList = getEnactments(workflowName);
            if (!enactmentList.isEmpty())
                return buildResultSet(columnHeaders, workflowName,
                    enactmentList);
        } catch (QueryError qe) {
        }
        return new ProbeDatabaseResultSet(0, columnHeaders, workflowName, null);
    }

    private void getQueryRunner() {
        plugin = require(QueryUtils.getDatabasePlugin(data, true));
        query = require(plugin.getObject(QueryRunner.class));
    }

    private String getWorkflowName() {
        String projectID = require(getString(PROJECT_ID, true));
        String workflowID = require(getString(WORKFLOW_SOURCE_ID, false));
        String workflowPhaseID = DatabasePluginUtils
                .getWorkflowPhaseIdentifier(projectID, workflowID);

        return (String) require(QueryUtils.singleValue(query.queryHql(
            WORKFLOW_NAME_QUERY, workflowPhaseID)));
    }

    private List getEnactments(String workflowName) {
        List result = query.queryHql(ENACTMENT_QUERY, workflowName);
        if (onlyInclude != null) {
            for (Iterator i = result.iterator(); i.hasNext();) {
                Object[] row = (Object[]) i.next();
                Object identifier = row[2];
                if (!onlyInclude.contains(identifier))
                    i.remove();
            }
        }
        return result;
    }

    private ProbeDatabaseResultSet buildResultSet(String[] columnHeaders,
            String workflowName, List enactmentList) {
        // retrieve time and completion date info for each enactment.
        List<Integer> enactmentKeys = QueryUtils.pluckColumn(enactmentList, 0);
        List taskStatus = query.queryHql(TASK_STATUS_QUERY, enactmentKeys);

        // the task status query will only return results for enactments
        // that were 100% complete. We only need data for those enactments.
        enactmentKeys = QueryUtils.pluckColumn(taskStatus, 0);
        int numRows = enactmentKeys.size();

        // retrieve the time-in-phase data for this workflow
        Map timeInPhase = null;
        if (numRows > 0)
            timeInPhase = QueryUtils.mapColumns(
                query.queryHql(TIME_IN_PHASE_QUERY, enactmentKeys), 0, 1);

        // Create an empty result set to hold the data.
        ProbeDatabaseResultSet result = new ProbeDatabaseResultSet(numRows,
                columnHeaders, workflowName, timeInPhase);
        if (numRows == 0)
            return result;

        // build maps to look up data more conveniently
        Map rootKeys = QueryUtils.mapColumns(enactmentList, 0, 1);
        Map rootIDs = QueryUtils.mapColumns(enactmentList, 0, 2);
        Map<Integer, String> rootPaths = DatabasePluginUtils
                .getDashPathsForPlanItems(query, rootKeys.values());

        // iterate over task status info and store it into the result set.
        for (int i = numRows; i-- > 0;) {
            Object[] taskStatusRow = (Object[]) taskStatus.get(i);
            Integer enactmentKey = (Integer) taskStatusRow[0];
            int row = i + 1;

            // retrieve the dashboard path for this row and store in result
            Integer rootKey = (Integer) rootKeys.get(enactmentKey);
            String enactmentPath = rootPaths.get(rootKey);
            result.setRowName(row, enactmentPath);

            // retrieve the plan item ID for this row and store in result
            String rootID = (String) rootIDs.get(enactmentKey);
            result.setData(row, ProbeData.IDENTIFIER, str(rootID));

            // store time and completion date data in the result.
            result.setData(row, ProbeData.EST_TIME, num(taskStatusRow[1], 60));
            result.setData(row, ProbeData.ACT_TIME, num(taskStatusRow[2], 60));
            result.setData(row, ProbeData.COMPLETED_DATE,
                date(taskStatusRow[3]));
        }

        // retrieve size data and store it into the result set
        String sizeUnits = require(getString("Size Units", false));
        List sizeData = query.queryHql(SIZE_QUERY, enactmentKeys, sizeUnits);
        for (int i = sizeData.size(); i-- > 0;) {
            Object[] sizeRow = (Object[]) sizeData.get(i);
            Integer enactmentKey = (Integer) sizeRow[0];
            int row = enactmentKeys.indexOf(enactmentKey) + 1;
            if (row == 0)
                continue;

            String measurementType = (String) sizeRow[1];
            int col = getSizeTargetColumn(measurementType);
            if (col != -1)
                result.setData(row, col, num(sizeRow[2], 1));
        }

        return result;
    }

    private int getSizeTargetColumn(String measurementType) {
        if ("Estimated Proxy".equals(measurementType))
            return ProbeData.EST_OBJ_LOC;
        else if ("Plan".equals(measurementType))
            return ProbeData.EST_NC_LOC;
        else if ("Actual".equals(measurementType))
            return ProbeData.ACT_NC_LOC;
        else
            return -1;
    }


    /**
     * Use historical time-in-phase data to spread a time estimate across the
     * tasks in the current process enactment
     * 
     * @param histData
     *            historical data
     * @param estimate
     *            the new total time estimate
     */
    public void spreadEstimatedTime(ProbeData histData, double estimate) {
        // get a list of the tasks in the current process enactment
        WorkflowEnactmentHelper tasks = new WorkflowEnactmentHelper(data,
                prefix);
        Map<String, String> targetTasks = tasks.getEnactmentTasks(
            TaskMapType.PhaseName, TaskNodeType.Leaf);
        if (targetTasks == null)
            // null indicates that the database queries failed. Abort.
            return;

        Map<String, String> pspTasks = tasks.getEnactmentTasks(
            TaskMapType.PhaseName, TaskNodeType.PSP);
        targetTasks.putAll(pspTasks);
        Set<String> workflowPspSteps = new HashSet(pspTasks.values());

        // if we were able to enumerate the tasks successfully, spread the new
        // time estimate across those tasks using historical time-in-phase data.
        if (!targetTasks.isEmpty()) {
            Map timeInPhase = ((ProbeDatabaseResultSet) histData.getResultSet())
                    .getTimeInPhase();
            spreadTimeUsingWeights(targetTasks, workflowPspSteps, timeInPhase,
                estimate);
        }
    }

    private void spreadTimeUsingWeights(Map<String, String> targetTasks,
            Set<String> workflowPspSteps, Map<String, Number> weights,
            double newTotal) {

        // if any of the tasks are complete, don't change their estimates.
        // instead, reduce the newTotal by the planned time of those completed
        // tasks.
        for (Iterator i = targetTasks.keySet().iterator(); i.hasNext();) {
            String oneTask = (String) i.next();
            if (!canChangeEstimate(oneTask)) {
                newTotal -= getEstimate(oneTask);
                i.remove();
            }
        }
        if (targetTasks.isEmpty())
            return;
        newTotal = Math.max(0, newTotal);

        // find the set of workflow phases that are represented by the tasks
        // in this process enactment. Add up the total historical time spent
        // in those phases.
        double totalWeight = 0;
        HashSet phasesPresent = new HashSet(targetTasks.values());
        mergeWeightsOfPspPhases(workflowPspSteps, weights);
        for (Object phaseName : phasesPresent)
            totalWeight += doubleValue(weights.get(phaseName));

        // if we have no historical data, just scale the current estimates
        // proportionately so they add up to the new total.
        if (totalWeight == 0) {
            spreadTimeBasedOnCurrentEstimates(targetTasks.keySet(), newTotal);
            return;
        }

        // otherwise, iterate over each of the represented workflow phases
        for (Object onePhase : phasesPresent) {
            // identify the percentage of the total estimate that should
            // be associated with this phase.
            double phaseWeight = doubleValue(weights.get(onePhase));
            double phaseTotal = newTotal * phaseWeight / totalWeight;

            // find a list of tasks that map to this workflow phase
            List matchingTasks = new ArrayList();
            for (Entry<String, String> e : targetTasks.entrySet()) {
                if (onePhase.equals(e.getValue()))
                    matchingTasks.add(e.getKey());
            }

            // spread this phase's time allocation over the matching task(s)
            spreadTimeBasedOnCurrentEstimates(matchingTasks, phaseTotal);
        }
    }

    /** @return true if we can change the estimate for a given task */
    private boolean canChangeEstimate(String path) {
        if (getValue(path, "Completed", false) != null)
            // don't change the estimates of tasks that are marked complete.
            return false;

        else if (getValue(path, "PSP Project", false) != null
                && getValue(path, "Planning/Completed", false) != null)
            // don't change the estimates of PSP tasks whose Planning phase
            // has been marked complete (because associated values are frozen)
            return false;

        // Check the estimate in question to see if it is read-only
        SimpleData sd = getValue(path, ESTIMATED_TIME, false);
        return (sd == null || sd.isEditable());
    }

    /**
     * Some tasks (such as PSP tasks) are estimated at a parent level, then time
     * is distributed across subtasks. The workflow task enumerator will list
     * those tasks with a phase like "PSP". The time-in-phase data, however,
     * will include separate entries for each subitem (for example,
     * "PSP/Planning", "PSP/Design", etc.
     * 
     * This method sums up the phase data for each PSP task, and creates a new
     * entry to store the weight of the PSP task.
     */
    private void mergeWeightsOfPspPhases(Set<String> workflowPspSteps,
            Map<String, Number> weights) {
        for (String onePspStep : workflowPspSteps) {
            double matchedWeight = 0;
            for (Entry<String, Number> e : weights.entrySet()) {
                if (Filter.pathMatches(e.getKey(), onePspStep))
                    matchedWeight += doubleValue(e.getValue());
            }
            weights.put(onePspStep, matchedWeight);
        }
    }

    /**
     * At certain times we need to spread time across a list of tasks, and we
     * don't have a set of historical data telling us how to do so.
     * 
     * When that happens, we first try to use the current planned times and use
     * those as "pseudo-weights." This scales all times proportionately, in a
     * manner similar to what the WBS Editor would do when we edit a top-down
     * value.
     * 
     * When the tasks in question do not have any planned times, we fall back
     * and spread the time across the tasks equally.
     */
    private void spreadTimeBasedOnCurrentEstimates(
            Collection<String> targetTasks, double newTotal) {

        // if we only have one task to change, or if we're setting the
        // estimates to zero, no spreading needs to occur. Just set the times.
        if (targetTasks.size() == 1 || newTotal == 0) {
            for (String oneTask : targetTasks)
                setEstimate(oneTask, newTotal);
            return;
        }

        // add up the current time estimates for the tasks in question.
        double oldTotal = 0;
        for (String oneTask : targetTasks)
            oldTotal += getEstimate(oneTask);

        if (oldTotal == 0) {
            // if the current tasks have no time estimates, distribute the
            // time equally across the various tasks.
            double equalPart = newTotal / targetTasks.size();
            for (String oneTask : targetTasks)
                setEstimate(oneTask, equalPart);

        } else {
            // otherwise, scale the current estimates proportionately so
            // they add up to the new total.
            double ratio = newTotal / oldTotal;
            for (String oneTask : targetTasks) {
                double oldEstimate = getEstimate(oneTask);
                double newEstimate = ratio * oldEstimate;
                setEstimate(oneTask, newEstimate);
            }
        }
    }


    private boolean isEnabled() {
        return getString(USES_DATABASE_TAG, false) != null;
    }

    private StringData str(String s) {
        return StringData.create(s);
    }

    private DoubleData num(Object n, int fraction) {
        if (n instanceof Number) {
            return new DoubleData(((Number) n).doubleValue() / fraction);
        } else {
            return ImmutableDoubleData.READ_ONLY_ZERO;
        }
    }

    private DateData date(Object d) {
        if (d instanceof Date) {
            return new DateData((Date) d, true);
        } else {
            return null;
        }
    }

    private double doubleValue(Object o) {
        return (o instanceof Number ? ((Number) o).doubleValue() : 0);
    }

    private double getEstimate(String prefix) {
        return getNum(prefix, ESTIMATED_TIME);
    }

    private void setEstimate(String prefix, double value) {
        data.userPutValue(
            DataRepository.createDataName(prefix, ESTIMATED_TIME),
            new DoubleData(value, true));
    }

    private double getNum(String prefix, String dataName) {
        SimpleData sd = getValue(prefix, dataName, false);
        return (sd instanceof DoubleData ? ((DoubleData) sd).getDouble() : 0);
    }

    private String getString(String dataName, boolean inherit) {
        return getString(prefix, dataName, inherit);
    }

    private String getString(String prefix, String dataName, boolean inherit) {
        SimpleData sd = getValue(prefix, dataName, inherit);
        return (sd == null ? null : sd.format());
    }

    private SimpleData getValue(String prefix, String dataName, boolean inherit) {
        SaveableData d;
        if (inherit)
            d = data.getInheritableValue(prefix, dataName);
        else
            d = data.getValue(DataRepository.createDataName(prefix, dataName));
        return (d == null ? null : d.getSimpleValue());
    }

    private <T> T require(T value) {
        if (value == null)
            throw new QueryError();
        else
            return value;
    }

    /**
     * Query to find the name of the workflow that generated the current task.
     */
    private static final String WORKFLOW_NAME_QUERY = //
    "select p.process.name from Phase p where p.identifier = ?";

    /**
     * Query to find all the times when this user has performed a PROBE task
     * during the enactment of a given process. The enactment keys returned will
     * identify the PROBE tasks within those enactments.
     */
    private static final String ENACTMENT_QUERY = //
    "select distinct pe.key, pe.rootItem.key, pe.rootItem.identifier "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join task.planItem.phase.mapsToPhase probePhase "
            + "where pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 "
            + "and probePhase.identifier = '*PROBE*/PROBE' "
            + "and pe.process.name = ?";

    /**
     * Query to find the completion date and planned/actual time for a set of
     * process enactments.
     * 
     * The process enactment keys passed in refer to the PROBE tasks; this must
     * be rejoined to the ProcessEnactment table to retrieve data for all of the
     * other tasks associated with the enactment.
     */
    private static final String TASK_STATUS_QUERY = //
    "select pe.key, sum(task.planTimeMin), sum(task.actualTimeMin), "
            + "max(task.actualCompletionDate) "
            + "from ProcessEnactment pe, ProcessEnactment pi, TaskStatusFact task "
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and pi.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.key "
            + "having max(task.actualCompletionDateDim.key) < 99990000 "
            + "order by max(task.actualCompletionDate)";

    /**
     * Query to retrieve the amount of time historically spent in each phase of
     * the active workflow
     */
    private static final String TIME_IN_PHASE_QUERY = //
    "select mapsTo.shortName, sum(task.actualTimeMin) "
            + "from ProcessEnactment pe, ProcessEnactment pi, TaskStatusFact task "
            + "join pi.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and pe.process.key = mapsTo.process.key "
            + "and pi.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by mapsTo.shortName";

    /**
     * Query to find the size data for a set of process enactments.
     * 
     * The process enactment keys passed in refer to the PROBE tasks. We can
     * query data from these tasks only, since PROBE-related size data will be
     * attached to these plan items.
     */
    private static final String SIZE_QUERY = //
    "select pe.key, size.measurementType.name, " //
            + "sum(size.addedAndModifiedSize) "
            + "from ProcessEnactment pe, SizeFact size "
            + "where pe.key in (?) "
            + "and pe.includesItem.key = size.planItem.key "
            + "and size.sizeMetric.shortName = ? "
            + "and size.versionInfo.current = 1 "
            + "group by pe.key, size.measurementType.name";


    private static final String USES_DATABASE_TAG = "PROBE_USE_DATABASE_FOR_HIST_DATA";

    private static final String PROJECT_ID = "Project_ID";

    private static final String WORKFLOW_SOURCE_ID = "Workflow_Source_ID";

    private static final String ESTIMATED_TIME = "Estimated Time";


    private class QueryError extends RuntimeException {
    }

}
