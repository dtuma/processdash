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

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.repository.DataImporter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class ImportNow extends TinyCGIBase {

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        // DataImporter.refreshPrefix() doesn't expect a hierarchy prefix as
        // input - instead, it wants an import prefix (e.g. "/Import_sf7a7s").
        // We don't have a consistent means of determining that prefix, so
        // we'll just refresh all imported data.
        DataImporter.refreshPrefix("/");
        DashController.printNullDocument(out);
    }

}
