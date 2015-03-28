// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;


public class SelectWBSNode extends SelectHierarchyNode {

    // start at the root of the current project.
    protected PropertyKey getStartingKey() {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        while (key != null) {
            String id = getID(hierarchy, key);
            if (id != null && id.endsWith("Root")) break;
            key = key.getParent();
        }
        return key;
    }

    protected void error() throws IOException {
        out.println("<HTML><BODY>");
        out.println("This script must be used from within a team project.");
        out.println("</BODY></HTML>");
    }

    // only display team roots and team nodes.
    protected boolean prune(DashHierarchy hierarchy, PropertyKey key) {
        String id = getID(hierarchy, key);
        if (id == null || id.length() == 0) return true;
        if (id.endsWith("EmptyNode")) return true;
        if (!id.endsWith("Root") && !id.endsWith("Node")) return true;
        return false;
    }

}
