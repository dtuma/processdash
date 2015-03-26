// Copyright (C) 2002-2003 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.data.applet.dom;


import com.sun.java.browser.dom.*;
import java.util.Vector;

import net.sourceforge.processdash.data.applet.*;


class DOMDelayedRedrawer extends Thread implements DOMAction {

    DOMService service;
    Vector itemsToNotify;
    volatile boolean isRunning = true;

    public DOMDelayedRedrawer(DOMService service) {
        this.service = service;
        this.itemsToNotify = new Vector();
        setDaemon(true);
    }



    public void run() {
        while (isRunning) {
            try {
                synchronized (this) {
                    // wait indefinitely until someone notifies us
                    if (itemsToNotify.size() == 0) wait();
                }
            } catch (InterruptedException ie) {}

            if (isRunning && itemsToNotify.size() > 0) try {
                // wait a fraction of a second more
                sleep(50);
                // then perform the queued redraw notifications
                service.invokeAndWait(this);
            } catch (Exception e) {}
        }
    }


    public void addField(DOMField f) {
        if (f != null) {
            synchronized(this) {
                itemsToNotify.addElement(f);
                notify();
            }
        }
    }

    public void quit() {
        isRunning = false;
        synchronized (this) {
            notify();
        }
        if (this.isAlive()) try {
            this.join();
        } catch (Exception e) {}
    }


    /** trigger redraw() on all the registered fields. */
    public Object run(DOMAccessor accessor) {
        DOMField f = null;
        int count = 0;
        while (isRunning && itemsToNotify.size() > 0) {
            f = (DOMField) itemsToNotify.elementAt(0);
            itemsToNotify.removeElementAt(0);
            try {
                if (isRunning)
                    f.redraw();
                count++;
            } catch (Throwable t) {
                System.err.println("Caught " + t);
                t.printStackTrace();
            }
        }
        debug("Redrew "+count+" DOMFields");
        return null;
    }


    protected void debug(String msg) {
        if (DataApplet.debug)
            System.out.println("DOMDelayedRedrawer: " + msg);
    }
}
