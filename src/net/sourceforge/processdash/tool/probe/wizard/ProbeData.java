// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.StringUtils;


public class ProbeData {

    public static final String PROBE_INPUT_METRIC =
        "PROBE_INPUT_SIZE_METRIC_NAME";
    public static final String PROBE_TARGET_METRIC =
        "PROBE_TARGET_SIZE_METRIC_NAME";
    public static final String PROBE_TARGET_TIME_METRIC =
        "PROBE_TARGET_TIME_METRIC_NAME";
    public static final String PROBE_LAST_RUN_PREFIX =
        "PROBE_Last_Run_Value/";

    // These constants are valid parameter values for getXYDataPoints().
    public static final int EST_OBJ_LOC  = 1;
    public static final int EST_NC_LOC = 2;
    public static final int ACT_NC_LOC = 3;
    public static final int EST_TIME        = 4;
    public static final int ACT_TIME        = 5;
    public static final int EXCLUDE         = 6;
    public static final int COMPLETED_DATE  = 7;
    public static final int IDENTIFIER      = 8;
    static final int NUM_COLUMNS = IDENTIFIER;
    /*
     *
    public static final int INPUT_SIZE_EST  = 1;
    public static final int OUTPUT_SIZE_EST = 2;
    public static final int OUTPUT_SIZE_ACT = 3;
    public static final int EST_TIME        = 4;
    public static final int ACT_TIME        = 5;
    public static final int EXCLUDE         = 6;
     */


    private DataRepository data;
    private String prefix;
    private ResultSet resultSet;
    private ProcessUtil processUtil;
    private String subsetPrefix;
    private boolean reportMode;
    private double productivity, prodStddev;

    private String timeDataName = null;
    private String[] dataNames = null;
    private String[] reportNames = null;

    /** Create a full set of historical data, including outliers, marked as
     * such.
     */
    public ProbeData(DataRepository data, String prefix,
                     String subsetPrefixParam) {
        this.data = data;
        this.prefix = prefix;
        this.processUtil = new ProcessUtil(data, prefix);
        this.subsetPrefix = getSubsetPrefix(data, prefix, subsetPrefixParam);
        this.reportMode = false;
        boolean clearOutlierMarks = (subsetPrefixParam != null);
        String[] conditions = shouldOnlyIncludeCompletedProjects(data, prefix)
                ? CONDITIONS : null;

        this.resultSet = new ProbeDatabaseUtil(data, prefix) //
                .loadData(getDataNames(), null);
        if (resultSet == null)
            resultSet = ResultSet.get(data, FOR_PARAM, conditions, ORDER_BY,
                getDataNames(), subsetPrefix);

        fixupResultSetColumnHeaders();
        markExclusions(data, prefix, clearOutlierMarks);
    }

    /** Get the applicable set of historical data which was saved in
     * the [PROBE_LIST] variable, if it has been set.  Otherwise, return
     * the default set of historical data.
     */
    public static ProbeData getEffectiveData(DataRepository data, String prefix) {
        String forListDataName = DataRepository.createDataName(prefix,
            PROBE_LIST_NAME);
        SaveableData probeList = data.getValue(forListDataName);
        if (probeList == null)
            return new ProbeData(data, prefix, (String) null);
        else
            return new ProbeData(data, prefix, ListData.asListData(probeList));
    }

    private ProbeData(DataRepository data, String prefix, ListData probeList) {
        this.data = data;
        this.prefix = prefix;
        this.processUtil = new ProcessUtil(data, prefix);
        //subsetPrefix = getSubsetPrefix(data, prefix, params);
        String[] conditions = shouldOnlyIncludeCompletedProjects(data, prefix)
                ? CONDITIONS : null;

        this.resultSet = new ProbeDatabaseUtil(data, prefix) //
                .loadData(getDataNames(), probeList);
        if (resultSet == null)
            resultSet = ResultSet.get(data, FOR_PROBE_LIST, conditions,
                ORDER_BY, getDataNames(), prefix);

        this.reportMode = false;
        fixupResultSetColumnHeaders();
        markExclusions(null, null, true);
        calcProductivity();
    }



