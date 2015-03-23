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


import com.sun.java.browser.dom.DOMService;

import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.Repository;

import org.w3c.dom.html.*;


import java.util.Vector;


class DOMSelectField extends DOMField {

    Vector optionList = null;


    public DOMSelectField(DOMService service, DOMDelayedRedrawer redrawer,
                          HTMLElement element, Repository data,
                          String dataPath) {
        super(service, redrawer, element, ((HTMLSelectElement) element).getName(),
              data, dataPath);

        /* getOptions fix - temporary

                                   // fill the optionList value with the various
        optionList = new Vector();  // OPTIONs that are a part of this SELECT tag.
        HTMLCollection formOptions = ((HTMLSelectElement) element).getOptions();
        HTMLOptionElement option;
        int numOptions = formOptions.getLength();
        for (int optIdx = 0;   optIdx < numOptions;   optIdx++)
            optionList.addElement(getOptionValue(formOptions, optIdx));
        */
    }


    public void fetch() { variantValue = i.getString(); }
    public void paint() {
        String text = (variantValue == null ? "" : (String) variantValue);
        setSelection((HTMLSelectElement) element, text);
    }
    public void parse() {
        variantValue = getSelection((HTMLSelectElement) element);
    }
    public void setReadOnly(boolean readOnly) {
        //Not in DOM??? ((HTMLSelectElement) element).setReadOnly(readOnly);
        if (DataApplet.ieVersion() > 0)
            ((HTMLSelectElement) element).setTabIndex(readOnly ? -1 : 0);
    }


    public void setSelection(HTMLSelectElement element, String text) {
        if (element != null) {
            element.setValue(text);
            /*
            for (int idx = optionList.size();   idx-- > 0; )
                if (text.equals((String)optionList.elementAt(idx))) {
                    element.setSelectedIndex(idx);
                    return;
                }
            */
        }
    }

    public String getSelection(HTMLSelectElement element) {
        if (element == null) return "";
        return element.getValue();
        /*
        int idx = element.getSelectedIndex();
        if (idx < 0 || idx >= optionList.size())
            return "";

        return (String) optionList.elementAt(idx);
        */
    }

    private static String getOptionValue(HTMLCollection formOptions, int idx) {
        HTMLOptionElement option = (HTMLOptionElement) formOptions.item(idx);
        String result = option.getValue();
        if (result == null || result.length() == 0) {
            result = option.getText();
            if (result != null) result = result.trim();
        }

        return result;
    }
}
