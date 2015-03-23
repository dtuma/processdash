// Copyright (C) 2003-2008 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.i18n.Translator;


public class ProcessAnalysisPage extends AnalysisPage {


    protected void writeHTML() throws IOException {
        writeHTMLHeader("Process.Title");

        // get information about the current process
        ListData failurePhases = getProcessList("Failure_Phase_List");
        boolean hasProductivity = metricIsDefined("Productivity");
        boolean hasYield = metricIsDefined("Yield");
        boolean hasAFR = metricIsDefined("AFR");

        // write the productivity charts
        if (hasProductivity) {
            writeChartHTML(LINE_CHART, PROD_CHART);
            if (hasYield)
                writeChartHTML(XY_CHART, PROD_VS_YIELD_CHART);
        }

        // write afr charts
        if (hasAFR) {
            if (hasProductivity)
                writeChartHTML(XY_CHART, PROD_VS_AFR_CHART);
            if (hasYield)
                writeChartHTML(XY_CHART, YIELD_VS_AFR_CHART);
        }

        for (int i = 1;   i < failurePhases.size();   i++) {
            String phaseArg = fmtArg("phase", failurePhases.get(i));
            if (hasAFR)
                writeChartHTML(XY_CHART, DEF_VS_AFR_CHART, phaseArg);
            if (hasYield)
                writeChartHTML(XY_CHART, DEF_VS_YIELD_CHART, phaseArg);
        }

        out.write("</body></html>\n");
    }


    private static final String PROD_CHART = "Prod";
    public void writeProdArgs() {
        writeSimpleChartArgs
            ("${Process.Productivity_Title}", getProductivityLabel(),
             null, "Productivity");
    }


    private static final String PROD_VS_YIELD_CHART = "ProdVsYield";
    public void writeProdVsYieldArgs() {
        writeSimpleXYChartArgs
            ("${Process.Productivity_Vs_Yield_Title}",
             "${Process.Yield_Label}", "Yield",
             getProductivityLabel(), "Productivity");
        out.println("f1=100%");
    }


    private static final String PROD_VS_AFR_CHART = "ProdVsAFR";
    public void writeProdVsAFRArgs() {
        writeSimpleXYChartArgs
            ("${Process.Productivity_Vs_AFR_Title}",
             "${Process.AFR_Label}", "AFR",
             getProductivityLabel(), "Productivity");
    }


    private static final String YIELD_VS_AFR_CHART = "YieldVsAFR";
    public void writeYieldVsAFRArgs() {
        writeSimpleXYChartArgs
            ("${Process.Yield_Vs_AFR_Title}",
             "${Process.AFR_Label}", "AFR",
             "${Process.Yield_Label}", "Yield");
        out.println("f2=100%");
    }


    private static final String DEF_VS_AFR_CHART = "DefVsAFR";
    public void writeDefVsAFRArgs() {
        String phaseName = getParameter("phase");
        String displayName = Translator.translate(phaseName);
        String aggrSize = getAggrSizeLabel();
        String title = resources.format
            ("Process.Defects_Vs_AFR_Title_FMT", displayName);
        String densityLabel = resources.format
            ("Defects.Density_Scatter.Axis_FMT", displayName, aggrSize);

        writeSimpleXYChartArgs
            (title, "${Process.AFR_Label}", "AFR",
             densityLabel, phaseName + "/Defect Density");
    }


    private static final String DEF_VS_YIELD_CHART = "DefVsYield";
    public void writeDefVsYieldArgs() {
        String phaseName = getParameter("phase");
        String displayName = Translator.translate(phaseName);
        String aggrSize = getAggrSizeLabel();
        String title = resources.format
            ("Process.Defects_Vs_Yield_Title_FMT", displayName);
        String densityLabel = resources.format
            ("Defects.Density_Scatter.Axis_FMT", displayName, aggrSize);

        writeSimpleXYChartArgs
            (title, "${Process.Yield_Label}", "Yield",
             densityLabel, phaseName + "/Defect Density");
        out.println("f1=100%");
    }



    private String getProductivityLabel() {
        String displayName = getSizeAbbrLabel();
        String label = resources.format
            ("Process.Productivity_Label_FMT", displayName);
        return label;
    }
}
