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

package net.sourceforge.processdash.data.applet.js;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.sourceforge.processdash.data.repository.DataRepository;


public class FormDataSession implements FormDataListener {

    private JSFieldManager mgr;
    private String sessionID;
    private int currentCoupon;
    private LinkedList formDataEvents;
    private long lastAccessTime;

    public FormDataSession(DataRepository data, String prefix, boolean unlock) {
        mgr = new JSFieldManager(unlock);
        mgr.addFormDataListener(this);
        mgr.initialize(data, prefix);
        formDataEvents = new LinkedList();
        sessionID = getNextSessionID(this);
        touch();
    }

    public void registerField(String id, String name, String type) {
        System.out.println("Session.registerField("+id+","+name+","+type+")");
        mgr.registerElement(id, name, type);
    }

    public String getSessionID() {
        return sessionID;
    }

    public void notifyListener(String id, String value) {
        mgr.notifyListener(id, value);
    }

    public synchronized void paintData(String id, String value, boolean readOnly) {
        System.out.println("paintData("+id+","+value+","+readOnly+")");
        Iterator i = formDataEvents.iterator();
        while (i.hasNext()) {
            FormDataEvent e = (FormDataEvent) i.next();
            if (e.id.equals(id)) i.remove();
        }
        formDataEvents.add
            (new FormDataEvent(++currentCoupon, id, value, readOnly));
        notify();
    }

    private void touch() {
        lastAccessTime = System.currentTimeMillis();
    }

    private static final int EVENT_DELAY = 1000;

    public FormDataEvent getNextEvent(int lastCoupon, boolean delay) {
        // Iterate through all past events and return the first one with
        // a coupon number higher than the given number.
        Iterator i = formDataEvents.iterator();
        FormDataEvent e = null;
        while (i.hasNext()) {
            e = (FormDataEvent) i.next();
            if (e != null && e.getCoupon() > lastCoupon) return e;
        }
        // if no event was found because the queue is empty, return null.
        if (e == null) return null;

        // if no event was found, and the most recent event in the queue
        // is fairly old, return null.
        if (!delay && System.currentTimeMillis() - e.timestamp > EVENT_DELAY)
            return null;

        // otherwise, wait a small amount of time to see if another event
        // arrives.
        try {
            synchronized (this) {
                wait(EVENT_DELAY);
            }
        } catch (InterruptedException ie) {}
        FormDataEvent le = (FormDataEvent) formDataEvents.getLast();
        if (le != e)
            // if a new event has arrived, find and return it.
            return getNextEvent(lastCoupon, delay);
        else
            // if no new event has arrived, return null.
            return null;
    }

    // initialize SESSION_ID to a quasi-random number.  This will make it
    // more unlikely that two sessions will share the same value if the
    // dashboard is stopped and restarted, and will help prevent browsers
    // from incorrectly caching form data pages.
    private static int SESSION_ID =
        (int) ((System.currentTimeMillis() >> 10) & 0x3F) << 6;

    private static Map SESSION_CACHE = new HashMap();
    private synchronized static String getNextSessionID(FormDataSession session) {
        String nextID = Integer.toString(++SESSION_ID);
        SESSION_CACHE.put(nextID, session);
        return nextID;
    }

    public static FormDataSession getSession(String sessionID) {
        // TODO - add logic that discards old/unused sessions
        FormDataSession result =
            (FormDataSession) SESSION_CACHE.get(sessionID);
        if (result != null)
            result.touch();
        return result;
    }
}
