// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.defects;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;

public class RepairDefectCounts {

    /** A boolean setting indicating whether the dataset repair has been run */
    public static final String RAN_XML_REPAIR_SETTING = "internal.ranXmlDefectCountCleanup";

    /**
     * Version 1.12 introduced a bug that can cause defect counts to be
     * incorrect. If we detect that the user has potentially been affected by
     * that bug, perform a repair.
     */
    public static void maybeRun(DashboardContext ctx, String dataDirectory) {
        if (Settings.getBool(DefectLog.USE_XML_SETTING, false) == false)
            return;
        if (Settings.getBool(RAN_XML_REPAIR_SETTING, false) == true)
            return;

        run(ctx, dataDirectory);
        InternalSettings.set(RAN_XML_REPAIR_SETTING, "true");
    }

    public static void run(DashboardContext ctx, String dataDirectory) {
        run(ctx, PropertyKey.ROOT, dataDirectory);
    }

    private static void run(DashboardContext ctx, PropertyKey pKey,
            String dataDirectory) {
        Prop prop = ctx.getHierarchy().pget(pKey);
        String path = pKey.path();
        String defLogName = prop.getDefectLog();

        // If this node has a defect log,
        if (defLogName != null && defLogName.length() != 0) {
            DefectLog dl = new DefectLog(dataDirectory + defLogName, path,
                ctx.getData());
            // read all the defects in that log, and
            Defect[] defects = dl.readDefects();
            // recalculate the associated data.
            dl.recalculateData(defects, ctx);
        }

        // recursively analyze all the children of this node.
        for (int i = 0; i < prop.getNumChildren(); i++)
            run(ctx, prop.getChild(i), dataDirectory);
    }

}
