// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;


import pspdash.Settings;
import java.applet.Applet;
import java.net.URL;


public class DataApplet extends java.applet.Applet {

    volatile boolean isRunning = false;
    RepositoryClient data = null;
    String dataPrefix = null;
    String requiredTag = null;
    String errorMsg = null;
    String readOnlyColorString = "#aaaaaa";
    HTMLFieldManager mgr = null;
    RepositoryWatcher watcher = null;


    DataApplet() {}


    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }


    protected void debug(String msg) {
        // System.out.println("DataApplet: " + msg);
    }


    private class RepositoryWatcher extends Thread {
        public boolean enabled = true;
        public RepositoryWatcher() {
            try {
                setName(getName() + "(RepositoryWatcher)");
            } catch (SecurityException e) {}
        }

        public void run() {
            // debug("RepositoryWatcher waiting on data...");
            try {
                synchronized (data) { data.wait(); }
            } catch (InterruptedException e) {};

            if (enabled) {
                // debug("RepositoryWatcher stopping applet...");
                DataApplet.this.stop();
                // debug("RepositoryWatcher starting applet...");
                DataApplet.this.start();
            }
            // debug("RepositoryWatcher done.");
        }
    }

    public void start() {
        debug("starting...");
        isRunning = true;

        requiredTag = getParameter("requiredTag");
        debug("url="+getDocumentBase()+", requiredTag="+requiredTag);

        try {
            data = new RepositoryClient(getDocumentBase(), requiredTag);
            dataPrefix = data.getDataPath();
            watcher = new RepositoryWatcher();
            watcher.start();

            String s;
            if ((s = Settings.getVal("browser.readonly.color")) != null)
                readOnlyColorString = s;

        } catch (RemoteException e) {
            debug("got remote exception.");
            data = null;
            errorMsg = "NO CONNECTION";
        } catch (ForbiddenException e) {
            debug("got forbidden exception.");
            data = null;
            errorMsg = "No such project OR project/process mismatch";
        } catch (Exception e) {
            printError(e);
        }

        if (!isRunning) return;

        if (mgr != null)
            mgr.initialize(data, (data == null ? errorMsg : dataPrefix));
    }



    public void stop() {
        isRunning = false;
        try {
            if (data != null) {
                // debug("data is not null...");
                if (watcher != null) {
                    // debug("disabling watcher...");
                    watcher.enabled = false;
                }
                // debug("quitting data...");
                data.quit();
                data = null;
                watcher = null;
            }
            if (mgr != null) {
                mgr.dispose(false);
                mgr = null;
            }
        } catch (Exception e) { printError(e); }

        dataPrefix = errorMsg = requiredTag = null;
        debug("stop complete.");
    }


    public String readOnlyColor() { return readOnlyColorString; }

    public boolean unlocked() { return ("true".equals(getParameter("unlock"))); }

    public void refreshPage() {
        try {
            String urlStr = getParameter("docURL");
            if (urlStr == null || urlStr.length() == 0)
                urlStr = getDocumentBase().toString();
            if (urlStr.indexOf('?') == -1)
                urlStr = urlStr + "?reload";
            else
                urlStr = urlStr + "&reload";

            URL url = new URL(urlStr);
            stop();
            getAppletContext().showDocument(url, "_self");
        } catch (Exception ioe) {
            printError(ioe);
        }
    }

}
