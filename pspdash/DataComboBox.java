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


package pspdash;

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import pspdash.data.DataRepository;
import pspdash.data.DoubleData;

public class DataComboBox extends JComboBox implements ILessThan {

    static public    String settingName = "hiddenData";
    static protected String delim       = "|";
    static protected Vector hiddenData  = null;

    DataComboBox(DataRepository dr) {
        super();
        setEditable (true);

        if (hiddenData == null) {
            hiddenData = new Vector();
            String hiddenText = Settings.getVal(settingName);
            if (hiddenText != null) {
                StringTokenizer st = new StringTokenizer (hiddenText, delim, false);
                while (st.hasMoreElements())
                    hiddenData.addElement (st.nextToken ());
            }
        }

        String s;
        int sepLoc;
        Enumeration keys = dr.keys();
        Vector v = new Vector();
        while (keys.hasMoreElements()) {
            s = (String)keys.nextElement();
            sepLoc = s.lastIndexOf ("/");
            if (sepLoc != -1)
                s = s.substring (sepLoc + 1);
            if (v.indexOf (s) == -1 && hiddenData.indexOf (s) == -1) {
                v.addElement (s);
            }
        }

        VectorQSort vqs = new VectorQSort (v, this);
        vqs.sort();
        v = vqs.getVector();

        keys = v.elements();
        while (keys.hasMoreElements()) {
            addItem ((String)keys.nextElement());
        }
    }

    public boolean lessThan(Object oFirst, Object oSecond) {
        return ((String)oFirst).compareTo ((String)oSecond) < 0;
    }

}
