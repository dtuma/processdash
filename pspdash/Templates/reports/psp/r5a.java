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
import pspdash.data.DoubleData;
import pspdash.data.ResultSet;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import com.jrefinery.chart.JFreeChart;

public class r5a extends CGIChartBase implements DefectAnalyzer.Task {

    // In the following map, keys are defect types (e.g. "Syntax") and
    // values are arrays of integers.
    protected Map defectCounts;

    /** Create a vertical bar chart. */
    public JFreeChart createChart() {
        JFreeChart chart =
            JFreeChart.createVerticalBarChart(data.catDataSource());
        setupCategoryChart(chart);
        return chart;
    }

    /** create the data upon which this chart is based. */
    protected void buildData() {
        initValues();
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           getPrefix(), parameters, this);

        int numRows = defectCounts.size();
        data = new ResultSet(numRows, 1);
        data.setColName(0, "Defect Type");
        data.setColName(1, "Total # Defects");
        Iterator i = defectCounts.keySet().iterator();
        String defectType;

        while (i.hasNext()) {
            defectType = (String) i.next();
            data.setRowName(numRows, defectType);
            data.setData(numRows, 1, new DoubleData(getRow(defectType)[0]));
            numRows--;
        }
        data.sortBy(1, true);
    }

    /** Generate an empty row of the appropriate size */
    private int[] emptyRow() {
        int [] result = new int[1];
        result[0] = 0;
        return result;
    }
    /** Initialize internal data structures to zero */
    private void initValues() {
        defectCounts = new HashMap();
        if (parameters.get("strict") != null)
            for (int i=PSP_DEFECT_TYPES.length; i-->0; )
                getRow(PSP_DEFECT_TYPES[i]);
    }
    /** list of defect types used by the PSP */
    private static final String [] PSP_DEFECT_TYPES = {
        "Documentation", "Syntax", "Build, package", "Assignment",
        "Interface", "Checking", "Data", "Function", "System", "Environment" };

    /** Lookup the row for a defect type - create it if it doesn't exist. */
    private int[] getRow(String defectType) {
        int [] result = (int[]) defectCounts.get(defectType);
        if (result == null)
            defectCounts.put(defectType, result = emptyRow());
        return result;
    }
    /** Increment a defect count for a particular defect type */
    protected void increment(int [] row) { row[0]++; }

    public void analyze(String path, Defect d) {
        increment(getRow(d.defect_type));
    }
}
