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
import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.Repository;

import org.w3c.dom.html.*;



abstract class DOMField extends HTMLField {

    DOMService service;
    DOMDelayedRedrawer redrawer;
    Repository data;
    HTMLElement element;

    public DOMField(DOMService service, DOMDelayedRedrawer redrawer,
                    HTMLElement element, String name,
                    Repository data, String dataPath) {
        this.service = service;
        this.redrawer = redrawer;
        this.element = element;

        if (data == null) {
            variantValue = dataPath;
            redraw();
            return;
        }

        // redraw(); // necessary?

        i = InterpreterFactory.create(data, name, dataPath);
        i.setConsumer(this);
    }


    /** update the Html element with variantValue.
     *  Warning - only call this method from the DOM access thread! */
    abstract public void paint();

    /** update variantValue from the HTML element.
     *  Warning - only call this method from the DOM access thread! */
    abstract public void parse();

    /** update the read only status of the HTML element.
     *  Warning - only call this method from the DOM access thread! */
    abstract public void setReadOnly(boolean readOnly);

    /** update variantValue from DataInterpreter i. */
    abstract public void fetch();


    /** Warning - only call this method from the DOM access thread! */
    public void redraw() {
        if (element != null) {
            paint();

            boolean readOnly = !isEditable();
            setReadOnly(readOnly);
            element.setClassName(readOnly ? "readOnlyElem" : "editableElem");
        }
    }


    public void repositoryChangedValue() {
        fetch();
        redrawer.addField(this);
    }


    /** Warning - only call this method from the DOM access thread! */
    public void unlock() {
        super.unlock();
        redraw();
    }


    private static final String PKG_PREFIX =
        "net.sourceforge.processdash.data.applet.dom.";
    protected void debug(String msg) {
        if (DataApplet.debug)
            // print this object's classname (minus package name) and the message
            System.out.println
                (getClass().getName().substring(PKG_PREFIX.length()) + ": " + msg);
    }


    private static String HTML_TRUE = "true";
    private static String HTML_FALSE = null;

    private static String[] HTML_TRUE_VALUES = {
        "true", "checked", "1.0" };
    private static String[] HTML_FALSE_VALUES = {
        null, "false", "unchecked", "", "0.0" };

    /** Warning - only call this method from the DOM access thread! */
    public void manuallySetReadOnly(boolean readOnly) {
        element.setAttribute("readOnly", (readOnly ? HTML_TRUE : HTML_FALSE));
        if (readOnly == isReadOnly()) return;

        // our changes didn't take. Try other values.
        String[] trialValues = (readOnly ? HTML_TRUE_VALUES : HTML_FALSE_VALUES);
        for (int i = 0;   i < trialValues.length;   i++) {
            element.setAttribute("readOnly", trialValues[i]);
            // if this trial value appeared to work, save it into the
            // appropriate static field
            if (readOnly == isReadOnly()) {
                debug("changing readOnly="+readOnly+" constant to "+trialValues[i]);
                if (readOnly)
                    HTML_TRUE = trialValues[i];
                else
                    HTML_FALSE = trialValues[i];
                return;
            }
        }
    }

    /* Warning - only call this method from the DOM access thread!
     *
     * subclasses should generally override this method.
     */
    protected boolean isReadOnly() {
        return false;
    }

    public void userEvent() {
        try {
            service.invokeAndWait(new ParseAction());
        } catch (DOMAccessException dae) {
            System.err.println("Caught " + dae);
            dae.printStackTrace();
        }
        if (i != null) i.userChangedValue(variantValue);
    }


    private class FieldAction implements DOMAction {
        public Object run(DOMAccessor accessor) {
            try {
                if (element != null)
                    act(element);
            } catch (Throwable t) {
                System.err.println("Caught " + t);
                t.printStackTrace();
                element = null;
            }
            return null;
        }
        public void act(HTMLElement element) { }
    }
    private class RedrawAction extends FieldAction {
        public void act(HTMLElement e) { redraw(); }
    }
    private class ParseAction extends FieldAction {
        public void act(HTMLElement e) { parse(); }
    }

}
