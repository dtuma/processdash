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
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Enumeration;

public class timecard extends TinyCGIBase {

    /** Write a standard CGI header.
     */
    protected void writeHeader() {
        out.print("Content-type: application/vnd.ms-excel\r\n\r\n");
    }

    private static final String HEADER_START =
        "<HTML><HEAD><TITLE>Excel Time Card</TITLE>\n" +
        //"<BODY><H1>Excel Time Card</H1>\n" +
        //"If you are seeing this message, ..." +
        "<TABLE BORDER CROSSTAB><TR>\n"+
        "<TH PAGEFIELD>Month</TH>" +
        "<TH COLFIELD>Day</TH>" +
        "<TH DATAFIELD AGGREGATOR=\"SUM\">Time</TH>";

    private static final String HEADER_MIDA = "<TH ROWFIELD>Hier";
    private static final String HEADER_MIDB = "</TH>";

    private static final String HEADER_END = "</TR>\n";

    private static SimpleDateFormat MONTH = null;
    private static SimpleDateFormat DAY = null;

    static {
        try {
            MONTH = new SimpleDateFormat("yyyy-MMM");
            MONTH.setTimeZone(TimeZone.getDefault());
            DAY = new SimpleDateFormat("d");
            DAY.setTimeZone(TimeZone.getDefault());
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {

        //String path = getPrefix();
        //PSPProperties props = getPSPProperties();
        //PropertyKey key = props.findExistingKey(path);
        System.out.println("getting time log");

        // Get a copy of the time log.
        TimeLog tl = new TimeLog();
        tl.readDefault();

        System.out.println("scanning for depth");
        // first, scan the time log to see how deep a hierarchy we need.
        Enumeration rows = tl.filter(PropertyKey.ROOT, null, null);
        TimeLogEntry tle;
        String entryPath;
        int depth = 1, currDepth, i;
        while (rows.hasMoreElements()) {
            tle = (TimeLogEntry) rows.nextElement();
            entryPath = tle.getPath();
            System.out.println(entryPath);
            currDepth = countSlashes(entryPath);
            if (currDepth > depth) depth = currDepth;
        }
        System.out.println("depth is " + depth);

        out.print(HEADER_START);
        for (i=0;   i++ < depth;  )
            out.print(HEADER_MIDA + i + HEADER_MIDB);
        out.print(HEADER_END);

        System.out.println("scanning to print");

        // Now scan the time log and print out each row.
        rows = tl.filter(PropertyKey.ROOT, null, null);
        StringTokenizer tok;
        while (rows.hasMoreElements()) {
            tle = (TimeLogEntry) rows.nextElement();

            out.println("<TR>");

            printCell(MONTH.format(tle.getStartTime()));
            printCell(DAY.format(tle.getStartTime()));
            printCell(Long.toString(tle.getElapsedTime()));

            tok = new StringTokenizer(tle.getPath(), "/");
            for (i=depth;   i-- > 0; )
                if (tok.hasMoreTokens())
                    printCell(tok.nextToken());
                else
                    printCell("&nbsp;");

            out.println("</TR>");
        }
        out.println("</TABLE>");
        out.println("</BODY></HTML>");
    }
    private int countSlashes(String path) {
        return (new StringTokenizer(path, "/")).countTokens();
    }

    private void printCell(String contents) {
        out.print("<TD>");
        out.print(contents);
        out.println("</TD>");
    }
}
