// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;

import java.io.IOException;

public class hist extends TinyCGIBase {

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
        out.print(TinyWebServer.urlEncodePath(subsetPrefix));
        out.print(DEST_URL);
        out.print("\r\n\r\n");
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException { }

}
