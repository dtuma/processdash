// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class ArchiveMetricsFileExporter implements Runnable,
        ArchiveMetricsXmlConstants {

    private static final String DATA_FILE_NAME = "data.xml";

    private static final String DEFECT_FILE_NAME = "defects.xml";

    private static final String EV_FILE_NAME = "ev.xml";

    private DashboardContext ctx;

    private File dest;

    private Collection filter;

    private List metricsIncludes;

    private List metricsExcludes;

    public ArchiveMetricsFileExporter(DashboardContext ctx, File dest,
            Collection filter) {
        this(ctx, dest, filter, null, null);
    }

    public ArchiveMetricsFileExporter(DashboardContext ctx, File dest,
            Collection filter, List metricsIncludes, List metricsExcludes) {
        this.ctx = ctx;
        this.dest = dest;
        this.filter = filter;
        this.metricsIncludes = metricsIncludes;
        this.metricsExcludes = metricsExcludes;
    }

        public void run() {
        try {
            doExport();
        } catch (Exception ioe) {
            ioe.printStackTrace();
            dest.delete();
        }
    }

    private void doExport() throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(
                new RobustFileOutputStream(dest));

        Collection taskListNames = writeData(zipOut);
        if (!taskListNames.isEmpty())
            writeTaskLists(zipOut, taskListNames);
        writeDefects(zipOut);
        writeManifest(zipOut, !taskListNames.isEmpty());

        zipOut.close();
    }

    private void writeManifest(ZipOutputStream zipOut, boolean includeTaskLists)
            throws IOException {
        zipOut.putNextEntry(new ZipEntry(MANIFEST_FILE_NAME));

        XmlSerializer xml = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            xml = factory.newSerializer();
        } catch (XmlPullParserException xppe) {
            throw new RuntimeException("Couldn't obtain xml serializer", xppe);
        }

        xml.setOutput(zipOut, ENCODING);
        xml.startDocument(ENCODING, Boolean.TRUE);
        xml.ignorableWhitespace(NEWLINE + NEWLINE);

        xml.startTag(null, ARCHIVE_ELEM);
        xml.attribute(null, TYPE_ATTR, FILE_TYPE_ARCHIVE);
        xml.ignorableWhitespace(NEWLINE);

        writeManifestMetaData(xml);
        writeManifestFileEntry(xml, DATA_FILE_NAME, FILE_TYPE_METRICS, "1");
        writeManifestFileEntry(xml, DEFECT_FILE_NAME, FILE_TYPE_DEFECTS, "1");
        if (includeTaskLists)
            writeManifestFileEntry(xml, EV_FILE_NAME, FILE_TYPE_EARNED_VALUE,
                    "1");
        xml.endTag(null, ARCHIVE_ELEM);
        xml.ignorableWhitespace(NEWLINE);
        xml.endDocument();

        zipOut.closeEntry();
    }

    private void writeManifestMetaData(XmlSerializer xml) throws IOException {
        xml.ignorableWhitespace(INDENT);
        xml.startTag(null, EXPORTED_TAG);
        String owner = ProcessDashboard.getOwnerName(ctx.getData());
        if (owner != null)
            xml.attribute(null, OWNER_ATTR, owner);

        xml.attribute(null, WHEN_ATTR, XMLUtils.saveDate(new Date()));

        List packages = TemplateLoader.getPackages();
        for (Iterator i = packages.iterator(); i.hasNext();) {
            DashPackage pkg = (DashPackage) i.next();
            xml.ignorableWhitespace(NEWLINE + INDENT + INDENT);
            xml.startTag(null, PACKAGE_ELEM);
            if (pkg.id != null)
                xml.attribute(null, PACKAGE_ID_ATTR, pkg.id);
            if (pkg.version != null)
                xml.attribute(null, VERSION_ATTR, pkg.version);
            xml.endTag(null, PACKAGE_ELEM);
        }

        xml.ignorableWhitespace(NEWLINE + INDENT);
        xml.endTag(null, EXPORTED_TAG);
        xml.ignorableWhitespace(NEWLINE);
    }

    private void writeManifestFileEntry(XmlSerializer xml, String filename,
            String type, String version) throws IOException {

        xml.ignorableWhitespace(INDENT);
        xml.startTag(null, FILE_ELEM);
        xml.attribute(null, FILE_NAME_ATTR, filename);
        xml.attribute(null, TYPE_ATTR, type);
        xml.attribute(null, VERSION_ATTR, version);
        xml.endTag(null, FILE_ELEM);
        xml.ignorableWhitespace(NEWLINE);
    }

    private Collection writeData(ZipOutputStream zipOut) throws IOException {
        zipOut.putNextEntry(new ZipEntry(DATA_FILE_NAME));

        Iterator iter = new ExportedDataValueIterator(ctx.getData(), filter);
        TaskListDataWatcher taskListWatcher = new TaskListDataWatcher(iter);
        DefaultDataExportFilter ddef = new DefaultDataExportFilter(
                taskListWatcher);
        ddef.setIncludes(metricsIncludes);
        ddef.setExcludes(metricsExcludes);
        ddef.init();

        DataExporter exp = new DataExporterXMLv1();
        exp.export(zipOut, ddef);

        zipOut.closeEntry();

        return taskListWatcher.getTaskListNames();
    }

    private void writeDefects(ZipOutputStream zipOut) throws IOException {
        zipOut.putNextEntry(new ZipEntry(DEFECT_FILE_NAME));

        DefectExporter exp = new DefectExporterXMLv1();
        exp.dumpDefects(ctx.getHierarchy(), filter, zipOut);

        zipOut.closeEntry();
    }

    private void writeTaskLists(ZipOutputStream zipOut, Collection taskListNames)
            throws IOException {

        Map schedules = getEVSchedules(taskListNames);

        zipOut.putNextEntry(new ZipEntry(EV_FILE_NAME));
        EVExporter exp = new EVExporterXMLv1();
        exp.export(zipOut, schedules);

        zipOut.closeEntry();
    }

    private Map getEVSchedules(Collection taskListNames) {
        Map schedules = new TreeMap();
        for (Iterator iter = taskListNames.iterator(); iter.hasNext();) {
            String taskScheduleName = (String) iter.next();
            EVTaskList tl = EVTaskList.openExisting(taskScheduleName, ctx
                    .getData(), ctx.getHierarchy(), ctx.getCache(), false);
            if (tl == null)
                continue;

            tl.recalc();
            schedules.put(taskScheduleName, tl);
        }
        return schedules;
    }

}
