// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.ResultSet;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;

public class defectlog extends TinyCGIBase implements DefectAnalyzer.Task {

    private String typeFilt, injFilt, remFilt;
    private HashSet projectList;

    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>Defect Log%for owner%%for path%</TITLE>%css%\n" +
        "<STYLE>\n" +
        "    @media print { TD { font-size: 8pt } }\n" +
        "    TABLE { empty-cells: show }\n" +
        "    .header { font-weight: bold }\n" +
        "    TD { vertical-align: baseline }\n" +
        "</STYLE></HEAD>\n" +
        "<BODY><H1>Defect Log%for path%</H1>\n";
    private static final String START_TEXT =
        "<TABLE BORDER><TR class=header>\n" +
        "<TD>Project/Task</TD>\n" +
        "<TD>Date</TD>\n" +
        "<TD>ID</TD>\n" +
        "<TD>Type</TD>\n" +
        "<TD>Injected</TD>\n" +
        "<TD>Removed</TD>\n" +
        "<TD>FixTime</TD>\n" +
        "<TD>FixDefect</TD>\n" +
        "<TD>Description</TD></TR>";

    private static final String TABLE_END_TEXT =
        "</TABLE>" +
        "<P class='doNotPrint'><A HREF=\"excel.iqy\"><I>Export to" +
        " Excel</I></A></P>";
    private static final String DISCLAIMER_TEXT =
        "<P class=doNotPrint><I>This view of the defect log is read-only. " +
        "To add entries to the defect log, use the defect button on the " +
        "dashboard. To edit or delete defects, use the defect log editor " +
        "(accessible from the Configuration menu of the dashboard).</I></P>";
    private static final String END_TEXT =
        "</BODY></HTML>";

    /** Generate CGI script output. */
    protected void writeContents() {

        String path = getPrefix();
        String title = For(path);
        String owner = For(getOwner());

        String header = HEADER_TEXT;
        header = StringUtils.findAndReplace(header, "%for owner%", owner);
        header = StringUtils.findAndReplace(header, "%for path%", title);
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
        out.print(header);

        typeFilt = getParameter("type");
        injFilt  = getParameter("inj");
        remFilt  = getParameter("rem");

        if (typeFilt != null || injFilt != null || remFilt != null) {
            out.println("Filtered to show only defects:<UL>");
            if (typeFilt != null)
                out.println("<LI>Of type &quot;" + typeFilt + "&quot;");
            if (injFilt != null)
                out.println("<LI>Injected in &quot;" + injFilt + "&quot;");
            if (remFilt != null)
                out.println("<LI>Removed in &quot;" + remFilt + "&quot;");
            out.println("</UL><P>");
        }
        out.print(START_TEXT);

        String forParam = getParameter("for");
        if (forParam != null && forParam.length() > 0)
            DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                               path, parameters, this);
        else
            DefectAnalyzer.run(getPSPProperties(), path, this);

        out.print(TABLE_END_TEXT);
        if (!parameters.containsKey("noDisclaimer"))
                out.print(DISCLAIMER_TEXT);
        out.println(END_TEXT);
    }

    private String For(String phrase) {
        if (phrase != null && phrase.length() > 1)
            return " for " + phrase;
        else
            return "";
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
        out.println("<TD>" + DateFormatter.formatDate(d.date) + "</TD>");
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
