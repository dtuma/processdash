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


import netscape.javascript.JSObject;


abstract class NSField extends HTMLField {

    Repository data;
    public JSObject element;

    public NSField(JSObject element, Repository data, String dataPath) {
        this.element = element;

        if (data == null) {
            variantValue = dataPath;
            redraw();
            return;
        }

        // redraw(); // necessary?

        String dataName = (String) element.getMember("name");
        i = InterpreterFactory.create(data, dataName, dataPath);
        i.setConsumer(this);
    }


    abstract public void paint();	// update the Html element with variantValue.
    abstract public void parse();	// update variantValue from the HTML element.
    abstract public void fetch();	// update variantValue from DataInterpreter i.


    private static Object EDITABLE = new Double(1.0);
    private static Object READONLY = new Double(0.0);

    public void redraw() {
        if (element != null) {
            paint();
            element.setMember("isEditable", (isEditable() ? EDITABLE : READONLY));
        }
    }

    public void repositoryChangedValue() {
        fetch();
        redraw();
    }

    public void userEvent() {
        if (element != null) parse();
        if (i != null) i.userChangedValue(variantValue);
    }

}
