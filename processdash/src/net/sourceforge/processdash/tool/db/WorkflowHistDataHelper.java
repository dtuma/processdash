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

package net.sourceforge.processdash.tool.db;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.util.DataPair;

public class WorkflowHistDataHelper {

    public static final String TOTAL_PHASE_KEY = "Total ";

    private DataContext data;

    private String contextProjectID;

    private String workflowName;

    private DatabasePlugin plugin;

    private QueryRunner query;

    private String onlyForProject;

    private String onlyForProjectsThrough;

    private boolean onlyCompleted = true;

    private Integer workflowKey;

    private Set<Integer> includedWorkflowKeys;

    private List enactments;

    private Map<String, String> phaseTypes;


    public WorkflowHistDataHelper(DataContext data, String contextProjectID,
            String workflowName) {
        this.data = data;
        this.contextProjectID = contextProjectID;
        this.workflowName = workflowName;
    }

    public String getContextProjectID() {
        return contextProjectID;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public boolean isOnlyCompleted() {
        return onlyCompleted;
    }

    public void setOnlyCompleted(boolean onlyCompleted) {
        this.onlyCompleted = onlyCompleted;
    }

    public String getOnlyForProject() {
        return onlyForProject;
    }

    public void setOnlyForProject(String onlyForProject) {
        this.onlyForProject = onlyForProject;
    }

    private void initQueryRunner() {
        if (query == null) {
            plugin = QueryUtils.getDatabasePlugin(data, true);
            query = plugin.getObject(QueryRunner.class);
        }
    }

    private List query(String hql, Object... args) {
        for (Object oneArg : args) {
            if (oneArg instanceof Collection) {
                // when empty collections are bound to an argument in an HQL
                // query, Hibernate throws an exception. Since all of our
                // collection arguments are "in" clauses, an empty collection
                // arg means that the result will be empty too. Handle this
                // case specially to avoid receiving the exception.
                if (((Collection) oneArg).isEmpty())
                    return Collections.EMPTY_LIST;
            }
        }
        initQueryRunner();
        return query.queryHql(hql, args);
    }

    public List<String> getWorkflowNamesForProject() {
        String workflowProcessIDPattern = DatabasePluginUtils
                .getWorkflowPhaseIdentifier(contextProjectID, "%");
        List result = query(WORKFLOW_LIST_QUERY, workflowProcessIDPattern);
        if (result.size() > 1)
            Collections.sort(result);
        return result;
    }

    private static final String WORKFLOW_LIST_QUERY = //
    "select p.name from Process p where p.identifier like ?";


    private List getEnactments() {
        if (enactments == null) {
            Set<Integer> workflowKeys = getIncludedWorkflowKeys();
            if (Settings.isTeamMode())
                enactments = query(TEAM_ENACTMENT_QUERY, workflowKeys);
            else
                enactments = query(PERSONAL_ENACTMENT_QUERY, workflowKeys);
            filterEnactments();
        }
        return enactments;
    }

    private List<Integer> getEnactmentKeys() {
        if (enactments == null)
            getEnactments();
        return QueryUtils.pluckColumn(enactments, ENACTMENT_KEY);
    }

    /**
     * Query to find all the enactments of a given process for an entire team.
     * The enactment keys returned will identify the root nodes of those
     * enactments.
     */
    private static final String TEAM_ENACTMENT_QUERY = //
    "select distinct pe.key, pe.rootItem.identifier "
            + "from ProcessEnactment pe "
            + "where pe.rootItem.key = pe.includesItem.key "
            + "and pe.process.key in (?)";

    /**
     * Query to find all the times when this individual user has performed a
     * PROBE task during the enactment of a given process. The enactment keys
     * returned will identify the PROBE tasks within those enactments.
     */
    private static final String PERSONAL_ENACTMENT_QUERY = //
    "select distinct pe.key, pe.rootItem.identifier "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join task.planItem.phase.mapsToPhase probePhase "
            + "where pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 "
            + "and probePhase.identifier = '*PROBE*/PROBE' "
            + "and pe.process.key in (?)";

    private static final int ENACTMENT_KEY = 0;

    private static final int ENACTMENT_ROOT_WBS_ID = 1;


    private void filterEnactments() {
        if (onlyForProject != null || onlyForProjectsThrough != null)
            applyProjectSpecificFilter();
        if (onlyCompleted)
            discardIncompleteEnactments();
    }

    private void applyProjectSpecificFilter() {
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] enactment = (Object[]) i.next();
            String rootItemID = (String) enactment[ENACTMENT_ROOT_WBS_ID];
            if (shouldExcludeProject(rootItemID))
                i.remove();
        }
    }

    private boolean shouldExcludeProject(String planItemID) {
        String projectID = getProjectID(planItemID);
        if (projectID == null)
            return true;
        else if (onlyForProject != null)
            return onlyForProject.equals(projectID);
        else if (onlyForProjectsThrough != null)
            return projectID.compareTo(onlyForProjectsThrough) <= 0;
        else
            return false;
    }

    private String getProjectID(String planItemID) {
        int colonPos = planItemID.indexOf(':');
        if (colonPos == -1)
            return null;
        else
            return planItemID.substring(0, colonPos);
    }

    private void discardIncompleteEnactments() {
        if (enactments.isEmpty())
            return;

        Set completedEnactmentKeys = new HashSet(query(
            ENACTMENT_COMPLETION_QUERY, getEnactmentKeys(), getWorkflowKey()));

        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] enactment = (Object[]) i.next();
            Object oneEnactmentKey = enactment[ENACTMENT_KEY];
            if (!completedEnactmentKeys.contains(oneEnactmentKey))
                i.remove();
        }
    }

    /**
     * Query to determine which workflows are 100% complete
     */
    private static final String ENACTMENT_COMPLETION_QUERY = //
    "select pe.key " //
            + "from ProcessEnactment pe, ProcessEnactment pi, TaskStatusFact task "
            + "join pi.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and mapsTo.process.key = ? "
            + "and pi.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.key "
            + "having max(task.actualCompletionDateDim.key) < 99990000";


    private Integer getWorkflowKey() {
        if (workflowKey == null)
            initWorkflowData();
        return workflowKey;
    }

    private Set<Integer> getIncludedWorkflowKeys() {
        if (includedWorkflowKeys == null)
            initWorkflowData();
        return includedWorkflowKeys;
    }

    private void initWorkflowData() {
        // find the key for the current workflow
        String workflowProcessIDPattern = DatabasePluginUtils
                .getWorkflowPhaseIdentifier(contextProjectID, "%");
        workflowKey = QueryUtils.singleValue(query(WORKFLOW_KEY_QUERY,
            workflowName, workflowProcessIDPattern));

        if (workflowKey == null) {
            workflowKey = -1;
            includedWorkflowKeys = Collections.EMPTY_SET;
        } else {
            // find all the workflows that map to the given workflow
            includedWorkflowKeys = new HashSet<Integer>(query.queryHql(
                WORKFLOW_MAPPING_QUERY, workflowKey));
        }
    }

    private static final String WORKFLOW_KEY_QUERY = //
    "select p.key from Process p where p.name = ? and p.identifier like ?";

    private static final String WORKFLOW_MAPPING_QUERY = //
    "select distinct p.process.key from Phase p " //
            + "join p.mapsToPhase mapsTo " //
            + "where mapsTo.process.key = ?";


    private List<String> getWorkflowSteps() {
        return query(WORKFLOW_PHASE_QUERY, getWorkflowKey());
    }

    private static final String WORKFLOW_PHASE_QUERY = //
    "select phase.shortName from Phase phase " //
            + "where phase.process.key = ? " //
            + "and phase.ordinal is not null " //
            + "order by phase.ordinal";

    public Map<String, DataPair> getTimeInPhase() {
        Map<String, DataPair> result = new LinkedHashMap();
        phaseTypes = new HashMap<String, String>();
        for (String step : getWorkflowSteps())
            result.put(step, new DataPair());
        DataPair total = new DataPair();

        List<Object[]> rawData = query(TIME_IN_PHASE_QUERY, getEnactmentKeys(),
            getWorkflowKey());
        for (Object[] row : rawData) {
            String stepName = (String) row[0];
            DataPair dataPair = result.get(stepName);
            if (dataPair == null) {
                stepName = "(" + stepName + ")";
                dataPair = new DataPair();
                result.put(stepName, dataPair);
            }
            phaseTypes.put(stepName, (String) row[1]);
            dataPair.plan = ((Number) row[2]).doubleValue();
            dataPair.actual = ((Number) row[3]).doubleValue();
            total.add(dataPair);
        }

        result.put(TOTAL_PHASE_KEY, total);

        return result;
    }

    public Map<String, String> getPhaseTypes() {
        return phaseTypes;
    }

    private static final String TIME_IN_PHASE_QUERY = //
    "select mapsTo.shortName, mapsTo.typeName, "
            + "sum(task.planTimeMin), sum(task.actualTimeMin) "
            + "from ProcessEnactment pe, ProcessEnactment pi, TaskStatusFact task "
            + "join pi.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and mapsTo.process.key = ? "
            + "and pi.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by mapsTo.shortName, mapsTo.typeName " //
            + "order by mapsTo.shortName";



    public Map<String, DataPair> getAddedAndModifiedSizes() {
        Map<String, DataPair> result = new TreeMap();

        List<Object[]> rawData = query(SIZE_QUERY, getEnactmentKeys());
        for (Object[] row : rawData) {
            String sizeUnits = (String) row[0];
            String measurementType = (String) row[1];
            double sizeValue = ((Number) row[2]).doubleValue();

            DataPair dataPair = result.get(sizeUnits);
            if (dataPair == null)
                result.put(sizeUnits, dataPair = new DataPair());

            if ("Plan".equals(measurementType))
                dataPair.plan += sizeValue;
            else if ("Actual".equals(measurementType))
                dataPair.actual += sizeValue;
        }

        return result;
    }

    /**
     * Query to find the size data for a set of process enactments.
     */
    private static final String SIZE_QUERY = //
    "select size.sizeMetric.shortName, size.measurementType.name, " //
            + "sum(size.addedAndModifiedSize) "
            + "from ProcessEnactment pe, ProcessEnactment pi, SizeFact size "
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and pi.includesItem.key = size.planItem.key "
            + "and size.versionInfo.current = 1 "
            + "group by size.sizeMetric.shortName, size.measurementType.name";

}