    public Vector getXYDataPoints(int xCol, int yCol) {
        Vector dataPoints = new Vector();
        int numRows = resultSet.numRows();
        for (int i = 1;   i <= numRows;   i++) {
            if (resultSet.getData(i, EXCLUDE) != null) continue;
            double[] dataPoint = new double[2];
            dataPoint[0] = asNumber(resultSet.getData(i, xCol));
            dataPoint[1] = asNumber(resultSet.getData(i, yCol));
            if (!badDouble(dataPoint[0]) && dataPoint[0] != 0 &&
                !badDouble(dataPoint[1]) && dataPoint[1] != 0)
                dataPoints.add(dataPoint);
        }
        return dataPoints;
    }

    public String getRowId(int row) {
        String result = resultSet.format(row, IDENTIFIER);
        if (!StringUtils.hasValue(result))
            result = resultSet.getRowName(row);
        return result;
    }

    public boolean isDatabaseMode() {
        return resultSet instanceof ProbeDatabaseResultSet;
    }

    public String getDatabaseWorkflowName() {
        if (isDatabaseMode())
            return ((ProbeDatabaseResultSet) resultSet).getWorkflowName();
        else
            return null;
    }

    public boolean isReportMode() {
        return reportMode;
    }

    public void setReportMode(boolean reportMode) {
        this.reportMode = reportMode;
    }

    /** Returns the value for a particular element in the current project */
    public double getCurrentValue(int col) {
        if (isReportMode()) {
            double result = getLastRunValue(col);
            if (!Double.isNaN(result))
                return result;
        }

        String elem = dataNames[col-1];
        double result = evaluateDataValue(elem);

        // the "estimated time" element in a database-driven project is
        // initially empty. Detect this scenario and sum up the estimated time
        // for all the tasks in the project.
        if (Double.isNaN(result) && col == EST_TIME && isDatabaseMode())
            result = new ProbeDatabaseUtil(data, prefix)
                    .getCurrentEstimatedWorkflowTime() / 60;

        return result;
    }

    private double getLastRunValue(int col) {
        String reportElem = reportNames[col-1];
        if (reportElem == null)
            return Double.NaN;
        else
            return evaluateDataValue(reportElem);
    }

