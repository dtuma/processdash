// Copyright (C) 2005-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.tool.export.mgr.Cancellable;
import net.sourceforge.processdash.tool.export.mgr.CompletionStatus;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.RobustFileWriter;

public class TextMetricsFileExporter implements Runnable,
        CompletionStatus.Capable, Cancellable {

    private DashboardContext ctx;

    private File dest;

    private Collection filter;

    private RobustFileWriter outWriter;

    private CompletionStatus completionStatus = CompletionStatus.NOT_RUN_STATUS;

    public TextMetricsFileExporter(DashboardContext ctx, File dest,
            Collection filter) {
        this.ctx = ctx;
        this.dest = dest;
        this.filter = filter;
    }

    public CompletionStatus getCompletionStatus() {
        return completionStatus;
    }

    public void tryCancel() {
        RobustFileWriter ow = outWriter;
        if (ow != null) {
            try {
                ow.abort();
            } catch (Exception e) {}
        }
    }

    public void run() {
        try {
            outWriter = new RobustFileWriter(dest, "UTF-8");
            PrintWriter out = new PrintWriter(new BufferedWriter(outWriter));

            // Find and print any applicable task lists.
            Iterator i = ctx.getData().getKeys();
            Set taskListNames = new HashSet();
            String name;
            int pos;
            while (i.hasNext()) {
                name = (String) i.next();
                pos = name.indexOf(TASK_ORD_PREF);
                if (pos != -1 && Filter.matchesFilter(filter, name))
                    taskListNames.add(name.substring(pos
                            + TASK_ORD_PREF.length()));
            }
            i = taskListNames.iterator();
            String owner = ProcessDashboard.getOwnerName(ctx.getData());
            while (i.hasNext()) {
                name = (String) i.next();
                EVTaskList tl = EVTaskList.openExisting(name, ctx.getData(),
                        ctx.getHierarchy(), ctx.getCache(), false);
                if (tl == null)
                    continue;

                tl.recalc();
                String xml = tl.getAsXML(false);
                name = exportedScheduleDataName(owner, name);
                out.write(name + ",");
                out.write(StringData.escapeString(xml));
                out.println();
            }

            ctx.getData().dumpRepository(out, filter,
                    DataRepository.DUMP_STYLE_TEXT);

            TimeLog tl = ctx.getTimeLog();
            Iterator keys = tl.filter(null, null, null);
            while (keys.hasNext()) {
                TimeLogEntry tle = (TimeLogEntry) keys.next();
                if (Filter.matchesFilter(filter, tle.getPath()))
                    out.println(toAbbrevString(tle));
            }

            out.println(DefectXmlConstantsv1.DEFECT_START_TOKEN);
            DefectExporterXMLv1 exp = new DefectExporterXMLv1();
            exp.dumpDefects(ctx.getHierarchy(), filter, out);

            out.close();
            outWriter = null;

            completionStatus = new CompletionStatus(CompletionStatus.SUCCESS,
                    dest, null);

        } catch (Exception ioe) {
            completionStatus = new CompletionStatus(CompletionStatus.ERROR,
                    dest, ioe);
            System.out.println("IOException: " + ioe);
            tryCancel();
        }

        ctx.getData().gc(filter);
    }

    private static String TASK_ORD_PREF = "/"
            + EVTaskListData.TASK_ORDINAL_PREFIX;

    private static Object toAbbrevString(TimeLogEntry tle) {
        return ("!" + FormatUtil.formatDateTime(tle.getStartTime()) + "!," + tle
                .getElapsedTime());
    }

    private static String exportedScheduleDataName(String owner,
            String scheduleName) {
        return ExportManager.exportedScheduleDataPrefix(owner, scheduleName)
                + TextMetricsFileImporter.XML_DATA_NAME_SUFFIX;
    }

}
