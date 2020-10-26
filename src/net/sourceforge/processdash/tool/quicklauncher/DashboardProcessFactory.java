// Copyright (C) 2006-2020 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.LightweightSet;

abstract class DashboardProcessFactory {

    protected List vmArgs;

    protected static final String WBS_EDITOR_MAIN_CLASS = "teamdash.wbs.WBSEditor";

    protected static final Resources resources = QuickLauncher.resources;

    public DashboardProcessFactory() {}

    public void addVmArg(String arg) {
        if (vmArgs == null)
            vmArgs = new LightweightSet();
        if (arg != null)
            for (String oneArg : arg.split("\n"))
                vmArgs.add(oneArg);
    }

    public void removeVmArg(String arg) {
        if (vmArgs != null && arg != null)
            for (String oneArg : arg.split("\n"))
                vmArgs.remove(oneArg);
    }

    public boolean hasVmArg(String arg) {
        return (vmArgs != null && arg != null && vmArgs.contains(arg));
    }

    public abstract Process launchDashboard(File pspdataDir, List extraVmArgs,
            List extraArgs) throws Exception;

    public abstract Process launchWBS(File wbsZipFile, List extraVmArgs,
            List extraArgs) throws Exception;

    public abstract Process launchJnlp(File jnlpFile, List extraVmArgs,
            List extraArgs) throws Exception;

}
