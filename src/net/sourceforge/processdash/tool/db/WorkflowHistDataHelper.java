// Copyright (C) 2014-2016 Tuma Solutions, LLC
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

import java.util.ArrayList;
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
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.util.DataPair;

public class WorkflowHistDataHelper {

    public static final String TOTAL_PHASE_KEY = "Total ";

    private QueryRunner query;

    private String workflowID;

    private String contextProjectID;

    private String workflowName;

    private String onlyForProject;

    private String onlyForProjectsThrough;

    private boolean onlyCompleted = true;

    private Integer workflowKey;

    private Set<Integer> includedWorkflowKeys;

    private List enactments;

    private Map<String, String> phaseTypes;

    public WorkflowHistDataHelper(QueryRunner query, String workflowID) {
        this.query = query;
        this.workflowID = workflowID;
        this.contextProjectID = workflowID.split(":")[1];
    }

    public String getContextProjectID() {
        return contextProjectID;
    }

    public String getWorkflowName() {
        if (workflowName == null)
            initWorkflowData();
        return workflowName;
    }

    public String getWorkflowID() {
        return workflowID;
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
        return query.queryHql(hql, args);
    }

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

    private List<Integer> getEnactmentRootKeys() {
        if (enactments == null)
            getEnactments();
        return QueryUtils.pluckColumn(enactments, ENACTMENT_ROOT_KEY);
    }

    /**
     * Query to find all the enactments of a given process for an entire team.
     * The rows returned will identify the key and identifier for the root nodes
     * of those enactments.
     */
    private static final String TEAM_ENACTMENT_QUERY = //
    "select distinct pe.rootItem.key, pe.rootItem.identifier "
            + "from ProcessEnactment pe "
            + "where pe.rootItem.key = pe.includesItem.key "
            + "and pe.process.key in (?)";

    /**
     * Query to find all the times when this individual user has performed a
     * PROBE task during the enactment of a given process. The rows returned
     * will identify the key and identifier for the root nodes of those
     * enactments.
     */
    private static final String PERSONAL_ENACTMENT_QUERY = //
    "select distinct pe.rootItem.key, pe.rootItem.identifier "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join task.planItem.phase.mapsToPhase probePhase "
            + "where pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 "
            + "and probePhase.identifier = '*PROBE*/PROBE' "
            + "and pe.process.key in (?)";

    private static final int ENACTMENT_ROOT_KEY = 0;

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
            return !onlyForProject.equals(projectID);
        else if (onlyForProjectsThrough != null)
            return projectID.compareTo(onlyForProjectsThrough) > 0;
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

