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
import pspdash.TimeLog;
import java.io.IOException;
import java.util.Enumeration;

public class timelog extends TinyCGIBase {

    private static final String START_TEXT =
        "<HTML><HEAD><TITLE>Time Log%for path%</TITLE>%css%\n" +
        "<STYLE>\n" +
        "    TABLE { empty-cells: show }\n" +
        "    .header { font-weight: bold }\n" +
        "    TD { vertical-align: baseline }\n" +
        "</STYLE></HEAD>\n" +
        "<BODY><H1>Time Log%for path%</H1>\n" +
        "<TABLE BORDER><TR class=header>\n" +
        "<TD>Project/Task</TD>\n" +
        "<TD>Phase</TD>\n" +
        "<TD>Start Time</TD>\n" +
        "<TD>Elapsed</TD>\n" +
        "<TD>Interrupt</TD></TR>\n";

    private static final String DISCLAIMER =
        "<P><I>This view of the time log is read-only. To add entries to " +
        "the time log, use the play/pause button on the dashboard. To edit " +
        "or delete time log entries, use the time log editor (accessible " +
        "from the Configuration menu of the dashboard).</I>";


    /** Generate CGI script output. */
    protected void writeContents() throws IOException {

        String path = getPrefix(), title;
        if (path != null && path.length() > 1)
            title = " for " + path;
        else
            title = "";

        String header = START_TEXT;
        header = StringUtils.findAndReplace(header, "%for path%", title);
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
        out.print(header);

        TimeLog tl = new TimeLog();
        tl.readDefault();

        PSPProperties props = getPSPProperties();
        Enumeration rows = tl.filter(props.findExistingKey(path), null, null);
        TimeLogEntry tle;
        String entryPath, phase;
        int slashPos;
        while (rows.hasMoreElements()) {
            tle = (TimeLogEntry) rows.nextElement();
            entryPath = tle.getPath();
            slashPos = entryPath.lastIndexOf("/");
            phase = entryPath.substring(slashPos+1);
            entryPath = entryPath.substring(0, slashPos);

            out.println("<TR>");
            out.println("<TD NOWRAP>" + entryPath + "</TD>");
            out.println("<TD>" + phase + "</TD>");
            out.println("<TD>" +
                        DateFormatter.formatDateTime(tle.getStartTime()) +
                        "</TD>");
            out.println("<TD>" + tle.getElapsedTime() + "</TD>");
            out.println("<TD>" + tle.getInterruptTime() + "</TD>");
            out.println("</TR>");
        }
        out.println("</TABLE>");

        if (parameters.get("skipFooter") == null)
            out.print(DISCLAIMER);

        out.println("</BODY></HTML>");
    }
}
