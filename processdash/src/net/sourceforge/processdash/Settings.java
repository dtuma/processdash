// Copyright (C) 2003-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sourceforge.processdash.security.DashboardPermission;

public class Settings {

    public static final String SYS_PROP_PREFIX = Settings.class.getName() + ".";
    public static final String READ_ONLY = "READ_ONLY";
    protected static Properties settings = null;
    protected static Properties serializable = null, defaults = null;
    protected static String homedir = null;
    static boolean readOnly;

    public static final String sep = System.getProperty("file.separator");

    /** Programmatically create a set of common defaults. */
    protected static Properties defaultProperties() {

        Properties defaults = new Properties();

        defaults.put("taskFile", "~/tasks");
        defaults.put("stateFile", "~/state");
        defaults.put("dateFormat", "MM/dd/yyyy|MM dd yyyy|MMM dd, yyyy");
        defaults.put("dateTimeFormat",
                     "MMM dd, yyyy hh:mm:ss aaa|MMM dd, yyyy hh:mm:ss aaa z");

        return defaults;
    }

    public static void initialize(Properties newSettings) {
        if (settings != null)
            checkPermission("initialize");

        if (newSettings == null)
            settings = defaultProperties();
        else
            settings = newSettings;
    }

    public static String getVal(String name) {
        String result = System.getProperty(SYS_PROP_PREFIX + name);
        if (result != null)
            return result;

        if (settings == null)
            return null;
        else
            return settings.getProperty(name);
    }

    public static String getVal(String name, String defaultValue) {
        String value = getVal(name);
        if (value != null && value.length() > 0)
            return value;
        else
            return defaultValue;
    }


    public static boolean getBool(String name, boolean defaultValue) {
        String value = getVal(name);
        if (value == null || value.length() == 0) return defaultValue;
        switch (value.charAt(0)) {
        case 't': case 'T': case 'y': case 'Y': return true;
        case 'f': case 'F': case 'n': case 'N': return false;
        }
        return defaultValue;
    }

    public static int getInt(String name, int defaultValue) {
        String setting = getVal(name);
        if (setting != null) try {
            return Integer.parseInt(setting);
        } catch (NumberFormatException nfe) {}
        return defaultValue;
    }


    public static String getFile(String name) {
        return translateFile(getVal(name));
    }

    public static String translateFile(String val) {
        if (val == null || homedir == null) return null;

        StringBuffer result = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(val, "~/", true);
        String token;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if      (token.equals("~"))  result.append(homedir);
            else if (token.equals("/"))  result.append(sep);
            else                         result.append(token);
        }

        return result.toString();
    }

    public static String getDir(String name, boolean endWithSep) {
        String result = getFile(name);
        if (result == null) return null;

        if (endWithSep)     // caller wants the value to end with a separator
            if (result.endsWith(sep))
                return result;
            else
                return result + sep;

        else                // caller doesn't want the value to end with separator
            if (result.endsWith(sep))
                return result.substring(0, result.length() - 1);
            else
                return result;
    }

    public static Properties getSettings() {
        if (serializable == null) {
            Properties results = new Properties(defaults);
            Enumeration keys = settings.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = settings.get(key);
                results.put(key, value);
            }
            serializable = results;
        }
        return serializable;
    }

    public static boolean isReadOnly() {
        return readOnly;
    }
    public static boolean isReadWrite() {
        return !readOnly;
    }

    protected static void checkPermission(String action) {
        if (System.getSecurityManager() != null) {
            DashboardPermission p = new DashboardPermission("settings."+action);
            p.checkPermission();
        }
    }

}
