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


import java.util.Hashtable;
import java.util.Enumeration;
import netscape.security.PrivilegeManager;
import netscape.security.ForbiddenTargetException;


public class NSDataApplet extends DataApplet {


    public NSDataApplet() {}


    public void start() {
        isRunning = true;

        try {
            mgr = new NSFieldManager(this);

                                      // get permission for our applet to make
            try {			// a socket connection
                PrivilegeManager.enablePrivilege("UniversalConnect");
            } catch (ForbiddenTargetException e) {
                // don't automatically give up.  If the user is running over an
                // http connection instead of the local hard drive, everything
                // will still work.
            }

            super.start();

        } catch (Exception e) {	// creating the NSFieldManager could throw
            printError(e);		// an exception
        }
    }

    public void notifyListener(Object element) {
        if (mgr != null)
            ((NSFieldManager)mgr).notifyListener(element);
    }

    protected void debug(String s) { /*System.out.println("NSDataApplet: "+s);*/}
}
