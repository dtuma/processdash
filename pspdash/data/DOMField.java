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


import com.sun.java.browser.dom.*;
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
            //setReadOnly(readOnly);
            element.setClassName(readOnly ? "readOnlyElem" : "editableElem");
            if (element instanceof HTMLInputElement)
                ((HTMLInputElement) element).setTabIndex(readOnly ? -1 : 0);
            else if (element instanceof HTMLTextAreaElement)
                ((HTMLTextAreaElement) element).setTabIndex(readOnly ? -1 : 0);
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
            try {/*
                HTMLDocument doc = (HTMLDocument) accessor.getDocument(null);
                HTMLFormElement form = (HTMLFormElement) doc.getForms().item(formNum);
                HTMLElement elem = (HTMLElement) form.getElements().item(elemNum);*/
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
