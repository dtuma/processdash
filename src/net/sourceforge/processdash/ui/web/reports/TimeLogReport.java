// Copyright (C) 2001-2017 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.RolledUpTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEntryVO;
import net.sourceforge.processdash.log.time.TimeLogWriter;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class TimeLogReport extends TinyCGIBase {

    public static final String PERMISSION = "pdash.indivData.timeLog";

    private static final Resources resources =
        Resources.getDashBundle("Time.Report");

    private static final String HEADER_TEXT =
        "<html><head><title>${Title}%for owner%%for path%</title>%css%\n" +
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/reports/timeReports.css\">"+
        "</head>\n" +
        "<body><h1>${Title}%for path%</h1>\n" +
        "<!-- cutStart -->";
    private static final String START_TEXT =
        "<p><table border class='timeLog'><tr>\n" +
        "<th>${Project}</th>\n" +
        "<th>${Phase}</th>\n" +
        "<th>${Start_Time}</th>\n" +
        "<th>${Elapsed}</th>\n" +
        "<th>${Interrupt}</th>\n";
    private static final String COMMENT_HEADER = "<th>${Comment}</th>\n";

    private static final String EXPORT_LINK =
        "<P class=doNotPrint><A HREF=\"excel.iqy\"><I>" +
        "${Export_to_Excel}</I></A></P>";

    private static final String BLOCKED_BY_PERMISSION =
        "<P class=doNotPrint><I>${No_Permission}</I></P>";
    private static final String FILTERED_BY_PERMISSION =
        "<P class=doNotPrint><I>${Filtered_By_Permission}</I></P>";
    private static final String DISCLAIMER =
        "<P class=doNotPrint><I>${Caveat}</I></P>";

    private boolean noPermission, someEntriesBlocked;


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
        // ensure the user has permission to view time log entries
        UserFilter privacyFilt = GroupPermission.getGrantedMembers(PERMISSION);
        noPermission = (privacyFilt == null);
        someEntriesBlocked = false;
        if (noPermission)
            return Collections.EMPTY_LIST;

        TimeLog tl = null;
        List l = null;
        String type = getParameter("type");
        if ("rollup".equals(type)) {
            // the legacy time log logic cannot support user group filters. If
            // someone with attempts to run that script, and they do not have
            // permission to view time logs from "Everyone," block the data.
            if (!UserGroup.isEveryone(privacyFilt)) {
                someEntriesBlocked = true;
                return Collections.EMPTY_LIST;
            } else {
                tl = new RolledUpTimeLog.FromResultSet(getDashboardContext(),
                        getPrefix(), parameters);
            }

        } else if ("db".equals(type)) {
            l = queryDatabaseForTimeLogEntries(privacyFilt);

        } else {
            tl = getDashboardContext().getTimeLog();
        }

        if (tl != null)
            l = Collections.list(tl.filter(getPrefix(), null, null));
        Collections.sort(l);
        return l;
    }

    private List queryDatabaseForTimeLogEntries(UserFilter privacyFilter) {
        // retrieve a mapping of dataset IDs, if we need it for testing privacy
        PDashQuery query = getPdash().getQuery();
        Map<Object, String> datasetIDs = null;
        if (!UserGroup.isEveryone(privacyFilter))
            datasetIDs = QueryUtils.mapColumns(query.query(DATASET_ID_QUERY));

        // retrieve the ID of the process whose phases we should map to
        String processID = getParameter("processID");
        if (processID == null)
            processID = new ProcessUtil(getDataContext()).getProcessID();

        // retrieve the raw data for the time log entries themselves
        List<Object[]> rawData = query.query(TIME_LOG_HQL, processID);
        rawData.addAll(query.query(TIME_LOG_UNCAT_HQL));

        // build a list of time log entries
        List<TimeLogEntry> result = new ArrayList<TimeLogEntry>();
        for (Object[] row : rawData) {
            String path = row[0] + "/" + row[1];
            if (path.startsWith("//"))
                path = path.substring(1);
            Date start = (Date) row[2];
            long delta = ((Number) row[3]).longValue();
            long interrupt = ((Number) row[4]).longValue();
            String comment = (String) row[5];

            // if a privacy filter is in effect, see if it excludes this entry
            if (datasetIDs != null) {
                String datasetID = datasetIDs.get(row[6]);
                if (!privacyFilter.getDatasetIDs().contains(datasetID)) {
                    someEntriesBlocked = true;
                    continue;
                }
            }

            // create a time log entry and add it to the list
            result.add(new TimeLogEntryVO(0, path, start, delta, //
                    interrupt, comment));
        }

        return result;
    }

    private static final String DATASET_ID_QUERY = "select " //
            + "p.person.key, p.value.text " //
            + "from PersonAttrFact as p "
            + "where p.attribute.identifier = 'person.pdash.dataset_id'";

    private static final String TIME_LOG_HQL = "select "
            + "t.planItem, phase.shortName, t.startDate, "
            + "t.deltaMin, t.interruptMin, comment.text, "
            + "t.dataBlock.person.key " //
            + "from TimeLogFact as t "
            + "join t.planItem.phase.mapsToPhase phase "
            + "left outer join t.comment comment "
            + "where phase.process.identifier = ? " //
            + "order by t.startDate";

    private static final String TIME_LOG_UNCAT_HQL = "select "
            + "t.planItem, ' ', t.startDate, "
            + "t.deltaMin, t.interruptMin, comment.text, "
            + "t.dataBlock.person.key " //
            + "from TimeLogFact as t " //
            + "left outer join t.comment comment "
            + "where t.planItem.phase is null " //
            + "order by t.startDate";



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

        String header = HEADER_TEXT;
        header = resources.interpolate(header, HTMLUtils.ESC_ENTITIES);
        header = StringUtils.findAndReplace(header, "%for owner%", owner);
        header = StringUtils.findAndReplace(header, "%for path%", title);
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
        out.print(header);

        if (noPermission) {
            interpOut(BLOCKED_BY_PERMISSION);
            out.print("</body></html>");
            return;
        }

        interpOut(START_TEXT);
        boolean showComments = !parameters.containsKey("hideComments");
        if (showComments)
            interpOut(COMMENT_HEADER);
        out.println("</tr>");

        String type = getParameter("type");
        ProcessUtil procUtil = null;
        if (!"rollup".equals(type) && !"db".equals(type))
            procUtil = new ProcessUtil(getDataContext());

        for (Iterator rows = l.iterator(); rows.hasNext();) {
            TimeLogEntry tle = (TimeLogEntry) rows.next();
            String entryPath = tle.getPath();
            String phase = (procUtil == null ? null
                    : procUtil.getEffectivePhase(entryPath, false));
            if (phase == null) {
                int slashPos = entryPath.lastIndexOf('/');
                phase = entryPath.substring(slashPos+1);
                entryPath = entryPath.substring(0, slashPos);
            }

            out.println("<TR>");
            out.println("<TD>" + HTMLUtils.escapeEntities(entryPath) + "</TD>");
            out.println("<TD>" + HTMLUtils.escapeEntities(phase) + "</TD>");
            out.println("<TD>" + (tle.getStartTime() == null ? ""
                    : FormatUtil.formatDateTime(tle.getStartTime())) + "</TD>");
            out.println("<TD>" + tle.getElapsedTime() + "</TD>");
            out.println("<TD>" + tle.getInterruptTime() + "</TD>");
            if (showComments) {
                String comment = tle.getComment();
                out.println("<TD>" + (comment == null ? ""
                        : HTMLUtils.escapeEntities(comment)) + "</TD>");
            }
            out.println("</TR>");
        }
        out.println("</TABLE>");

        if (!isExporting() && someEntriesBlocked)
            interpOut(FILTERED_BY_PERMISSION);

        out.println("<!-- cutEnd -->");

        if (parameters.get("skipFooter") == null) {
            if (!isExportingToExcel())
                out.print(resources.interpolate(EXPORT_LINK,
                        HTMLUtils.ESC_ENTITIES));
            if (!isExporting() && !"rollup".equals(type) && !"db".equals(type)
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

    private void interpOut(String html) {
        out.println(resources.interpolate(html, HTMLUtils.ESC_ENTITIES));
    }

    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_XML = "xml";

}
