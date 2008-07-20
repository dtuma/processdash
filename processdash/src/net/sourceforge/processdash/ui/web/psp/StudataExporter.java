// Copyright (C) 2006 Tuma Solutions, LLC
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class StudataExporter extends TinyCGIBase {


    protected void writeContents() throws IOException {
        List projectPaths = getProjectPaths();

        ResultSet data = ResultSet.get(getDataRepository(), parameters,
                "/To Date/PSP/All", getPSPProperties());

        String clipboardData = getClipboardData(projectPaths, data);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(clipboardData), null);

        String studentName = getOwner();
        String forStudent = "";
        if (studentName != null)
            forStudent = " for " + HTMLUtils.escapeEntities(studentName);

        if (clipboardData.length() == 0) {
            out.print("<html><head>");
            out.print("<title>No STUDATA Found" + forStudent + "</title>");
            out.print("</head><body>\n");
            out.print("<h1>No STUDATA Found" + forStudent + "</h1>\n");
            out.print("No STUDATA was found to export" + forStudent
                    + ". Possible reasons:<ul>\n");
            out.print("<li>The student has not marked any of their PSP " +
                        "assignments complete.</li>\n");
            out.print("<li>The student has edited their hierarchy, and their "
                    + "PSP program assignments are not located underneath "
                    + HTMLUtils.escapeEntities(getPrefix()) + "</li>\n");
            out.print("</ul></body></html>\n");

        } else {
            out.print("<html><head>");
            out.print("<title>Exported STUDATA" + forStudent + "</title>");
            out.print("</head><body>\n");
            out.print("<h1>Exported STUDATA" + forStudent + "</h1>\n");
            out.print("Project metrics were copied to the clipboard (in a "
                    + "format ready for pasting into a STU#.xls spreadsheet) "
                    + "for the following programs:<ul>\n");
            for (Iterator i = projectPaths.iterator(); i.hasNext();) {
                String path = (String) i.next();
                out.print("<li>" + HTMLUtils.escapeEntities(path) + "</li>\n");
            }
            out.print("</ul>\n");
            out.print("Note: you may need to unlock the STU#.xls spreadsheet "
                    + "(Tools > Protection > Unprotect Sheet) in order to "
                    + "paste the data.\n");
            out.print("</body></html>\n");
        }
    }

    private List getProjectPaths() {
        DashHierarchy hier = getDashboardContext().getHierarchy();
        PropertyKey parent = hier.findExistingKey(getPrefix());
        int numKids = hier.getNumChildren(parent);
        List projectPaths = new ArrayList(numKids);
        for (int i = 0;  i < numKids; i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            projectPaths.add(child.path());
        }
        return projectPaths;
    }

    private String getClipboardData(List projectPaths, ResultSet data) {
        StringBuffer result = new StringBuffer();
        for (Iterator i = projectPaths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            int row = indexOfPath(data, path);
            if (row != -1)
                copyRowData(data, row, result);
            else
                i.remove();
        }
        return result.toString();
    }

    private int indexOfPath(ResultSet data, String path) {
        for (int i = data.numRows();  i > 0;  i--) {
            if (path.equals(data.getRowName(i)))
                return i;
        }
        return -1;
    }

    private void copyRowData(ResultSet data, int row, StringBuffer dest) {
        for (int col = 2;  col < data.numCols();  col++)
            dest.append(data.format(row, col)).append("\t");
        dest.append(data.format(row, data.numCols())).append("\n");
    }
}
