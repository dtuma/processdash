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


import com.sun.java.browser.dom.*;
import java.util.Vector;


class DOMDelayedRedrawer extends Thread implements DOMAction {

    DOMService service;
    Vector itemsToNotify;
    volatile boolean isRunning = true;

    public DOMDelayedRedrawer(DOMService service) {
        this.service = service;
        this.itemsToNotify = new Vector();
        setDaemon(true);
        start();
    }



    public void run() {
        while (isRunning) {
            try {
                synchronized (this) {
                    // wait indefinitely until someone notifies us
                    if (itemsToNotify.size() == 0) wait();
                }
                // then wait a fraction of a second more.
                sleep(50);
            } catch (InterruptedException ie) {}

            if (isRunning && itemsToNotify.size() > 0)
                service.invokeLater(this);
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

    public synchronized void quit() {
        isRunning = false;
        notify();
    }


    /** trigger redraw() on all the registered fields. */
    public Object run(DOMAccessor accessor) {
        DOMField f = null;
        int count = 0;
        while (itemsToNotify.size() > 0) {
            f = (DOMField) itemsToNotify.elementAt(0);
            itemsToNotify.removeElementAt(0);
            try {
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
