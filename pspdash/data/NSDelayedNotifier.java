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


import java.util.Vector;


class NSDelayedNotifier extends Thread {

    Vector itemsToNotify = new Vector();

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    // wait indefinitely until someone notifies us
                    if (itemsToNotify.size() == 0) wait();
                }
                // then wait a tenth of a second more.
                sleep(100);
            } catch (InterruptedException ie) {}

            // finally, trigger userEvent() on all the registered fields.
            while (itemsToNotify.size() > 0) {
                NSField f = (NSField) itemsToNotify.elementAt(0);
                itemsToNotify.removeElementAt(0);
                f.userEvent();
            }
        }
    }
    public synchronized void addField(NSField f) {
        if (f != null) {
            itemsToNotify.addElement(f);
            notify();
        }
    }
}
