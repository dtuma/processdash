// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/** This class reads a collection of settings from a URL, and adds them to
 * a user's dashboard Settings file.
 * 
 * This class normally would be run during the installation process, to
 * optionally tweak dashboard configuration settings.
 */
public class MergeSettings {

    public static void main(String[] args) {
        if (args.length == 2) {
            String url = args[0];
            String destDir = args[1];
            try {
                merge(url, destDir);
            } catch (Exception e) {
            }
        }
        System.exit(0);
    }

    private static void merge(String url, String destDir) throws Exception {
        if ("none".equals(url))
            return;

        Properties propsIn = new Properties();
        propsIn.load(new URL(url).openStream());
        if (propsIn.isEmpty())
            return;

        File destFile = new File(destDir, getSettingsFilename());

        Properties orig = new Properties();
        try {
            FileInputStream origIn = new FileInputStream(destFile);
            orig.load(origIn);
            origIn.close();
        } catch (Exception e) {
        }

        Properties propsOut = new Properties();
        for (Iterator i = propsIn.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            if (!key.startsWith(PROP_PREFIX))
                continue;

            String settingName = key.substring(PROP_PREFIX.length());
            String value = (String) e.getValue();
            if (!orig.containsKey(settingName))
                propsOut.put(settingName, value);
        }

        if (propsOut.isEmpty())
            return;

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destFile, true))); // append = true
        out.newLine();
        out.newLine();
        for (Iterator i = propsOut.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            // this logic isn't quite up-to-par with the java.util.Properties
            // spec, but unfortunately, neither is FileSettings (which we will
            // use to read the data back in).  This mimics the behavior of
            // the FileSettings class.
            out.write((String) e.getKey());
            out.write("=");
            out.write((String) e.getValue());
            out.newLine();
        }
        out.close();
    }

    private static final String getSettingsFilename() {
        if (System.getProperty("os.name").toUpperCase().startsWith("WIN"))
            return "pspdash.ini";
        else
            return ".pspdash";
    }

    private static final String PROP_PREFIX = "pspdash.";

}
