// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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


package net.sourceforge.processdash.ui.web.reports;


import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.Defect;
import net.sourceforge.processdash.log.DefectAnalyzer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class DefectLogReport extends TinyCGIBase implements DefectAnalyzer.Task {

    private static final Resources resources =
        Resources.getDashBundle("Defects.Report");

    private String typeFilt, injFilt, remFilt;

    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>${Title}%for owner%%for path%</TITLE>%css%\n" +
        "<STYLE>\n" +
        "    @media print { TD { font-size: 8pt } }\n" +
        "    TABLE { empty-cells: show }\n" +
        "    .header { font-weight: bold }\n" +
        "    TD { vertical-align: baseline }\n" +
        "</STYLE></HEAD>\n" +
        "<BODY><H1>${Title}%for path%</H1>\n";
    private static final String START_TEXT =
        "<TABLE BORDER><TR class=header>\n" +
        "<TD>${Project}</TD>\n" +
        "<TD>${Date}</TD>\n" +
        "<TD>${ID}</TD>\n" +
        "<TD>${Type}</TD>\n" +
        "<TD>${Injected}</TD>\n" +
        "<TD>${Removed}</TD>\n" +
        "<TD>${FixTime}</TD>\n" +
        "<TD>${FixDefect}</TD>\n" +
        "<TD>${Description}</TD></TR>";

    private static final String TABLE_END_TEXT =
        "</TABLE>" +
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
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
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
        if (forParam != null && forParam.length() > 0)
            DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                               path, parameters, this);
        else
            DefectAnalyzer.run(getPSPProperties(), path, true, this);

        out.println(resources.interpolate(TABLE_END_TEXT, HTMLUtils.ESC_ENTITIES));

        if (getParameter("EXPORT") == null && !parameters.containsKey("noDisclaimer"))
            out.println(resources.interpolate(DISCLAIMER,
                                              HTMLUtils.ESC_ENTITIES));
        out.println(END_TEXT);
    }

    private String For(String phrase) {
        if (phrase != null && phrase.length() > 1)
            return resources.format("For_FMT", phrase);
        else
            return "";
    }

    private String tr(String text) {
        return Translator.translate(text);
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
        out.println("<TD NOWRAP>" + path + "</TD>");
        out.println("<TD>" + FormatUtil.formatDate(d.date) + "</TD>");
        out.println("<TD>" + d.number + "</TD>");
        out.println("<TD>" + d.defect_type + "</TD>");
        out.println("<TD>" + d.phase_injected + "</TD>");
        out.println("<TD>" + d.phase_removed + "</TD>");
        out.println("<TD>" + d.fix_time + "</TD>");
        out.println("<TD>" + d.fix_defect + "</TD>");
        out.println("<TD>" + d.description + "</TD>");
        out.println("</TR>");
    }
}
