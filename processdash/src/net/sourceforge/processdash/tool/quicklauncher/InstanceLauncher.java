// Copyright (C) 2006 Tuma Solutions, LLC
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
import java.util.Collections;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;

abstract class InstanceLauncher implements Runnable {

    /** The name of a file that will ALWAYS exist in a data directory */
    protected static final String DATA_DIR_FILE_ITEM = "global.dat";

    static final Resources resources = QuickLauncher.resources;

    protected DashboardProcessFactory processFactory;

    protected int id;

    protected String display;

    protected Process process;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String d) {
        this.display = d;
    }

    public void setProcessFactory(DashboardProcessFactory processFactory) {
        this.processFactory = processFactory;
    }

    protected void launchApp(File pspdataDir) throws LaunchException {
        try {
            String notifyArg = "-D"
                    + ProcessDashboard.NOTIFY_ON_OPEN_ID_PROPERTY + "=" + id;
            String windowTitle = getDisplay();
            process = processFactory.launchDashboard(pspdataDir,
                    Collections.singletonList(notifyArg),
                    windowTitle == null ? null : Collections
                            .singletonList(windowTitle));
        } catch (Exception e) {
            String message = resources.format("Errors.Cant_Launch", e
                    .getLocalizedMessage());
            throw new LaunchException(message, e);
        }
    }

    protected void waitForCompletion() throws InterruptedException {
        process.waitFor();
    }

    protected boolean eq(Object a, Object b) {
        if (a == b)
            return true;
        return (a != null && a.equals(b));
    }
}
