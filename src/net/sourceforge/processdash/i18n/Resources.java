// Copyright (C) 2003-2016 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.MockMap;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;


public class Resources extends ResourceBundle implements StringMapper {

    private String bundleName;
    private String bundlePrefix;
    private ResourceBundle delegate;

    private static Locale TARGET_LOCALE = Locale.getDefault();
    private static Logger logger = Logger.getLogger(Resources.class.getName());

    private Resources(String bundleName, String bundlePrefix,
                      ResourceBundle delegate, ResourceBundle parent) {
        this.bundleName = bundleName;
        this.bundlePrefix = bundlePrefix;
        this.delegate = delegate;
        setParent(parent);
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getBundlePrefix() {
        return bundlePrefix;
    }

    protected Object handleGetObject(String key) {
        Object result = null;

        // if the key refers to a different bundle, redirect.
        if (isRedirectKey(key))
            return getRedirectedObject(key);

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
            return interpolate((String) result);
        else
            return result;
    }


    private boolean isRedirectKey(String key) {
        return (key.startsWith("/") && key.indexOf(':') > 1);
    }

    private Object getRedirectedObject(String key) {
        int pos = key.indexOf(':', 1);
        String redirectBundleName = key.substring(1, pos);
        String redirectKey = key.substring(pos+1);
        ResourceBundle redirectBundle = getDashBundle(redirectBundleName);
        return redirectBundle.getString(redirectKey);
    }


    public Enumeration getKeys() {
        // This won't include keys we inherit from our parent.
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


    public String interpolate(String s) {
        return interpolate(s, null);
    }
    public String interpolate(String s, StringMapper escape) {
        if (escape == null)
            return StringUtils.interpolate(this, s);
        else
            return StringUtils.interpolate
                (StringUtils.concat(escape, this), s);
    }

    public Map asMap() {
        Map result = new HashMap();

        Enumeration e = delegate.getKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = getString(key);
            result.put(key, value);
        }

        return result;
    }


    private static Resources globalResources = null;
    private static MessageFormat dialogIndicatorFormat = null;

    private static void initGlobalResources() {
        if (globalResources == null)
            globalResources = getResourceBundle("resources.(Resources)", null);
    }

    public static void setTargetLocale(Locale l) {
        TARGET_LOCALE = l;
        globalResources = null;
    }

    public static Resources getGlobalBundle() {
        initGlobalResources();
        return globalResources;
    }

    private static String addDialogIndicator(String value) {
        if (dialogIndicatorFormat == null)
            dialogIndicatorFormat = new MessageFormat
                (getGlobalBundle().getString("Dialog_Indicator__FMT"));

        return dialogIndicatorFormat.format(new Object[] { value });
    }

    private static ClassLoader RESOURCE_LOADER = null;
    private static ClassLoader getResourceLoader() {
        if (RESOURCE_LOADER == null)
            RESOURCE_LOADER = makeResourceLoader();
        return RESOURCE_LOADER;
    }
    public static void setResourceLoader(ClassLoader cl) {
        RESOURCE_LOADER = cl;
        globalResources = null;
        dialogIndicatorFormat = null;
    }

    private static ClassLoader makeResourceLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return makeResourceLoader0();
            }});
    }
    private static ClassLoader makeResourceLoader0() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    Resources.class.getResourceAsStream("ResourceLoaders.txt"),
                    "UTF-8"));
            String className;
            while ((className = in.readLine()) != null)
                try {
                    Class clz = Class.forName(className);
                    ClassLoader result = (ClassLoader) clz.newInstance();
                    logger.config("Using i18n resource loader " + className);
                    return result;
                } catch (Throwable t) {}

        } catch (Throwable t) {}

        return Resources.class.getClassLoader();
    }

    private static final boolean TIME_LOADING = false;
    private static final char PREFIX_SEPARATOR = '\u0000';

    public static Resources getDashBundle(String bundleName) {
        initGlobalResources();
        return getDashBundle(bundleName, globalResources);
    }
    public static Resources getDashBundle(String bundleName, Resources parent) {
        bundleName = bundleName.replace('.', PREFIX_SEPARATOR);
        return getResourceBundle("resources." + bundleName, parent);
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
            bundlePrefix = bundleName.substring(bundleBaseName.length()+1)
                .replace(PREFIX_SEPARATOR, '.');

        ResourceBundle realBundle = ResourceBundle.getBundle
            (bundleBaseName, TARGET_LOCALE, getResourceLoader());
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
        String[] result = StringUtils.split(s, "\r\n");
        if (result.length == 1 && s.length() > 80)
            result = StringUtils.breakDownString(s, 65, true);
        return result;
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

    public Map asJSTLMap() {
        return new MapImpl();
    }

    private class MapImpl extends MockMap<String, Object> {

        @Override
        public Object get(Object key) {
            String resKey = (String) key;
            if (resKey.endsWith("_FMT")) {
                return new DeferredFormatImpl(resKey);
            } else {
                return getString(resKey);
            }
        }

    }

    private class DeferredFormatImpl extends MockMap<String, Object> {

        private String resKey;
        private List args;

        public DeferredFormatImpl(String resKey) {
            this.resKey = resKey;
            this.args = new ArrayList();
        }

        @Override
        public Object get(Object key) {
            args.add(key);
            return this;
        }

        @Override
        public String toString() {
            return format(resKey, args.toArray());
        }

    }

}
