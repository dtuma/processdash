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

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

public class Settings {


    private static Properties settings = null;
    private static String homedir = null;
    private static String settingsFile = null;

    public static final String sep = System.getProperty("file.separator");


    public static void initialize(String settingsFile) {
        if (settings != null)
            return;

        String cwd  = System.getProperty("user.dir");
        String home = System.getProperty("user.home");
        homedir = home;

        InputStream in;

        // create application defaults.  First, get a set of common defaults.
        //
        Properties defaults = defaultProperties();

        try {
            // now supplement the defaults by reading the system-wide settings file.
            // This file should be in the same directory as the Settings.class file.
            //
            in = Settings.class.getResourceAsStream("pspdash.ad");

            if (in != null) {
                Properties systemDefaults = new Properties(defaults);
                systemDefaults.load(in);
                in.close();
                defaults = systemDefaults;
            }

        } catch (Exception e) { e.printStackTrace(); }

        // finally, open the user's settings file and load those properties.  The
        // default search path for these user settings is:
        //    * the current directory
        //    * the user's home directory (specified by the system property
        //          "user.home")
        //
        // on Windows systems, this will look for a file named "pspdash.ini".
        // on all other platforms, it will look for a file named ".pspdash".
        //
        settings = new Properties(defaults);

        String filename = getSettingsFilename();

        try {
            if (settingsFile != null && settingsFile.length() != 0)
                in = new FileInputStream(settingsFile);
            else {
                try {
                    homedir = cwd;
                    in = new FileInputStream(settingsFile=(homedir + sep + filename));
                } catch (Exception e1) {
                    homedir = home;
                    in = new FileInputStream(settingsFile=(homedir + sep + filename));
                }
            }

            settings.load(in);
            in.close();

        } catch (Exception e) {
            System.out.println("could not read user preferences file from any of");
            System.out.println("     " + cwd + sep + filename);
            System.out.println("     " + home + sep + filename);
            System.out.println("...using system-wide defaults.");

            // what a brain-dead default home directory.  DONT use C:\WINDOWS as
            // the user's home dir, no matter the cost.
            if (homedir.toUpperCase().endsWith("WINDOWS"))
                homedir = cwd;
            settingsFile = homedir + sep + filename;
        }
        Settings.settingsFile = settingsFile;
    }
    private static final String getSettingsFilename() {
        if (System.getProperty("os.name").toUpperCase().startsWith("WIN"))
            return "pspdash.ini";
        else
            return ".pspdash";
    }
    public static String getSettingsFileName() {
        return settingsFile;
    }


    /** Programmatically create a set of common defaults. */
    private static Properties defaultProperties() {

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
        String val = getVal(name);
        if (val == null) return null;

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

    public static void set(String name, String value) {
        if (settings != null) settings.put(name, value);
    }

    public static Properties getSettings() {
        return settings;
    }
}
