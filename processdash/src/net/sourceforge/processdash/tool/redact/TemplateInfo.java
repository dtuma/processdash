// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;

public class TemplateInfo {

    public static final void addSafeNamesOfProcessPhases(HierarchyNodeMapper m) {
        if (TEMPLATES == null)
            // Future enhancement: if we have not been given a dashboard
            // context, look through the backup we are filtering for clues
            // about the phases in various metrics collection frameworks.
            return;

        for (Iterator i = TEMPLATES.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            PropertyKey key = (PropertyKey) e.getKey();
            Prop prop = (Prop) e.getValue();
            if (PROCESS_PHASE_STATUS.equals(prop.getStatus())) {
                String phaseName = Prop.unqualifiedName(key.name());
                m.addSafeName(phaseName);
            }
        }
    }

    public static void setDashboardContext(DashboardContext ctx) {
        if (ctx instanceof ProcessDashboard) {
            TEMPLATES = ((ProcessDashboard) ctx).getTemplateProperties();
        }
    }

    private static DashHierarchy TEMPLATES = null;

    private static final String PROCESS_PHASE_STATUS = "ME<>";

}
