// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004-2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.log.Defect;
import net.sourceforge.processdash.log.DefectAnalyzer;
import net.sourceforge.processdash.util.XMLUtils;


public class DefectExporter implements DefectXmlConstants {


    public static void dumpDefects(ProcessDashboard parent, Vector filter, PrintWriter out) {
        out.println(DEFECT_START_TOKEN);
        out.println("<defects>");

        for (Iterator i = filter.iterator(); i.hasNext();) {
            String path = (String) i.next();
            DefectAnalyzer.run(parent.getProperties(), path,
                               new DefectWriter(out, path));
        }

        out.println("</defects>");
    }

    private static class DefectWriter implements DefectAnalyzer.Task {


        private PrintWriter out;
        private String basePath;

        public DefectWriter(PrintWriter out, String basePath) {
            this.out = out;
            this.basePath = basePath + "/";
        }

        public void analyze(String path, Defect d) {
            out.print("<" + DEFECT_TAG);
            writeAttr(PATH_ATTR, path);
//            if (path.startsWith(basePath))
//                writeAttr("relPath", path.substring(basePath.length()));
            if (d.date != null)
                writeAttr(DATE_ATTR, XMLUtils.saveDate(d.date));
            writeAttr(NUM_ATTR, d.number);
            writeAttr(TYPE_ATTR, d.defect_type);
            writeAttr(INJECTED_ATTR, d.phase_injected);
            writeAttr(REMOVED_ATTR, d.phase_removed);
            writeAttr(FIX_TIME_ATTR, d.fix_time);
            writeAttr(FIX_DEFECT_ATTR, d.fix_defect);
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
