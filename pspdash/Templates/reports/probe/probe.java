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

import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.Correlation;
import pspdash.data.ListData;
import pspdash.data.LinearRegression;
import pspdash.data.NumberData;
import pspdash.data.SaveableData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class probe extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String prefix = getPrefix();

        // Get the estimated object loc which the user entered on
        // their Size Estimating Template
        double estObjLOC = getEstObjLOC(prefix);
        if (badDouble(estObjLOC) || estObjLOC == 0) {
            printNeedSizeEstimatingTemplate();
            return;
        }

        HistData data = new HistData(getDataRepository(), prefix);
        printHeader(prefix, data, estObjLOC);
        printSizeSection(data, estObjLOC);
        printTimeSection(data, estObjLOC);
        printFooter();
    }

    protected double getEstObjLOC(String prefix) {
        DataRepository data = getDataRepository();
        String dataName = data.createDataName(prefix, "Estimated Object LOC");
        System.out.println("data=" + data + ", prefix="+prefix+", dataname="+dataName);
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof NumberData)
            return ((NumberData) d).getDouble();
        else
            return Double.NaN;
    }

    protected void printNeedSizeEstimatingTemplate() {
        out.print("Need Size Estimating Template.");
    }

    private static final String HEADER_HTML =
        "<html><head><title>PROBE Report</title>\n"+
        "<script>function popup() {\n"+
        "   var newWin = "+
        "       window.open('','popup','width=430,height=330,dependent=1');\n"+
        "   newWin.focus();\n"+
        "}</script>\n"+
        "</head><body><h1>PROBE Report</h1>\n";

    protected void printHeader(String prefix, HistData data, double estObjLOC){
        out.print(HEADER_HTML);
        out.print("<h2>" + prefix + "</h2>\n");
        printDataTable(data);
        printEstObjLOC(prefix, estObjLOC);
        out.print("<form action='probe.class' method=post>");
    }

    protected void printDataTable(HistData data) {
        if (data.resultSet.numRows() == 0) {
            out.print("<p>You do not have any historical data.</p>");
            return;
        }
        out.print("The calculations in the report below are based upon the "+
                  "following set of historical data:\n"+
                  "<table border style='margin-left:1cm'>"+
                  "<tr><th>Project/Task</th>"+
                  "<th>Estimated Object LOC</th>" +
                  "<th>Estimated New &amp; Changed LOC</th>"+
                  "<th>Actual New &amp; Changed LOC</th>" +
                  "<th>Estimated Hours</th>" +
                  "<th>Actual Hours</th></tr>\n");
        for (int r = 1;   r <= data.resultSet.numRows();   r++) {
            out.print("<tr>");
            out.print("<td nowrap>");
            out.print(HTMLUtils.escapeEntities(data.resultSet.getRowName(r)));
            for (int c = 1;   c <= data.resultSet.numCols();   c++) {
                out.print("</td><td align=center>");
                out.print(data.resultSet.format(r, c));
            }
            out.print("</td></tr>\n");
        }
        out.print("</table>\n");
    }
    protected void printEstObjLOC(String prefix, double estObjLOC) {
        out.print("<p>From your Size Estimating Template, your Estimated "+
                  "Object LOC is <tt><b>");
        out.print(HistData.formatNumber(estObjLOC));
        out.print("</b></tt>.<p><i>(If you want to alter your Estimated "+
                  "Object LOC, you should do so now before using the report "+
                  "below.)</i>");
    }

    protected void printSizeSection(HistData data, double estObjLOC) {

        out.print("<hr><h2>Size</h2><b>To create your final size estimate,\n" +
                  "please choose from the following PROBE "+
                  "methods:</b><br><br>\n");

        // Calculate data for each of the PROBE methods for size.
        ArrayList sizeMethods = new ArrayList();
        sizeMethods.add(new RegressionMethod (data, estObjLOC, EST_OBJ,
                                              ACT_NC, "A", "size"));
        sizeMethods.add(new RegressionMethod (data, estObjLOC, EST_NC,
                                              ACT_NC, "B", "size"));
        sizeMethods.add(new AveragingMethod  (data, estObjLOC, EST_OBJ,
                                              ACT_NC, "C1", "size"));
        sizeMethods.add(new AveragingMethod  (data, estObjLOC, EST_NC,
                                              ACT_NC, "C2", "size"));
        sizeMethods.add(new MethodD (data, estObjLOC, "size"));

        printMethods(sizeMethods);
    }

    protected void printTimeSection(HistData data, double estObjLOC) {

        out.print("<hr><h2>Time</h2><b>To create your time estimate,\n" +
                  "please choose from the following PROBE methods:</b>" +
                  "<br><br>\n");

        // Calculate data for each of the PROBE methods for time.
        ArrayList timeMethods = new ArrayList();
        timeMethods.add(new RegressionMethod (data, estObjLOC, EST_OBJ,
                                              ACT_TIME, "A", "time"));
        timeMethods.add(new RegressionMethod (data, estObjLOC, EST_NC,
                                              ACT_TIME, "B", "time"));
        timeMethods.add(new AveragingMethod  (data, estObjLOC, EST_OBJ,
                                              ACT_TIME, "C1", "time"));
        timeMethods.add(new AveragingMethod  (data, estObjLOC, EST_NC,
                                              ACT_TIME, "C2", "time"));
        timeMethods.add(new AveragingMethod  (data, estObjLOC, ACT_NC,
                                              ACT_TIME, "C3", "time") {
                public double getRating() {
                    observations.clear();
                    return (rating < 0.0 ? rating
                            : Method.PROBE_METHOD_D + 0.00001); } });
        timeMethods.add(new MethodD (data, estObjLOC, "time"));

        printMethods(timeMethods);
    }

    protected void printMethods(ArrayList methods) {

        out.print("<table>\n");

        Collections.sort(methods);
        Iterator i = methods.iterator();
        boolean isBest = true;
        while (i.hasNext()) {
            ((Method) i.next()).printRow(out, isBest);
            isBest = false;
            if (i.hasNext()) out.print(DIVIDER);
        }

        out.print("</table>\n\n\n");
    }
    protected void printFooter() {
        out.print("</form></body></html>");
    }
    private boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    private static final String DIVIDER =
        "<tr><td></td><td bgcolor='#c0c0c0'>" +
        "<img src='line.png' width=1 height=1></td><td></td></tr>\n";


    public static final int EST_OBJ  = HistData.EST_OBJ_LOC;
    public static final int EST_NC   = HistData.EST_NC_LOC;
    public static final int ACT_NC   = HistData.ACT_NC_LOC;
    public static final int EST_TIME = HistData.EST_TIME;
    public static final int ACT_TIME = HistData.ACT_TIME;

}

/*

Design:

1) Get the prefix.

2) check to see if the user has filled out the size estimating
    template. If not, chastise them.

3) Get the list of historical data (est/act N&C LOC, est obj loc, act time)
    if there is no historical data, display a polite message explaining that
    PROBE only works with historical data.

4) display a report header showing a table of their historical data,
    and their current estimated obj loc which serves as the input.

5) Perform PROBE analyses on various columns. Try to rank the various
    PROBE techniques based on an intelligent assessment of their validity.

Should we do size, then do time? Or should we display both on one report?

Display both



Design: have an inner class which encapsulates a probe method.
    Constructor: (historical data set, est obj loc)

    Methods:
        getRating()  - returns a number which rates this method in terms
            of










**/
