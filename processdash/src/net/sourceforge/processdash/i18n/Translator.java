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

import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.templates.TemplateLoader;


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
        TRANSLATOR = null;
        createCustomEngine();
        if (TRANSLATOR == null) createDefaultEngine();
        STRICT_STRING_TRANSLATION = Settings.getBool
            ("i18n.strictTranslation", true);
        //System.out.println("Translator is " +
        //                   (TRANSLATOR == null ? "OFF" : "ON"));
    }


    private static void createCustomEngine() {
        try {
            URL u = getCustomEngineClassURL();
            if (u == null) return;

            String path = u.toString();
            int pos = path.lastIndexOf('/');
            String className = path.substring(pos+1, path.length()-6);
            path = path.substring(0, pos+1);

            URL[] classPath = new URL[1];
            classPath[0] = new URL(path);
            URLClassLoader cl = new URLClassLoader(classPath);
            Class c = cl.loadClass(className);
            try {
                Constructor cstr = c.getConstructor(new Class[] { Map.class });
                TRANSLATOR = (TranslationEngine) cstr.newInstance
                    (new Object[] { buildTranslationItemsMap() });
            } catch (Exception e) {
                TRANSLATOR = (TranslationEngine) c.newInstance();
            }

        } catch (Exception e) {
            TRANSLATOR = null;
        }
    }

    private static URL getCustomEngineClassURL() {
        Locale l = Locale.getDefault();
        String language = l.getLanguage();
        String country = l.getCountry();
        URL result;

        if (language != null && language.length() != 0) {
            if (country != null && country.length() != 0) {
                result = findTemplateClass("_" + language + "_" + country);
                if (result != null) return result;
            }
            result = findTemplateClass("_" + language);
            if (result != null) return result;
        }
        return null;
    }


    private static URL findTemplateClass(String qualifiers) {
        String resName = "resources/Translator" + qualifiers + ".class";
        return TemplateLoader.resolveURL(resName);
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
