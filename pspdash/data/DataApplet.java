// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash.data;


import pspdash.Settings;
import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;


public class DataApplet extends java.applet.Applet
    implements RepositoryClientListener
{

    volatile boolean isRunning = false;
    RepositoryClient data = null;
    String dataPrefix = null;
    String requiredTag = null;
    static int ieVersion = -1;
    static int nsVersion = -1;
    String errorMsg = null;
    String readOnlyColorString = "#aaaaaa";
    HTMLFieldManager mgr = null;
    AudioClip dataStoredSound = null;
    public static boolean debug = false;


    DataApplet() {}


    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }


    protected void debug(String msg) {
        if (debug)
            System.out.println("DataApplet: " + msg);
    }

    public void repositoryClientStopping() {
        debug("RepositoryClientListener stopping applet...");
        stop();
        debug("RepositoryClientListener starting applet...");
        start();
    }

    public void start() {
        debug("starting...");
        isRunning = true;

        requiredTag = getParameter("requiredTag");
        ieVersion = getIntParameter("ieVersion");
        nsVersion = getIntParameter("nsVersion");
        debug = getBoolParameter("debug", false);
        debug("url="+getDocumentBase()+", requiredTag="+requiredTag);

        try {
            data = new RepositoryClient(getDocumentBase(), requiredTag);
            dataPrefix = data.getDataPath();
            data.addRepositoryClientListener(this);

            String s;
            if ((s = Settings.getVal("browser.readonly.color")) != null)
                readOnlyColorString = s;
            debug = Settings.getBool("dataApplet.debug", false);
            if (!Settings.getBool("pauseButton.quiet", false))
                dataStoredSound = getAudioClip(getCodeBase(), "/dataStored.au");

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
                debug("data is not null; unregistering listener...");
                data.removeRepositoryClientListener(this);
                debug("quitting data...");
                data.quit();
                data = null;
            }
            if (mgr != null) {
                mgr.dispose(false);
                mgr = null;
            }
        } catch (Exception e) { printError(e); }

        dataPrefix = errorMsg = requiredTag = null;
        debug("stop complete.");
    }


    public void repositoryClientStoredData(String name) {
        if (dataStoredSound != null)
            dataStoredSound.play();
    }


    public String readOnlyColor() { return readOnlyColorString; }

    public int getIntParameter(String name) {
        try {
            return Integer.parseInt(getParameter(name));
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean getBoolParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) return defaultValue;
        switch (value.charAt(0)) {
        case 't': case 'T': case 'y': case 'Y': return true;
        case 'f': case 'F': case 'n': case 'N': return false;
        }
        return defaultValue;
    }

    public static int ieVersion() { return ieVersion; }

    public static int nsVersion() { return nsVersion; }

    public boolean unlocked() { return getBoolParameter("unlock", false); }

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
