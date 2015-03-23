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
import net.sourceforge.processdash.i18n.Translator;


public class DefectAnalysisPage extends AnalysisPage {


    protected void writeHTML() throws IOException {
        writeHTMLHeader("Defects.Title");

        // get information about the current process
        ListData developmentPhases = getProcessList("Development_Phase_List");
        ListData qualityPhases = getProcessList("Quality_Phase_List");
        ListData failurePhases = getProcessList("Failure_Phase_List");

        // write the total defects chart
        writeChartHTML(LINE_CHART, TOTAL_CHART);

        // write the "injected in phase" charts
        for (int i = 0;  i < developmentPhases.size();   i++)
            writeChartHTML(LINE_CHART, INJ_CHART,
                           fmtArg("phase", developmentPhases.get(i)));

        // write the "removed in phase" charts
        for (int i = 0;  i < qualityPhases.size();   i++)
            writeChartHTML(LINE_CHART, REM_CHART,
                           fmtArg("phase", qualityPhases.get(i)));

        // write the cumulative injection percentage chart
        if (developmentPhases.size() > 1)
            writeChartHTML(LINE_CHART, CUM_INJ_PCT_CHART);

        // write the cumulative removal percentage chart
        if (qualityPhases.size() > 1)
            writeChartHTML(LINE_CHART, CUM_REM_PCT_CHART);

        // write the removal rates chart
        if (qualityPhases.size() > 0)
            writeChartHTML(LINE_CHART, REM_RATES_CHART);

        if (qualityPhases.size() > 1)
            // write the defect removal leverage chart
            writeChartHTML(LINE_CHART, LEVERAGE_CHART);

        // write scatter plots of failure phase densities
        for (int i = 1;   i < failurePhases.size();   i++)
            writeChartHTML(XY_CHART, FAIL_SCATTER_CHART,
                           fmtArg("phaseX", failurePhases.get(i-1)) + "&" +
                           fmtArg("phaseY", failurePhases.get(i)));

        // write pie charts of injected/removed defects by phase
        writeChartHTML(PIE_CHART, INJ_PIE_CHART);
        writeChartHTML(PIE_CHART, REM_PIE_CHART);

        out.write("</body></html>\n");
    }

    private void writeDefaultArgs() {
        String aggrSize = getAggrSizeLabel();
        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("h1=");
        out.println(resources.format("Defects.Density_Header_FMT", aggrSize));
        out.print("units=");
        out.println(resources.format("Defects.Density_Units_FMT", aggrSize));
        out.print("headerComment=");
        out.println(resources.format("Defects.Density_Comment_FMT", aggrSize));
    }


    private static final String TOTAL_CHART = "Total";
    public void writeTotalArgs() {
        writeDefaultArgs();
        out.println("d1=Defect Density");
        out.print("title=");
        out.println(resources.getString("Defects.Total_Title"));
    }


    private static final String INJ_CHART = "Injected";
    public void writeInjectedArgs() {
        writeDefaultArgs();
        String phaseName = getParameter("phase");
        String displayName = Translator.translate(phaseName);
        out.print("title=");
        out.println(resources.format("Defects.Injected_Title_FMT", displayName));
        out.println("d1=" + phaseName + "/Defect Injection Density");
    }


    private static final String REM_CHART = "Removed";
    public void writeRemovedArgs() {
        writeDefaultArgs();
        String phaseName = getParameter("phase");
        String displayName = Translator.translate(phaseName);
        out.print("title=");
        out.println(resources.format("Defects.Removed_Title_FMT", displayName));
        out.println("d1=" + phaseName + "/Defect Density");
    }


    private static final String CUM_INJ_PCT_CHART = "CumInjPct";
    public void writeCumInjPctArgs() {
        writeCumulativePercentArgs
            ("Development_Phase_List",
             "Defects.Cumulative_Injection_Percent_Title",
             "Defects Injected");
    }


    private static final String CUM_REM_PCT_CHART = "CumRemPct";
    public void writeCumRemPctArgs() {
        writeCumulativePercentArgs
            ("Quality_Phase_List",
             "Defects.Cumulative_Removal_Percent_Title",
             "Defects Removed");
    }


