// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.rest.to.RestTask;

public class RestDefectLogService {

    private static RestDefectLogService svc;

    public static RestDefectLogService get() {
        if (svc == null)
            svc = new RestDefectLogService();
        return svc;
    }


    private DashboardContext ctx;

    private DashHierarchy hier;

    private RestDefectLogService() {
        ctx = RestDashContext.get();
        hier = ctx.getHierarchy();
    }

    public boolean defectsAllowed(RestTask task) {
        PropertyKey key = hier.findKeyByNodeID(task.getId());
        Object log = hier.defectLog(key, "ignored/");
        return log != null;
    }

}
