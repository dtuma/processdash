// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.io.IOException;
import java.util.Date;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class EnableNodeIDs extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        out.write("<html><head><title>Enable Hierarchy Node IDs</title></head>"
                + "<body><h1>Enable Node IDs</h1><p>");

        if (parameters.containsKey("clear")) {
            clearNodeIDs();
            out.write("Hierarchy node IDs were cleared at " + new Date()
                    + ". <a href='enableNodeIDs'>Click here</a> to reassign.");

        } else {
            boolean madeChange = DashController.assignHierarchyNodeIDs();
            if (madeChange) {
                out.write("Assigned hierarchy node IDs at " + new Date());
            } else {
                out.write("Node IDs were already in place. No changes needed.");
            }
        }

        out.write("</p></body></html>");
    }

    private void clearNodeIDs() {
        for (Prop p : getPSPProperties().values())
            p.setNodeID(null);
    }

}
