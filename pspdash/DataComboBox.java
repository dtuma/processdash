// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.awt.event.*;
import javax.swing.*;
import java.text.Collator;
import java.util.*;
import java.io.*;
import pspdash.data.DataRepository;


public class DataComboBox extends JComboBox {

    static public    String settingName  = "hiddenData";
    static protected String delim        = "|";
    static protected Vector hiddenData   = null;
    static protected Map    translations = null;

    DataComboBox(DataRepository dr) {
        super();

        Iterator i = getAllDataNames(dr).iterator();
        while (i.hasNext())
            addItem((String) i.next());

        if (translations != null)
            setRenderer(new ToolTipCellRenderer(null, translations));
    }

    private static Set dataNameList = null;

    public static void clearCachedNameList() { dataNameList = null; }

    public static Set getAllDataNames(DataRepository dr) {
        if (dataNameList != null) return dataNameList;

        Perl5Util perl = PerlPool.get();
        Set result;
        String hiddenDataRE = "m\n(" + Settings.getVal(settingName) + ")\ni";
        if (Translator.isTranslating()) {
            translations = new HashMap();
            result = new TreeSet(new TranslatingSorter());
        } else {
            translations = null;
            result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        }

        String dataName, trans;
        int sepLoc;
        Iterator dataNames = dr.getDataElementNameSet().iterator();
        while (dataNames.hasNext()) {
            dataName = (String) dataNames.next();

                                      // ignore transient and anonymous data.
            if (dataName.indexOf("//") != -1) continue;

            try {
                if (!perl.match(hiddenDataRE, dataName)) {
                    if (translations != null) {
                        trans = Translator.translate(dataName);
                        if (trans != dataName)
                            translations.put(dataName, trans);
                    }
                    result.add(dataName);
                }
            } catch (Perl5Util.RegexpException p5ure) {
                System.err.println("The user setting, 'hiddenData', is not a valid " +
                                   "regular expression.");
                break;
            }
        }

        PerlPool.release(perl);
        dataNameList = Collections.unmodifiableSet(result);
        if (translations != null)
            translations = Collections.unmodifiableMap(translations);
        return result;
    }

    private static class TranslatingSorter implements Comparator {
        protected Collator collator = Collator.getInstance();
        public int compare(Object o1, Object o2) {
            Object t1 = translations.get(o1); if (t1 == null) t1 = o1;
            Object t2 = translations.get(o2); if (t2 == null) t2 = o2;
            return collator.compare(t1, t2);
        }
    }
}
