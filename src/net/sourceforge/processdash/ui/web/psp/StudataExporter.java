// Copyright (C) 2006-2018 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.psp;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class StudataExporter extends TinyCGIBase {

    @Override
    protected void doPost() throws IOException {
        rejectCrossSiteRequests(env);
        parseFormData();
        StudataExporterPrefs prefs = new StudataExporterPrefs();
        String error = prefs.saveNewPrefs(parameters);

        out.write("Location: studata");
        if (error != null)
            out.write("?exportPrefs&" + error);
        out.write("\r\n\r\n");
    }

    protected void writeContents() throws IOException {
        StudataExporterPrefs prefs = new StudataExporterPrefs();
        if (parameters.containsKey("exportPrefs") || !prefs.isValid()) {
            prefs.writeForm(out, parameters);
            return;
        }

        List projectPaths = getProjectPaths();

        ResultSet data = ResultSet.get(getDataRepository(), parameters,
                "/To Date/PSP/All", getPSPProperties());

        adjustDataAndPaths(projectPaths, data);

        String studentName = getOwner();
        String forStudent = "";
        if (studentName != null)
            forStudent = " for " + HTMLUtils.escapeEntities(studentName);

        if (projectPaths.isEmpty()) {
            out.print("<html><head>");
            out.print("<title>No Student Data Found" + forStudent + "</title>");
            out.print("</head><body>\n");
            out.print("<h1>No Student Data Found" + forStudent + "</h1>\n");
            out.print("No student data was found to export" + forStudent
                    + ". Possible reasons:<ul>\n");
            out.print("<li>The student has not logged time to any of their " +
                        "PSP assignments.</li>\n");
            out.print("<li>The student has edited their hierarchy, and their "
                    + "PSP program assignments are not located underneath "
                    + HTMLUtils.escapeEntities(getPrefix()) + "</li>\n");
            out.print("</ul></body></html>\n");

        } else if (performExport(prefs, projectPaths, data)) {
            out.print("<html><head>");
            out.print("<title>Exported Student Data" + forStudent + "</title>");
            out.print("</head><body>\n");
            out.print("<h1>Exported Student Data" + forStudent + "</h1>\n");
            out.print(prefs.getHeaderMessage());
            out.print("<ul>\n");
            for (Iterator i = projectPaths.iterator(); i.hasNext();) {
                String path = (String) i.next();
                out.print("<li>" + HTMLUtils.escapeEntities(path) + "</li>\n");
            }
            out.print("</ul>\n");
            out.print(prefs.getFooterMessage());
            out.print("<hr><p><a href='studata?exportPrefs'>Export "
                    + "options...</a></p>");
            out.print("</body></html>\n");
        }
    }


    private List getProjectPaths() {
        DashHierarchy hier = getDashboardContext().getHierarchy();
        PropertyKey parent = hier.findExistingKey(getPrefix());
        int numKids = hier.getNumChildren(parent);
        List projectPaths = new ArrayList(numKids);
        ListData projectPathsData = new ListData();
        for (int i = 0;  i < numKids; i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            projectPaths.add(child.path());
            projectPathsData.add(StringData.create(child.path()));
        }
        getDataRepository().putValue("///STUDATA_List", projectPathsData);
        return projectPaths;
    }

    private void adjustDataAndPaths(List projectPaths, ResultSet data) {
        for (Iterator i = projectPaths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            int row = indexOfPath(data, path);

            // if this path is not represented in the result data,
            // remove it from the list of project paths.
            if (row == -1) {
                i.remove();
                continue;
            }

            // if this path is a PSP0 project, zero out the time estimates.
            if (hasTag(path, "PSP0"))
                clobberPSP0TimeEstimates(data, row);
        }
    }

    private void clobberPSP0TimeEstimates(ResultSet data, int row) {
        for (int col = data.numCols();  col > 0;  col--) {
            String colName = data.getColName(col);
            if (colName.endsWith("EstTime"))
                data.setData(row, col, ImmutableDoubleData.READ_ONLY_ZERO);
        }
    }

    /**
     * Export the data according to the preferences provided by the user.
     */
    private boolean performExport(StudataExporterPrefs prefs,
            List projectPaths, ResultSet data) {
        switch (prefs.getMethod()) {
        case Clipboard:
            exportToClipboard(projectPaths, data);
            return true;

        case Xml:
            return exportToXml(prefs, projectPaths, data);

        default:
            throw new IllegalStateException("No recognized export method");
        }

    }

    // routines for exporting to the clipboard

    private void exportToClipboard(List projectPaths, ResultSet data) {
        StringBuffer result = new StringBuffer();
        for (Iterator i = projectPaths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            int row = indexOfPath(data, path);
            if (row != -1)
                copyRowData(data, row, result);
        }
        setClipboard(result.toString());
    }

    private void copyRowData(ResultSet data, int row, StringBuffer dest) {
        for (int col = 2;  col < data.numCols();  col++)
            dest.append(data.format(row, col)).append("\t");
        dest.append(data.format(row, data.numCols())).append("\n");
    }

    // routines for exporting to XML

    private boolean exportToXml(StudataExporterPrefs prefs, List projectPaths,
            ResultSet data) {
        File outputFile = prefs.getTargetFile(getOwner());
        try {
            writeXmlFile(outputFile, projectPaths, data);
            setClipboard(outputFile.getAbsolutePath());
            return true;
        } catch (IOException ioe) {
            writeExportException(outputFile, ioe);
            return false;
        }
    }

    private void writeXmlFile(File outputFile, List projectPaths,
            ResultSet data) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(
                outputFile));
        DataContext profileData = getDataRepository().getSubcontext(
            getStudentProfilePath());
        StudataExporterXml.writeXmlData(out, profileData, projectPaths, data);
        out.close();
    }

    private String getStudentProfilePath() {
        DashHierarchy hier = getDashboardContext().getHierarchy();
        PropertyKey parent = hier.findExistingKey(getPrefix());
        int numKids = hier.getNumChildren(parent);
        for (int i = 0;  i < numKids;  i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            String path = child.path();
            if (hasTag(path, "PspForEngV3_Student_Profile"))
                return path;
        }

        // no student profile was found.  Create an imaginary one; it will
        // still allow the name and programming language to be exported.
        return getPrefix() + "/No Student Profile Found";
    }

    private void writeExportException(File outputFile, IOException ioe) {
        out.print("<html><head>");
        out.print("<title>Unable to Export Student Data</title>");
        out.print("</head><body>\n");
        out.print("<h1>Unable to Export Student Data</h1>\n");
        out.print("<p>The Process Dashboard attempted to export student data "
                + "to the file:</p>\n<pre>        ");
        out.print(outputFile.getPath());
        out.print("</pre>");
        out.print("<p>Unfortunately, this was unsuccessful. Please ensure "
                + "that you can write to the file in question.  Then "
                + "<a href='studata'>refresh this page</a> to try the export "
                + "again. (To change the location where the file will be "
                + "saved, click the \"Export options\" link below.)</p>");
        out.print("<hr><p><a href='studata?exportPrefs'>Export "
                + "options...</a></p>");
        out.print("</body></html>\n");
    }


    // Reusable routines

    private void setClipboard(String clipboardData) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new StringSelection(clipboardData), null);
    }

    private int indexOfPath(ResultSet data, String path) {
        for (int i = data.numRows();  i > 0;  i--) {
            if (path.equals(data.getRowName(i)))
                return i;
        }
        return -1;
    }

    private boolean hasTag(String path, String tagName) {
        String dataName = DataRepository.createDataName(path, tagName);
        return (getDataRepository().getSimpleValue(dataName) instanceof TagData);
    }

}
