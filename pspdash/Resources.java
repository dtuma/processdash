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

import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.NoSuchElementException;

public class Resources extends ResourceBundle {

    private String bundleName;

    private Resources(String bundleName, ResourceBundle parent) {
        this.bundleName = bundleName;
        setParent(parent);
    }

    protected Object handleGetObject(String key) { return null; }
    public Enumeration getKeys() { return parent.getKeys(); }
    public Locale getLocale() { return parent.getLocale(); }



    private static Resources globalResources = null;
    private static MessageFormat dialogIndicatorFormat = null;

    private static void initGlobalResources() {
        if (globalResources == null) {
            globalResources = getDashBundle("pspdash.Resources");
        }
    }

    public static String getGlobalString(String key) {
        initGlobalResources();
        return globalResources.getString(key);
    }

    public static String addDialogIndicator(String value) {
        if (dialogIndicatorFormat == null)
            dialogIndicatorFormat = new MessageFormat
                (getGlobalString("Dialog_Indicator_FMT"));

        return dialogIndicatorFormat.format(new Object[] { value });
    }


    private static class TemplateClassLoader extends ClassLoader {

        protected Class findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
        protected URL findResource(String name) {
            if (name.startsWith("pspdash"))
                name = "resources" + name.substring(7);
            else if (name.startsWith("/pspdash"))
                name = "/resources" + name.substring(8);
            return TemplateLoader.resolveURL(name);
        }

    }

    private static final ClassLoader RESOURCE_LOADER =
        new TemplateClassLoader();


    private static final boolean TIME_LOADING = false;

    public static Resources getDashBundle(String bundleName) {
        long start = 0;
        if (TIME_LOADING)
            start = System.currentTimeMillis();

        ResourceBundle parentBundle = ResourceBundle.getBundle
            (bundleName, Locale.getDefault(), RESOURCE_LOADER);
        Resources result = new Resources(bundleName, parentBundle);

        if (TIME_LOADING) {
            long end = System.currentTimeMillis();
            long delta = end - start;
            System.out.println("Loading bundle Took " + delta + " ms");
        }

        return result;
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
        return getStrings(this, prefix, keys);
    }
    public static String[] getStrings(ResourceBundle bundle,
                                      String prefix,
                                      String[] keys) {
        String[] result = new String[keys.length];
        for (int i = keys.length;   i-- > 0; )
            result[i] = bundle.getString(prefix + keys[i]);
        return result;
    }

    public int[] getInts(String prefix, String[] keys) {
        return getInts(this, prefix, keys);
    }
    public static int[] getInts(ResourceBundle bundle,
                                String prefix,
                                String[] keys) {
        int[] result = new int[keys.length];
        for (int i = keys.length;   i-- > 0; ) try {
            result[i] = Integer.parseInt(bundle.getString(prefix + keys[i]));
        } catch (NumberFormatException nfe) {
            result[i] = -1;
        }
        return result;
    }

}
