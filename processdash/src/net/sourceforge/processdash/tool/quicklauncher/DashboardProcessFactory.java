// Copyright (C) 2006-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.LightweightSet;

abstract class DashboardProcessFactory {

    protected List vmArgs;

    protected static final Resources resources = QuickLauncher.resources;

    public DashboardProcessFactory() {}

    public void addVmArg(String arg) {
        if (vmArgs == null)
            vmArgs = new LightweightSet();
        if (arg != null)
            vmArgs.add(arg);
    }

    public void removeVmArg(String arg) {
        if (vmArgs != null && arg != null)
            vmArgs.remove(arg);
    }

    public abstract Process launchDashboard(File pspdataDir, List extraVmArgs,
            List extraArgs) throws Exception;

}