    private double evaluateDataValue(String elem) {
        try {
            if (elem.indexOf('[') == -1)
                return asNumber(data.getSimpleValue
                                (DataRepository.createDataName(prefix, elem)));
            else
                return asNumber(data.evaluate(elem, prefix));
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    public void saveLastRunValues() {
        saveLastRunValue(EST_OBJ_LOC);
        saveLastRunValue(EST_NC_LOC);
        saveLastRunValue(EST_TIME);
    }

    private void saveLastRunValue(int col) {
        String elem = getDataName(col, true);
        String dataName = DataRepository.createDataName(prefix, elem);
        SimpleData sd = data.getSimpleValue(dataName);
        dataName = DataRepository.createDataName(prefix,
            PROBE_LAST_RUN_PREFIX + elem);
        data.userPutValue(dataName, sd);
    }

    /** Returns "to date" productivity in LOC/hour. */
    public double getProductivity() { return productivity; }
    public double getProdStddev()   { return prodStddev; }
    public ProcessUtil getProcessUtil() { return processUtil; }
    public ResultSet getResultSet() { return resultSet; }

    public void discardExcludedProjectsOnOrAfter(Date cutoff) {
        for (int row = resultSet.numRows();  row > 0;  row--) {
            // if this row represents data for a project that was NOT excluded,
            // don't even think about deleting it.
            if (resultSet.getData(row, EXCLUDE) == null)
                continue;

            // get the completion date for the project represented by the
            // current row
            Date completed = DateData.valueOf(resultSet.getData(row,
                    COMPLETED_DATE));

            // if this row is for a project that was completed on or after
            // the given date, discard it.
            if (completed != null && completed.compareTo(cutoff) >= 0)
                resultSet.removeRow(row);
        }
    }

    private void markExclusions(DataRepository data, String prefix,
                                boolean clearOutlierMarks) {
        SimpleData d = null;
        ListData l = null;
        if (!clearOutlierMarks) {
            String dataName = DataRepository.createDataName(prefix, PROBE_LIST_NAME);
            d = data.getSimpleValue(dataName);
            if (d instanceof ListData) l = (ListData) d;
            else if (d instanceof StringData) l = ((StringData) d).asList();
        }

        if (clearOutlierMarks || l == null) {
            for (int row = resultSet.numRows();   row > 0;   row--)
                resultSet.setData(row, EXCLUDE, null);

        } else {
            for (int row = resultSet.numRows();   row > 0;   row--) {
                prefix = getRowId(row);
                resultSet.setData
                    (row, EXCLUDE,
                     l.contains(prefix) ? null : TagData.getInstance());
            }
        }
    }

    private String[] getDataNames() {
        if (dataNames == null) {
            timeDataName = processUtil
                    .getProcessString(PROBE_TARGET_TIME_METRIC);
            if (!StringUtils.hasValue(timeDataName))
                timeDataName = "Time";

            String[] result = new String[NUM_COLUMNS];
            result[EST_OBJ_LOC - 1] =
                processUtil.getProcessString(PROBE_INPUT_METRIC);
            String targetMetric =
                processUtil.getProcessString(PROBE_TARGET_METRIC);
            result[EST_NC_LOC - 1] = "Estimated " + targetMetric;
            result[ACT_NC_LOC - 1] = targetMetric;
            result[EST_TIME - 1] = "[Estimated " + timeDataName + "] / 60";
            result[ACT_TIME - 1] = "[" + timeDataName + "] / 60";

            // dummy - generate an empty column for exclusion data.
            result[EXCLUDE - 1] = "null///null";
            result[COMPLETED_DATE - 1] = "Completed";
            result[IDENTIFIER - 1] = "null///null";
            dataNames = result;

            result = new String[NUM_COLUMNS];
            result[EST_OBJ_LOC - 1] =
                PROBE_LAST_RUN_PREFIX + dataNames[EST_OBJ_LOC - 1];
            result[EST_NC_LOC - 1] =
                PROBE_LAST_RUN_PREFIX + dataNames[EST_NC_LOC - 1];
            result[EST_TIME - 1] = "[" + PROBE_LAST_RUN_PREFIX
                    + "Estimated " + timeDataName + "] / 60";
            reportNames = result;
        }
        return dataNames;
    }

    public String getDataName(int col, boolean fix) {
        String[] names = getDataNames();
        if (fix && col == EST_TIME) return "Estimated " + timeDataName;
        if (fix && col == ACT_TIME) return timeDataName;
        return names[col - 1];
    }

    private void fixupResultSetColumnHeaders() {
        // In the new PSP materials, the Added & Modified metric typically
        // appears with the adjective "Planned" instead of "Estimated"
        // (probably to reduce student confusion over the "P" and "E" metrics).
        // Internally, our metrics all begin with the word "Estimated" - so
        // here we tweak the display name.  Note that this is an English-only
        // problem, so no internationalized support is necessary.
        String colName = resultSet.getColName(EST_NC_LOC);
        if (colName.startsWith("Estimated Added & Modified ")) {
            colName = "Planned" + colName.substring(9);
            resultSet.setColName(EST_NC_LOC, colName);
        }

        // Here, we prepend the word "Actual" to the added & modified metric.
        // This provides consistency with the comparable "Hours" metrics.
        colName = resultSet.getColName(ACT_NC_LOC);
        if (colName.startsWith("Added & Modified ")) {
            colName = "Actual " + colName;
            resultSet.setColName(ACT_NC_LOC, colName);
        }

        resultSet.setColName(EST_TIME, "Estimated Hours");
        resultSet.setColName(ACT_TIME, "Actual Hours");
        resultSet.setColName(EXCLUDE, "Exclude?");
    }

    void calcProductivity() {
        double loc = 0, hours = 0;
        double[] dataPoint;
        Vector v = getXYDataPoints(ACT_NC_LOC, ACT_TIME);
        Iterator i = v.iterator();
        while (i.hasNext()) {
            dataPoint = (double[]) i.next();
            loc += dataPoint[0];
            hours += dataPoint[1];
        }
        productivity = loc / hours;

        // Calculate the variance of productivity from the "To Date" value
        double variance = 0, point;
        i = v.iterator();
        while (i.hasNext()) {
            dataPoint = (double[]) i.next();
            point = dataPoint[0] / dataPoint[1];
            point = productivity - point;
            variance += point * point;
        }
        variance = variance / (v.size() - 1);
        prodStddev = Math.sqrt(variance);
    }

    private double asNumber(Object o) {
        if (o instanceof NumberData)
            return ((NumberData) o).getDouble();
        else
            return Double.NaN;
    }

    private String getSubsetPrefix(DataRepository data, String prefix,
                                   String subsetPrefixParam) {
        // if a subset prefix was included in the query parameters for
        // this HTTP request, use it unequivocally.
        subsetPrefix = subsetPrefixParam;
        if (subsetPrefix != null && subsetPrefix.length() != 0)
            return subsetPrefix;

        // next, check to see if the user has saved a PROBE subset prefix.
        subsetPrefix = lookupString(data, prefix, PROBE_SUBSET_NAME);
        if (subsetPrefix != null) return subsetPrefix;

        // next, use the general subset prefix set for the project.
        String rollupID = processUtil.getRollupID();
        String subsetPrefixName = StringUtils.findAndReplace
            (SUBSET_PREFIX_NAME, "PID", rollupID);
        subsetPrefix = lookupString(data, prefix, subsetPrefixName);
        if (subsetPrefix != null) return subsetPrefix;

        // finally, use the default subset prefix.
        return StringUtils.findAndReplace(DEFAULT_PREFIX, "PID", rollupID);
    }

    private String lookupString(DataRepository data, String prefix, String n) {
        String dataName = DataRepository.createDataName(prefix, n);
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null) return null;
        String result = d.format();
        if (result.length() == 0) return null;
        return result;
    }

    private boolean shouldOnlyIncludeCompletedProjects(DataRepository data,
            String prefix) {
        String dataName = DataRepository.createDataName(prefix,
            PROBE_ONLY_COMPLETED_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        if (d != null && d.test() == false)
            return false;
        else
            return true;
    }

    public String storeChartData(ResultSet chartData, ProbeMethod method) {
        String dataName = "PROBE_Chart_Data///" + method.getMethodLetter()
                + "_" + method.methodPurpose.getKey();
        ListData l = new ListData();
        l.add(chartData);
        data.putValue(DataRepository.createDataName(prefix, dataName), l);
        return dataName;
    }

    private static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }


    private static final String PROBE_SUBSET_NAME  = "PROBE_SUBSET";
    private static final String PROBE_ONLY_COMPLETED_NAME = "PROBE_ONLY_COMPLETED";
    public static final String PROBE_LIST_NAME     = "PROBE_LIST";
    static final String SUBSET_PREFIX_NAME =
        "PID To Date Subset Prefix";
    private static final String DEFAULT_PREFIX = "/To Date/PID/All";

    private static final String   FOR_PROBE_LIST  = "["+PROBE_LIST_NAME+"]";
    private static final String   FOR_PARAM  = "[Rollup_List]";
    private static final String[] CONDITIONS = { "Completed" };
    private static final String   ORDER_BY   = "Completed";

}
