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


import java.util.Enumeration;
import java.util.Hashtable;
import netscape.javascript.JSObject;


class NSFieldManager implements HTMLFieldManager {

    JSObject window = null;
    Hashtable inputListeners = null;
    Repository data = null;
    String dataPath = null;
    boolean isRunning, unlocked;



    NSFieldManager(DataApplet a) throws Exception {
        isRunning = true;
        inputListeners = new Hashtable();
        unlocked = a.unlocked();

        // First order of business: get the current browser window object.
        // Sometimes this will fail if the browser is slow in coming up,
        // so we will try repeatedly until we succeed.

        window = null;
        for (int i = 50;   isRunning && (i != 0);   i--) try {
            window = JSObject.getWindow(a);
            break;
        } catch (Exception e) {
            try {                     // Pause before retrying...
                Thread.currentThread().sleep(100);
            } catch (InterruptedException ie) {}
        }

        if (window == null)
            throw new Exception("Javascript not available in this window.");
    }



    public void initialize(Repository data, String dataPath) {
        debug("initializing...");
        if (!isRunning) return; // abort if we have been terminated.

        this.data = data;
        this.dataPath = dataPath;

        JSObject document = (JSObject) window.getMember("document");
        JSObject formList = (JSObject) document.getMember("forms");

        if (formList != null) {
            int numForms = ((Double)formList.getMember("length")).intValue();
            for (int formIdx = 0;   formIdx < numForms; formIdx++) {
                JSObject form = (JSObject) formList.getSlot(formIdx);
                JSObject elementList = (JSObject) form.getMember("elements");
                int numElements = ((Double)elementList.getMember("length")).intValue();
                for (int elementIdx = 0;  elementIdx < numElements;  elementIdx++) {
                    if (!isRunning) return; // abort if we have been terminated
                    reinititializeFormElement
                        ((JSObject)elementList.getSlot(elementIdx));
                }
            }
        }

        debug("initialization complete.");
    }


    public void dispose(boolean repositoryExists) {
        isRunning = false;
        if (!repositoryExists) data = null;

        try {
            // debug("erasing listeners...");
            Enumeration listeners = inputListeners.keys();
            while (listeners.hasMoreElements())
                destroyInputListener((JSObject)listeners.nextElement());
        } catch (Exception e) { printError(e); }
        window = null;
        inputListeners = null;
        data = null;
        dataPath = null;
    }



    private void destroyInputListener(JSObject element) {
        NSField f = (NSField)inputListeners.remove(element);
        if (f != null)
            f.dispose(data != null);
    }


    public void reinititializeFormElement(JSObject element) {
        destroyInputListener(element);
        HTMLField f = null;

        try {
            String elementType = (String)element.getMember("type");
            // debug("Initializing a "+elementType+" element named ");
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
                inputListeners.put(element, f);
                if (unlocked) f.unlock();
            }
        } catch (Exception e) {}
    }


    public void notifyListener(Object element) {
        // debug("notifyListener called by " + (String)element.getMember("name"));
        NSField f = (NSField)inputListeners.get(element);
        if (f != null)
            f.userEvent();
    }

    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }
    private void debug(String s) {
        // System.out.println("NSFieldManager."+s);
    }
}
