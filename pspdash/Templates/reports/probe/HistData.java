// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.TinyWebServer;
import pspdash.HTMLUtils;
import pspdash.data.ResultSet;
import pspdash.data.DataRepository;
import pspdash.data.DoubleData;
import pspdash.data.NumberData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;
import pspdash.data.StringData;
import pspdash.data.TagData;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

class HistData {
    ResultSet resultSet;
    String xyURL;
    String subsetPrefix;
    private double productivity, prodStddev;

    /** Create a full set of historical data, including outliers, marked as
     * such.
     */
    public HistData(DataRepository data, String prefix,
                    String subsetPrefixParam) {
        subsetPrefix = getSubsetPrefix(data, prefix, subsetPrefixParam);
        boolean clearOutlierMarks = (subsetPrefixParam != null);

        resultSet = ResultSet.get(data, FOR_PARAM, CONDITIONS, ORDER_BY,
                                  DATA_NAMES, subsetPrefix);
        markExclusions(data, prefix, clearOutlierMarks);
    }

    /** Get the applicable set of historical data which was saved in
     * the [PROBE_LIST] variable.
     */
    public HistData(DataRepository data, String prefix) {
        //subsetPrefix = getSubsetPrefix(data, prefix, params);
        resultSet = ResultSet.get(data, FOR_PROBE_LIST, CONDITIONS, ORDER_BY,
                                  //null, null,
                                  DATA_NAMES, prefix);
        markExclusions(null, null, true);
        calcProductivity();
        xyURL = "../xy.class?for=%5b"+PROBE_LIST_NAME+"%5d";
    }

    // These constants are valid parameter values for getXYDataPoints().
    public static final int EST_OBJ_LOC = 1;
    public static final int EST_NC_LOC  = 2;
    public static final int ACT_NC_LOC  = 3;
    public static final int EST_TIME    = 4;
    public static final int ACT_TIME    = 5;
    public static final int EXCLUDE     = 6;

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

    /** Returns "to date" productivity in LOC/hour. */
    public double getProductivity() { return productivity; }
    public double getProdStddev()   { return prodStddev; }

    private void markExclusions(DataRepository data, String prefix,
                                boolean clearOutlierMarks) {
        SimpleData d = null;
        ListData l = null;
        if (!clearOutlierMarks) {
            String dataName = data.createDataName(prefix, PROBE_LIST_NAME);
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

    private void calcProductivity() {
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
        subsetPrefix = lookupString(data, prefix, SUBSET_PREFIX_NAME);
        if (subsetPrefix != null) return subsetPrefix;

        // finally, use the default subset prefix.
        return DEFAULT_PREFIX;
    }
    private String lookupString(DataRepository data, String prefix, String n) {
        String dataName = data.createDataName(prefix, n);
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null) return null;
        String result = d.format();
        if (result.length() == 0) return null;
        return result;
    }

    public void printDataTable(PrintWriter out) {
        int numRows = resultSet.numRows();
        out.print("<input type=hidden name="+SAVE_TASK_DATA+" value=1>");
        if (numRows == 0) {
            out.print("<p>You do not have any historical data.</p>");
            return;
        }

        out.print("The PROBE calculations will be based upon the "+
                  "following set of historical data:<br>&nbsp;\n"+
                  "<table border style='margin-left:1cm'>"+
                  "<tr><th>Project/Task</th>"+
                  "<th>Estimated Object LOC</th>" +
                  "<th>Estimated New &amp; Changed LOC</th>"+
                  "<th>Actual New &amp; Changed LOC</th>" +
                  "<th>Estimated Hours</th>" +
                  "<th>Actual Hours</th>" +
                  "<th>Exclude?</th></tr>\n");
        for (int r = 1;   r <= resultSet.numRows();   r++) {
            out.print("<tr>");
            out.print("<td nowrap>");
            out.print(HTMLUtils.escapeEntities(resultSet.getRowName(r)));
            for (int c = 1;   c < resultSet.numCols();   c++) {
                out.print("</td><td align=center>");
                out.print(resultSet.format(r, c));
            }
            out.print("<td align=center>\n"+
                      "<input type=hidden name='"+TASK_FIELD+r+"' value='"+
                      HTMLUtils.escapeEntities(resultSet.getRowName(r))+
                      "'>\n"+
                      "<input type=checkbox name='"+EXCLUDE_FIELD+r+"'");
            if (resultSet.getData(r, EXCLUDE) != null)
                out.print(" checked");
            out.print("></td></tr>\n");
        }
        out.print("</table>\n"+
                  "<p style='margin-left:1cm'><font size=-1>"+
                  "<i>(<b>Advanced:</b> if you feel that "+
                  "one or more of the projects in the list above is an "+
                  "&quot;<a href='outlier.htm'"+probe.LINK_ATTRS+
                  "><u>outlier</u></a>,&quot; you may exclude it from the "+
                  "PROBE calculations by checking the appropriate box in the "+
                  "&quot;Exclude&quot; column.  Unless you <b>really</b> "+
                  "understand what you are doing, it is best to leave all "+
                  "the boxes unchecked.)</i></font>\n");
    }

    private static final String TASK_FIELD = "TaskName";
    private static final String EXCLUDE_FIELD = "Exclude";
    private static final String SAVE_TASK_DATA = "SaveTaskData";

    public static void savePostedData(DataRepository data,
                                      String prefix, Map parameters) {
        if (parameters.get(SAVE_TASK_DATA) == null) return;

        // From the posted data, create the effective list of projects.
        ListData probeList = new ListData();
        int r = 1;
        while (true) {
            String taskName = (String) parameters.get(TASK_FIELD+r);
            if (taskName == null) break;
            if (parameters.get(EXCLUDE_FIELD+r) == null)
                probeList.add(taskName);
            r++;
        }

        // save that list to the PROBE_SUBSET for this project.
        String dataName = data.createDataName(prefix, PROBE_LIST_NAME);
        data.putValue(dataName, probeList);
    }

    private static final String PROBE_SUBSET_NAME  = "PROBE_SUBSET";
    private static final String PROBE_LIST_NAME    = "PROBE_LIST";
    static final String SUBSET_PREFIX_NAME =
        "PSP To Date Subset Prefix";
    private static final String DEFAULT_PREFIX = "/To Date/PSP/All";

    private static final String   FOR_PROBE_LIST  = "["+PROBE_LIST_NAME+"]";
    private static final String   FOR_PARAM  = "[Rollup_List]";
    private static final String[] CONDITIONS = { "Completed" };
    private static final String   ORDER_BY   = "Completed";
    private static final String[] DATA_NAMES = {
        "Estimated Object LOC",
        "Estimated New & Changed LOC",
        "New & Changed LOC",
        "[Estimated Time] / 60",       // needed?
        "[Time] / 60",
        "null///null" };               // dummy - generate an extra column.


    public static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    public static String formatNumber(double num) {
        return DoubleData.formatNumber(num);
    }
}
