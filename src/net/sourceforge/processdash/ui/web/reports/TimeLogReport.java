// Copyright (C) 2001-2009 Tuma Solutions, LLC
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


import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.RolledUpTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogWriter;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class TimeLogReport extends TinyCGIBase {

    private static final Resources resources =
        Resources.getDashBundle("Time.Report");

    private static final String START_TEXT =
        "<html><head><title>${Title}%for owner%%for path%</title>%css%\n" +
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/reports/timeReports.css\">"+
        "</head>\n" +
        "<body><h1>${Title}%for path%</h1>\n" +
        "<!-- cutStart --><p><table border class='timeLog'><tr>\n" +
        "<th>${Project}</th>\n" +
        "<th>${Phase}</th>\n" +
        "<th>${Start_Time}</th>\n" +
        "<th>${Elapsed}</th>\n" +
        "<th>${Interrupt}</th>\n" +
        "<th>${Comment}</th></tr>\n";

    private static final String EXPORT_LINK =
        "<P class=doNotPrint><A HREF=\"excel.iqy\"><I>" +
        "${Export_to_Excel}</I></A></P>";

    private static final String DISCLAIMER =
        "<P class=doNotPrint><I>${Caveat}</I></P>";


    @Override
    protected void writeHeader() {}

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        List timeLogEntries = getTimeLogEntries();

        String format = getParameter(FORMAT_PARAM);
        if (FORMAT_XML.equals(format))
            writeXml(timeLogEntries);
        else
            writeHtml(timeLogEntries);
    }

    private List getTimeLogEntries() throws IOException {
        TimeLog tl;
        String type = getParameter("type");
        if ("rollup".equals(type))
            tl = new RolledUpTimeLog.FromResultSet(getDashboardContext(),
                    getPrefix(), parameters);
        else
            tl = getDashboardContext().getTimeLog();
        List l = Collections.list(tl.filter(getPrefix(), null, null));
        Collections.sort(l);
        return l;
    }



    /** Write time log data in XML format */
    private void writeXml(List timeLogEntries) throws IOException {
        out.write("Content-Type: text/xml\r\n\r\n");
        out.flush();

        TimeLogWriter.write(outStream, timeLogEntries.iterator(), false);
    }



    /** Write time log data in HTML format */
    private void writeHtml(List l) throws IOException {
        super.writeHeader();

        String path = getPrefix();
        String title = For(path);
        String owner = For(getOwner());

        String header = START_TEXT;
        header = resources.interpolate(header, HTMLUtils.ESC_ENTITIES);
        header = StringUtils.findAndReplace(header, "%for owner%", owner);
        header = StringUtils.findAndReplace(header, "%for path%", title);
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
        out.print(header);

        ProcessUtil procUtil = new ProcessUtil(getDataContext());

        for (Iterator rows = l.iterator(); rows.hasNext();) {
            TimeLogEntry tle = (TimeLogEntry) rows.next();
            String entryPath = tle.getPath();
            String phase = procUtil.getEffectivePhase(entryPath, false);
            if (phase == null) {
                int slashPos = entryPath.lastIndexOf('/');
                phase = entryPath.substring(slashPos+1);
                entryPath = entryPath.substring(0, slashPos);
            }

            out.println("<TR>");
            out.println("<TD NOWRAP>" + HTMLUtils.escapeEntities(entryPath)
                    + "</TD>");
            out.println("<TD>" + HTMLUtils.escapeEntities(phase) + "</TD>");
            out.println("<TD>" +
                        FormatUtil.formatDateTime(tle.getStartTime()) +
                        "</TD>");
            out.println("<TD>" + tle.getElapsedTime() + "</TD>");
            out.println("<TD>" + tle.getInterruptTime() + "</TD>");
            String comment = tle.getComment();
            out.println("<TD>" + (comment == null ? ""
                    : HTMLUtils.escapeEntities(comment)) + "</TD>");
            out.println("</TR>");
        }
        out.println("</TABLE><!-- cutEnd -->");

        if (parameters.get("skipFooter") == null) {
            if (!isExportingToExcel())
                out.print(resources.interpolate(EXPORT_LINK,
                        HTMLUtils.ESC_ENTITIES));
            String type = getParameter("type");
            if (!isExporting() && !"rollup".equals(type)
                    && !parameters.containsKey("noDisclaimer")) {
                StringBuffer html = new StringBuffer(resources.interpolate(
                        DISCLAIMER, HTMLUtils.ESC_ENTITIES));
                StringUtils.findAndReplace(html, "&lt;a&gt;",
                        "<a href='../control/showTimeLog'>");
                StringUtils.findAndReplace(html, "&lt;/a&gt;", "</a>");
                out.print(html.toString());
            }
        }

        out.println("</BODY></HTML>");
    }

    private String For(String phrase) {
        if (phrase != null && phrase.length() > 1)
            return HTMLUtils.escapeEntities(resources.format("For_FMT", phrase));
        else
            return "";
    }

    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_XML = "xml";

}
