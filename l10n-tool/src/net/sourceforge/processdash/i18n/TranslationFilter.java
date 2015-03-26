// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.util.Comparator;

public class TranslationFilter implements Comparator {

    private static final int NEEDS_NO_TRANSLATION = 0;
    private static final int NEEDS_TRANSLATION = 1;

    public int compare(Object key, Object value) {
        if (keyNeedsNoTranslation((String) key))
            return NEEDS_NO_TRANSLATION;
        else if (valueNeedsNoTranslation
                 ((String) value, canBeTranslated((String) key)))
            return NEEDS_NO_TRANSLATION;
        else
            return NEEDS_TRANSLATION;
    }

    private boolean keyNeedsNoTranslation(String key) {
        if (key.endsWith("_")) return true;
        if (key.indexOf("__") != -1) return true;
        return false;
    }

    private boolean canBeTranslated(String key) {
        if (key.startsWith("(Resources)") ||
            key.startsWith("org") ||
            key.startsWith("com"))
            return false;

        return true;
    }

    private boolean valueNeedsNoTranslation(String value,
                                            boolean isTranslated) {
        if (value == null)
            // if no default translation is provided, then no translation is
            // required.
            return true;

        value = removeVariables(value);
        return valueContainsNoCharacters(value);
    }

    private String removeVariables(String value) {
        while (true) {
            int beg = value.indexOf("${");
            if (beg == -1) beg = value.indexOf('{');
            if (beg == -1) return value;

            int end = value.indexOf("}", beg);
            if (end == -1) return value;

            value = value.substring(0, beg) + value.substring(end+1);
        }
    }

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private boolean valueContainsNoCharacters(String value) {
        for (int i = 0;   i < value.length();   i++) {
            if (ALPHA.indexOf(Character.toUpperCase(value.charAt(i))) != -1)
                return false;
        }
        return true;
    }
}