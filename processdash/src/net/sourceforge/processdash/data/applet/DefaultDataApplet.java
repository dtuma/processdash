// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.data.applet;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLEncoder;


public class DefaultDataApplet extends DataApplet {


    public DefaultDataApplet() {}


    private Class[] CONSTR_PARAM = { DataApplet.class };

    public void start() {
        isRunning = true;

        try {
            boolean disableDOM = getBoolParameter("disableDOM", false);
            Class cls = getFieldManagerClass(disableDOM);
            Constructor cstr = cls.getConstructor(CONSTR_PARAM);
            mgr = (HTMLFieldManager) cstr.newInstance(new Object[] { this });
            super.start();

        } catch (Throwable e) {
            // creating or initializing the HTMLFieldManager in an unsupported
            // browser could cause various exceptions or errors to be thrown.
            System.out.println
                ("Your current browser configuration appears to be incapable\n"
                 + "of supporting the dashboard. The error encountered was:");
            System.out.println(e);
            e.printStackTrace();

            redirectToProblemURL(e);
        }
    }

    private Class getFieldManagerClass(boolean disableDOM) throws Exception {
        String packageName = DefaultDataApplet.class.getPackage().getName();
        if (!disableDOM) {
            String javaVer = System.getProperty("java.version");
            if (javaVer.compareTo("1.4.2") >= 0)
                try {
                    return Class.forName(packageName + ".dom.DOMFieldManager");
                } catch (Throwable e) {}
        }

        return Class.forName(packageName + ".ns.NSFieldManager");
    }

    public void notifyListener(Object id) {
        debug("NSDataApplet.notifyListener("+id+")");
        if (isRunning && mgr != null)
            mgr.notifyListener(id);
    }

    protected void debug(String s) {
        if (DataApplet.debug)
            System.out.println("NSDataApplet: "+s);
    }

    private static final String PROBLEM_URL =
        "/help/Topics/Troubleshooting/DataApplet/OtherBrowser.htm";


    private void redirectToProblemURL(Throwable t) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintWriter w = new PrintWriter(buf);
            t.printStackTrace(w);
            w.flush();
            String javaVersion = System.getProperty("java.vendor") +
                " JRE " + System.getProperty("java.version") +
                "; " + System.getProperty("os.name");
            // DO NOT fix the deprecated statement on the next line! This code
            // needs to run in Java 1.1 JVMs.
            String urlStr = PROBLEM_URL +
                "?JAVA_VERSION=" + URLEncoder.encode(javaVersion) +
                "&ERROR_MESSAGE=" + URLEncoder.encode(buf.toString());
            URL url = new URL(getDocumentBase(), urlStr);
            getAppletContext().showDocument(url, "_top");
        } catch (IOException ioe) {}
    }

}
