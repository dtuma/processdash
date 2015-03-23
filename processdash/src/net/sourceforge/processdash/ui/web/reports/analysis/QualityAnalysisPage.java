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


public class QualityAnalysisPage extends AnalysisPage {


    protected void writeHTML() throws IOException {
        writeHTMLHeader("Quality.Title");
        boolean hasYield = metricIsDefined("Yield");
        boolean hasAFR = metricIsDefined("AFR");
        boolean hasAppraisal = metricIsDefined("% Appraisal COQ");
        boolean hasFailure = metricIsDefined("% Failure COQ");

        if (hasYield)
            writeChartHTML(LINE_CHART, YIELD_CHART);
        if (hasFailure)
            writeChartHTML(LINE_CHART, FAIL_COQ_CHART);
        if (hasAppraisal)
            writeChartHTML(LINE_CHART, APPR_COQ_CHART);
        if (hasAppraisal && hasFailure)
            writeChartHTML(LINE_CHART, TOTAL_COQ_CHART);
        if (hasAFR)
            writeChartHTML(LINE_CHART, AFR_CHART);

        ListData mainAppraisalPhases = getMainAppraisalPhases();
        if (mainAppraisalPhases.size() > 0) {
            writeChartHTML(LINE_CHART, REVIEW_RATE_CHART);
            for (int i = 0;   i < mainAppraisalPhases.size();   i++) {
                if (hasYield)
                    writeChartHTML(XY_CHART, REV_RATE_VS_PROC_YIELD,
                                   fmtArg("phase", mainAppraisalPhases.get(i)));
                writeChartHTML(XY_CHART, REV_RATE_VS_PHASE_YIELD,
                               fmtArg("phase", mainAppraisalPhases.get(i)));
            }
            if (hasYield && mainAppraisalPhases.size() > 1)
                writeChartHTML(XY_CHART, COMBINED_REV_RATE_VS_PROC_YIELD);
        }

        out.write("</body></html>\n");
    }

    private static final String YIELD_CHART = "Yield";
    public void writeYieldArgs() {
        writeSimpleChartArgs
            ("${Quality.Yield_Title}", "${Quality.Yield_Label}", null,
             "Yield");
        out.println("f1=100%");
    }

    private static final String FAIL_COQ_CHART = "FailCOQ";
    public void writeFailCOQArgs() {
        writeSimpleChartArgs
            ("${Quality.Failure_COQ_Title}", "${Quality.Failure_COQ_Label}",
             null, "% Failure COQ");
    }

    private static final String APPR_COQ_CHART = "ApprCOQ";
    public void writeApprCOQArgs() {
        writeSimpleChartArgs
            ("${Quality.Appraisal_COQ_Title}",
             "${Quality.Appraisal_COQ_Label}", null, "% Appraisal COQ");
    }

    private static final String TOTAL_COQ_CHART = "TotalCOQ";
    public void writeTotalCOQArgs() {
        writeSimpleChartArgs
            ("${Quality.Total_COQ_Title}",
             "${Quality.Total_COQ_Label}", null, "% COQ");

    }

    private static final String AFR_CHART = "AFR";
    public void writeAFRArgs() {
        writeSimpleChartArgs
            ("${Quality.AFR_Title}", "${Quality.AFR_Label}", null, "AFR");
    }

    private static final String REVIEW_RATE_CHART = "ReviewRate";
    public void writeReviewRateArgs() {
        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("title=");
        out.println(resources.format
                    ("Quality.Review_Rate_Title_FMT", getSizeAbbrLabel()));

        ListData appraisalPhases = getMainAppraisalPhases();
        int insertAll = 0;
        if (appraisalPhases.size() > 1) {
            insertAll = 1;
            out.print("h1=");
            out.println(resources.getString("Quality.Review_Rate_All_Label"));
            out.print("d1=");
            writeCombinedReviewRateEqn(appraisalPhases);
        }

        for (int i = 0;   i < appraisalPhases.size();   i++) {
            String num = Integer.toString(i + 1 + insertAll);
            String phaseName = (String) appraisalPhases.get(i);
            String displayName = Translator.translate(phaseName);
            out.println("d" + num + "=" + phaseName + "/Appraisal Rate");
            out.println("h" + num + "=" + displayName);
        }
    }

    private ListData getMainAppraisalPhases() {
        ListData appraisalPhases = getProcessList("Appraisal_Phase_List");
        ListData mainAppraisalPhases = findPhasesForSizeMetric
            (appraisalPhases, getSizeMetric());
        return mainAppraisalPhases;
    }


    private ListData findPhasesForSizeMetric(ListData phaseList,
                                             String sizeMetric) {
        ListData result = new ListData();
        for (int i = 0;   i < phaseList.size();   i++) {
             String phase = (String) phaseList.get(i);
             String phaseSize = getProcessString(phase + "/SIZE_METRIC_NAME");
             if (phaseSize == null || phaseSize.length() == 0 ||
                 phaseSize.equals(sizeMetric))
                 result.add(phase);
        }
        return result;
    }


    private void writeCombinedReviewRateEqn(ListData phases) {
        out.print("[");
        out.print(escData(getSizeMetric()));
        out.print("] * 60 / ");
        out.println(getCumPhaseSum(phases, null, "Time"));
    }

    private static final String REV_RATE_VS_PROC_YIELD = "RateVsYield";
    public void writeRateVsYieldArgs() {
        String phase = getParameter("phase");
        String phaseName = Translator.translate(phase);
        String sizeAbbr = getSizeAbbrLabel();
        String title = resources.format
            ("Quality.Review_Rate_Vs_Process_Yield_Title_FMT",
             phaseName, sizeAbbr);
        String rateLabel = resources.format
            ("Quality.Phase_Review_Rate_FMT", phaseName, sizeAbbr);
        writeSimpleXYChartArgs
            (title,
             rateLabel, phase + "/Appraisal Rate",
             "${Quality.Yield_Label}", "Yield");
        out.println("f2=100%");
    }


    private static final String REV_RATE_VS_PHASE_YIELD = "RateVsPhaseYield";
    public void writeRateVsPhaseYieldArgs() {
        String phase = getParameter("phase");
        String phaseName = Translator.translate(phase);
        String sizeAbbr = getSizeAbbrLabel();
        String title = resources.format
            ("Quality.Review_Rate_Vs_Phase_Yield_Title_FMT", phaseName, sizeAbbr);
        String rateLabel = resources.format
            ("Quality.Phase_Review_Rate_FMT", phaseName, sizeAbbr);
        String yieldLabel = resources.format
            ("Quality.Phase_Yield_Label_FMT", phaseName, sizeAbbr);
        writeSimpleXYChartArgs
            (title,
             rateLabel,  phase + "/Appraisal Rate",
             yieldLabel, phase + "/% Phase Yield");
        out.println("f2=100%");
    }

    private static final String COMBINED_REV_RATE_VS_PROC_YIELD = "CombRateVsYield";
    public void writeCombRateVsYieldArgs() {
        String sizeAbbr = getSizeAbbrLabel();
        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("title=");
        out.println(resources.getString
                    ("Quality.Combined_Review_Rate_Vs_Yield_Title"));

        out.print("h1=");
        out.println(resources.format
                    ("Quality.Combined_Review_Rate_Label_FMT", sizeAbbr));
        out.print("d1=");
        writeCombinedReviewRateEqn(getMainAppraisalPhases());

        out.println("d2=Yield");
        out.println("f2=100%");
        out.print("h2=");
        out.println(resources.getString("Quality.Yield_Label"));
    }
}
