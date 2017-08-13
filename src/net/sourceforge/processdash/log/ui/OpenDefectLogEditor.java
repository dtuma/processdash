// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.awt.Window;
import java.io.IOException;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ui.WindowTracker;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class OpenDefectLogEditor extends TinyCGIBase {

    @Override
    protected void writeHeader() {}

    @Override
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        Window w = DashController.showDefectLogEditor(getPrefix());

        out.print("Expires: 0\r\n");
        if (w != null && isJsonRequest()) {
            out.print("Content-type: application/json\r\n\r\n");
            out.print(WindowTracker.getWindowOpenedJson(w));
        } else {
            super.writeHeader();
            DashController.printNullDocument(out);
        }
    }

}
