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

import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class Resources extends ResourceBundle {

    private String bundleName;
    private String bundlePrefix;
    private ResourceBundle delegate;

    private Resources(String bundleName, String bundlePrefix,
                      ResourceBundle delegate, ResourceBundle parent) {
        this.bundleName = bundleName;
        this.bundlePrefix = bundlePrefix;
        this.delegate = delegate;
        setParent(parent);
    }

    protected Object handleGetObject(String key) {
        Object result = null;

        // first try appending the bundle prefix, if present.
        if (bundlePrefix != null) try {
            String prefixedKey = bundlePrefix + "." + key;
            result = delegate.getObject(prefixedKey);
        } catch (MissingResourceException mre) { }

        // if no prefix, or if prefixed key not found, lookup the plain key.
        if (result == null) try {
            result = delegate.getObject(key);
        } catch (MissingResourceException mre) { }

        if (result instanceof String)
            return interpolate((String) result, false);
        else
            return result;
    }

    public Enumeration getKeys() {
        // this isn't exactly accurate.  It won't include the
        // inherited keys from our parent.
        return delegate.getKeys();
    }
    public Locale getLocale() {
        return delegate.getLocale();
    }

    public String getDlgString(String key) {
        String result = null;
        try {
            result = getString(key + "_DLG");
        } catch (MissingResourceException mre) {}
        if (result == null)
            result = addDialogIndicator(getString(key));
        return result;
    }

    public String getHTML(String key) {
        return HTMLUtils.escapeEntities(getString(key));
    }


    private static final String VAR_START_PAT = "${";
    private static final String VAR_END_PAT = "}";
    public String interpolate(String s, boolean html) {
        while (true) {
            int beg = s.indexOf(VAR_START_PAT);
            if (beg == -1) return s;

            int end = s.indexOf(VAR_END_PAT, beg);
            if (end == -1) return s;

            String var = s.substring(beg+VAR_START_PAT.length(), end);
            String replacement = getString(var);
            if (html)
                replacement = HTMLUtils.escapeEntities(replacement);
            s = s.substring(0, beg) + replacement +
                s.substring(end+VAR_END_PAT.length());
        }
    }



    private static Resources globalResources = null;
    private static MessageFormat dialogIndicatorFormat = null;

    private static void initGlobalResources() {
        if (globalResources == null)
            globalResources = getResourceBundle("resources.Resources", null);
    }

    public static Resources getGlobalBundle() {
        initGlobalResources();
        return globalResources;
    }

    private static String addDialogIndicator(String value) {
        if (dialogIndicatorFormat == null)
            dialogIndicatorFormat = new MessageFormat
                (getGlobalBundle().getString("Dialog_Indicator_FMT"));

        return dialogIndicatorFormat.format(new Object[] { value });
    }


    private static class SafeTemplateClassLoader extends ClassLoader {

        protected Class findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
        protected URL findResource(String name) {
            name = name.replace('$', '.');
            return findResourceImpl(name);
        }
        protected URL findResourceImpl(String mappedName) {
            if (!mappedName.startsWith("/"))
                mappedName = "/" + mappedName;
            mappedName = "Templates" + mappedName;
            return Resources.class.getClassLoader().getResource(mappedName);
        }

    }

    private static class TemplateClassLoader extends SafeTemplateClassLoader {
        protected URL findResourceImpl(String mappedName) {
            return TemplateLoader.resolveURL(mappedName);
        }
    }

    private static ClassLoader makeResourceLoader() {
        try {
            Class.forName("org.w3c.dom.Element");
            return new TemplateClassLoader();
        } catch (Throwable t) { }
        System.out.println("XML classes unavailable - using safe loader");
        return new SafeTemplateClassLoader();
    }
    private static final ClassLoader RESOURCE_LOADER = makeResourceLoader();


    private static final boolean TIME_LOADING = false;
    private static final char PREFIX_SEPARATOR = '_';

    public static Resources getDashBundle(String bundleName) {
        initGlobalResources();
        bundleName = bundleName.replace('.', PREFIX_SEPARATOR);
        return getResourceBundle("resources." + bundleName, globalResources);
    }
    public static Resources getTemplateBundle(String bundleName) {
        initGlobalResources();
        return getResourceBundle(bundleName, globalResources);
    }
    private static Resources getResourceBundle(String bundleName,
                                               ResourceBundle parent) {
        long start = 0;
        if (TIME_LOADING)
            start = System.currentTimeMillis();

        String bundlePrefix = null;
        String bundleBaseName = getBundleBaseName(bundleName);
        if (!bundleName.equals(bundleBaseName))
            bundlePrefix = bundleName.substring(bundleBaseName.length()+1);

        ResourceBundle realBundle = ResourceBundle.getBundle
            (bundleBaseName, Locale.getDefault(), RESOURCE_LOADER);
        Resources result = new Resources(bundleBaseName, bundlePrefix,
                                         realBundle, parent);

        if (TIME_LOADING) {
            long end = System.currentTimeMillis();
            long delta = end - start;
            System.out.println("Loading bundle Took " + delta + " ms");
        }

        return result;
    }
    private static final String getBundleBaseName(String bundleName) {
        int pos = bundleName.indexOf(PREFIX_SEPARATOR);
        if (pos == -1) return bundleName;
        return bundleName.substring(0, pos);
    }

    public String format(String key, Object[] args) {
        return format(this, key, args);
    }
    public static String format(ResourceBundle bundle, String key,
                                Object[] args) {
        return MessageFormat.format(bundle.getString(key), args);
    }

    public String format(String key, Object arg1) {
        return format(this, key, arg1);
    }
    public static String format(ResourceBundle bundle, String key,
                                Object arg1) {
        return format(bundle, key, new Object[] { arg1 });
    }

    public String format(String key, Object arg1, Object arg2) {
        return format(this, key, arg1, arg2);
    }
    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2) {
        return format(bundle, key, new Object[] { arg1, arg2 });
    }

    public String format(String key, Object arg1, Object arg2, Object arg3) {
        return format(this, key, arg1, arg2, arg3);
    }
    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2, Object arg3) {
        return format(bundle, key, new Object[] { arg1, arg2, arg3 });
    }

    public String format(String key, Object arg1, Object arg2, Object arg3,
                                    Object arg4) {
        return format(this, key, arg1, arg2, arg3, arg4);
    }
    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2, Object arg3,
                                Object arg4) {
        return format(bundle, key, new Object[] { arg1, arg2, arg3, arg4 });
    }


    private static String[] split_(String s) {
        return StringUtils.split(s, "\r\n");
    }

    public String[] formatStrings(String key, Object[] args) {
        return formatStrings(this, key, args);
    }
    public static String[] formatStrings(ResourceBundle bundle,
                                         String key, Object[] args) {
        return split_(format(bundle, key, args));
    }

    public String[] formatStrings(String key, Object arg1) {
        return formatStrings(this, key, arg1);
    }
    public static String[] formatStrings(ResourceBundle bundle, String key,
                                         Object arg1) {
        return split_(format(bundle, key, arg1));
    }

    public String[] formatStrings(String key, Object arg1, Object arg2) {
        return formatStrings(this, key, arg1, arg2);
    }
    public static String[] formatStrings(ResourceBundle bundle, String key,
                                         Object arg1, Object arg2) {
        return split_(format(bundle, key, arg1, arg2));
    }

    public String[] formatStrings(String key, Object arg1, Object arg2,
                                  Object arg3) {
        return formatStrings(this, key, arg1, arg2, arg3);
    }
    public static String[] formatStrings(ResourceBundle bundle, String key,
                                         Object arg1, Object arg2,
                                         Object arg3) {
        return split_(format(bundle, key, arg1, arg2, arg3));
    }

    public String[] formatStrings(String key, Object arg1, Object arg2,
                                  Object arg3, Object arg4) {
        return formatStrings(this, key, arg1, arg2, arg3, arg4);
    }
    public static String[] formatStrings(ResourceBundle bundle, String key,
                                         Object arg1, Object arg2,
                                         Object arg3, Object arg4) {
        return split_(format(bundle, key, arg1, arg2, arg3, arg4));
    }

    public String[] getStrings(String key) {
        return getStrings(this, key);
    }
    public static String[] getStrings(ResourceBundle bundle, String key) {
        return split_(bundle.getString(key));
    }

    public String[] getStrings(String prefix, String[] keys) {
        return getStrings(this, prefix, keys, "");
    }
    public String[] getStrings(String[] keys, String suffix) {
        return getStrings(this, "", keys, suffix);
    }
    public String[] getStrings(String prefix, String[] keys, String suffix) {
        return getStrings(this, prefix, keys, suffix);
    }
    public static String[] getStrings(ResourceBundle bundle,
                                      String prefix,
                                      String[] keys,
                                      String suffix) {
        String[] result = new String[keys.length];
        for (int i = keys.length;   i-- > 0; )
            result[i] = bundle.getString(prefix + keys[i] + suffix);
        return result;
    }



    public int[] getInts(String prefix, String[] keys) {
        return getInts(this, prefix, keys, "");
    }
    public int[] getInts(String[] keys, String suffix) {
        return getInts(this, "", keys, suffix);
    }
    public int[] getInts(String prefix, String[] keys, String suffix) {
        return getInts(this, prefix, keys, suffix);
    }
    public static int[] getInts(ResourceBundle bundle,
                                String prefix,
                                String[] keys,
                                String suffix) {
        int[] result = new int[keys.length];
        for (int i = keys.length;   i-- > 0; ) try {
            result[i] = Integer.parseInt
                (bundle.getString(prefix + keys[i] + suffix));
        } catch (NumberFormatException nfe) {
            result[i] = -1;
        }
        return result;
    }

}
