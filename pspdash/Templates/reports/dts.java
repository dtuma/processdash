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

public class dts extends TinyCGIBase {

    private static final String NAME = "name";

    /** Generate CGI script output. */
    protected void writeContents() {

        DefectTypeStandard defectTypeStandard = null;
        if (parameters.get(NAME) != null)
            defectTypeStandard = DefectTypeStandard.getByName
                (getParameter(NAME), getDataRepository());
        if (defectTypeStandard == null)
            defectTypeStandard = DefectTypeStandard.get
                (getPrefix(), getDataRepository());

        String name = defectTypeStandard.getName();
        String title = (name == null ? "" : " (" + name + ")");

        out.println("<HTML><HEAD>" + cssLinkHTML());
        out.println("<TITLE>Defect Type Standard" + title + "</TITLE>");
        out.println("</HEAD><BODY>");
        out.println("<H1>Defect Type Standard" + title + "</H1>");
        out.println("<TABLE BORDER><TR><TD><B>Type</B></TD>");
        out.println("    <TD><B>Description</B></TD></TR>");

        String type, description;
        for (int i=0;  i<defectTypeStandard.options.size();  i++) {
            type = (String) defectTypeStandard.options.elementAt(i);
            description = (String) defectTypeStandard.comments.get(type);
            if (description == null) description = "&nbsp;";

            out.println("<TR><TD VALIGN=baseline>" + type + "</TD>");
            out.println("    <TD VALIGN=baseline>" + description +
                        "</TD></TR>");
        }
        out.println("</TABLE>");
        if ("PSP - text".equals(name) || "PSP - numbers".equals(name))
            out.println(COPYRIGHT_NOTICE);
        out.println("</BODY></HTML>");
    }

    private static final String COPYRIGHT_NOTICE =
        "<HR><FONT SIZE='-1'><I>Adapted from\n" +
        "<a href='/help/Topics/Overview/disc-eng.html'>A&nbsp;Discipline\n" +
        "for Software Engineering</a>, copyright &copy;&nbsp;1995\n" +
        "Addison-Wesley. Used by permission.</I></FONT>\n";
}
