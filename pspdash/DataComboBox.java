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
import com.oroinc.text.perl.Perl5Util;
import com.oroinc.text.perl.MalformedPerl5PatternException;

public class DataComboBox extends JComboBox implements ILessThan {

    static public    String settingName = "hiddenData";
    static protected String delim       = "|";
    static protected Vector hiddenData  = null;

    DataComboBox(DataRepository dr) {
        super();
        setEditable (true);

        Iterator i = getAllDataNames(dr).iterator();
        while (i.hasNext())
            addItem((String) i.next());

        if (hiddenData == null) {
            hiddenData = new Vector();
            String hiddenText = Settings.getVal(settingName);
            if (hiddenText != null) {
                StringTokenizer st = new StringTokenizer (hiddenText, delim, false);
                while (st.hasMoreElements())
                    hiddenData.addElement (st.nextToken ());
            }
        }
    }

    public boolean lessThan(Object oFirst, Object oSecond) {
        return ((String)oFirst).compareTo ((String)oSecond) < 0;
    }

    public static Set getAllDataNames(DataRepository dr) {
        Perl5Util perl = new Perl5Util();
        Set result = new TreeSet();
        String hiddenDataRE = "m\n^(" + Settings.getVal(settingName) + ")$\ni";

        String dataName;
        int sepLoc;
        Iterator dataNames = dr.getKeys();
        while (dataNames.hasNext()) {
            dataName = (String) dataNames.next();

                                      // ignore transient and anonymous data.
            if (dataName.indexOf("//") != -1) continue;

                                      // get the name of the data element only.
            sepLoc = dataName.lastIndexOf ("/");
            if (sepLoc != -1)
                dataName = dataName.substring (sepLoc + 1);

            try {
                if (!perl.match(hiddenDataRE, dataName))
                    result.add(dataName);
            } catch (MalformedPerl5PatternException mp5pe) {
                System.err.println("The user setting, 'hiddenData', is not a valid " +
                                   "regular expression.");
                break;
            }
        }

        return result;
    }

}
