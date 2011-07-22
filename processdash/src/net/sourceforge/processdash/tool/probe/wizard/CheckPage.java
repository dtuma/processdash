// Copyright (C) 2002-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;

import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;


public class CheckPage extends WizardPage {

    private static final int LOW_PROD = -1;
    private static final int GOOD_PROD = 0;
    private static final int HIGH_PROD = 1;
    private static final int NO_PROD = 2;


    private static final String[] CMP_RES_KEYS = {
        "Check.Productivity.Low_HTML_FMT",
        "Check.Productivity.Comparable_HTML_FMT",
        "Check.Productivity.High_HTML_FMT",
    };
    private static final String CLOSE_WINDOW_HYPERLINK =
        "<a href='#' onClick='window.close(); return false;'>";

    protected ProbeData histData;
    protected ProcessUtil processUtil;
    protected double size, time, estProductivity, histProductivity, histDev;
    protected int cmpFlag;

    public void writeHTMLContents() {
        calcData(false);
        String sizeLabel =
            histData.getResultSet().getColName(ProbeData.ACT_NC_LOC);

        writeStepTitle(resources.getString("Check.Title"));
        out.print("<p>");
        out.println(resources.getHTML("Check.Header"));
        out.print("<ul><li>");
        out.print(resources.format
            ("Check.Size_FMT", FormatUtil.formatNumber(size), sizeLabel));
        out.print("</li><li>");
        out.print(resources.format
            ("Check.Time_FMT", FormatUtil.formatNumber(time)));
        out.println("</li></ul><p>");

        out.print("<p>");
        writeProductivityStatement();
        out.println("</p>");

        String resKey = "Check.Productivity.Success_Instruction_HTML";
        out.print("<p>");
        if (cmpFlag == LOW_PROD || cmpFlag == HIGH_PROD) {
            resKey = "Check.Productivity.Reestimate_Instruction_HTML";
            setNextPage("Size");
        }
        out.print("<p>");
        out.print(StringUtils.findAndReplace(resources.getString(resKey),
                "<a>", CLOSE_WINDOW_HYPERLINK));
        out.println("</p>");
    }

    @Override
    protected void writeFooterButtons() {
        if (cmpFlag == LOW_PROD || cmpFlag == HIGH_PROD)
            writeReestimationButtons();
        else
            super.writeFooterButtons();
    }

    private void writeReestimationButtons() {
        out.write("<p style='text-align: center'>");
        out.print("<input type=submit name=continue value=\"");
        out.print(resources.getHTML("Check.Reevaluate_Button"));
        out.print("\"></p>");

        out.write("<p style='text-align: center'>");
        out.print("<input type=button name=finish value=\"");
        out.print(resources.getHTML("Check.Keep_Estimates_Button"));
        out.print("\" onClick='window.close()'></p>");
    }

    public boolean parseFormData() {
        return true;
    }

    public boolean writeReportSection() {
        calcData(true);
        writeSectionTitle(resources.getString("Check.Report_Title"));
        out.print("<p style='margin-left:1cm'>");
        writeProductivityStatement();
        out.println("</p>");
        return true;
    }

    protected void calcData(boolean forReport) {
        histData = ProbeData.getEffectiveData(data, prefix);
        histData.setReportMode(forReport);
        size = histData.getCurrentValue(ProbeData.EST_NC_LOC);
        time = histData.getCurrentValue(ProbeData.EST_TIME);
        processUtil = histData.getProcessUtil();

        // check to see if their estimates are reasonable.
        estProductivity = size / time;
        histProductivity = histData.getProductivity();
        histDev = histData.getProdStddev();
        // handle the case where they have only one historical data
        // point, by assuming a 30% variation in productivity.
        if (histDev == 0 || Double.isInfinite(histDev)|| Double.isNaN(histDev))
            histDev = histProductivity * 0.30;

        if (Double.isNaN(histProductivity) ||
            Double.isInfinite(histProductivity))
            cmpFlag = NO_PROD;
        else if (estProductivity > histProductivity + histDev)
            cmpFlag = HIGH_PROD;
        else if (estProductivity < histProductivity - histDev)
            cmpFlag = LOW_PROD;
        else
            cmpFlag = GOOD_PROD;
    }

    private void writeProductivityStatement() {
        out.println(resources.format
                    ("Check.Productivity.Plain_HTML_FMT",
                     processUtil.formatProductivity(estProductivity)));
        if (cmpFlag != NO_PROD)
            out.println(resources.format(CMP_RES_KEYS[cmpFlag+1],
                        processUtil.formatProductivity(histProductivity),
                        FormatUtil.formatNumber(histDev)));
    }

}
