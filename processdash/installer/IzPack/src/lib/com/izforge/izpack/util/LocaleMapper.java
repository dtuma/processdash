// Copyright (C) 2003 Tuma Solutions, LLC
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

package com.izforge.izpack.util;

import java.util.Locale;

/** Mapper to translate ISO3 codes to and from ISO-639 codes.
 * 
 * I've never figured out why the author of IzPack didn't use any of the
 * Locale support that is built into Java.  That support is based upon
 * ISO-639 language codes, while IzPack uses "ISO3" codes instead.  This
 * class translates between the two.
 */
public class LocaleMapper {

    private static final String[][] ISO_CODES = {
        { "cat", "ca" },  // Catalan
        { "chn", "zh" },  // Chinese
        { "deu", "de" },  // German
        { "eng", "en" },  // English
        { "fin", "fi" },  // Finnish
        { "fra", "fr" },  // French
        { "hun", "hu" },  // Hungarian
        { "ita", "it" },  // Italian
        { "jpn", "ja" },  // Japanese
        { "ned", "nl" },  // Dutch
        { "pol", "pl" },  // Polish
        { "por", "pt" },  // Brazilian Portuguese
        { "rom", "ro" },  // Romanian
        { "rus", "ru" },  // Russian
        { "spa", "es" },  // Spanish
        { "svk", "sk" },  // Slovak
        { "swe", "sv" },  // Swedish
        { "ukr", "uk" },  // Ukrainian
    };

    public static Locale getLocaleForISO3(String iso3) {
        for (int i = ISO_CODES.length;   i-- > 0;   )
            if (ISO_CODES[i][0].equalsIgnoreCase(iso3))
                return new Locale(ISO_CODES[i][1]);
        return null;
    }

    public static String getISO3forLocale(Locale loc) {
        if (loc == null)
            return null;
        String lang = loc.getLanguage();
        for (int i = ISO_CODES.length;   i-- > 0;   )
            if (ISO_CODES[i][1].equalsIgnoreCase(lang))
                return ISO_CODES[i][0];
        return null;
    }

    public static String getISO3forDefaultLocale() {
        String result = getISO3forLocale(Locale.getDefault());
        if (result == null)
            return "eng";
        else
            return result;
    }

    public static String getDisplayName(String iso3) {
        Locale l = getLocaleForISO3(iso3);
        if (l == null)
            return iso3;
        else
            return l.getDisplayLanguage();
    }
}
