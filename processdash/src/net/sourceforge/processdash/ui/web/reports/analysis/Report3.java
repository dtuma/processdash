// Copyright (C) 2001-2013 Tuma Solutions, LLC
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
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;



public class Report3 extends AnalysisPage implements DefectAnalyzer.Task {


    private static final String CHROMELESS = "R3chromeless";


    private static final String TOTAL_CATEGORY_KEY = "TOTAL_CATEGORY_KEY";


    // variables used to hold/collect data during a run
    private String sizeMetric;
    private String defectLogParam;
    private List failurePhases;
    private List injectionCategories, removalCategories;
    protected int [][] count  = null;
    protected float [][] time = null;
    private int TOTAL_INJ, TOTAL_REM;


    protected void writeHTML() throws IOException {
        initValues();
        writeHTMLHeader();
        writeTableD21();
        writeTableD22();
        writeHTMLFooter();
    }


    private void writeHTMLHeader() {
        if (!parameters.containsKey(INCLUDABLE_PARAM)) {
            out.print
                ("<HTML><head>" +
                 "<link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\">"+
                 "<link rel=\"stylesheet\" type=\"text/css\" href=\"/reports/defectReports.css\">"+
                 "<title>");
            out.print(resources.getHTML("R3.Title"));
            out.print("</title>\n</head>\n<BODY>\n");

            if (!parameters.containsKey(CHROMELESS)) {
                out.print("<H1>");
                out.print(HTMLUtils.escapeEntities(localizePrefix(getPrefix())));
                out.print("</H1>\n");
            }
        }
        String title = getParameter("Heading");
        if (title == null && !parameters.containsKey(CHROMELESS))
            title = resources.getString("R3.Title");
        if (title != null && title.length() > 0) {
            out.print("<h2>");
            out.print(HTMLUtils.escapeEntities(title));
            out.print("</h2>\n");
        }
    }


    protected void writeHTMLFooter() {
        if (!parameters.containsKey(INCLUDABLE_PARAM)) {
            if (!parameters.containsKey(CHROMELESS)) {
                out.print("<P><A HREF=\"" + PATH_TO_REPORTS + "excel.iqy\"><I>");
                out.print(resources.getHTML("Export_to_Excel"));
                out.println("</I></A>");
            }
            out.println("</BODY></HTML>");
        }
    }


    private void initValues() {
        failurePhases = getProcessListPlain("Failure_Phase_List");
        sizeMetric = getSizeMetric();

        injectionCategories = getProcessListPlain("Development_Phase_List");
        injectionCategories.add(TOTAL_CATEGORY_KEY);

        removalCategories = new LinkedList(failurePhases);
        removalCategories.add(TOTAL_CATEGORY_KEY);

        int injLen = injectionCategories.size();
        int remLen = removalCategories.size();
        count = new int[injLen][remLen];
        time = new float[injLen][remLen];

        for (int inj=0;  inj<injLen;  inj++)
            for (int rem=0;  rem<remLen;  rem++)
                time[inj][rem] = count[inj][rem] = 0;

        TOTAL_INJ = injLen-1;
        TOTAL_REM = remLen-1;

        DefectAnalyzer.refineParams(parameters, getDataContext());

        defectLogParam = getDefectLogParam();
        if (defectLogParam.length() > 0)
                defectLogParam = defectLogParam + "&";
    }


    private void writeTableD21() throws IOException {
        if (parameters.containsKey("hideD21"))
                return;

        failurePhases = getProcessListPlain("Failure_Phase_List");

        // open table
        out.println("<p><table name='D21' border class='R3table'>");

        // write top-level headers
        out.println("<TR>");
        printRes("<TD class='R3header' colspan=4>${R3.D21.Defect_Densities}</TD>");

        if (failurePhases.size() > 0) {
            int colSpan = failurePhases.size() * 2;
            out.print("<TD class='R3header' colspan=" + colSpan + ">");
            out.print(resources.getHTML("R3.D21.Phase_Defects"));
            out.println("</TD>");
        }
        out.println("</TR>");

        // write column headers
        writeTableD21ColHeaders();

        // write data for each project
        String[] projects = ResultSet.getPrefixList(getDataRepository(),
            parameters, getPrefix());
        for (int i = 0;   i < projects.length;   i++)
            writeTableD21Row(projects[i]);

        // write a total row
        writeTableD21Row(null);

        // close the table
        out.println("</table></p>");
    }


    private void writeTableD21ColHeaders() {
        out.println("<TR>");
        printRes("<TD class='R3task'>${Project/Task}</TD>");

        String sizeLabel = Translator.translate(sizeMetric);
        out.println("<TD>" + HTMLUtils.escapeEntities(sizeLabel) + "</TD>");
        printRes("<TD>${Defects.Total_Title}</TD>");

        String aggrSizeLabel = getAggrSizeLabel();
        String densityLabel = resources.format
            ("Defects.Density_Units_FMT", aggrSizeLabel);
        out.println("<TD>" + HTMLUtils.escapeEntities(densityLabel) + "</TD>");

        for (int i = 0;   i < failurePhases.size();   i++) {
            String phase = (String) failurePhases.get(i);
            String phaseName = Translator.translate(phase);
            String heading = resources.format("R3.D21.Found_FMT", phaseName);
            out.println("<TD>" + HTMLUtils.escapeEntities(heading) + "</TD>");

            heading = resources.format("R3.D21.Density_FMT", phaseName,
                                       aggrSizeLabel);
            out.println("<TD>" + HTMLUtils.escapeEntities(heading) + "</TD>");
        }
        out.println("</TR>");
    }


