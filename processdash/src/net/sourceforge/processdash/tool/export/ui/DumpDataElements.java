// Copyright (C) 2003-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.ui;


import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.repository.DataRepository;
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
            dumpText(filter, "calc".equalsIgnoreCase(format));
        Runtime.getRuntime().gc();
    }

    private void dumpXml(Vector filter) throws IOException {
        Iterator iter = new ExportedDataValueIterator(getDataRepository(),
                getPSPProperties(), filter, null, null);

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

    private void dumpText(Vector filter, boolean calcStyle) {
        int style = (calcStyle
                ? DataRepository.DUMP_STYLE_CALC
                : DataRepository.DUMP_STYLE_DATA);
        getDataRepository().dumpRepository(out, filter, style);
    }

}
