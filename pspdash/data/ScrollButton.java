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

import java.util.Hashtable;
import netscape.javascript.JSObject;

public class ScrollButton implements InputListener {

    DataApplet data;
    JSObject button;
    int increment = 1;
    int currentIncrement;
    JSObject form, formElements;

    public ScrollButton(DataApplet data, JSObject button, String param) {
        this.data = data;
        this.button = button;

        form = (JSObject) button.getMember("form");
        formElements  = (JSObject) form.getMember("elements");

        form.setMember("minIndex", new Double(0.0));

        try {
                                    // see if the param is a number like +12 or -3
            increment = Integer.parseInt(param);
        } catch (NumberFormatException e) { try {
                                    // see if the parameter is a sign like + or -
            increment = Integer.parseInt(param + "1");
        } catch (NumberFormatException e2) {}}
    }

    private int extractNumberSuffix(StringBuffer s) {
        int result = -1;
        int pos = s.length();
        while (pos != 0)
            if (! Character.isDigit(s.charAt(pos-1)))
                break;
            else
                pos--;

        try {
            result = Integer.parseInt(s.toString().substring(pos));
            s.setLength(pos);
        } catch (Exception e) {}
        return result;
    }


    private void incrementElement(JSObject element) {
        String type = (String) element.getMember("type");
        if ("button".equalsIgnoreCase(type) ||
            "reset".equalsIgnoreCase(type) ||
            "submit".equalsIgnoreCase(type))
            return;


        InputName param = new InputName(element);
        System.err.println("incrementing element "+param.name);
        StringBuffer name = new StringBuffer(param.name);
        int number = extractNumberSuffix(name);
        if (number == -1)
            return;

        param.name = name.append(number + currentIncrement).toString();
        element.setMember("name", param.toString());
        System.err.println("    done incrementing, name is now "+
                           (String) element.getMember("name"));
        data.reinititializeFormElement(element);
    }

    public void event() {
        int minIndex = ((Double) form.getMember("minIndex")).intValue();
        System.err.println("ScrollButton.event called, minIndex="+minIndex);

        currentIncrement = increment;
        if (minIndex + increment < 0 )
            currentIncrement = 0 - minIndex;

        form.setMember("minIndex", new Double(minIndex + currentIncrement));

        int numElements = ((Double) formElements.getMember("length")).intValue();
        System.err.println("    numElements = "+numElements);
        for (int index = 0;   index < numElements;   index++)
            incrementElement((JSObject) formElements.getSlot(index));
    }

    public void destroy(boolean dataRepositoryExists) {
        data = null;
        button = null;
        formElements = null;
    }
}
