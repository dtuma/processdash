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

package net.sourceforge.processdash.data.applet;

import pspdash.data.DoubleData;

// import netscape.javascript.JSObject;

public class InputName {
    public String name, flags, value;

    public InputName(String text, String prefix) {
        int tabpos;
        if (text.startsWith("[")) {
            text = text.substring(1);
            tabpos = text.lastIndexOf(']');
        } else
            tabpos = text.indexOf('\t');
        if (tabpos == -1) {
            name = text;
            flags = value = "";
        } else {
            name = text.substring(0, tabpos);
            int equalpos = text.indexOf('=', tabpos+1);
            if (equalpos == -1) {
                flags = text.substring(tabpos+1);
                value = "";
            } else {
                flags = text.substring(tabpos+1, equalpos);
                value = text.substring(equalpos+1);
            }
        }
        if (prefix == null) prefix = "";
        if (name == null || name.length() == 0)
            name = prefix;
        else if (!name.startsWith("/"))
            name = prefix + "/" + name;
    }

    //  public InputName(JSObject o) {
    //    this((String) o.getMember("name"));
    //  }

    public boolean hasFlag(char flag) {
        return (flags.indexOf(flag) != -1);
    }

    public int digitFlag() {
        for (int pos = flags.length();  pos != 0; ) {
            char c = flags.charAt(--pos);
            if ((c <= '9') && (c >= '0')) return (c - '0');
        }
        return DoubleData.AUTO_DECIMAL;
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        if (flags != null && flags.length() != 0) result.append(flags);
        if (value != null && value.length() != 0) {
            result.append('=');
            result.append(value);
        }
        if (result.length() == 0)
            return name;
        else {
            result.insert(0, '\t');
            result.insert(0, name);
            return result.toString();
        }
    }

}
