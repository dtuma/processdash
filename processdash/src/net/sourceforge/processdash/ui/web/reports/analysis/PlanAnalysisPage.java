// Copyright (C) 2003-2009 Tuma Solutions, LLC
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
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Translator;


public class PlanAnalysisPage extends AnalysisPage {



    protected void writeHTML() throws IOException {
        writeHTMLHeader("Plan.Title");

        // get information about the current process
        ListData overheadPhases = getProcessList("Overhead_Phase_List");
        ListData failurePhases = getProcessList("Failure_Phase_List");

        // write the size charts
        if (metricIsDefined("SIZE_METRIC_NAME")) {
            writeChartHTML(LINE_CHART, SIZE_CHART);
            writeChartHTML(LINE_CHART, SIZE_ERR_CHART);
        }

        // write the total time charts
        writeChartHTML(LINE_CHART, TIME_CHART);
        writeChartHTML(LINE_CHART, TIME_ERR_CHART);

        // write overhead time charts
        for (int i = 0;   i < overheadPhases.size();   i++)
            writeChartHTML(LINE_CHART, PCT_TIME_CHART,
                           fmtArg("phase", overheadPhases.get(i)));
        if (overheadPhases.size() > 1)
            writeChartHTML(LINE_CHART, PCT_OVERHEAD_TIME_CHART);

        // write failure time charts
        for (int i = 0;   i < failurePhases.size();   i++)
            writeChartHTML(LINE_CHART, PCT_TIME_CHART,
                           fmtArg("phase", failurePhases.get(i)));
        if (failurePhases.size() > 1)
            writeChartHTML(LINE_CHART, PCT_FAILURE_TIME_CHART);

        // write time in phase chart
        writeChartHTML(PIE_CHART, PHASE_TIME_CHART);

        out.write("</body></html>\n");
    }


    private static final String SIZE_CHART = "Size";
    public void writeSizeArgs() {
        String sizeMetric = getSizeMetric();
        String displayName = Translator.translate(sizeMetric);
        writeSimpleChartArgs
            ("${Plan.Size_Title}", displayName, null, sizeMetric);
    }


    private static final String SIZE_ERR_CHART = "SizeErr";
    public void writeSizeErrArgs() {
        String dataElem = "Size Estimating Error";
        String dataName = DataRepository.createDataName(getPrefix(), dataElem);
        if (getDataRepository().getSimpleValue(dataName) == null)
            dataElem = getSizeMetric() + " Estimating Error";

        writeSimpleChartArgs
            ("${Plan.Size_Estimating_Error_Title}", "${Plan.Percent_Error}",
             null, dataElem);
        out.println("f1=100%");
    }


    private static final String TIME_CHART = "Time";
    public void writeTimeArgs() {
        writeSimpleChartArgs
            ("${Plan.Time_Title}", "${Hours}", "(${Hours})", "[Time] / 60");
    }


    private static final String TIME_ERR_CHART = "TimeErr";
    public void writeTimeErrArgs() {
        writeSimpleChartArgs
            ("${Plan.Time_Estimating_Error_Title}", "${Plan.Percent_Error}",
             null, "Time Estimating Error");
        out.println("f1=100%");
    }


    private static final String PCT_TIME_CHART = "PctTime";
    public void writePctTimeArgs() {
        String phaseName = getParameter("phase");
        String displayName = Translator.translate(phaseName);
        String title = resources.format("Plan.Percent_Time_FMT", displayName);
        writeSimpleChartArgs(title, "${Percent_Units}", null,
                             phaseName + "/%/Time");
        out.print("units=");
        out.println(resources.getString("Percent_Units"));
    }


    private static final String PCT_OVERHEAD_TIME_CHART = "PctOverhead";
    public void writePctOverheadArgs() {
        writeSumPhasePctArgs("Overhead_Phase_List",
                             "Plan.Percent_Overhead_Time");
    }


    private static final String PCT_FAILURE_TIME_CHART = "PctFailure";
    public void writePctFailureArgs() {
        writeSumPhasePctArgs("Failure_Phase_List",
                             "Plan.Percent_Failure_Time");
    }


    private void writeSumPhasePctArgs(String listName, String titleKey) {
        StringBuffer function = new StringBuffer();
        function.append("sumFor(\"Time\", [").append(listName)
            .append("]) / [Time]");
        writeSimpleChartArgs("${"+titleKey+"}", "${Percent_Units}", null,
                             function.toString());
        out.print("units=");
        out.println(resources.getString("Percent_Units"));
        out.println("f1=100%");
    }


    private static final String PHASE_TIME_CHART = "PhaseTime";
    public void writePhaseTimeArgs() {
        out.println("for=.");
        out.println("colorScheme=byPhase");
        out.println("skipRowHdr=true");
        out.print("title=");
        out.println(resources.getString("Plan.Time_In_Phase_Title"));

        ListData phaseList = getProcessList("Phase_List");
        for (int i = 0;   i < phaseList.size();   i++) {
            String num = Integer.toString(i + 1);
            String phaseName = (String) phaseList.get(i);
            String displayName = Translator.translate(phaseName);
            out.print("h" + num + "=");
            out.println(displayName);
            out.print("d" + num + "=");
            out.print(phaseName);
            out.println("/%/Time");
        }
    }
}