    private void writeCumulativePercentArgs(String listName, String titleKey, String dataName) {
        ListData allPhases = getProcessList("Phase_List");
        ListData developmentPhases = getProcessList(listName);

        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("title=");
        out.println(resources.getString(titleKey));
        out.print("units=");
        out.println(resources.getString("Percent_Units"));

        for (int i = 0;  i < developmentPhases.size();   i++) {
            String num = Integer.toString(developmentPhases.size() - i);
            String phaseName = (String) developmentPhases.get(i);
            String displayName = Translator.translate(phaseName);
            out.print("h" + num + "=");
            out.println(displayName);
            out.println("f" + num + "=100%");
            out.print("d" + num + "=");
            out.println(getCumPhaseSumPct(allPhases, phaseName, dataName));
        }
    }

    private static final String REM_RATES_CHART = "RemRate";
    public void writeRemRateArgs() {
        ListData qualityPhases = getProcessList("Quality_Phase_List");

        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("title=");
        out.println(resources.getString("Defects.Removal_Rate.Title"));
        out.print("units=");
        out.println(resources.getString("Defects.Removal_Rate.Units"));
        out.print("headerComment=");
        out.println(resources.getString("Defects.Removal_Rate.Comment"));

        for (int i = 0;  i < qualityPhases.size();   i++) {
            String num = Integer.toString(qualityPhases.size() - i);
            String phaseName = (String) qualityPhases.get(i);
            String displayName = Translator.translate(phaseName);
            out.println("h" + num + "=" + displayName);
            out.println("d" + num + "=" + phaseName + "/Defects Removed per Hour");
        }
    }


    private static final String LEVERAGE_CHART = "DRL";
    public void writeDRLArgs() {
        ListData qualityPhases = getProcessList("Quality_Phase_List");
        String lastFailurePhase = Translator.translate
            ((String) qualityPhases.get(qualityPhases.size()-1));

        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
        out.print("title=");
        out.println(resources.getString("Defects.Leverage.Title"));
        out.print("headerComment=");
        out.println(resources.format("Defects.Leverage.Comment_FMT",
                                     lastFailurePhase));
        out.print("h1=");
        out.println(lastFailurePhase);
        out.println("d1=1.0  /* [ignored but necessary] */");

        for (int i = 0;  i < qualityPhases.size() - 1;   i++) {
            String num = Integer.toString(qualityPhases.size() - i);
            String phaseName = (String) qualityPhases.get(i);
            String displayName = Translator.translate(phaseName);
            out.println("h" + num + "=" + displayName);
            out.println("d" + num + "=" + phaseName + "/DRL");
        }
    }

    private static final String FAIL_SCATTER_CHART = "RemScatter";
    public void writeRemScatterArgs() {
        String phaseX = getParameter("phaseX");
        String phaseY = getParameter("phaseY");
        String displayX = Translator.translate(phaseX);
        String displayY = Translator.translate(phaseY);
        String aggrSize = getAggrSizeLabel();

        writeDefaultArgs();
        out.print("title=");
        out.println(resources.format("Defects.Density_Scatter.Title_FMT",
                                     displayX, displayY));
        out.print("h1=");
        out.println(resources.format("Defects.Density_Scatter.Axis_FMT",
                                     displayX, aggrSize));
        out.print("d1=");
        out.print(phaseX);
        out.println("/Defect Density");

        out.print("h2=");
                out.println(resources.format("Defects.Density_Scatter.Axis_FMT",
                                             displayY, aggrSize));
        out.print("d2=");
        out.print(phaseY);
        out.println("/Defect Density");
    }


    private static final String INJ_PIE_CHART = "InjPie";
    public void writeInjPieArgs() {
        writePhasePieArgs("Defects.Injected_By_Phase_Title",
                          "Defects Injected");
    }


    private static final String REM_PIE_CHART = "RemPie";
    public void writeRemPieArgs() {
        writePhasePieArgs("Defects.Removed_By_Phase_Title", "Defects Removed");
    }


    private void writePhasePieArgs(String titleKey, String dataElem) {
        out.println("for=.");
        out.println("colorScheme=byPhase");
        out.println("skipRowHdr=true");
        out.print("title=");
        out.println(resources.getString(titleKey));

        ListData allPhases = getProcessList("Phase_List");
        for (int i = 0;  i < allPhases.size();   i++) {
            String num = Integer.toString(i+1);
            String phaseName = (String) allPhases.get(i);
            String displayName = Translator.translate(phaseName);
            out.println("h" + num + "=" + displayName);
            out.println("d" + num + "=" + phaseName + "/" + dataElem);
        }
    }

}