    private void writeTableD21Row(String project) {
        out.println("<TR>");
        out.print("<TD class='R3task'>");
        if (project == null)
            out.print(resources.getString("Totals"));
        else
            out.print(HTMLUtils.escapeEntities(project));
        out.println("</TD>");

        out.println("<TD>" + getNumber(project, sizeMetric) + "</TD>");
        String defectCount = getNumber(project, "Defects Removed");
        String defCountHTML = getDefectCountHTML
            (project, defectCount, null, null);
        out.println("<TD>" + defCountHTML + "</TD>");
        out.println("<TD>" + getNumber(project, "Defect Density") + "</TD>");

        for (int i = 0;   i < failurePhases.size();   i++) {
            String phase = (String) failurePhases.get(i);

            defectCount = getNumber(project, phase + "/Defects Removed");
            defCountHTML = getDefectCountHTML
                (project, defectCount, null, phase);
            out.println("<TD>" + defCountHTML + "</TD>");

            out.println("<TD>" + getNumber(project, phase + "/Defect Density")
                        + "</TD>");
        }
        out.println("</TR>");
    }


    protected void writeTableD22() {
        if (parameters.containsKey("hideD22"))
            return;

        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
            getPrefix(), parameters, this);

        eliminateEmptyValues();
        if (count == null) return;

        int numRemCategories = 0;
        for (int i = 0;   i < removalCategories.size();   i++)
            if (removalCategories.get(i) != null)
                numRemCategories++;

        // write table header
        out.println("<P><table name='D22' border class='R3table'><TR>");
        out.print("<TD colspan=");
        out.print(Integer.toString(numRemCategories+2));
        printRes(" class='R3header'>${R3.D22.Title}</TD></TR>");

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
            printRes("<TD class='R3subcat'>${R3.D22.Total_Fix_Time}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + time(i, r) + "</TD>");
            }
            out.println("</TR>");

            // write total defect count
            printRes("<TR><TD class='R3subcat'>${R3.D22.Total_Defects}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + count(i, r) + "</TD>");
            }
            out.println("</TR>");

            // write average fix times
            printRes("<TR><TD class='R3subcat'>${R3.D22.Average_Fix_Time}</TD>");
            for (int r = 0;   r < removalCategories.size();   r++) {
                if (removalCategories.get(r) != null)
                    out.println("<TD>" + avgTime(i, r) + "</TD>");
            }
            out.println("</TR>");
        }

        out.println("</table></p>");
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


    public void analyze(String path, Defect d) {
        int inj = injectionCategories.indexOf(cleanPhase(d.phase_injected));
        int rem = removalCategories.indexOf(cleanPhase(d.phase_removed));
        float fixtime = d.getFixTime();

        if (inj != -1 && rem != -1) {
            count[inj][rem] += d.fix_count;
            time [inj][rem] += fixtime;
        }

        if (inj != -1) {
            count[inj][TOTAL_REM] += d.fix_count;
            time [inj][TOTAL_REM] += fixtime;
        }

        if (rem != -1) {
            count[TOTAL_INJ][rem] += d.fix_count;
            time [TOTAL_INJ][rem] += fixtime;
        }

        count[TOTAL_INJ][TOTAL_REM] += d.fix_count;
        time [TOTAL_INJ][TOTAL_REM] += fixtime;
    }


    private String cleanPhase(String phase) {
        int slashPos = phase.lastIndexOf('/');
        if (slashPos == -1)
            return phase;
        else
            return phase.substring(slashPos+1);
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


    private String getNumber(String prefix, String name) {
        if (prefix == null) prefix = getPrefix();
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        if (val == null)
            return NA;
        else
            return val.format();
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
        String text = (count == 0 ? NA : Integer.toString(count));
        return getDefectCountHTML(null, text, inj, rem);
    }


    protected String getDefectCountHTML(String path, String count,
                                        String inj, String rem) {
        if (count == NA || "0".equals(count) || exporting()) return count;

        StringBuffer html = new StringBuffer();
        html.append("<a href=\"");
        if (path != null)
            html.append(WebServer.urlEncodePath(path))
                .append("//reports/defectlog.class?");
        else
            html.append(PATH_TO_REPORTS + "defectlog.class?" + defectLogParam);

        if (inj != null && inj != TOTAL_CATEGORY_KEY) {
            html.append("inj=")
                .append(HTMLUtils.urlEncode(inj));
            if (rem != null && rem != TOTAL_CATEGORY_KEY)
                html.append("&");
        }
        if (rem != null && rem != TOTAL_CATEGORY_KEY)
            html.append("rem=")
                .append(HTMLUtils.urlEncode(rem));
        html.append("\">").append(count).append("</a>");
        return html.toString();
    }
}
