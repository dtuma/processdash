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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import net.sourceforge.processdash.templates.TemplateLoader;


public class Translator {

    static final String BUNDLE_NAME = "Translator";

    /** The translation engine in use */
    private static TranslationEngine TRANSLATOR = null;


    /** Returns true if a translation engine is operating.
     */
    public static final boolean isTranslating() {
        return TRANSLATOR != null;
    }


    /** Translate a string.
     *
     * The string can contain HTML markup, which will not be translated.
     * If no tranlation engine is operating, the original string will
     * be returned unchanged.
     *
     * @param s the string to translate
     * @return a translated version of the string.
     */
    public static final String translate(String s) {
        try{
            if (TRANSLATOR == null || s == null)
                return s;
            else
                return TRANSLATOR.translateString(s);
        } catch (Exception e) {
            e.printStackTrace();
            return s;
        }
    }

    /** Translate the contents of a stream on-the-fly.
     *
     * The stream can contain HTML markup, which will not be translated.
     * If no tranlation engine is operating, the original stream will
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
            TRANSLATOR = (TranslationEngine) c.newInstance();

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
            ResourceBundle r = Resources.getDashBundle(BUNDLE_NAME);
            TRANSLATOR = new DefaultEngine(r);
        } catch (Exception e) {
        }
    }

    public static String unpackKey(String dictTerm) {
        StringBuffer result = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(dictTerm, "_ ");
        result.append(tok.nextToken());
        while (tok.hasMoreTokens())
            result.append(" ").append(tok.nextToken());
        return result.toString();
    }




    /** Default simple translation engine
     *
     * This engine translates verbatim words and phrases based upon
     * mappings found in a resource bundle.  It cannot translate streams,
     * and it does not find translatable words/phrases that are substrings
     * of a string to translate.
     */
    public static class DefaultEngine implements TranslationEngine {

        private Map translations;

        public DefaultEngine(ResourceBundle r) {
            translations = new HashMap();
            Enumeration e = r.getKeys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String text = unpackKey(key);
                String replacement = r.getString(key);
                translations.put(text, replacement);
                translations.put(text.toLowerCase(), replacement);
            }
        }


        public String translateString(String s) {
            if (s == null) return null;

            String result = (String) translations.get(s);
            if (result != null) return result;

            result = (String) translations.get(s.toLowerCase());
            if (result != null) return result;

            return s;
        }


        public Reader translateStream(Reader r) {
            // the default engine does not attempt stream translation.
            return r;
        }

    }

}
