// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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


package net.sourceforge.processdash.ui.web.reports.analysis;


import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.Defect;
import net.sourceforge.processdash.log.DefectAnalyzer;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;



public class Report5 extends CGIChartBase implements DefectAnalyzer.Task {

    private static final Resources resources =
        Resources.getDashBundle("Analysis.Pareto");
    private static final String PATH_TO_REPORTS = AnalysisPage.PATH_TO_REPORTS;
    private NumberFormat nf = NumberFormat.getNumberInstance();

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
        if (getParameter("imgType") != null)
            writeImgTag();
        else if (getParameter("type") == null)
            writeHtmlContents();
        else
            super.writeContents();
    }

    private void writeImgTag() {
        if ("excel".equals(parameters.get("EXPORT")))
            return;

        String query = (String) env.get("QUERY_STRING");
        query = StringUtils.findAndReplace(query, "imgType=", "type=");

        out.print("<p><img width='500' height='400' src=\"");
        out.print(getScriptName());
        out.print("?");
        out.print(HTMLUtils.escapeEntities(query));
        out.print("&categoryLabels=vertical&width=500&height=400\"></p>\n\n");
    }

    private static final String HTML_HEADER =
        "<HTML><head>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<title>${Title_Long}</title>\n" +
        "</head><BODY>\n" +
        "<H1>%PATH%</H1>\n";
    private static final String TITLE_TEXT =
        "<H2>${Title_Long}</H2>\n";

    private void writeHtmlContents() {
        if (!parameters.containsKey(AnalysisPage.INCLUDABLE_PARAM)) {
            String text = HTML_HEADER;
            text = StringUtils.findAndReplace
                (text, "%PATH%",
                 HTMLUtils.escapeEntities
                    (AnalysisPage.localizePrefix(getPrefix())));
            out.print(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
        }
        out.print(resources.interpolate(TITLE_TEXT, HTMLUtils.ESC_ENTITIES));

        boolean strict = parameters.containsKey("strict");
        boolean nokids = parameters.containsKey(DefectAnalyzer.NO_CHILDREN_PARAM);

        for (int i = 0;   i < PARAM_FLAG.length;   i++) {
            out.print("<P><IMG WIDTH=500 HEIGHT=400 SRC=\"");
            out.print(getScriptName());
            out.print("?type=");
            out.print(PARAM_FLAG[i]);
            if (strict) out.print("&strict");
            if (nokids) out.print("&" + DefectAnalyzer.NO_CHILDREN_PARAM);
            out.print("&qf="+PATH_TO_REPORTS+"compProj.rpt");
            out.print("&categoryLabels=vertical&width=500&height=400\"></P>");
        }

        if (!parameters.containsKey(AnalysisPage.INCLUDABLE_PARAM))
            out.print("</BODY></HTML>");
    }

    private String getScriptName() {
        String scriptName = (String) env.get("SCRIPT_NAME");
        int slashPos = scriptName.lastIndexOf('/');
        if (slashPos != -1) scriptName = scriptName.substring(slashPos + 1);
        return scriptName;
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
        DefectAnalyzer.refineParams(parameters, getDataContext());
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           getPrefix(), parameters, this);

        int numRows = defectData.size();
        data = new ResultSet(numRows, 1);
        data.setColName(0, resources.getString("Defect_Type"));
        data.setColName(1, resources.getString(KEYS[reportType] + "_Axis"));
        if (parameters.get("title") == null)
            parameters.put
                ("title", resources.getString(KEYS[reportType] + "_Title"));
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
            increment(row, nf.parse(data).floatValue());
        } catch (ParseException nfe) {}
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
