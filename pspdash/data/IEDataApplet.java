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
import java.util.Vector;
import com.ms.osp.*;

public class IEDataApplet extends DataApplet {


    private DataSourceListener myDSL = null;
    boolean browserGotManager = false;


    IEDataApplet() {}


    public void start() {

        /*
         * The first order of business is to create our IEFieldManager. That way,
         * if IE asks us for it while we are doing other things, it will exist.
         */
        mgr = new IEFieldManager(this);
        browserGotManager = false;

        super.start();              // initiate top-level DataApplet start().

        if (!browserGotManager)
            notifyListeners();
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
}
