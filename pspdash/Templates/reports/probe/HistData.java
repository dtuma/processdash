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
import pspdash.data.ResultSet;
import pspdash.data.DataRepository;
import pspdash.data.DoubleData;
import pspdash.data.NumberData;
import pspdash.data.SimpleData;

import java.util.Iterator;
import java.util.Vector;

class HistData {
    ResultSet resultSet;
    String subsetPrefix;
    String xyURL;
    double productivity, prodStddev;

    public HistData(DataRepository data, String prefix) {
        subsetPrefix = getSubsetPrefix(data, prefix);
        resultSet = ResultSet.get(data, FOR_PARAM, CONDITIONS, ORDER_BY,
                                  DATA_NAMES, subsetPrefix);
        calcProductivity();
        xyURL = TinyWebServer.urlEncodePath(subsetPrefix) +
            "//reports/xy.class" +
            "?for=%5bRollup_List%5d" +
            "&where=Completed" +
            "&order=Completed";
    }

    // These constants are valid parameter values for getXYDataPoints().
    public static final int EST_OBJ_LOC = 1;
    public static final int EST_NC_LOC  = 2;
    public static final int ACT_NC_LOC  = 3;
    public static final int EST_TIME    = 4;
    public static final int ACT_TIME    = 5;

    public Vector getXYDataPoints(int xCol, int yCol) {
        Vector dataPoints = new Vector();
        int numRows = resultSet.numRows();
        for (int i = 1;   i <= numRows;   i++) {
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

    private String getSubsetPrefix(DataRepository data, String prefix) {
        String dataName = data.createDataName(prefix, DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        String subsetPrefix = null;
        if (d != null) subsetPrefix = d.format();
        if (subsetPrefix == null || subsetPrefix.length() == 0)
            subsetPrefix = DEFAULT_PREFIX;
        return subsetPrefix;
    }

    private static final String DATA_NAME = "PSP To Date Subset Prefix";
    private static final String DEFAULT_PREFIX = "/To Date/PSP/All";

    private static final String   FOR_PARAM  = "[Rollup_List]";
    private static final String[] CONDITIONS = { "Completed" };
    private static final String   ORDER_BY   = "Completed";
    private static final String[] DATA_NAMES = {
        "Estimated Object LOC",
        "Estimated New & Changed LOC",
        "New & Changed LOC",
        "[Estimated Time] / 60",       // needed?
        "[Time] / 60" };


    public static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    public static String formatNumber(double num) {
        return DoubleData.formatNumber(num);
    }
}
