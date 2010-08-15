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


package pspdash.reporttemp;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import pspdash.CGIChartBase;
import pspdash.Defect;
import pspdash.DefectAnalyzer;
import pspdash.DefectTypeStandard;
import pspdash.HTMLUtils;
import pspdash.Resources;
import pspdash.StringUtils;
import pspdash.data.DoubleData;
import pspdash.data.ResultSet;



public class Report5 extends CGIChartBase implements DefectAnalyzer.Task {

    private static final Resources resources =
        Resources.getDashBundle("pspdash.Analysis");
    private static final String PATH_TO_REPORTS = AnalysisPage.PATH_TO_REPORTS;

    private static final int DEFECT_COUNT = 0;
    private static final int FIX_TIME = 1;
    private static final String[] PARAM_FLAG = {
        "defectCount",
        "fixTime" };
    private static final String[] KEYS = {
        "Num_Defects",
        "Fix_Time" };


    // In the following map, keys are defect types (e.g. "Syntax") and
    // values are arrays of floats.
    protected Map defectData;

    protected int reportType;

    protected void writeHeader() {
        if (getParameter("type") == null)
            writeHtmlHeader();
        else
            super.writeHeader();
    }

    protected void writeContents() throws IOException {
        if (getParameter("type") == null)
            writeHtmlContents();
        else
            super.writeContents();
    }

    private static final String HTML_HEADER =
        "<HTML><head>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<title>${Pareto.Title_Long}</title>\n" +
        "</head><BODY>\n" +
        "<H1>%PATH%</H1>\n";
    private static final String TITLE_TEXT =
        "<H2>${Pareto.Title_Long}</H2>\n";

    private void writeHtmlContents() {
        if (!parameters.containsKey(AnalysisPage.INCLUDABLE_PARAM)) {
                String text = HTML_HEADER;
                text = StringUtils.findAndReplace
                    (text, "%PATH%", HTMLUtils.escapeEntities(getPrefix()));
                out.print(AnalysisPage.interpolate(text, true));
        }
        out.print(AnalysisPage.interpolate(TITLE_TEXT, true));

        String scriptName = (String) env.get("SCRIPT_NAME");
        int slashPos = scriptName.lastIndexOf('/');
        if (slashPos != -1) scriptName = scriptName.substring(slashPos + 1);
        boolean strict = parameters.containsKey("strict");

        for (int i = 0;   i < PARAM_FLAG.length;   i++) {
            out.print("<P><IMG WIDTH=500 HEIGHT=400 SRC=\"");
            out.print(scriptName);
            out.print("?type=");
            out.print(PARAM_FLAG[i]);
            if (strict) out.print("&strict");
            out.print("&qf="+PATH_TO_REPORTS+"compProj.rpt");
            out.print("&categoryLabels=vertical&width=500&height=400\"></P>");
        }

        if (!parameters.containsKey(AnalysisPage.INCLUDABLE_PARAM))
                out.print("</BODY></HTML>");
    }

    /** Create a vertical bar chart. */
    public JFreeChart createChart() {
        JFreeChart chart = null;
        if (get3DSetting()) {
            chart = ChartFactory.createBarChart3D
                (null, null, null, data.catDataSource(),
                 PlotOrientation.VERTICAL, false, false, false);
            chart.getPlot().setForegroundAlpha(ALPHA);

        } else {
            chart = ChartFactory.createBarChart
                (null, null, null, data.catDataSource(),
                 PlotOrientation.VERTICAL, false, false, false);
        }

        setupCategoryChart(chart);
        return chart;
    }

    /** create the data upon which this chart is based. */
    protected void buildData() {
        initValues();
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           getPrefix(), parameters, this);

        int numRows = defectData.size();
        data = new ResultSet(numRows, 1);
        data.setColName(0, resources.getString("Pareto.Defect_Type"));
        data.setColName(1, resources.getString("Pareto."+KEYS[reportType] + "_Axis"));
        if (parameters.get("title") == null)
            parameters.put
                ("title", resources.getString("Pareto."+KEYS[reportType] + "_Title"));
        Iterator i = defectData.keySet().iterator();
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
    private float[] emptyRow() {
        float [] result = new float[1];
        result[0] = (float) 0.0;
        return result;
    }
    /** Initialize internal data structures to zero */
    private void initValues() {
        String typeParam = getParameter("type");
        reportType = 0;
        for (int i = 0; i < PARAM_FLAG.length; i++)
            if (PARAM_FLAG[i].equals(typeParam)) {
                reportType = i;
                break;
            }

        defectData = new HashMap();
        if (parameters.get("strict") != null) {
            DefectTypeStandard dts =
                DefectTypeStandard.get(getPrefix(), getDataRepository());
            for (int i=dts.options.size();  i-->0; )
                getRow((String) dts.options.elementAt(i));
        }
    }

    /** Lookup the row for a defect type - create it if it doesn't exist. */
    private float[] getRow(String defectType) {
        float [] result = (float[]) defectData.get(defectType);
        if (result == null)
            defectData.put(defectType, result = emptyRow());
        return result;
    }
    /** Increment the data for a particular defect type */
    protected void increment(float [] row, float data) { row[0] += data; }
    protected void increment(float [] row, String data) {
        try {
            row[0] += Float.parseFloat(data);
        } catch (NumberFormatException nfe) {}
    }

    public void analyze(String path, Defect d) {
        switch (reportType) {
            case FIX_TIME:
                increment(getRow(d.defect_type), d.fix_time);
                break;

            case DEFECT_COUNT:
                increment(getRow(d.defect_type), 1.0f);
                break;
        }
    }
}