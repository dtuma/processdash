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


import java.util.Vector;
import java.util.Hashtable;
import com.sun.java.browser.dom.*;
import org.w3c.dom.html.*;


class DOMFieldManager implements HTMLFieldManager {

    DOMService service = null;
    DOMDelayedRedrawer redrawer = null;
    Vector inputListeners = null;
    Hashtable inputListenersH = null;
    Repository data = null;
    String dataPath = null;
    boolean isRunning, unlocked;
    DataApplet a = null;



    DOMFieldManager(DataApplet a) throws Exception {
        this.a = a;
        isRunning = true;
        inputListeners = new Vector();
        inputListenersH = new Hashtable();
        unlocked = a.unlocked();
        try {
            service = DOMService.getService(a);
            redrawer = new DOMDelayedRedrawer(service);
        } catch (Exception e) {
            service = null;
            printError(e);
        }
    }

    /*
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
    */


    public void initialize(Repository data, String dataPath) {
        debug("initializing...");
        if (!isRunning) return; // abort if we have been terminated.

        this.data = data;
        this.dataPath = dataPath;

        try {
            service.invokeAndWait(new DOMAction() {
                public Object run(DOMAccessor accessor) {
                    doInitialize(accessor);
                    return null;
                } } );
        } catch (DOMAccessException dae) {
            System.err.println("Caught " + dae);
            dae.printStackTrace();
        }

        debug("initialization complete.");
    }

    private void doInitialize(DOMAccessor accessor) {

        try {
            HTMLDocument doc = (HTMLDocument) accessor.getDocument(a);
            HTMLCollection formList = doc.getForms();
            int elemNum = 0;

            if (formList != null) {
                int numForms = formList.getLength();
                for (int formIdx = 0;   formIdx < numForms; formIdx++) {
                    HTMLFormElement form = (HTMLFormElement) formList.item(formIdx);
                    HTMLCollection elementList = form.getElements();
                    int numElements = elementList.getLength();
                    for (int elementIdx = 0;  elementIdx < numElements;  elementIdx++) {
                        if (!isRunning) return; // abort if we have been terminated
                        reinititializeFormElement
                            ((HTMLElement) elementList.item(elementIdx),
                             formIdx, elementIdx, elemNum++);
                    }
                }
            }
        } catch (Exception e) { printError(e); }
    }



    public void dispose(boolean repositoryExists) {
        isRunning = false;
        if (!repositoryExists) data = null;

        DOMDelayedRedrawer r = redrawer;
        redrawer = null;
        if (r != null) r.quit();

        try {
            // debug("erasing listeners...");
            for (int i = inputListeners.size();   i-- > 0; )
                destroyInputListener(i);

        } catch (Exception e) { printError(e); }
        inputListeners = null;
        inputListenersH = null;
        data = null;
        dataPath = null;
    }


    private void destroyInputListener(int pos) {
        DOMField f = null;
        try {
            f = (DOMField) inputListeners.elementAt(pos);
            inputListeners.setElementAt(null, pos);
        } catch (ArrayIndexOutOfBoundsException e) {}

        if (f != null)
            f.dispose(data != null);
    }


    public void reinititializeFormElement
        (HTMLElement element, int formIdx, int elemIdx, int pos) {
        destroyInputListener(pos);
        HTMLField f = null;
        HTMLInputElement input = null;

        try {
            if (element instanceof HTMLInputElement) {
                input = (HTMLInputElement) element;
                String inputType = input.getType();
                // debug("Initializing a "+elementType+" element named ");
                if ("text".equalsIgnoreCase(inputType) ||
                    "hidden".equalsIgnoreCase(inputType))
                {
                    if (!"requiredTag".equalsIgnoreCase(input.getName()))
                        f = new DOMTextField
                            (service, redrawer, element, data, dataPath);
                }
                else if ("checkbox".equalsIgnoreCase(inputType))
                    f = new DOMCheckboxField
                        (service, redrawer, element, data, dataPath);

            } else if (element instanceof HTMLSelectElement)
                f = new DOMSelectField
                    (service, redrawer, element, data, dataPath);

            else if (element instanceof HTMLTextAreaElement)
                f = new DOMTextAreaField
                    (service, redrawer, element, data, dataPath);

            // etc.

            if (f != null) {
                while (inputListeners.size() < pos+1)
                    inputListeners.addElement(null);
                inputListeners.setElementAt(f, pos);
                //element.setMember(INDEX_ATTR, new Integer(pos));
                if (unlocked) f.unlock();
            }
            String key = formIdx + "," + elemIdx;
            inputListenersH.put(key, new Integer(pos));
        } catch (Exception e) {
            System.out.println("Caught " + e);
            e.printStackTrace();
        }
    }
    private static final String INDEX_ATTR = "dashIndex";


    public void notifyListener(String key) {
        // debug("notifyListener called by " + (String)element.getMember("name"));
        DOMField f = null;
        //debug("notifyListener("+key+")");
        Object pos = inputListenersH.get(key);
        //debug("notifyListener: pos ="+pos);
        if (pos instanceof Integer)
            f = (DOMField) inputListeners.elementAt(((Integer) pos).intValue());
        //debug("notifyListener: f ="+f);

        if (f != null) f.userEvent();
    }

    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }
    private void debug(String s) {
        System.out.println("DOMFieldManager."+s);
    }
}

/*

    DOMService service = null;
    try {
        service = DOMService. getService(MyApplet);
        String title = (String) service.invokeAndWait( new DOMAction() {
            public Object run( DOMAccessor accessor) {
                 HTMLDocument doc = (HTMLDocument) accessor.getDocument();
                 return doc.getTitle(); } });
    } catch (DOMUnsupportedException e1) {
    } catch (DOMAccessException e2) { }

 */
