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


import java.util.Vector;
import com.sun.java.browser.dom.*;

import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.Repository;

import org.w3c.dom.html.*;



public class DOMFieldManager implements HTMLFieldManager, DataListener {

    DOMService service = null;
    Vector inputListeners = null;
    Repository data = null;
    String dataPath = null;
    DOMDelayedRedrawer redrawer = null;
    volatile boolean isRunning;
    boolean unlocked;
    DataApplet applet = null;

    public DOMFieldManager(DataApplet a) throws Exception {
        debug("constructor starting");
        isRunning = true;
        inputListeners = new Vector();
        unlocked = a.unlocked();
        this.applet = a;

        try {
            service = DOMService.getService(a);
            redrawer = new DOMDelayedRedrawer(service);
        } catch (Exception e) {
            service = null;
            printError(e);
        }

        debug("constructor finished");
    }



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
        } catch (Exception e) {
            printError(e);
        }

        debug("initialization complete.");
    }

    private void doInitialize(DOMAccessor accessor) {

        try {
            HTMLDocument doc = (HTMLDocument) accessor.getDocument(applet);
            HTMLCollection formList = doc.getForms();

            // Build an internal list of all the elements on the form. (This
            // is necessary because otherwise Netscape 6 has a nasty habit of
            // reordering the list of elements in the form as we initialize
            // them.)
            Vector allElements = new Vector();
            if (formList != null) {
                int numForms = formList.getLength();
                for (int formIdx = 0;   formIdx < numForms; formIdx++) {
                    HTMLFormElement form = (HTMLFormElement) formList.item(formIdx);
                    HTMLCollection elementList = form.getElements();
                    int numElements = elementList.getLength();
                    for (int elementIdx = 0;  elementIdx < numElements;  elementIdx++) {
                        if (!isRunning) return; // abort if we have been terminated
                        allElements.addElement(elementList.item(elementIdx));
                    }
                }
            }

            // Discard any previous input listeners
            for (int i = inputListeners.size();   i-- > 0; )
                destroyInputListener(i);

            // Now walk through our list of elements and initialize them.
            for (int elemNum = 0;   elemNum < allElements.size();   elemNum++) {
                if (!isRunning) return; // abort if we have been terminated
                reinititializeFormElement((HTMLElement)allElements.elementAt(elemNum));
            }

        } catch (Exception e) { printError(e); }

        if (isRunning) redrawer.start();
    }



    public void dispose(boolean repositoryExists) {
        isRunning = false;
        if (!repositoryExists) data = null;

        DOMDelayedRedrawer r = redrawer;
        redrawer = null;
        if (r != null) r.quit();

        try {
            debug("erasing listeners...");
            for (int i = inputListeners.size();   i-- > 0; )
                destroyInputListener(i);

        } catch (Exception e) { printError(e); }
        service = null;
        inputListeners = null;
        data = null;
        dataPath = null;
    }


    private void destroyInputListener(int pos) {
        DOMField f = null;
        try {
            f = (DOMField) inputListeners.elementAt(pos);
            inputListeners.setElementAt(null, pos);
        } catch (ArrayIndexOutOfBoundsException e) {}

        if (f != null) {
            debug("disposing field #"+pos+": "+f);
            f.dispose(data != null);
        }
    }


    public void reinititializeFormElement(HTMLElement element) {
        HTMLField f = null;
        HTMLInputElement input = null;

        try {
            int pos = -1;
            String id = element.getId();
            if (id == null || !id.startsWith(ELEM_ID_PREFIX)) return;
            pos = Integer.parseInt(id.substring(ELEM_ID_PREFIX.length()));

            if (element instanceof HTMLInputElement) {
                input = (HTMLInputElement) element;
                String inputType = input.getType();
                if (nameToAvoid(input.getName())) return;

                if ("text".equalsIgnoreCase(inputType) ||
                    "hidden".equalsIgnoreCase(inputType))
                    f = new DOMTextField
                        (service, redrawer, element, data, dataPath);

                else if ("checkbox".equalsIgnoreCase(inputType))
                    f = new DOMCheckboxField
                        (service, redrawer, element, data, dataPath);

            } else if (element instanceof HTMLSelectElement) {
                if (nameToAvoid(((HTMLSelectElement) element).getName())) return;
                f = new DOMSelectField
                    (service, redrawer, element, data, dataPath);

            } else if (element instanceof HTMLTextAreaElement) {
                if (nameToAvoid(((HTMLTextAreaElement) element).getName())) return;
                f = new DOMTextAreaField
                    (service, redrawer, element, data, dataPath);
            }

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
    private static final String ELEM_ID_PREFIX = "dashelem_";
    private boolean nameToAvoid(String name) {
        return (name == null || name.length() == 0 ||
                name.indexOf("NOT_DATA") != -1 ||
                "requiredTag".equalsIgnoreCase(name));
    }


    public void notifyListener(Object id, Object value) {
        debug("notifyListener called by id="+id);
        DOMField f = null;

        int idx = -1;
        try {
            Object pos = id;

            if (pos instanceof String && ((String) pos).startsWith(ELEM_ID_PREFIX))
                pos = ((String) pos).substring(ELEM_ID_PREFIX.length());

            debug("\tpos="+pos);
            idx = intValue(pos);
        } catch (Exception e) {
            printError(e);
        }

        if (idx >= 0 && idx < inputListeners.size())
            f = (DOMField) inputListeners.elementAt(idx);

        debug("\tfield="+f);
        if (f != null) f.userEvent();
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
            System.out.println("DOMFieldManager: "+s);
    }


    public void registerElement(String elementID, Object elementName, Object elementType) {
    }



    public String getDataNotification() {
        return null;
    }
}
