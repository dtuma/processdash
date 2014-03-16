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

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.DatabasePluginUtils;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;

public class ProbeDatabaseUtil {

    private static final String USES_DATABASE_TAG = "PROBE_USE_DATABASE_FOR_HIST_DATA";

    private DataRepository data;

    private String prefix;

    private String[] columnHeaders;

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

        this.columnHeaders = columnHeaders;
        if (onlyInclude != null)
            this.onlyInclude = new HashSet(onlyInclude.asList());

        String workflowName = "";
        try {
            getQueryRunner();
            workflowName = getWorkflowName();
            List enactmentList = getEnactments(workflowName);
            if (!enactmentList.isEmpty())
                return buildResultSet(workflowName, enactmentList);
        } catch (QueryError qe) {
        }
        return createResultSet(workflowName, 0);
    }

    private void getQueryRunner() {
        plugin = require(QueryUtils.getDatabasePlugin(data, true));
        query = require(plugin.getObject(QueryRunner.class));
    }

    private String getWorkflowName() {
        String projectID = require(getValue("Project_ID", true));
        String workflowID = require(getValue("Workflow_Source_ID", false));
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

    private ProbeDatabaseResultSet buildResultSet(String workflowName,
            List enactmentList) {
        // retrieve time and completion date info for each enactment.
        List<Integer> enactmentKeys = QueryUtils.pluckColumn(enactmentList, 0);
        List taskStatus = query.queryHql(TASK_STATUS_QUERY, enactmentKeys);

        // the task status query will only return results for enactments
        // that were 100% complete. We only need data for those enactments.
        enactmentKeys = QueryUtils.pluckColumn(taskStatus, 0);

        // Create an empty result set to hold the data.
        ProbeDatabaseResultSet result = createResultSet(workflowName,
            enactmentKeys.size());
        if (enactmentKeys.isEmpty())
            return result;

        // build maps to look up data more conveniently
        Map rootKeys = QueryUtils.mapColumns(enactmentList, 0, 1);
        Map rootIDs = QueryUtils.mapColumns(enactmentList, 0, 2);
        Map<Integer, String> rootPaths = DatabasePluginUtils
                .getDashPathsForPlanItems(query, rootKeys.values());

        // iterate over task status info and store it into the result set.
        for (int i = taskStatus.size(); i-- > 0;) {
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
        String sizeUnits = require(getValue("Size Units", false));
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

    /** Create a result set for the data, with a given number of rows */
    private ProbeDatabaseResultSet createResultSet(String workflowName,
            int numRows) {
        return new ProbeDatabaseResultSet(numRows, columnHeaders, workflowName);
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


    private boolean isEnabled() {
        return getValue(USES_DATABASE_TAG, false) != null;
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

    private String getValue(String dataName, boolean inherit) {
        SaveableData d;
        if (inherit)
            d = data.getInheritableValue(prefix, dataName);
        else
            d = data.getValue(DataRepository.createDataName(prefix, dataName));
        if (d == null)
            return null;
        SimpleData sd = d.getSimpleValue();
        return (sd == null ? null : sd.format());
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
            + "from ProcessEnactment pe, ProcessEnactment pi, "
            + "TaskStatusFact task " //
            + "where pe.key in (?) " //
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key "
            + "and pi.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.key "
            + "having max(task.actualCompletionDateDim.key) < 99990000 "
            + "order by max(task.actualCompletionDate)";

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

    private class QueryError extends RuntimeException {
    }

}
