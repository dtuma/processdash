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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pspdash.Defect;
import pspdash.DefectAnalyzer;
import pspdash.DefectTypeStandard;
import pspdash.HTMLUtils;
import pspdash.StringUtils;
import pspdash.Translator;



public class Report4 extends AnalysisPage implements DefectAnalyzer.Task {


    private static final String TOTAL = resources.getString("Total");

    // In the following map, keys are defect types (e.g. "Syntax") and
    // values are arrays of integers.
    protected Map defectCounts;
    protected int[] totals;
    protected List categories, injCategories, remCategories;
    protected Category atCompile, remCompile;
    protected List allPhases, develPhases, failPhases;
    protected boolean strict = true;
    protected boolean hasCompile = false;


    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>${R4.Title}</TITLE>%css%\n" +
        "<STYLE>\n" +
        "    TABLE { empty-cells: show }\n" +
        "    TD { text-align:center; vertical-align: baseline }\n" +
        "    .header { font-weight: bold; vertical-align:bottom }\n" +
        "    .footnote { font-size: small; font-style:italic }\n" +
        "    @media print { .doNotPrint { display: none } }\n" +
        "</STYLE></HEAD>\n" +
        "<BODY><H1>%path%</H1>";
    private static final String TITLE_TEXT =
        "<H2>${R4.Title}</H2>";

    protected static final String FOOTNOTE =
        "<P class=footnote><span class=doNotPrint>" +
        "${R4.Strict_Footnote_HTML}</span></P>";

    private static final String D24_HEADER =
        "<H3>${R4.D24.Title}</H3>\r\n" +
        "<TABLE NAME=D24 BORDER>\r\n" +
        "<TR class=header><TD>${R4.D24.Defect_Type}</TD>\r\n" +
        "<TD VALIGN=bottom>${R4.D24.Compile_Entry}</TD>" +
        "<TD VALIGN=bottom>${R4.D24.Compile_Found}</TD>" +
        "<TD VALIGN=bottom>${R4.D24.Compile_Percent}</TD></TR>\n";


    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        String path = getPrefix();
        strict = (parameters.get("strict") != null);

