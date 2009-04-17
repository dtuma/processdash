// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process;

import java.util.List;
import java.util.Vector;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.PropertyKey;

/**
 * This class locates and lists all of the process scripts and tools that are
 * associated with a particular task in the dashboard hierarchy.
 * 
 * @author Tuma
 */
public class ScriptEnumerator {

    public static List<ScriptID> getScripts(DashboardContext ctx, String path) {
        PropertyKey key = ctx.getHierarchy().findClosestKey(path);
        return getScripts(ctx, key);
    }

    public static List<ScriptID> getScripts(DashboardContext ctx,
            PropertyKey path) {
        Vector<ScriptID> result = ctx.getHierarchy().getScriptIDs(path);

        return result;
    }
}
