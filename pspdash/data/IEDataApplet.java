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
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.lang.reflect.Constructor;
import com.ms.osp.*;

public class IEDataApplet extends DataApplet {


    private DataSourceListener myDSL = null;
    boolean browserGotManager = false;


    IEDataApplet() {}


    public void start() {
        try {

            /*
             * The first order of business is to create our
             * IEFieldManager. That way, if IE asks us for it while we are
             * doing other things, it will exist.
             */
            mgr = new IEFieldManager(this);
            browserGotManager = false;

            super.start();              // initiate top-level DataApplet start().

            // if (!browserGotManager)
            notifyListeners();

        } catch (Throwable t) {
            System.out.println
                ("The Microsoft Java Virtual Machine in your installation of\n"+
                 "Internet Explorer appears to be incapable of supporting\n"+
                 "the dashboard. The dashboard will attempt to use the Sun\n"+
                 "Java Plug-in instead.");
            redirectAndForcePlugin();
        }
    }


    public boolean isEditable(String fieldName) {
        return (mgr == null || ((IEFieldManager) mgr).isEditable(fieldName));
    }


    public Object msDataSourceObject(String qualifier)
    {
        debug("msDataSourceObject returning "+mgr);
        browserGotManager = (mgr != null);
        return mgr;
    }


    public void addDataSourceListener(DataSourceListener listener)
         throws java.util.TooManyListenersException
    {
        debug("addDataSourceListener, param is " + listener);
        if (myDSL != null)
            com.ms.com.ComLib.release(myDSL);

        myDSL = new OLEDBDSLWrapper(listener);
        //myDSL = wrapListener(listener);
    }


    public void removeDataSourceListener(DataSourceListener listener)
    {
        debug("removeDataSourceListener");
        // BUGBUG: Shouldn't have to call release here. This is a
        //         bug in the VM implementation.

        com.ms.com.ComLib.release(myDSL);
        myDSL = null;
    }


    // This function should probably be private, but for
    // now leave public so script writer can call to test notifications

    public void notifyListeners()
    {
        debug("notifyListeners");
        if (myDSL != null)
            myDSL.dataMemberChanged("");

        debug("listeners notified.");
    }


    protected void debug(String msg) {
        // System.out.println("IEDataApplet: " + msg);
    }


    private DataSourceListener wrapListener(DataSourceListener listener) {
        try {
            Class wrapperClass = Class.forName("pspdash.data.OLEDBDSLWrapper");
            Constructor c = wrapperClass.getDeclaredConstructor(CONSTRUCTOR_ARGS);
            Object[] constructor_args = { listener };
            return (DataSourceListener) c.newInstance(constructor_args);
        } catch (Throwable e) {
            OLEDBAlertWindow.display();
            return null;
        }
    }
    private static final Class[] CONSTRUCTOR_ARGS = { DataSourceListener.class };

    private void redirectAndForcePlugin() {
          try {
              String urlStr = getParameter("docURL");
              if (urlStr == null || urlStr.length() == 0)
                  urlStr = getDocumentBase().toString();
              if (urlStr.indexOf('?') == -1)
                  urlStr = urlStr + "?ForceJavaPlugIn";
              else
                  urlStr = urlStr + "&ForceJavaPlugIn";

              URL url = new URL(urlStr);
              getAppletContext().showDocument(url, "_self");
          } catch (IOException ioe) {
              System.out.println(ioe);
          }
    }
}
