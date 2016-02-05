// Copyright (C) 2001-2016 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui.web.reports;



import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.ui.lib.HTMLMarkup;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class DefectLogReport extends TinyCGIBase implements DefectAnalyzer.Task {

    private static final Resources resources =
        Resources.getDashBundle("Defects.Report");

    private String typeFilt, injFilt, remFilt;

    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>${Title}%for owner%%for path%</TITLE>\n" +
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\">"+
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/reports/defectReports.css\">"+
        "</HEAD>\n" +
        "<BODY><H1>${Title}%for path%</H1><!-- cutStart -->\n";
    private static final String START_TEXT =
        "<p><table border class='defectLog'><tr>\n" +
        "<th>${Project}</th>\n" +
        "<th>${Date}</th>\n" +
        "<th>${ID}</th>\n" +
        "<th>${Type}</th>\n" +
        "<th>${Injected}</th>\n" +
        "<th>${Removed}</th>\n" +
        "<th>${FixTime}</th>\n" +
        "<th>${FixCount}</th>\n" +
        "<th>${FixDefect}</th>\n" +
        "<th>${FixPending}</th>\n" +
        "<th>${Description}</th></tr>";

    private static final String TABLE_END_TEXT =
        "</table></p><!-- cutEnd -->" +
        "<P class='doNotPrint'><A HREF=\"excel.iqy\"><I>" +
        "${Export_to_Excel}</I></A></P>" ;

    private static final String DISCLAIMER =
        "<P class=doNotPrint><I>${Caveat}</I></P>";
    private static final String END_TEXT =
        "</BODY></HTML>";

    /** Generate CGI script output. */
    protected void writeContents() {

        String path = getPrefix();
        String title = For(path);
        String owner = For(getOwner());

        String header = resources.interpolate
            (HEADER_TEXT, HTMLUtils.ESC_ENTITIES);
        header = StringUtils.findAndReplace(header, "%for owner%", owner);
        header = StringUtils.findAndReplace(header, "%for path%", title);
        out.print(header);

        typeFilt = getParameter("type");
        injFilt  = getParameter("inj");
        remFilt  = getParameter("rem");

        if (typeFilt != null || injFilt != null || remFilt != null) {
            out.println(resources.getHTML("Filter.Header") + "<UL>");
            if (typeFilt != null)
                out.println("<LI>" + HTMLUtils.escapeEntities
                            (resources.format("Filter.Type_FMT",
                                              tr(typeFilt))));
            if (injFilt != null)
                out.println("<LI>" + HTMLUtils.escapeEntities
                            (resources.format("Filter.Injected_FMT",
                                              tr(injFilt))));
            if (remFilt != null)
                out.println("<LI>" + HTMLUtils.escapeEntities
                            (resources.format("Filter.Removed_FMT",
                                              tr(remFilt))));
            out.println("</UL><P>");
        }
        out.print(resources.interpolate(START_TEXT, HTMLUtils.ESC_ENTITIES));

        String forParam = getParameter("for");
        if (forParam != null && forParam.length() > 0) {
            DefectAnalyzer.refineParams(parameters, getDataContext());
            DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                               path, parameters, this);
        } else
            DefectAnalyzer.run(getPSPProperties(), path, true, this);

        out.println(resources.interpolate(TABLE_END_TEXT, HTMLUtils.ESC_ENTITIES));

        if (getParameter("EXPORT") == null && !parameters.containsKey("noDisclaimer"))
            out.println(resources.interpolate(DISCLAIMER,
                                              HTMLUtils.ESC_ENTITIES));
        out.println(END_TEXT);
    }

    private String For(String phrase) {
        if (phrase != null && phrase.length() > 1)
            return esc(resources.format("For_FMT", phrase));
        else
            return "";
    }

    private String tr(String text) {
        return Translator.translate(text);
    }

    private String esc(String text) {
        if (text == null)
            return "";
        else
            return HTMLUtils.escapeEntities(text);
    }

    private boolean phaseMatches(String a, String b) {
        return (a.equalsIgnoreCase(b) || a.endsWith("/" + b));
    }

    public void analyze(String path, Defect d) {
        if ((typeFilt != null && !d.defect_type.equalsIgnoreCase(typeFilt)) ||
            (injFilt != null && !phaseMatches(d.phase_injected, injFilt)) ||
            (remFilt != null && !phaseMatches(d.phase_removed, remFilt)))
            return;

        out.println("<TR>");
        out.println("<TD>" + esc(path) + "</TD>");
        out.println("<TD>" + FormatUtil.formatDate(d.date) + "</TD>");
        out.println("<TD>" + esc(d.number) + "</TD>");
        out.println("<TD>" + esc(d.defect_type) + "</TD>");
        out.println("<TD>" + esc(d.phase_injected) + "</TD>");
        out.println("<TD>" + esc(d.phase_removed) + "</TD>");
        out.println("<TD ALIGN='center'>" + esc(d.getLocalizedFixTime()) + "</TD>");
        out.println("<TD ALIGN='center'>" + Integer.toString(d.fix_count) + "</TD>");
        out.println("<TD ALIGN='center'>" + esc(d.fix_defect) + "</TD>");
        out.println("<TD ALIGN='center'>" + (d.fix_pending ? "*" : "") + "</TD>");
        out.println("<TD>" + HTMLMarkup.textToHtml(d.description) + "</TD>");
        out.println("</TR>");
    }
}
