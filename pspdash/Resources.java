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
// E-Mail POC:  tuma@users.sourceforge.net

package pspdash;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class Resources {

    private static ResourceBundle globalResources = null;
    private static MessageFormat dialogIndicatorFormat = null;

    private Resources() {}

    private static void initGlobalResources() {
        if (globalResources == null) {
            globalResources = getBundle("pspdash.Resources");

        }
    }

    public static String getString(String key) {
        initGlobalResources();
        return globalResources.getString(key);
    }

    public static String addDialogIndicator(String value) {
        if (dialogIndicatorFormat == null)
            dialogIndicatorFormat = new MessageFormat
                (getString("Dialog_Indicator_FMT"));

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

    public static ResourceBundle getBundle(String bundleName) {
        long start = 0;
        if (TIME_LOADING)
            start = System.currentTimeMillis();

        ResourceBundle result = ResourceBundle.getBundle
            (bundleName, Locale.getDefault(), RESOURCE_LOADER);

        if (TIME_LOADING) {
            long end = System.currentTimeMillis();
            long delta = end - start;
            System.out.println("Loading bundle Took " + delta + " ms");
        }

        return result;
    }

    public static String format(ResourceBundle bundle, String key,
                                Object[] args) {
        return MessageFormat.format(bundle.getString(key), args);
    }

    public static String format(ResourceBundle bundle, String key,
                                Object arg1) {
        return format(bundle, key, new Object[] { arg1 });
    }

    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2) {
        return format(bundle, key, new Object[] { arg1, arg2 });
    }

    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2, Object arg3) {
        return format(bundle, key, new Object[] { arg1, arg2, arg3 });
    }

    public static String format(ResourceBundle bundle, String key,
                                Object arg1, Object arg2, Object arg3,
                                Object arg4) {
        return format(bundle, key, new Object[] { arg1, arg2, arg3, arg4 });
    }



    public static String[] format(String delim, ResourceBundle bundle,
                                  String key, Object[] args) {
        return StringUtils.split(format(bundle, key, args), delim);
    }

    public static String[] format(String delim, ResourceBundle bundle,
                                  String key, Object arg1) {
        return StringUtils.split(format(bundle, key, arg1), delim);
    }

    public static String[] format(String delim, ResourceBundle bundle,
                                  String key, Object arg1, Object arg2) {
        return StringUtils.split(format(bundle, key, arg1, arg2), delim);
    }

    public static String[] format(String delim, ResourceBundle bundle,
                                  String key, Object arg1, Object arg2,
                                  Object arg3) {
        return StringUtils.split(format(bundle, key, arg1, arg2, arg3), delim);
    }

    public static String[] format(String delim, ResourceBundle bundle,
                                  String key, Object arg1, Object arg2,
                                  Object arg3, Object arg4) {
        return StringUtils.split
            (format(bundle, key, arg1, arg2, arg3, arg4), delim);
    }

    public static String[] getStrings(ResourceBundle bundle, String key) {
        return StringUtils.split(bundle.getString(key), "\r\n");
    }

    public static String[] getStrings(ResourceBundle bundle,
                                      String prefix,
                                      String[] keys) {
        String[] result = new String[keys.length];
        for (int i = keys.length;   i-- > 0; )
            result[i] = bundle.getString(prefix + keys[i]);
        return result;
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
