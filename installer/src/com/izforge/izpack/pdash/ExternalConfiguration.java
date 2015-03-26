// Copyright (C) 2006-2011 Tuma Solutions, LLC
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
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package com.izforge.izpack.pdash;

import java.net.URL;
import java.util.Properties;

/** Manages optional configuration settings that are stored externally to the
 * installer.
 * 
 * Although it is nice for an installer to be a self-contained JAR file, there
 * are times you would prefer for it to be less opaque.  For example, you might
 * give an installer file to a customer, and want them to be able to customize
 * certain aspects of the installation process without contacting you.
 * 
 * This file automates a standard approach for discovering such configuration
 * settings.  It looks for a file called "custom-install.ini" in the same
 * directory as the installer JAR file.  If it is present, its contents will
 * be read and provided by this class.  If no such file is present, this class
 * will behave as if the file was present and empty.
 */
public class ExternalConfiguration {

    private static final String INSTALL_INI_FILENAME = "custom-install.ini";

    private static Properties CONFIG = null;

    public static Properties getConfig() {
        if (CONFIG == null)
            try {
                CONFIG = openConfig();
            } catch (Exception e) {
            }

        if (CONFIG == null)
            CONFIG = new Properties();

        return CONFIG;
    }

    private static Properties openConfig() throws Exception {
        Properties result = new Properties();
        readConfig(result, getURL1());
        readConfig(result, getURL2());
        return result;
    }

    private static void readConfig(Properties dest, String url) {
        try {
            URL iniURL = new URL(url);
            Properties p = new Properties();
            p.load(iniURL.openStream());
            dest.putAll(p);
        } catch (Exception e) {}
    }

    public static String getURL1() {
        // find a "custom-install.ini" file that is inside the installation JAR
        // file, and return the URL.  If no such file exists, return "none".
        URL result = ExternalConfiguration.class.getResource("/res/"
                + INSTALL_INI_FILENAME);
        if (result == null)
            return "none";
        else
            return result.toString();
    }

    public static String getURL2() {
        // find a "custom-install.ini" file that is contained in the same
        // directory as the installation JAR file, and return the URL.  If no
        // such file exists, return "none".
        String myURL = ExternalConfiguration.class.getResource(
                "ExternalConfiguration.class").toString();
        int exclPos = myURL.indexOf("!/");
        if (exclPos == -1 || !myURL.startsWith("jar:"))
            return "none";

        String jarURL = myURL.substring(4, exclPos);
        int slashPos = jarURL.lastIndexOf('/');
        if (slashPos == -1)
            return "none";

        String urlPrefix = jarURL.substring(0, slashPos + 1);
        return urlPrefix + INSTALL_INI_FILENAME;
    }

}
