// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2006 Software Process Dashboard Initiative
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.FileProperties;
import net.sourceforge.processdash.util.RobustFileWriter;

public class InternalSettings extends Settings {

    private static FileProperties fsettings = null;
    private static String settingsFile = null;
    private static PropertyChangeSupport propSupport =
        new PropertyChangeSupport(InternalSettings.class);
    public static final String sep = System.getProperty("file.separator");
    private static boolean dirty;
    private static boolean disableChanges;

    private static final Logger logger = Logger
              .getLogger(InternalSettings.class.getName());

    public static void initialize(String settingsFile) {
        checkPermission("initialize");

        if (settings != null)
            return;

        String cwd  = System.getProperty("user.dir");
        String home = System.getProperty("user.home");
        homedir = home;

        InputStream in;

        // create application defaults.  First, get a set of common defaults.
        //
        defaults = defaultProperties();

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

        //
        Properties propertyComments = new Properties();
        try {
            propertyComments.load
                (Settings.class.getResourceAsStream("pspdash.ad-comments"));
        } catch (Exception e0) {}

        // finally, open the user's settings file and load those properties.  The
        // default search path for these user settings is:
        //    * the current directory
        //    * the user's home directory (specified by the system property
        //          "user.home")
        //
        // on Windows systems, this will look for a file named "pspdash.ini".
        // on all other platforms, it will look for a file named ".pspdash".
        //
        settings = fsettings = new FileProperties(defaults, propertyComments);
        fsettings.setDateStamping(false);

        String filename = getSettingsFilename();
        dirty = disableChanges = false;

        try {
            if (settingsFile != null && settingsFile.length() != 0) {
                in = new FileInputStream(settingsFile);
                fsettings.setFilename(settingsFile);
            } else {
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

            homedir = cwd;
            settingsFile = homedir + sep + filename;
            dirty = true;
        }
        InternalSettings.settingsFile = settingsFile;
        fsettings.setFilename(settingsFile);
        fsettings.setHeader(PROPERTIES_FILE_HEADER);
        fsettings.setKeepingStrangeKeys(true);
    }
    private static final String getSettingsFilename() {
        if (System.getProperty("os.name").toUpperCase().startsWith("WIN"))
            return "pspdash.ini";
        else
            return ".pspdash";
    }
    public static String getSettingsFileName() {
        checkPermission("getFileName");
        return settingsFile;
    }
    private static final String PROPERTIES_FILE_HEADER =
        "User preferences for the PSP Dashboard tool " +
        "(NOTE: When specifying names of files or directories within this " +
        "file, use a forward slash as a separator.  It will be translated " +
        "into an appropriate OS-specific directory separator automatically.)";

    public static void set(String name, String value, String comment) {
        set0(name, value, comment);
    }

    public static void set(String name, String value) {
        set0(name, value, null);
    }

    private static synchronized void set0(String name, String value,
              String comment) {
        checkPermission("write."+name);

        if (disableChanges)
            return;

        String oldValue = fsettings.getProperty(name);

        if (value == null)
            fsettings.remove(name);

        else {
            fsettings.put(name, value);
            if (comment != null)
                fsettings.setComment(name, comment);
        }

        serializable = null;
        dirty = true;

        saveSettings();

        propSupport.firePropertyChange(name, oldValue, value);
    }

    public static String getExtendableVal(String name, String sep) {
        String result = getVal(name);
        String extra = getVal("additional." + name);
        if (extra != null) {
            if (result == null)
                result = extra;
            else
                result = result + sep + extra;
            set(name, result);
            set("additional." + name, null);
        }
        return result;
    }

    static synchronized void saveSettings() {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (fsettings != null) try {
                    Writer out = new RobustFileWriter(fsettings.getFilename());
                    fsettings.store(out);
                    out.close();
                    dirty = false;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unable to save settings file.", e);
                }
                return null;
            }});
    }

    public static synchronized boolean isDirty() {
        return dirty;
    }

    static synchronized void setDisableChanges(boolean disable) {
        disableChanges = disable;
        logger.info("Settings changes "
                + (disableChanges ? "disabled." : "enabled."));
    }

    public static void addPropertyChangeListener(PropertyChangeListener l) {
        propSupport.addPropertyChangeListener(l);
    }
    public static void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propSupport.addPropertyChangeListener(propertyName, l);
    }

    public static void removePropertyChangeListener(PropertyChangeListener l) {
        propSupport.removePropertyChangeListener(l);
    }
    public static void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propSupport.removePropertyChangeListener(propertyName, l);
    }

    static void loadLocaleSpecificDefaults(ResourceBundle resources) {
        checkPermission("initialize");
        defaults.put("dateFormat", resources.getString("Date_Format"));
        defaults.put("dateTimeFormat", resources.getString("Date_Time_Format"));
        defaults.put("http.charset", resources.getString("HTTP_charset_"));
    }

}
