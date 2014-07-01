// Copyright (C) 2003-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.applet.js;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import net.sourceforge.processdash.Settings;
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
        JSRepository jsRepository = null;
        if (data != null)
            jsRepository = new JSRepository(data);
        mgr.initialize(jsRepository, prefix);
        formDataEvents = new LinkedList();
        sessionID = getNextSessionID(this);
        touch();
        log.log(Level.FINE, "Created FormDataSession, id={0}", sessionID);
    }

    public void dispose() {
        log.log(Level.FINE, "Disposing FormDataSession, id={0}", sessionID);
        if (mgr != null) {
            mgr.dispose(true);
            mgr = null;
        }
        synchronized (formDataEvents) {
            formDataEvents.clear();
        }
    }

    public void registerField(String id, String name, String type) {
        log.entering("FormDataSession", "registerField",
                     new Object[] { id, name, type });
        mgr.registerElement(id, name, type);
        touch();
    }

    public String getSessionID() {
        return sessionID;
    }

    public void notifyListener(String id, String value) {
        mgr.notifyListener(id, value);
    }

    public void paintData(String id, String value, boolean readOnly) {
        log.entering("FormDataSession", "paintData",
                     new Object[] { id, value, Boolean.valueOf(readOnly) });

        synchronized (formDataEvents) {
            Iterator i = formDataEvents.iterator();
            while (i.hasNext()) {
                FormDataEvent e = (FormDataEvent) i.next();
                if (e.id.equals(id)) i.remove();
            }
            formDataEvents.add
                (new FormDataEvent(++currentCoupon, id, value, readOnly));
            formDataEvents.notify();
        }
    }

    private void touch() {
        lastAccessTime = System.currentTimeMillis();
    }

    private static final int EVENT_DELAY = 1000;

    public enum EventWaitDelay { NONE, AUTO, SHORT, LONG };

    public FormDataEvent getNextEvent(int lastCoupon, EventWaitDelay delay) {
        // Iterate through all past events and return the first one with
        // a coupon number higher than the given number.
        FormDataEvent e = null;
        synchronized (formDataEvents) {
            Iterator i = formDataEvents.iterator();
            while (i.hasNext()) {
                e = (FormDataEvent) i.next();
                if (e != null && e.getCoupon() > lastCoupon) return e;
            }

            // No event was found.  If the caller does not want us to delay for
            // events, return null.
            if (delay == EventWaitDelay.NONE) return null;

            // No event was found, and the caller wants us to decide whether a
            // short wait is appropriate
            if (delay == EventWaitDelay.AUTO) {
                // if the queue is empty, or the most recent event in the queue
                // is fairly old, return null.
                if (e == null
                        || System.currentTimeMillis() - e.timestamp > EVENT_DELAY)
                    return null;
            }

            // otherwise, wait some amount of time to see if another event
            // arrives.
            try {
                int waitTime = delay == EventWaitDelay.LONG ? REFRESH_DELAY
                        : EVENT_DELAY;
                formDataEvents.wait(waitTime);
            } catch (InterruptedException ie) {}

            // if the queue was emptied while we were waiting, return null.
            if (formDataEvents.isEmpty())
                return null;

            FormDataEvent le = (FormDataEvent) formDataEvents.getLast();
            if (le != e)
                // if a new event has arrived, find and return it.
                return getNextEvent(lastCoupon, EventWaitDelay.NONE);
            else
                // if no new event has arrived, return null.
                return null;
        }
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
        session.touch();
        SESSION_CACHE.put(nextID, session);
        DISPOSAL_TIMER.restart();
        return nextID;
    }


    static int REFRESH_DELAY =
        1000 * Settings.getInt("browserData.refreshInterval", 15);

    private static int TIMEOUT_DURATION = 2 /*iterations*/ * 
        Math.max(REFRESH_DELAY, 15000) /*milliseconds*/;

    public synchronized static FormDataSession getSession(String sessionID) {
        FormDataSession result = null;
        Iterator i = SESSION_CACHE.entrySet().iterator();
        long obsoleteTime = System.currentTimeMillis() - TIMEOUT_DURATION;
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            FormDataSession session = (FormDataSession) entry.getValue();
            if (sessionID.equals(entry.getKey())) {
                session.touch();
                result = session;
            } else if (session.lastAccessTime < obsoleteTime) {
                session.dispose();
                i.remove();
            }
        }
        if (!SESSION_CACHE.isEmpty())
            DISPOSAL_TIMER.restart();
        return result;
    }

    private static final Timer DISPOSAL_TIMER = new Timer(
            TIMEOUT_DURATION + 1000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    log.finer("FormDataSession disposal timer running");
                    getSession("null");
                }
            });
    static {
        DISPOSAL_TIMER.setRepeats(false);
    }

    private static Logger log = Logger.getLogger(FormDataSession.class.getName());
}
