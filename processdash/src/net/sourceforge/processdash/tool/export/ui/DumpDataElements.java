// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.ui;


import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.net.http.TinyCGIHighVolume;
import net.sourceforge.processdash.tool.export.impl.DataExporter;
import net.sourceforge.processdash.tool.export.impl.DataExporterXMLv1;
import net.sourceforge.processdash.tool.export.impl.DefaultDataExportFilter;
import net.sourceforge.processdash.tool.export.impl.ExportedDataValueIterator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class DumpDataElements extends TinyCGIBase implements TinyCGIHighVolume {

    protected void writeHeader() {
        out.print("Content-type: text/plain\r\n\r\n");
        out.flush();
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        Vector filter = null;
        String prefix = getPrefix();
        if (prefix != null && prefix.length() > 1) {
            filter = new Vector();
            filter.add(prefix);
        }
        String format = getParameter("format");
        if ("xml".equalsIgnoreCase(format))
            dumpXml(filter);
        else
            dumpText(filter);
    }

    private void dumpXml(Vector filter) throws IOException {
        Iterator iter = new ExportedDataValueIterator(getDataRepository(), filter);

        DefaultDataExportFilter ddef = new DefaultDataExportFilter(iter);
        if (parameters.containsKey("showToDate"))
            ddef.setSkipToDateData(false);
        if (parameters.containsKey("showZero"))
            ddef.setSkipZero(false);
        if (parameters.containsKey("showInfNaN"))
            ddef.setSkipInfNaN(false);
        ddef.init();

        DataExporter exp = new DataExporterXMLv1();
        exp.export(outStream, ddef);
    }
    private boolean getBool(String name, boolean defaultVal) {
        String strVal = getParameter(name);
        if (strVal == null || strVal.length() == 0)
            return defaultVal;
        else
            return "true".equalsIgnoreCase(strVal);
    }

    private void dumpText(Vector filter) {
        getDataRepository().dumpRepository(out, filter, true);
    }

}
