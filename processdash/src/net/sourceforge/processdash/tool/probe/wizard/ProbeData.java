// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.probe.wizard;


import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
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

    // These constants are valid parameter values for getXYDataPoints().
    public static final int EST_OBJ_LOC  = 1;
    public static final int EST_NC_LOC = 2;
    public static final int ACT_NC_LOC = 3;
    public static final int EST_TIME        = 4;
    public static final int ACT_TIME        = 5;
    public static final int EXCLUDE         = 6;
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
    private double productivity, prodStddev;

    private String[] dataNames = null;

    /** Create a full set of historical data, including outliers, marked as
     * such.
     */
    public ProbeData(DataRepository data, String prefix,
                     String subsetPrefixParam) {
        this.data = data;
        this.prefix = prefix;
        this.processUtil = new ProcessUtil(data, prefix);
        this.subsetPrefix = getSubsetPrefix(data, prefix, subsetPrefixParam);
        boolean clearOutlierMarks = (subsetPrefixParam != null);

        this.resultSet = ResultSet.get(data, FOR_PARAM, CONDITIONS, ORDER_BY,
                                       getDataNames(), subsetPrefix);
        fixupResultSetColumnHeaders();
        markExclusions(data, prefix, clearOutlierMarks);
    }

    /** Get the applicable set of historical data which was saved in
     * the [PROBE_LIST] variable.
     */
    public ProbeData(DataRepository data, String prefix) {
        this.data = data;
        this.prefix = prefix;
        this.processUtil = new ProcessUtil(data, prefix);
        //subsetPrefix = getSubsetPrefix(data, prefix, params);
        this.resultSet = ResultSet.get(data, FOR_PROBE_LIST, CONDITIONS, ORDER_BY,
                                       getDataNames(), prefix);
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

    /** Returns the value for a particular element in the current project */
    public double getCurrentValue(int col) {
        try {
            String elem = dataNames[col-1];
            if (elem.indexOf('[') == -1)
                return asNumber(data.getSimpleValue
                                (DataRepository.createDataName(prefix, elem)));
            else
                return asNumber(data.evaluate(dataNames[col-1], prefix));
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    /** Returns "to date" productivity in LOC/hour. */
    public double getProductivity() { return productivity; }
    public double getProdStddev()   { return prodStddev; }
    public ProcessUtil getProcessUtil() { return processUtil; }
    public ResultSet getResultSet() { return resultSet; }

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
                prefix = resultSet.getRowName(row);
                resultSet.setData
                    (row, EXCLUDE,
                     l.contains(prefix) ? null : TagData.getInstance());
            }
        }
    }

    private String[] getDataNames() {
        if (dataNames == null) {
            String[] result = new String[EXCLUDE];
            result[EST_OBJ_LOC - 1] =
                processUtil.getProcessString(PROBE_INPUT_METRIC);
            result[EST_NC_LOC - 1] =
                "Estimated " + processUtil.getSizeMetric();
            result[ACT_NC_LOC - 1] = processUtil.getSizeMetric();
            result[EST_TIME - 1] = "[Estimated Time] / 60";
            result[ACT_TIME - 1] = "[Time] / 60";

            // dummy - generate an extra column.
            result[EXCLUDE - 1] = "null///null";
            dataNames = result;
        }
        return dataNames;
    }
    public String getDataName(int col, boolean fix) {
        if (fix && col == EST_TIME) return "Estimated Time";
        if (fix && col == ACT_TIME) return "Time";
        else return getDataNames()[col - 1];
    }

    private void fixupResultSetColumnHeaders() {
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

    private static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }


    private static final String PROBE_SUBSET_NAME  = "PROBE_SUBSET";
    public static final String PROBE_LIST_NAME     = "PROBE_LIST";
    static final String SUBSET_PREFIX_NAME =
        "PID To Date Subset Prefix";
    private static final String DEFAULT_PREFIX = "/To Date/PID/All";

    private static final String   FOR_PROBE_LIST  = "["+PROBE_LIST_NAME+"]";
    private static final String   FOR_PARAM  = "[Rollup_List]";
    private static final String[] CONDITIONS = { "Completed" };
    private static final String   ORDER_BY   = "Completed";

}