        initValues();
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           path, parameters, this);
        eliminateEmptyValues();

        writeHTMLHeader(path);

        writeTableD23();
        if (hasCompile)
            writeTableD24();

        writeHTMLFooter();
    }


    protected void writeHTMLHeader(String path) {
        if (!parameters.containsKey(INCLUDABLE_PARAM)) {
                String header = interpolate(HEADER_TEXT, true);
                header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
                header = StringUtils.findAndReplace
                    (header, "%path%",
                     HTMLUtils.escapeEntities(path));
                out.println(header);
        }
        printRes(TITLE_TEXT);
    }


    private void writeHTMLFooter() {
        if (!parameters.containsKey(INCLUDABLE_PARAM)) {
                printRes("<P class='doNotPrint'><A HREF=\"" +
                         PATH_TO_REPORTS+"excel.iqy\"><I>" +
                         "${Export_to_Excel}</I></A></P>");
                if (strict) {
                    String query = (String) env.get("QUERY_STRING");
                    query = StringUtils.findAndReplace(query, "strict", "notstrict");
                    String script = (String) env.get("SCRIPT_NAME");
                    script = script.substring(script.lastIndexOf('/')+1);
                    String anchor = "<A HREF='" + script + "?" + query + "'>";
                    String footnote = interpolate(FOOTNOTE, false);
                    footnote = StringUtils.findAndReplace(footnote, "<A>", anchor);
                    footnote = StringUtils.findAndReplace(footnote, "<a>", anchor);
                    out.println("<P><HR>" + footnote);
                }
                out.println("</BODY></HTML>");
        }
    }


    private void writeTableD23() {
        // write table header
        writeD23Header();

        // write table data
        String defectLogParam = getDefectLogParam();

        Iterator defectTypes = defectCounts.keySet().iterator();
        String defectType;
        int [] row;
        while (defectTypes.hasNext()) {
            defectType = (String) defectTypes.next();
            row = (int[]) defectCounts.get(defectType);
            writeD23Row(defectLogParam, defectType, row);
        }
        writeD23Row(defectLogParam, TOTAL, totals);

        out.println("</TABLE>");
    }


        private void writeD23Header() {
        out.print("<H3>");
        out.print(resources.getHTML("R4.D23.Title"));
        out.println("</H3>");
        out.println("<TABLE NAME=D23 BORDER><TR class=header><TD></TD>");

        int injLen = injCategories.size();
        String txt =
            "<TD colspan=##>${R4.D23.Number_Injected}</TD>\r\n" +
            "<TD colspan=##>${R4.D23.Percentage_Injected}</TD>\r\n";
        txt = StringUtils.findAndReplace(txt, "##", Integer.toString(injLen));
        out.print(interpolate(txt, true));

        int remLen = remCategories.size();
        txt =
            "<TD colspan=##>${R4.D23.Number_Removed}</TD>\r\n" +
            "<TD colspan=##>${R4.D23.Percentage_Removed}</TD></TR>\r\n";
        txt = StringUtils.findAndReplace(txt, "##", Integer.toString(remLen));
        out.print(interpolate(txt, true));

        out.print("<TR><TD>");
        out.print(resources.getHTML("R4.D23.Type"));
        out.println("</TD>");
        printColHeaders(injCategories);
        printColHeaders(injCategories);
        printColHeaders(remCategories);
        printColHeaders(remCategories);
        out.println("</TR>");
    }


    private void printColHeaders(List categories) {
        for (Iterator i = categories.iterator(); i.hasNext();) {
            Category c = (Category) i.next();
            out.print("<TD>");
            out.print(HTMLUtils.escapeEntities(c.displayName()));
            out.print("</TD>");
        }
        out.println();
    }



    protected void writeD23Row(String param, String label, int [] row) {
        String dt = param;
        if (!label.startsWith(TOTAL)) {
            dt += ("&type=" + urlEncode(label));
            if (!strict &&
                    isEmptyRow(row, injCategories) &&
                    isEmptyRow(row, remCategories))
                return;
        }
        out.print("<TR><TD>");
        if (!exporting())
                out.print("<A HREF=\"" + PATH_TO_REPORTS + "defectlog.class?" + dt +"\">");
        out.print(HTMLUtils.escapeEntities(label));
        if (!exporting())
                out.print("</A>");
                out.println("</TD>");

        Iterator i;
        for (i = injCategories.iterator();   i.hasNext(); )
            out.println("<TD>" + fc(dt, row, (Category) i.next()) + "</TD>");
        for (i = injCategories.iterator();   i.hasNext(); )
            out.println("<TD>" + fp(row, (Category) i.next()) + "</TD>");

        for (i = remCategories.iterator();   i.hasNext(); )
            out.println("<TD>" + fc(dt, row, (Category) i.next()) + "</TD>");
        for (i = remCategories.iterator();   i.hasNext(); )
            out.println("<TD>" + fp(row, (Category) i.next()) + "</TD>");

        out.println("</TR>");
    }



    private void writeTableD24() {
        out.print(interpolate(D24_HEADER, true));

        Iterator defectTypes = defectCounts.keySet().iterator();
        while (defectTypes.hasNext()) {
            String defectType = (String) defectTypes.next();
            int[] row = (int[]) defectCounts.get(defectType);
            writeD24Row(defectType, row);
        }
        writeD24Row(TOTAL, totals);

        out.println("</TABLE>");
    }

    protected void writeD24Row(String label, int [] row) {
        int atCompilePos = categories.indexOf(atCompile);
        int remCompilePos = categories.indexOf(remCompile);
        if (!strict && 0 == (row[atCompilePos] +
                             row[remCompilePos]))
            return;

        out.println("<TR><TD>" +
                    HTMLUtils.escapeEntities(label) +
                    "</TD>");
        out.println("<TD>" + fc(null, row, atCompile) + "</TD>");
        out.println("<TD>" + fc(null, row, remCompile) + "</TD>");
        out.println("<TD>" + fp(row, remCompilePos,
                                atCompilePos) + "</TD></TR>");
    }



    private boolean isEmptyRow(int[] row, List categoryList) {
        Iterator i = categoryList.iterator();
        while (i.hasNext()) {
            int pos = categories.indexOf(i.next());
            if (pos >=0 && row[pos] > 0) return false;
        }

        return true;
    }


    /** format a count, found in slot col of array row. */
    protected String fc(String dt, int [] row, Category cat) {
        int col = categories.indexOf(cat);
        if (row[col] == 0) return NA;
        if (dt == null || exporting()) return Integer.toString(row[col]);
        return "<A HREF=\"" + PATH_TO_REPORTS + "defectlog.class?"
            + cat.getFilter() + "&" + dt + "\">" + row[col] + "</A>";
    }
    /** format a percentage, calculated by dividing item n of row by item d */
    protected String fp(int num, int denom) {
        if (num == 0 || denom == 0) return NA;
        return formatPercent(((double) num) / denom);
    }
    protected String fp(int [] row, Category c) {
        int n = categories.indexOf(c);
        return fp(row[n], totals[n]);
    }
    protected String fp(int [] row, int n) { return fp(row[n], totals[n]); }
    protected String fp(int [] row, int n, int d) { return fp(row[n],row[d]); }
    private static final String NA = resources.getString("R4.NA");

    protected String formatPercent(double d) {
        return Integer.toString((int) Math.round(d * 100)) + "%";
    }


    /** Initialize internal data structures to zero */
    private void initValues() {
        categories = new LinkedList();
        injCategories = new LinkedList();
        remCategories = new LinkedList();
        develPhases = getProcessListPlain("Development_Phase_List");
        failPhases = getProcessListPlain("Failure_Phase_List");
        allPhases = getProcessListPlain("Phase_List");

        for (Iterator i = develPhases.iterator(); i.hasNext();) {
            Category c = new InjCategory((String) i.next());
            categories.add(c);
            injCategories.add(c);
        }
        for (Iterator i = failPhases.iterator(); i.hasNext();) {
            Category c = new RemCategory((String) i.next());
            categories.add(c);
            remCategories.add(c);
        }
        hasCompile = allPhases.contains("Compile");
        if (hasCompile) {
            atCompile = new AtEntryCategory(allPhases, "Compile");
            remCompile = new FilteredByPhaseCategory(allPhases, "Compile");
            categories.add(atCompile);
            categories.add(remCompile);
        }

        totals = emptyRow();
        defectCounts = new TreeMap();
        if (strict) {
            DefectTypeStandard dts =
                DefectTypeStandard.get(getPrefix(), getDataRepository());
            for (int i=dts.options.size();  i-->0; )
                getRow((String) dts.options.elementAt(i));
        }
    }


    /** Discard defect categories containing no data */
    private void eliminateEmptyValues() {
        for (Iterator i = injCategories.iterator();   i.hasNext(); )
            if (totals[categories.indexOf(i.next())] == 0)
                i.remove();

        for (Iterator i = remCategories.iterator();   i.hasNext(); )
            if (totals[categories.indexOf(i.next())] == 0)
                i.remove();
    }


    /** Lookup the row for a defect type - create it if it doesn't exist. */
    private int[] getRow(String defectType) {
        int [] result = (int[]) defectCounts.get(defectType);
        if (result == null)
            defectCounts.put(defectType, result = emptyRow());
        return result;
    }

    /** Generate an empty row of the appropriate size */
    private int[] emptyRow() {
        int numCat = categories.size();
        int [] result = new int[numCat];
        for (int i=0;  i<numCat;  i++) result[i] = 0;
        return result;
    }

    /** Increment a defect count for a particular defect type */
    protected void increment(int [] row, int type) {
        totals[type]++;
        row[type]++;
    }

    /** Implement DefectAnalyzer.Task */
    public void analyze(String path, Defect d) {
        int [] row = getRow(d.defect_type);

        for (int i = categories.size();   i-- > 0; ) {
            Category c = (Category) categories.get(i);
            if (c.matches(d)) increment(row, i);
        }
    }



    private static class Category {
        protected String phaseName;
        public boolean matches(Defect d) { return false; }
        public String getFilter() { return ""; }

        protected String cleanupPhase(String phaseName) {
            int pos = phaseName.lastIndexOf('/');
            if (pos == -1)
                return phaseName;
            else
                return phaseName.substring(pos+1);
        }

        protected boolean matches(String phaseName) {
            return (this.phaseName.equals(cleanupPhase(phaseName)));
        }

        public String displayName() {
            return Translator.translate(phaseName);
        }
    }


    private static final class InjCategory extends Category {
        public InjCategory(String phaseName) {
            this.phaseName = phaseName;
        }
        public boolean matches(Defect d) {
            return matches(d.phase_injected);
        }
        public String getFilter() {
            return "inj=" + urlEncode(phaseName);
        }
    }


    private static final class RemCategory extends Category {
        public RemCategory(String phaseName) {
            this.phaseName = phaseName;
        }
        public boolean matches(Defect d) {
            return matches(d.phase_removed);
        }
        public String getFilter() {
            return "rem=" + urlEncode(phaseName);
        }
    }


    private static class AtEntryCategory extends Category {
        private List phases;
        private int phasePos;
        public AtEntryCategory (List phases, String phaseName) {
            this.phases = phases;
            this.phaseName = phaseName;
            this.phasePos = phases.indexOf(phaseName);
        }
        public boolean matches(Defect d) {
            int injPos = phases.indexOf(cleanupPhase(d.phase_injected));
            if (injPos >= phasePos) return false;

            int remPos = phases.indexOf(cleanupPhase(d.phase_removed));
            if (remPos == -1 || remPos >= phasePos) return true;

            return false;
        }
    }


    private static class FilteredByPhaseCategory extends AtEntryCategory {
        public FilteredByPhaseCategory (List phases, String phaseName) {
            super(phases, phaseName);
        }
        public boolean matches(Defect d) {
            return super.matches(d) && super.matches(d.phase_removed);
        }
    }
}
