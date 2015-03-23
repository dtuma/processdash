// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.data.applet.ns;


import java.util.Vector;

import net.sourceforge.processdash.data.repository.Repository;
import netscape.javascript.JSObject;


class NSSelectField extends NSField {

    Vector optionList = null;
    JSObject formOptions = null;

    private static boolean USE_DOUBLE = true;

    public NSSelectField(JSObject element, Repository data, String dataPath) {
        super(element, data, dataPath);

                                    // fill the optionList value with the various
        optionList = new Vector();  // OPTIONs that are a part of this SELECT tag.
        formOptions = (JSObject) element.getMember("options");
        JSObject option;
        int numOptions = NSFieldManager.intValue(formOptions.getMember("length"));
        for (int optIdx = 0;   optIdx < numOptions;   optIdx++)
            optionList.addElement(getOptionValue(formOptions, optIdx));

        if (variantValue != null) paint();
    }


    public void fetch() { variantValue = i.getString(); }
    public void paint() { setSelection((String) variantValue); }
    public void parse() { variantValue = getSelection(); }

    public void setSelection(String text) {
        if (element != null && optionList != null) {
            for (int idx = optionList.size();   idx-- > 0; )
                if (stringEquals(text, (String)optionList.elementAt(idx))) {
                    element.setMember("selectedIndex", makeInt(idx));

                    // check to see if our changes took.
                    int resultingValue = NSFieldManager.intValue
                        (element.getMember("selectedIndex"));
                    if (idx != resultingValue) {
                        // if the changes didn't take, try the other approach.
                        USE_DOUBLE = !USE_DOUBLE;
                        element.setMember("selectedIndex", makeInt(idx));
                    }

                    return;
                }
        }
    }

    private boolean stringEquals(String a, String b) {
        if ((a == null || a.length() == 0) &&
            (b == null || b.length() == 0)) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private Object makeInt(int i) {
        if (USE_DOUBLE)
            return new Double(i);
        else
            return new Integer(i);
    }

    public String getSelection() {
        if (element == null) return "";

        int idx = NSFieldManager.intValue(element.getMember("selectedIndex"));
        if (idx == -1 || idx >= optionList.size())
            return "";

        return (String) optionList.elementAt(idx);
    }

    private static String getOptionValue(JSObject formOptions, int idx) {
        JSObject option = (JSObject) formOptions.getSlot(idx);
        String result = (String) option.getMember("value");
        if (result == null || result.trim().length() == 0) {
            result = (String) option.getMember("text");
            if (result != null) result = result.trim();
        }

        return result;
    }
}
