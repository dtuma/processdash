// Copyright (C) 2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui.web.psp;


import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class HistoricalDataReport extends TinyCGIBase {

    private static final String DATA_NAME = "PSP To Date Subset Prefix";
    private static final String DEFAULT_PREFIX = "/To Date/PSP/All";
    private static final String DEST_URL = "//reports/table.class?qf=hist.rpt";

    /** Write the CGI header. */
    protected void writeHeader() {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String dataName = DataRepository.createDataName(prefix, DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        String subsetPrefix = null;
        if (d != null) subsetPrefix = d.format();
        if (subsetPrefix == null || subsetPrefix.length() == 0)
            subsetPrefix = DEFAULT_PREFIX;

        out.print("Location: ");
        out.print(WebServer.urlEncodePath(subsetPrefix));
        out.print(DEST_URL);
        out.print("\r\n\r\n");
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException { }

}
