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

package net.sourceforge.processdash.i18n;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;


public class TranslationFilter implements Comparator {

    private static final int NEEDS_NO_TRANSLATION = 0;
    private static final int NEEDS_TRANSLATION = 1;
    private TranslationEngine engine;

    public TranslationFilter() {
        engine = createClobberingEngine();
    }

    public int compare(Object key, Object value) {
        if (keyNeedsNoTranslation((String) key))
            return NEEDS_NO_TRANSLATION;
        else if (keyNeedsTranslation((String) key))
            return NEEDS_TRANSLATION;
        else if (valueNeedsNoTranslation((String) value))
            return NEEDS_NO_TRANSLATION;
        else
            return NEEDS_TRANSLATION;
    }

    private boolean keyNeedsNoTranslation(String key) {
        if (key.endsWith("_")) return true;
        if (key.indexOf("__") != -1) return true;
        return false;
    }

    private boolean keyNeedsTranslation(String key) {
        if (key.startsWith("(Resources)"))
            return true;
        if (key.startsWith("org"))
            return true;
        return false;
    }


    private boolean valueNeedsNoTranslation(String value) {
        if (value == null)
            // if no default translation is provided, then no translation is
            // required.
            return true;

        value = removeVariables(value);
        value = removeTranslatables(value);
        return valueContainsNoCharacters(value);
    }

    private String removeVariables(String value) {
        while (true) {
            int beg = value.indexOf("${");
            if (beg == -1) return value;

            int end = value.indexOf("}", beg);
            if (end == -1) return value;

            value = value.substring(0, beg) + value.substring(end+1);
        }
    }

    private String removeTranslatables(String value) {
        return engine.translateString(value);
    }

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private boolean valueContainsNoCharacters(String value) {
        for (int i = 0;   i < value.length();   i++) {
            if (ALPHA.indexOf(Character.toUpperCase(value.charAt(i))) != -1)
                return false;
        }
        return true;
    }

    private TranslationEngine createClobberingEngine() {
        HashMap clobberingMap = new HashMap();
        Resources r = Resources.getGlobalBundle();
        Enumeration e = r.getKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            clobberingMap.put(key, "");
        }

        return new DefaultEngine(clobberingMap);
    }
}
