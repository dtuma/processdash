// Copyright (C) 2006-2019 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

abstract class DashboardInstance {

    public static final int NONE = -1;
    public static final int EXTRACTING = 0;
    public static final int LAUNCHING = 1;
    public static final int RUNNING = 2;

    private static final String[] STATUS_KEYS = { "Extracting", "Launching",
            "Running" };

    private static final String[] STATUS_NAMES = QuickLauncher.resources
            .getStrings("Status.", STATUS_KEYS);

    /** The name of a file that will ALWAYS exist in a data directory */
    protected static final String DATA_DIR_FILE_ITEM = "global.dat";

    /** The name of a file that will ALWAYS exist in a WBS directory */
    protected static final String WBS_DIR_FILE_ITEM = "wbs.xml";

    static final Resources resources = QuickLauncher.resources;


    protected int id;

    protected int status = NONE;

    private File launchTarget;

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

    public File getLaunchTarget() {
        return launchTarget;
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
            List vmArgs, File launchTarget) throws LaunchException {
        try {
            setStatus(LAUNCHING);

            List extraVmArgs = new ArrayList();
            if (vmArgs != null)
                extraVmArgs.addAll(vmArgs);
            String notifyArg = "-D"
                + ProcessDashboard.NOTIFY_ON_OPEN_ID_PROPERTY + "=" + id;
            extraVmArgs.add(notifyArg);

            String windowTitle = getDisplay();
            if (windowTitle != null) {
                String titleArg = "-D" + Settings.SYS_PROP_PREFIX
                        + ProcessDashboard.WINDOW_TITLE_SETTING + "="
                        + windowTitle;
                extraVmArgs.add(titleArg);
            }

            this.launchTarget = launchTarget;
            process = createProcess(processFactory, launchTarget, extraVmArgs,
                null);
        } catch (Exception e) {
            String message = resources.format("Errors.Cant_Launch", e
                    .getLocalizedMessage());
            throw new LaunchException(message, e);
        }
    }

    protected Process createProcess(DashboardProcessFactory processFactory,
            File launchTarget, List extraVmArgs, List extraArgs)
            throws Exception {
        return processFactory.launchDashboard(launchTarget, extraVmArgs,
            extraArgs);
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
