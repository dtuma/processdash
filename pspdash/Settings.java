// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.util.Properties;
import java.util.StringTokenizer;

public class Settings {

    protected static Properties settings = null;
    protected static String homedir = null;

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
        if (newSettings == null)
            settings = defaultProperties();
        else
            settings = newSettings;
    }


    public static String getVal(String name) {
        if (settings == null)
            return null;
        else
            return settings.getProperty(name);
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
}
