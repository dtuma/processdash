// Copyright (C) 2011 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.setup;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import net.sourceforge.processdash.ui.web.TinyCGIBase;


public class RestoreIndivData extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        parseFormData();

        File pdashFile = new File(getParameter("filename"));
        RestoreIndivDataWorker worker = new RestoreIndivDataWorker(
                getDashboardContext(), getPrefix(), pdashFile);
        worker.run();

        out.write("<html><body>Restored data at " + new Date()
                + "</body></html>");
    }

}
