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
import java.io.IOException;
import java.util.Enumeration;

public class defectlog extends TinyCGIBase implements DefectAnalyzer.Task {

    private String typeFilt, injFilt, remFilt;

    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>Defect Log%for path%</TITLE>\n" +
        "<STYLE>\n" +
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

    private static final String END_TEXT =
        "</TABLE>" +
        "<P><I>This view of the defect log is read-only. To add, edit, or " +
        "delete defects, use the defect log editor (accessible from the " +
        "Configuration menu of the dashboard).</I>" +
        "</BODY></HTML>";

    /** Generate CGI script output. */
    protected void writeContents() {

        String path = getPrefix(), title;
        if (path != null && path.length() > 1)
            title = " for " + path;
        else
            title = "";

        out.print(StringUtils.findAndReplace(HEADER_TEXT, "%for path%",
                                             title));
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
            out.println("</UL>");
        }
        out.print(START_TEXT);

        DefectAnalyzer.run(getPSPProperties(), path, this);

        out.println(END_TEXT);
    }

    public void analyze(String path, Defect d) {
        if ((typeFilt != null && !d.defect_type.equalsIgnoreCase(typeFilt)) ||
            (injFilt != null && !d.phase_injected.equalsIgnoreCase(injFilt)) ||
            (remFilt != null && !d.phase_removed.equalsIgnoreCase(remFilt)))
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
