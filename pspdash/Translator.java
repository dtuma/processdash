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

import java.util.*;

public class Translator {

    private static boolean IS_TRANSLATING = false;
    public static final boolean isTranslating() { return IS_TRANSLATING; }

    private static Resources translations;
    private static Set keys;

    public static void init() {
        translations = null;
        keys = null;
        IS_TRANSLATING = false;

        try {
            translations = Resources.getDashBundle("pspdash.Translator");
            keys = new HashSet();
            /* new TreeSet(new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String s1 = (String) o1;
                        String s2 = (String) o2;
                        int result = s2.length() - s1.length();
                        if (result != 0) return result;
                        return s1.compareTo(s2);
                    }
                    public boolean equals(Object obj) { return this == obj; }
                    }); */
            Enumeration e = translations.getKeys();
            while (e.hasMoreElements())
                keys.add(e.nextElement());
        } catch (MissingResourceException mre) {
        }
        IS_TRANSLATING = !keys.isEmpty();
    }

    public static final String translate(String s) {
        if (IS_TRANSLATING || keys == null || s == null) return s;

        // try a naiive approach - is the string itself a key?
        if (keys.contains(s))
            return translations.getString(s);

        /* Disabled for now - doesn't work properly.  Probably need regexps.
        // optimistically assume that we won't find anything to translate.
        Iterator i = keys.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            if (s.indexOf(key) != -1)
                // if we find something to translate, bite the bullet
                // and translate the string.
                return reallyTranslate(s, key, i);
        }
        */

        // our optimism was rewarded - there were no translatable
        // strings in the input!
        return s;
    }

    private static final String reallyTranslate(String s, String key,
                                                Iterator i)
    {
        StringBuffer buf = new StringBuffer(s);
        StringUtils.findAndReplace(buf, key, translations.getString(key));

        while (i.hasNext()) {
            key = (String) i.next();
            if (StringUtils.indexOf(buf, key) != -1)
                StringUtils.findAndReplace
                    (buf, key, translations.getString(key));
        }

        return buf.toString();
    }

}
