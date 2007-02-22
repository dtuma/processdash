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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

abstract class DashboardInstance {

    public static final int NONE = -1;
    public static final int LAUNCHING = 0;
    public static final int RUNNING = 1;

    private static final String[] STATUS_KEYS = { "Launching", "Running" };

    private static final String[] STATUS_NAMES = QuickLauncher.resources
            .getStrings("Status.", STATUS_KEYS);

    /** The name of a file that will ALWAYS exist in a data directory */
    protected static final String DATA_DIR_FILE_ITEM = "global.dat";

    static final Resources resources = QuickLauncher.resources;


    protected int id;

    protected int status = NONE;

    protected int port = -1;

    protected String display;

    protected Process process;

    protected InstanceList observer;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        fireInstanceChanged();
    }

    public String getStatusDisplay() {
        if (status == NONE)
            return "";
        else
            return STATUS_NAMES[status];
    }

    public int getPort() {
        return port;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String d) {
        this.display = d;
        fireInstanceChanged();
    }

    protected void fireInstanceChanged() {
        if (observer != null)
            observer.instanceChanged(this);
    }

    public void setObserver(InstanceList o) {
        this.observer = o;
    }

    public abstract void launch(DashboardProcessFactory processFactory);

    protected void launchApp(DashboardProcessFactory processFactory,
            List vmArgs, File pspdataDir) throws LaunchException {
        try {
            setStatus(LAUNCHING);

            List extraVmArgs = new ArrayList();
            if (vmArgs != null)
                extraVmArgs.addAll(vmArgs);
            String notifyArg = "-D"
                + ProcessDashboard.NOTIFY_ON_OPEN_ID_PROPERTY + "=" + id;
            extraVmArgs.add(notifyArg);

            List programArgs = null;
            String windowTitle = getDisplay();
            if (windowTitle != null)
                programArgs = Collections.singletonList(windowTitle);

            process = processFactory.launchDashboard(pspdataDir, extraVmArgs,
                    programArgs);
        } catch (Exception e) {
            String message = resources.format("Errors.Cant_Launch", e
                    .getLocalizedMessage());
            throw new LaunchException(message, e);
        }
    }

    protected void waitForCompletion() {
        if (process != null) {
            RuntimeUtils.doWaitFor(process);
            setStatus(NONE);
        }
    }

    protected boolean eq(Object a, Object b) {
        if (a == b)
            return true;
        return (a != null && a.equals(b));
    }

    public void handleNotification(Element notification) {
        setStatus(RUNNING);
        this.port = XMLUtils.getXMLInt(notification, "httpPort");
    }



}
