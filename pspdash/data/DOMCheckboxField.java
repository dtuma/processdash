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


import com.sun.java.browser.dom.DOMService;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLInputElement;


class DOMCheckboxField extends DOMField {


    public DOMCheckboxField(DOMService service, DOMDelayedRedrawer redrawer,
                            HTMLElement element, Repository data,
                            String dataPath) {

        super(service, redrawer, element, ((HTMLInputElement) element).getName(),
              data, dataPath);
    }


    public void fetch() {
        variantValue = i.getBoolean();
    }

    public void paint() {
        /* The following line is causing a stack overflow error, in the routine
            sun.plugin.dom.DOMObjectHelper.setBooleanMemberNoEx
        ((HTMLInputElement) element).setChecked(Boolean.TRUE.equals(variantValue));
        */
    }

    public void parse() {
        variantValue = new Boolean(((HTMLInputElement) element).getChecked());
    }

    public void setReadOnly(boolean readOnly) {
        ((HTMLInputElement) element).setReadOnly(readOnly);
        ((HTMLInputElement) element).setTabIndex(readOnly ? -1 : 0);
    }

}