        Set completedEnactmentRootKeys = new HashSet(query(
            ENACTMENT_COMPLETION_QUERY, getEnactmentRootKeys(),
            getIncludedWorkflowKeys(), getWorkflowKey()));

        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] enactment = (Object[]) i.next();
            Object oneEnactmentRootKey = enactment[ENACTMENT_ROOT_KEY];
            if (!completedEnactmentRootKeys.contains(oneEnactmentRootKey))
                i.remove();
        }
    }

    /**
     * Query to determine which workflows are 100% complete
     */
    private static final String ENACTMENT_COMPLETION_QUERY = //
    "select pe.rootItem.key " //
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) "
            + "and pe.process.key in (?) "
            + "and mapsTo.process.key = ? "
            + "and pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.rootItem.key "
            + "having max(task.actualCompletionDateDim.key) < 99990000";

    /**
     * Print a list of the plan items whose data has contributed to this
     * historical data set.
     */
    public void debugPrintEnactments() {
        System.out.println("Process enactment steps for " + workflowName + ":");
        List<Object[]> enactmentSteps = query.queryHql(ENACTMENT_DEBUG_QUERY,
            getEnactmentRootKeys(), getIncludedWorkflowKeys(), getWorkflowKey());
        while (!enactmentSteps.isEmpty()) {
            Object[] oneRow = enactmentSteps.get(0);
            Integer rootKey = (Integer) oneRow[0];
            Object rootPlanItem = oneRow[1];
            String rootPath = rootPlanItem.toString();
            int rootPathLen = rootPath.length();

            List<String> includedPaths = new ArrayList<String>();
            for (Iterator i = enactmentSteps.iterator(); i.hasNext();) {
                Object[] includedRow = (Object[]) i.next();
                Integer includedRootKey = (Integer) includedRow[0];
                if (!includedRootKey.equals(rootKey))
                    continue;

                String includedPath = includedRow[2].toString();
                if (includedPath.startsWith(rootPath))
                    includedPath = includedPath.substring(rootPathLen + 1);
                includedPaths.add(includedPath);
                i.remove();
            }

            System.out.println(rootPath + " " + includedPaths);
        }
        System.out.println("-------------------------------------");
    }

    private static final String ENACTMENT_DEBUG_QUERY = //
    "select pe.rootItem.key, pe.rootItem, pe.includesItem "
            + "from ProcessEnactment pe "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and pe.includesItem.key <> pe.rootItem.key "
            + "and mapsTo.process.key = ? " //
            + "order by pe.rootItem.key";

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
        Object[] row = QueryUtils.singleValue(query(WORKFLOW_KEY_QUERY,
            workflowID));
        workflowKey = (Integer) row[0];
        workflowName = (String) row[1];

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
    "select p.key, p.name from Process p where p.identifier = ?";

    private static final String WORKFLOW_MAPPING_QUERY = //
    "select distinct p.process.key from Phase p " //
            + "join p.mapsToPhase mapsTo " //
            + "where mapsTo.process.key = ?";


    private List<String> getWorkflowSteps() {
        return query(WORKFLOW_PHASE_QUERY, getWorkflowKey());
    }

    private Map<Integer, String> getWorkflowStepsByKey() {
        String query = "select phase.key," + WORKFLOW_PHASE_QUERY.substring(6);
        return QueryUtils.mapColumns(query(query, getWorkflowKey()));
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

        List<Object[]> rawData = query(TIME_IN_PHASE_QUERY,
            getEnactmentRootKeys(), getIncludedWorkflowKeys(), getWorkflowKey());
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
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and mapsTo.process.key = ? "
            + "and pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by mapsTo.shortName, mapsTo.typeName " //
            + "order by mapsTo.shortName";



    public Map<String, DataPair> getAddedAndModifiedSizes() {
        Map<String, DataPair> result = new TreeMap();

        List<Object[]> rawData = query(SIZE_QUERY, getEnactmentRootKeys(),
            getIncludedWorkflowKeys());
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
            + "from ProcessEnactment pe, SizeFact size "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and pe.includesItem.key = size.planItem.key "
            + "and size.versionInfo.current = 1 "
            + "group by size.sizeMetric.shortName, size.measurementType.name";



    /**
     * Retrieve the actual defects injected and removed by workflow phase.
     * 
     * @return an array with three elements. The element at position 0 contains
     *         the injected defects by phase, and the element at position 1
     *         contains the removed defects. Although the Map values are
     *         {@link DataPair} objects, only their "actual" value will contain
     *         data; the "plan" value will be zero. The two maps will contain
     *         entries for "Before Development" and "After Development", in
     *         addition to a Total row. The Map in position 2 of the result will
     *         contain a single entry, with the Total key and a DataPair whose
     *         actual field gives the number of unrecognized defects (whose
     *         injection and removed phases are both not part of this workflow).
     */
    public Map<String, DataPair>[] getDefectsByPhase() {
        Map<Integer, String> stepMap = getWorkflowStepsByKey();
        Map<String, DataPair>[] result = new Map[3];
        result[INJ] = new LinkedHashMap<String, DataPair>();
        result[REM] = new LinkedHashMap<String, DataPair>();
        result[INJ].put(Defect.BEFORE_DEVELOPMENT, new DataPair());
        for (String step : stepMap.values()) {
            result[INJ].put(step, new DataPair());
            result[REM].put(step, new DataPair());
        }
        result[REM].put(Defect.AFTER_DEVELOPMENT, new DataPair());
        DataPair unrecognized = new DataPair();
        result[UNK] = Collections.singletonMap(TOTAL_PHASE_KEY, unrecognized);
        DataPair total = new DataPair();
        result[INJ].put(TOTAL_PHASE_KEY, total);
        result[REM].put(TOTAL_PHASE_KEY, total);

        List<Object[]> rawData1 = query(DEFECT_QUERY_1, getEnactmentRootKeys(),
            getIncludedWorkflowKeys());
        List<Object[]> rawData2 = query(DEFECT_QUERY_2, getWorkflowKey(),
            getIncludedWorkflowKeys());
        mapMissingPhases(stepMap, rawData1, rawData2);
        addDefectData(result, unrecognized, total, rawData1, stepMap);
        addDefectData(result, unrecognized, total, rawData2, stepMap);

        return result;
    }

    private void mapMissingPhases(Map<Integer, String> stepMap,
            List<Object[]>... defectPhaseLists) {
        Set<Integer> missingKeys = new HashSet<Integer>();
        for (List<Object[]> oneDefectList : defectPhaseLists) {
            for (Object[] oneDefect : oneDefectList) {
                for (Object onePhaseKey : oneDefect) {
                    if (!stepMap.containsKey(onePhaseKey))
                        missingKeys.add((Integer) onePhaseKey);
                }
            }
        }
        if (!missingKeys.isEmpty()) {
            QueryUtils.mapColumns(stepMap,
                query(PHASE_MAP_QUERY, missingKeys, getWorkflowKey()));
        }
    }

    private void addDefectData(Map<String, DataPair>[] result,
            DataPair unrecognized, DataPair total, List<Object[]> rawData,
            Map<Integer, String> stepMap) {
        for (Object[] row : rawData) {
            Integer injPhaseKey = (Integer) row[INJ];
            String injPhaseName = stepMap.get(injPhaseKey);
            Integer remPhaseKey = (Integer) row[REM];
            String remPhaseName = stepMap.get(remPhaseKey);

            // if neither phase was recognized, this is most likely a legacy
            // defect that was recorded against MCF buckets. But even if it
            // wasn't, it is a defect that spanned this workflow process and
            // isn't relevant to our calculations. Disregard it.
            if (injPhaseName == null && remPhaseName == null) {
                unrecognized.actual++;
                continue;
            }

            if (injPhaseName == null)
                injPhaseName = Defect.BEFORE_DEVELOPMENT;
            result[INJ].get(injPhaseName).actual++;

            if (remPhaseName == null)
                remPhaseName = Defect.AFTER_DEVELOPMENT;
            result[REM].get(remPhaseName).actual++;

            total.actual++;
        }
    }

    private static final int INJ = 0, REM = 1, UNK = 2;

    /*
     * Query to find defects logged against one of our included enactments
     */
    private static final String DEFECT_QUERY_1 = "select " //
            + "defect.injectedPhase.key, " //
            + "defect.removedPhase.key " //
            + "from ProcessEnactment pe, DefectLogFact defect " //
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) " //
            + "and pe.includesItem.key = defect.planItem.key " //
            + "and defect.versionInfo.current = 1";

    /*
     * Query to find defects that were injected in one of our workflow phases,
     * but were not logged against an enactment of this workflow
     */
    private static final String DEFECT_QUERY_2 = "select " //
            + "injected.key, " //
            + "defect.removedPhase.key " //
            + "from DefectLogFact as defect " //
            + "join defect.injectedPhase.mapsToPhase injected " //
            + "where injected.process.key = ? " //
            + "and not exists ( " //
            + "    from ProcessEnactment pi " //
            + "    where defect.planItem.key = pi.includesItem.key " //
            + "    and pi.process.key in (?)) "
            + "and defect.versionInfo.current = 1";

    /*
     * Query to find the workflow phases that a set of other phases map to
     */
    private static final String PHASE_MAP_QUERY = "select " //
            + "missingPhase.key, " //
            + "targetPhase.shortName " //
            + "from Phase missingPhase " //
            + "join missingPhase.mapsToPhase targetPhase " //
            + "where missingPhase.key in (?) " //
            + "and targetPhase.process.key = ?";

}
