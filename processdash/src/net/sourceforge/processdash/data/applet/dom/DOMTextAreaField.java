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


package net.sourceforge.processdash.data.applet.dom;


import com.sun.java.browser.dom.DOMService;

import net.sourceforge.processdash.data.applet.*;

import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLTextAreaElement;

import pspdash.data.Repository;


class DOMTextAreaField extends DOMField {

    public DOMTextAreaField(DOMService service, DOMDelayedRedrawer redrawer,
                            HTMLElement element, Repository data,
                            String dataPath) {

        super(service, redrawer, element,
              ((HTMLTextAreaElement) element).getName(), data, dataPath);
    }

    public void fetch() { variantValue = i.getString(); }

    public void paint() {
        String text = (variantValue == null ? "" : variantValue.toString());
        ((HTMLTextAreaElement) element).setValue(text);
    }

    public void parse() {
        variantValue = ((HTMLTextAreaElement) element).getValue();
    }

    protected boolean isReadOnly() {
        return ((HTMLTextAreaElement) element).getReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        // Broken?? ((HTMLTextAreaElement) element).setReadOnly(readOnly);
        manuallySetReadOnly(readOnly);
        if (DataApplet.ieVersion() > 0)
            ((HTMLTextAreaElement) element).setTabIndex(readOnly ? -1 : 0);
    }
}
