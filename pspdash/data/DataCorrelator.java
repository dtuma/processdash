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

package pspdash.data;


import java.util.Enumeration;
import java.util.Vector;
import java.lang.Double;

import pspdash.Settings;

public class DataCorrelator {

    private DataRepository data;
    private String xName, yName;
    private Vector dataPoints;
    private Vector theFilter;
    public Vector dataNames;
    public LinearRegression r;
    public Correlation c;

    public DataCorrelator(DataRepository data, String xName, String yName,
                          Vector filter) {
        this.data  = data;
        this.xName = xName;
        this.yName = yName;
        this.theFilter = filter;

        recalc();
    }

    public Vector getDataNames () {
        return dataNames;
    }

    public Vector getDataPoints () {
        return dataPoints;
    }

    public void recalc() {
        scanRepository();
        r = new LinearRegression(dataPoints);
        c = new Correlation(dataPoints);
    }

    private boolean matchesFilter (String name) {
        if (theFilter == null)
            return true;
        for (int ii = 0; ii < theFilter.size(); ii++) {
            if (name.startsWith((String)theFilter.elementAt(ii)))
                return true;
        }
        return false;
    }

    private void scanRepository() {
        Enumeration keys = data.keys();
        String name, xFullName, prefix, yFullName, completedFullName;
        int prefixPos;
        DoubleData x, y;
        DateData completed;
        double[] dataPoint;

        dataPoints = new Vector();
        dataNames = new Vector();

        while (keys.hasMoreElements()) {
            name = (String)keys.nextElement();
            if (name.endsWith(xName) && matchesFilter(name)) try {
                prefix = name.substring(0, name.length() - xName.length());
                xFullName = name;
                yFullName = prefix + yName;
                completedFullName = prefix + "Completed";

                y = (DoubleData) data.getValue(yFullName);
                x = (DoubleData) data.getValue(xFullName);
                completed = (DateData) data.getValue(completedFullName);

                if (x != null && !Double.isNaN(x.getDouble()) &&
                    y != null && !Double.isNaN(y.getDouble()) &&
                    (completed != null || onlyCompleted() == false)) {

                    dataPoint = new double[] {x.getDouble(), y.getDouble()};
                    dataPoints.addElement(dataPoint);
                    dataNames.addElement(prefix);
                }
            } catch (Exception e) {};
        }
    }

    private static Boolean onlyCompleted = null;
    private static boolean onlyCompleted() {
        if (onlyCompleted == null)
            onlyCompleted =
                new Boolean(Settings.getVal("probeDialog.onlyCompleted"));

        return onlyCompleted.booleanValue();
    }

}
