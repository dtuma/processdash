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

public class Settings {


    private static Properties settings = null;


    public static void initialize(String settingsFile) {
        if (settings != null)
            return;

        String cwd  = System.getProperty("user.dir");
        String home = System.getProperty("user.home");
        String sep  = System.getProperty("file.separator");
        String homedir = home;
        InputStream in;

        // create application defaults.  First, create a set of defaults
        // programmatically based on the current context.
        //
        Properties defaults = new Properties();
        defaults.put("taskFile", "~" + sep + "tasks");
        defaults.put("stateFile", "~" + sep + "state");
        defaults.put("browser.command", "netscape");
        defaults.put("templates.URL.prefix", "file:");
        defaults.put("templates.directory", "~" + sep + "Template");
        defaults.put("dateFormat", "MM/dd/yyyy|MM dd yyyy|MMM dd, yyyy");
        defaults.put("dateTimeFormat", "MMM dd, yyyy hh:mm:ss aaa|MMM dd, yyyy hh:mm:ss aaa z");

        try {
            // now supplement the defaults by reading the system-wide settings file.
            // This file should be in the same directory as the Settings.class file.
            //
            in = Class.forName("pspdash.Settings").getResourceAsStream("pspdash.ad");

            if (in != null) {
                Properties systemDefaults = new Properties(defaults);
                systemDefaults.load(in);
                in.close();
                defaults = systemDefaults;
            }

        } catch (Exception e) { e.printStackTrace(); }

        // finally, open the user's settings file and load those properties.  The
        // default search path for these user settings is:
        //    * ".pspdash" in the current directory
        //    * "pspdash.ini" in the current directory
        //    * ".pspdash" in the user's home directory (specified by the system
        //		property "user.home")
        //    * "pspdash.ini" in the user's home directory.
        //
        settings = new Properties(defaults);

        try {
            if (settingsFile != null && settingsFile.length() != 0)
                in = new FileInputStream(settingsFile);
            else {
                try {
                    homedir = cwd;
                    in = new FileInputStream(settingsFile=(cwd + sep + ".pspdash"));
                } catch (Exception e1) { try {
                    in = new FileInputStream(settingsFile=(cwd + sep + "pspdash.ini"));
                } catch (Exception e2) { try {
                    homedir = home;
                    in = new FileInputStream(settingsFile=(home + sep + ".pspdash"));
                } catch (Exception e3) {
                    in = new FileInputStream(settingsFile=(home + sep + "pspdash.ini"));
                } } } }

            settings.load(in);
            in.close();

        } catch (Exception e) {
            System.err.println("could not read user preferences file from any of");
            System.err.println("     " + cwd + sep + ".pspdash");
            System.err.println("     " + cwd + sep + "pspdash.ini");
            System.err.println("     " + home + sep + ".pspdash");
            System.err.println("     " + home + sep + "pspdash.ini");
            System.err.println("...using system-wide defaults.");
        }

        String homeDirPrefix = "~" + sep;
        Enumeration names = settings.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = settings.getProperty(name);
            if (value.startsWith(homeDirPrefix)) {
                value = homedir + value.substring(1);
                settings.put(name, value);
            }
        }
    }


    public static void initialize(Properties newSettings) {
        if (newSettings == null)
            initialize("");
        else
            settings = newSettings;
    }


    public static String getVal(String name) {
        if (settings == null)
            return null;
        else
            return settings.getProperty(name);
    }

    public static void set(String name, String value) {
        if (settings != null) settings.put(name, value);
    }

    public static Properties getSettings() {
        return settings;
    }
}

