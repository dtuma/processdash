// Copyright (C) 2003-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.processdash.Settings;


public class Translator {

    /** The translation engine in use */
    private static TranslationEngine TRANSLATOR = null;

    /** Translation resources in use */
    private static Map TRANSLATION_ITEMS = null;

    /** Should string translations be strict? */
    private static boolean STRICT_STRING_TRANSLATION = false;


    /** Returns true if a translation engine is operating.
     */
    public static final boolean isTranslating() {
        return TRANSLATOR != null;
    }


    /** Translate a string.
     *
     * The string can contain HTML markup, which will not be translated.
     * If no translation engine is operating, the original string will
     * be returned unchanged.
     *
     * @param s the string to translate
     * @return a translated version of the string.
     */
    public static final String translate(String s) {
        try{
            if (TRANSLATOR == null || s == null)
                return s;
            else if (STRICT_STRING_TRANSLATION) {
                String result = (String) TRANSLATION_ITEMS.get(s);
                return (result != null ? result : s);
            } else
                return TRANSLATOR.translateString(s);
        } catch (Exception e) {
            e.printStackTrace();
            return s;
        }
    }

    /** Translate the contents of a stream on-the-fly.
     *
     * The stream can contain HTML markup, which will not be translated.
     * If no translation engine is operating, the original stream will
     * be returned unchanged.
     *
     * @param s the stream to translate
     * @return a stream which returns a translated version of the original
     *     contents.
     */
    public static final Reader translate(Reader s) {
        if (TRANSLATOR == null || s == null)
            return s;
        else
            return TRANSLATOR.translateStream(s);
    }




    public static void init() {
        String mode = Settings.getVal("i18n.translationMode", "auto");
        if ("off".equals(mode)) {
            TRANSLATOR = null;
        } else {
            if (TRANSLATOR == null)
                createDefaultEngine();
            STRICT_STRING_TRANSLATION = "strict".equals(mode);
        }
    }

    private static void createDefaultEngine() {
        try {
            Resources r = Resources.getGlobalBundle();
            Locale l = r.getLocale();
            String lang = l.getLanguage();
            if (lang != null && lang.length() != 0) {
                TRANSLATOR = new DefaultEngine(buildTranslationItemsMap());
                System.out.println("Created default translation engine.");
            }
        } catch (Exception e) {
        }
    }

    private static Map buildTranslationItemsMap() {
        Resources r = Resources.getGlobalBundle();
        Map m = r.asMap();

        TRANSLATION_ITEMS = new HashMap();
        TRANSLATION_ITEMS.putAll(m);
        Iterator i = m.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            if (key.indexOf('_') == -1)
                continue;

            String modKey = key.replace('_', ' ');
            TRANSLATION_ITEMS.put(modKey, r.getString(key));
        }

        return m;
    }

}
