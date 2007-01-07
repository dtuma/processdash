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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
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

    protected void waitForCompletion() throws InterruptedException {
        doWaitFor(process);
        setStatus(NONE);
    }

    /**
     * Method to perform a "wait" for a process and return its exit value. This
     * is a workaround for <CODE>process.waitFor()</CODE> never returning.
     */
    protected static int doWaitFor(Process p) {

        int exitValue = -1; // returned to caller when p is finished

        try {

            InputStream in = p.getInputStream();
            InputStream err = p.getErrorStream();

            boolean finished = false; // Set to true when p is finished

            while (!finished) {
                try {

                    while (in.available() > 0)
                        in.read();

                    while (err.available() > 0)
                        err.read();

                    // Ask the process for its exitValue. If the process
                    // is not finished, an IllegalThreadStateException
                    // is thrown. If it is finished, we fall through and
                    // the variable finished is set to true.

                    exitValue = p.exitValue();
                    finished = true;

                } catch (IllegalThreadStateException e) {

                    // Process is not finished yet;
                    // Sleep a little to save on CPU cycles
                    Thread.sleep(500);
                }
            }


        } catch (Exception e) {
            // unexpected exception! print it out for debugging...
            System.err.println("doWaitFor(): unexpected exception - "
                    + e.getMessage());
        }

        // return completion status to caller
        return exitValue;
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
