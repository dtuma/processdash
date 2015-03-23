// Copyright (C) 2001-2012 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui.web.reports.analysis;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.ui.web.reports.DiscChart;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



public class Report5 extends CGIChartBase implements DefectAnalyzer.Task {

    private static final Resources resources =
        Resources.getDashBundle("Analysis.Pareto");
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
        if (parameters.containsKey("snippet"))
            writeSnippet();
        else if (getParameter("type") == null)
            writeHtmlContents();
        else
            super.writeContents();
    }

    private void writeSnippet() throws IOException {
        if ("excel".equals(parameters.get("EXPORT")))
            return;

        String chartStyleParam = "";
        if ("Disc".equals(getParameter("ChartType")))
            chartStyleParam = "&disc";

        out.print(SNIPPET_HEADER);
        for (int i = 0; i < KEYS.length; i++) {
            String paramName = "Show_" + KEYS[i];
            Object paramVal = parameters.get(paramName);
            if (paramVal != null && !"".equals(paramVal)) {
                String imageUri = getScriptName() + "?for=auto&html"
                        + "&width=500&height=400&categoryLabels=vertical"
                        + chartStyleParam + "&type=" + PARAM_FLAG[i];

                String absImageUri = resolveRelativeURI(imageUri);
                String imageHtml = getRequestAsString(absImageUri);
                out.print("<p>");
                out.print(imageHtml);
                out.print("</p>\n\n");
            }
        }
        out.print("</body></html>");
    }

    private static final String SNIPPET_HEADER =
        "<HTML><head>\n" +
        "<script type='text/javascript' src='/lib/overlib.js'></script>\n" +
        "</head><body>\n";

    private static final String HTML_HEADER =
        "<HTML><head>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<script type='text/javascript' src='/lib/overlib.js'></script>\n" +
        "<title>${Title_Long}</title>\n" +
        "</head><BODY>\n" +
        "<H1>%PATH%</H1>\n";
    private static final String TITLE_TEXT =
        "<H2>${Title_Long}</H2>\n";

    private void writeHtmlContents() throws IOException {
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
        boolean disc = parameters.containsKey("disc");

        for (int i = 0;   i < PARAM_FLAG.length;   i++) {
            StringBuffer imageUri = new StringBuffer(getScriptName());
            imageUri.append("?html&type=").append(PARAM_FLAG[i]);
            if (strict) imageUri.append("&strict");
            if (nokids) imageUri.append("&"+DefectAnalyzer.NO_CHILDREN_PARAM);
            if (disc) imageUri.append("&disc");
            imageUri.append("&qf="+PATH_TO_REPORTS+"compProj.rpt")
                    .append("&categoryLabels=vertical&width=500&height=400");

            String absImageUri = resolveRelativeURI(imageUri.toString());
            String imageHtml = getRequestAsString(absImageUri);
            out.print("<P>");
            out.print(imageHtml);
            out.print("</P>\n");
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
        if (parameters.containsKey("disc"))
            return DiscChart.createDiscChart(data, parameters);
        else
            return createBarChart();
    }

    private JFreeChart createBarChart() {
        JFreeChart chart = null;
        if (get3DSetting()) {
            chart = ChartFactory.createBarChart3D
                (null, null, null, data.catDataSource(),
                 PlotOrientation.VERTICAL, false, true, false);
            chart.getPlot().setForegroundAlpha(ALPHA);

        } else {
            chart = ChartFactory.createBarChart
                (null, null, null, data.catDataSource(),
                 PlotOrientation.VERTICAL, false, true, false);
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

    public void analyze(String path, Defect d) {
        switch (reportType) {
            case FIX_TIME:
                increment(getRow(d.defect_type), d.getFixTime());
                break;

            case DEFECT_COUNT:
                increment(getRow(d.defect_type), d.fix_count);
                break;
        }
    }
}
