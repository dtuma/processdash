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

package net.sourceforge.processdash.ui.web.reports.analysis;


import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.Defect;
import net.sourceforge.processdash.log.DefectAnalyzer;
import net.sourceforge.processdash.util.HTMLUtils;



public class Report3 extends AnalysisPage implements DefectAnalyzer.Task {

    private static final String TOTAL_CATEGORY_KEY = "TOTAL_CATEGORY_KEY";
    private int TOTAL_INJ, TOTAL_REM;

    private List injectionCategories, removalCategories;
    protected int [][] count  = null;
    protected float [][] time = null;


    protected void writeHTML() throws IOException {
        writeHTMLHeader();
        writeTableD21();
        writeTableD22();
        writeHTMLFooter();
    }

    private void writeHTMLHeader() {
        out.print
            ("<HTML><head>" +
             "<link rel=stylesheet type=\"text/css\" href=\"/style.css\">" +
             "<style>\n" +
             "TD        { text-align: center }\n" +
             "TD.header { font-weight: bold;  font-size: large }\n" +
             "TD.task   { text-align: left; white-space: nowrap }\n" +
             "</style>\n" +
             "<title>");
        out.print(resources.getHTML("R3.Title"));
        out.print
            ("</title>\n</head>\n<BODY>\n<H1>");
        out.print(HTMLUtils.escapeEntities(localizePrefix(getPrefix())));
        out.print("</H1>\n<H2>");
        out.print(resources.getHTML("R3.Title"));
        out.print("</H2>\n");
    }

    protected void writeHTMLFooter() {
        out.print("<P><A HREF=\"../excel.iqy\"><I>");
        out.print(resources.getHTML("Export_to_Excel"));
        out.println("</I></A>");
        out.println("</BODY></HTML>");
    }

    private void writeTableD21() throws IOException {
        List failurePhases = getList("Failure_Phase_List");

        out.println("<TABLE NAME=D21 BORDER>");
        out.println("<TR>");
        printRes("<TD class=header colspan=4>${R3.Defect_Densities}</TD>");

        if (failurePhases.size() > 0) {
            int colSpan = failurePhases.size() * 2;
            out.print("<TD class=header colspan=" + colSpan + ">");
            out.print(resources.getHTML("R3.Phase_Defects"));
            out.println("</TD>");
        }
        out.println("</TR>");

        String script = (String) env.get("SCRIPT_NAME");
        int pos = script.indexOf("reports/");
        script = script.substring(pos+8) + "?type=";
        String argStart = "qf=" + HTMLUtils.urlEncode(script);

        String fullURI = (String) env.get("REQUEST_URI");
        int slashPos = fullURI.indexOf("//");

        String tableURL = fullURI.substring(0, slashPos+2) +
            "reports/table.class?includable&" + argStart;
        out.print(new String(getRequest(tableURL+D21_DATA_ROWS+"&c0=task", true), "UTF-8"));
        out.print(new String(getRequest(tableURL+D21_TOTAL_ROW+"&c1=task", true), "UTF-8"));
        out.println("</TABLE>");
    }


    private static final String D21_DATA_ROWS = "D21Rows";
    public void writeD21RowsArgs() {
        out.println("qf=compProj.rpt");
        writeD21Args(1);
    }

    private static final String D21_TOTAL_ROW = "D21TotalRow";
    public void writeD21TotalRowArgs() {
        out.println("for=.");
        out.println("skipColHdr=1");
        out.println("skipRowHdr=1");
        out.print("d1=\"");
        out.println(resources.getString("Totals"));
        writeD21Args(2);
    }

    private void writeD21Args(int col) {
        List failurePhases = getList("Failure_Phase_List");

        String sizeMetric = getProcessString("SIZE_METRIC_NAME");
        String displayName = Translator.translate(sizeMetric);
        out.println("d"+col+"="+sizeMetric);
        out.println("h"+col+"="+displayName);
        col++;

        out.println("d"+col+"=Defects Removed");
        out.println("h"+col+"=" + resources.getString("Defects.Total_Title"));
        col++;

        String aggrSizeLabel = getAggrSizeLabel();
        String aggrDensityLabel = resources.format
            ("Defects.Density_Units_FMT", aggrSizeLabel);
        out.println("d"+col+"=Defect Density");
        out.println("h"+col+"=" + aggrDensityLabel);
        col++;

        for (int i = 0;   i < failurePhases.size();   i++) {
            String phase = (String) failurePhases.get(i);
            String phaseName = Translator.translate(phase);
            out.println("d"+col+"=" + phase + "/Defects Removed");
            out.print("h"+col+"=");
            out.println(resources.format("R3.D21.Found_FMT", phaseName));
            col++;

            out.println("d"+col+"=" + phase + "/Defect Density");
            out.print("h"+col+"=");
            out.println(resources.format("R3.D21.Density_FMT", phaseName,
                                         aggrSizeLabel));
            col++;
        }
    }

    protected void writeTableD22() {
        initD22Values();
        HashMap m = new HashMap();
        m.put("for", "[Rollup_List]");
        m.put("order", "Completed");
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           getPrefix(), m, this);
        eliminateEmptyValues();
        if (count == null) return;

        int numRemCategories = 0;
        for (int i = 0;   i < removalCategories.size();   i++)
            if (removalCategories.get(i) != null)
                numRemCategories++;

        // write table header
        out.println("<P><TABLE NAME=D22 BORDER><TR>");
        out.print("<TD colspan=");
        out.print(Integer.toString(numRemCategories+2));
        printRes(" class=header>${R3.D22.Title}</TD></TR>");

