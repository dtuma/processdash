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
import net.sourceforge.processdash.data.repository.Repository;

import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLInputElement;



class DOMCheckboxField extends DOMField {

    private static boolean setManually = false;
    private static String HTML_TRUE = "true";
    private static String HTML_FALSE = null;

    private static String[] HTML_TRUE_VALUES = {
        "true", "checked", "1.0" };
    private static String[] HTML_FALSE_VALUES = {
        null, "false", "unchecked", "", "0.0" };


    public DOMCheckboxField(DOMService service, DOMDelayedRedrawer redrawer,
                            HTMLElement element, Repository data,
                            String dataPath) {

        super(service, redrawer, element, ((HTMLInputElement) element).getName(),
              data, dataPath);
    }


    public void fetch() {
        debug("fetching - value is '"+i.getValue()+"'");
        variantValue  = i.getBoolean();
    }

    public void paint() {
        boolean desiredValue = Boolean.TRUE.equals(variantValue);
        debug("painting checkbox, variantValue="+desiredValue);
        if (!setManually) {
            ((HTMLInputElement) element).setChecked(desiredValue);
            if (desiredValue == isChecked()) return;
        }

        debug("setting attribute manually");
        setManually = true;
        element.setAttribute("checked", (desiredValue ? HTML_TRUE : HTML_FALSE));

        // now check to see if our changes "took."
        if (desiredValue != isChecked()) {
            // our changes didn't take. Try other values.
            String[] trialValues =
                (desiredValue ? HTML_TRUE_VALUES : HTML_FALSE_VALUES);
            for (int i = 0;   i < trialValues.length;   i++) {
                element.setAttribute("checked", trialValues[i]);
                // if this trial value appeared to work, save it into the
                // appropriate static field
                if (desiredValue == isChecked()) {
                    debug("changing "+desiredValue+" constant to '"+trialValues[i]+"'");
                    if (desiredValue)
                        HTML_TRUE = trialValues[i];
                    else
                        HTML_FALSE = trialValues[i];
                    break;
                }
            }
        }

        debug("done with paint; value is now "+isChecked());
    }

    public void parse() {
        variantValue = new Boolean(isChecked());
    }

    private boolean isChecked() {
        return ((HTMLInputElement) element).getChecked();
    }

    protected boolean isReadOnly() {
        return ((HTMLInputElement) element).getReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        // Broken?? ((HTMLInputElement) element).setReadOnly(readOnly);
        manuallySetReadOnly(readOnly);
        if (DataApplet.ieVersion() > 0)
            ((HTMLInputElement) element).setTabIndex(readOnly ? -1 : 0);
    }
}
