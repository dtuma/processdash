// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.data.applet.ns;


import java.util.Vector;


import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.Repository;
import netscape.javascript.JSObject;


public class NSFieldManager implements HTMLFieldManager, DataListener {

    JSObject window = null;
    Vector inputListeners = null;
    Repository data = null;
    String dataPath = null;
    NSDelayedNotifier notifier = null;
    boolean isRunning, unlocked;
    private DataApplet applet = null;



    public NSFieldManager(DataApplet a) throws Exception {
        debug("constructor starting");
        isRunning = true;
        inputListeners = new Vector();
        unlocked = a.unlocked();
        this.applet = a;

        notifier = new NSDelayedNotifier();
        notifier.setDaemon(true);
        notifier.start();

        // First order of business: get the current browser window object.
        // Sometimes this will fail if the browser is slow in coming up,
        // so we will try repeatedly until we succeed.

        window = null;
        for (int i = 50;   isRunning && (i != 0);   i--) try {
            window = JSObject.getWindow(a);
            break;
        } catch (Exception e) {
            try {                     // Pause before retrying...
                Thread.sleep(100);
            } catch (InterruptedException ie) {}
        }

        if (window == null)
            throw new Exception("Javascript not available in this window.");

        debug("constructor finished");
    }



    public void initialize(Repository data, String dataPath) {
        debug("initializing...");
        if (!isRunning) return; // abort if we have been terminated.

        this.data = data;
        this.dataPath = dataPath;

        debug("get document...");
        JSObject document = (JSObject) window.getMember("document");
        debug("get forms...");
        JSObject formList = (JSObject) document.getMember("forms");

        // Build an internal list of all the elements on the form. (This
        // is necessary because otherwise Netscape 6 has a nasty habit of
        // reordering the list of elements in the form as we initialize
        // them.)
        Vector allElements = new Vector();
        if (formList != null) {
            debug("get forms.length...");
            int numForms = intValue(formList.getMember("length"));
            for (int formIdx = 0;   formIdx < numForms; formIdx++) {
                debug("get a form...");
                JSObject form = (JSObject) formList.getSlot(formIdx);
                debug("get elements...");
                JSObject elementList = (JSObject) form.getMember("elements");
                debug("get elements.length...");
                int numElements = intValue(elementList.getMember("length"));
                for (int elementIdx = 0;  elementIdx < numElements;  elementIdx++) {
                    if (!isRunning) return; // abort if we have been terminated
                    debug("add an element...");
                    allElements.addElement(elementList.getSlot(elementIdx));
                }
            }
        }

            // Discard any previous input listeners
            for (int i = inputListeners.size();   i-- > 0; )
                destroyInputListener(i);

        // Now walk through our list of elements and initialize them.
        for (int elemNum = 0;   elemNum < allElements.size();   elemNum++) {
            if (!isRunning) return; // abort if we have been terminated
            reinititializeFormElement((JSObject)allElements.elementAt(elemNum));
        }

        debug("initialization complete.");
    }


    public void dispose(boolean repositoryExists) {
        isRunning = false;
        if (!repositoryExists) data = null;

        try {
            debug("erasing listeners...");
            for (int i = inputListeners.size();   i-- > 0; )
                destroyInputListener(i);

        } catch (Exception e) { printError(e); }
        window = null;
        inputListeners = null;
        data = null;
        dataPath = null;
    }


    private void destroyInputListener(int pos) {
        NSField f = null;
        try {
            f = (NSField) inputListeners.elementAt(pos);
            inputListeners.setElementAt(null, pos);
        } catch (ArrayIndexOutOfBoundsException e) {}

        if (f != null)
            f.dispose(data != null);
    }


    public void reinititializeFormElement(JSObject element) {
        HTMLField f = null;

        try {
            int pos = -1;
            String id = (String) element.getMember(INDEX_ATTR);
            if (id == null || !id.startsWith(ELEM_ID_PREFIX)) return;
            pos = Integer.parseInt(id.substring(ELEM_ID_PREFIX.length()));

            String elementType = (String)element.getMember("type");
            String elementName = (String)element.getMember("name");
            debug("Initializing a "+elementType+" element named "+elementName);
            if (nameToAvoid(elementName)) return;

            if ("text".equalsIgnoreCase(elementType) ||
                "hidden".equalsIgnoreCase(elementType) ||
                "textarea".equalsIgnoreCase(elementType))
                f = new NSTextField(element, data, dataPath);

            else if ("checkbox".equalsIgnoreCase(elementType))
                f = new NSCheckboxField(element, data, dataPath);

            else if ("select-one".equalsIgnoreCase(elementType))
                f = new NSSelectField(element, data, dataPath);

            // etc.

            if (f != null) {
                debug("storing element");
                while (inputListeners.size() < pos+1)
                    inputListeners.addElement(null);
                inputListeners.setElementAt(f, pos);
                debug("customizing element");
                if (unlocked) f.unlock();
                f.maybeAddActiveListener(this);
            }
        } catch (Exception e) {
            printError(e);
        }
    }
    private static final String INDEX_ATTR = "id";
    private static final String ELEM_ID_PREFIX = "dashelem_";
    private boolean nameToAvoid(String name) {
        return (name == null || name.length() == 0 ||
                name.indexOf("NOT_DATA") != -1 ||
                "requiredTag".equalsIgnoreCase(name));
    }


    public void notifyListener(Object id, Object value) {
        debug("notifyListener called by an element with id "+id);
        NSField f = null;

        int idx = -1;
        try {
            Object pos = id;

            if (pos instanceof String && ((String) pos).startsWith(ELEM_ID_PREFIX))
                pos = ((String) pos).substring(ELEM_ID_PREFIX.length());

            idx = intValue(pos);
        } catch (Exception e) {
            printError(e);
        }

        if (idx >= 0 && idx < inputListeners.size())
            f = (NSField) inputListeners.elementAt(idx);

        debug("field="+f);
        notifier.addField(f);
    }

    public void dataValuesChanged(Vector v) { dataValueChanged(null); }
    public void dataValueChanged(DataEvent e) { applet.refreshPage(); }

    public static int intValue(Object o) {
        if (o == null) return -1;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {}
        return -1;
    }

    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }
    private void debug(String s) {
        if (DataApplet.debug)
            System.out.println("NSFieldManager: "+s);
    }

    public void registerElement(String elementID, Object elementName, Object elementType) {
    }



    public String getDataNotification() {
        return null;
    }
}