        // write column header row
        out.println("<TR><TD colspan=2></TD>");
        for (int i = 0;   i < removalCategories.size();   i++) {
            String remCat = (String) removalCategories.get(i);
            if (remCat == null) continue;

            out.print("<TD VALIGN=bottom>");
            printResPhaseOrTotal("R3.D22.Found", remCat);
            out.println("</TD>");
        }
        out.println("</TR>");

        // write rows of defect data
        for (int i = 0;   i < injectionCategories.size();   i++) {
            String injCat = (String) injectionCategories.get(i);
            if (injCat == null) continue;

            // write row header for injected phase
            out.print("<TR><TD rowspan=3>");
            printResPhaseOrTotal("R3.D22.Injected", injCat);
            out.println("</TD>");

            // write total fix times
            printRes("<TD ALIGN=right NOWRAP>${R3.D22.Total_Fix_Time}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + time(i, r) + "</TD>");
            }
            out.println("</TR>");

            // write total defect count
            printRes("<TR><TD ALIGN=right NOWRAP>${R3.D22.Total_Defects}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + count(i, r) + "</TD>");
            }
            out.println("</TR>");

            // write average fix times
            printRes("<TR><TD ALIGN=right NOWRAP>${R3.D22.Average_Fix_Time}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + avgTime(i, r) + "</TD>");
            }
            out.println("</TR>");
        }

        out.println("</TABLE></BODY></HTML>");
    }

    private void eliminateEmptyValues() {
        if (count[TOTAL_INJ][TOTAL_REM] == 0) {
            count = null;
            time = null;
            return;
        }

        for (int i = 0;   i < TOTAL_INJ;   i++) {
            if (count[i][TOTAL_REM] == 0)
                injectionCategories.set(i, null);
        }

        for (int r = 0;   r < TOTAL_REM;   r++) {
            if (count[TOTAL_INJ][r] == 0)
                removalCategories.set(r, null);
        }
    }

    private void initD22Values() {
        injectionCategories = getD22List("Development_Phase_List");
        removalCategories = getD22List("Failure_Phase_List");
        int injLen = injectionCategories.size();
        int remLen = removalCategories.size();
        count = new int[injLen][remLen];
        time = new float[injLen][remLen];

        for (int inj=0;  inj<injLen;  inj++)
            for (int rem=0;  rem<remLen;  rem++)
                time[inj][rem] = count[inj][rem] = 0;

        TOTAL_INJ = injLen-1;
        TOTAL_REM = remLen-1;
    }

    private List getD22List(String name) {
        List result = getList(name);
        result.add(TOTAL_CATEGORY_KEY);
        return result;
    }

    private static final String NA = resources.getString("R3.D22.NA");
    protected String count(int i, int r) {
        return getDefectCountHTML
            (count[i][r],
             (String) injectionCategories.get(i),
             (String) removalCategories.get(r));
    }
    protected String time(int i, int r) {
        return (time[i][r] == 0 ? NA : nf.format(time[i][r]));
    }
    protected String avgTime(int i, int r) {
        return (count[i][r] == 0 ? NA : nf.format(time[i][r] / count[i][r]));
    }
    private static NumberFormat nf = NumberFormat.getInstance();
    static { nf.setMaximumFractionDigits(2); }

    private String cleanPhase(String phase) {
        int slashPos = phase.lastIndexOf('/');
        if (slashPos == -1)
            return phase;
        else
            return phase.substring(slashPos+1);
    }

    public void analyze(String path, Defect d) {
        int inj = injectionCategories.indexOf(cleanPhase(d.phase_injected));
        int rem = removalCategories.indexOf(cleanPhase(d.phase_removed));
        float fixtime = Float.parseFloat(d.fix_time);

        if (inj != -1 && rem != -1) {
            count[inj][rem] += 1;
            time [inj][rem] += fixtime;
        }

        if (inj != -1) {
            count[inj][TOTAL_REM] += 1;
            time [inj][TOTAL_REM] += fixtime;
        }

        if (rem != -1) {
            count[TOTAL_INJ][rem] += 1;
            time [TOTAL_INJ][rem] += fixtime;
        }

        count[TOTAL_INJ][TOTAL_REM] += 1;
        time [TOTAL_INJ][TOTAL_REM] += fixtime;
    }


    private List getList(String name) {
        ListData list = null;
        String dataName = DataRepository.createDataName(getPrefix(), name);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        if (val instanceof ListData)
            list = (ListData) val;
        else if (val instanceof StringData)
            list = ((StringData) val).asList();

        List result = new LinkedList();
        for (int i = 0;  i < list.size();   i++)
            result.add(list.get(i));
        return result;
    }

    protected void printRes(String txt) {
        out.println(resources.interpolate(txt, HTMLUtils.ESC_ENTITIES));
    }

    protected void printResPhaseOrTotal(String resKey, String arg) {
        if (arg == TOTAL_CATEGORY_KEY) {
            resKey = resKey + "_Total";
            out.print(HTMLUtils.escapeEntities(resources.getString(resKey)));
        } else {
            arg = Translator.translate(arg);
            resKey = resKey + "_FMT";
            out.print(HTMLUtils.escapeEntities(resources.format(resKey, arg)));
        }
    }

    protected String getDefectCountHTML(int count, String inj, String rem) {
        if (count == 0)
            return NA;

        StringBuffer html = new StringBuffer();
        html.append("<a href=\"defectLog.class?");
        if (inj != null && inj != TOTAL_CATEGORY_KEY) {
            html.append("inj=")
                .append(HTMLUtils.urlEncode(inj));
            if (rem != null && rem != TOTAL_CATEGORY_KEY)
                html.append("&");
        }
        if (rem != null && rem != TOTAL_CATEGORY_KEY)
            html.append("rem=")
                .append(HTMLUtils.urlEncode(rem));
        html.append("\">").append(Integer.toString(count)).append("</a>");
        return html.toString();
    }
}
