// Copyright (C) 2004-2012 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectAnalyzer;
import net.sourceforge.processdash.util.XMLUtils;

public class DefectExporterXMLv1 implements DefectExporter,
        DefectXmlConstantsv1 {

    public void dumpDefects(DashHierarchy hierarchy, Collection filter,
            OutputStream out) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, ENCODING);
        PrintWriter pw = new PrintWriter(writer);
        pw.write(XML_HEADER + NEWLINE + NEWLINE);
        dumpDefects(hierarchy, filter, pw);
    }

    public void dumpDefects(DashHierarchy hierarchy, Collection filter,
            PrintWriter out) {
        out.println("<defects>");

        for (Iterator i = filter.iterator(); i.hasNext();) {
            String path = (String) i.next();
            DefectAnalyzer.run(hierarchy, path, true,
                    new DefectWriter(out, path));
        }

        out.println("</defects>");
        out.flush();
    }

    private class DefectWriter implements DefectAnalyzer.Task {

        private PrintWriter out;

        // private String basePath;

        public DefectWriter(PrintWriter out, String basePath) {
            this.out = out;
            // this.basePath = basePath + "/";
        }

        public void analyze(String path, Defect d) {
            out.print("<" + DEFECT_TAG);
            writeAttr(PATH_ATTR, path);
            // if (path.startsWith(basePath))
            // writeAttr("relPath", path.substring(basePath.length()));
            if (d.date != null)
                writeAttr(DATE_ATTR, XMLUtils.saveDate(d.date));
            writeAttr(NUM_ATTR, d.number);
            writeAttr(DEFECT_TYPE_ATTR, d.defect_type);
            writeAttr(INJECTED_ATTR, d.phase_injected);
            writeAttr(REMOVED_ATTR, d.phase_removed);
            writeAttr(FIX_TIME_ATTR, Float.toString(d.getFixTime()));
            writeAttr(FIX_DEFECT_ATTR, d.fix_defect);
            if (d.fix_count != 1)
                writeAttr(FIX_COUNT_ATTR, Integer.toString(d.fix_count));
            if (d.fix_pending)
                writeAttr(FIX_PENDING_ATTR, "true");
            writeAttr(DESCRIPTION_ATTR, d.description);
            out.println("/>");
        }

        private void writeAttr(String name, String value) {
            if (value != null) {
                out.print(" ");
                out.print(name);
                out.print("=\"");
                out.print(XMLUtils.escapeAttribute(value));
                out.print("\"");
            }
        }

    }
}
