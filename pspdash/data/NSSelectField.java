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
import netscape.javascript.JSObject;


class NSSelectField extends NSField {

    Vector optionList = null;
    JSObject formOptions = null;


    public NSSelectField(JSObject element, Repository data, String dataPath) {
        super(element, data, dataPath);

                                    // fill the optionList value with the various
        optionList = new Vector();	// OPTIONs that are a part of this SELECT tag.
        formOptions = (JSObject) element.getMember("options");
        JSObject option;
        int numOptions = ((Double)formOptions.getMember("length")).intValue();
        for (int optIdx = 0;   optIdx < numOptions;   optIdx++)
            optionList.addElement(getOptionValue(formOptions, optIdx));
    }


    public void fetch() { variantValue = i.getString(); }
    public void paint() { setSelection((String) variantValue); }
    public void parse() { variantValue = getSelection(); }

    public void setSelection(String text) {
        if (element != null) {
            for (int idx = optionList.size();   idx-- > 0; )
                if (text.equals((String)optionList.elementAt(idx))) {
                    element.setMember("selectedIndex", new Double(idx));
                    return;
                }
        }
    }

    public String getSelection() {
        if (element == null) return "";

        int idx = ((Double) element.getMember("selectedIndex")).intValue();
        if (idx >= optionList.size())
            return "";

        return (String) optionList.elementAt(idx);
    }

    private static String getOptionValue(JSObject formOptions, int idx) {
        JSObject option = (JSObject) formOptions.getSlot(idx);
        String result = (String) option.getMember("value");
        if (result.length() == 0)
            result = (String) option.getMember("text");

        return result;
    }
}
