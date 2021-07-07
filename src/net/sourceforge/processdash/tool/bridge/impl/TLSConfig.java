// Copyright (C) 2021 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
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

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.util.Properties;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.RuntimeUtils;

public class TLSConfig {

    public static void autoConfigure() {
        autoConfigure(null, null);
    }

    public static void autoConfigure(Properties config, String configPrefix) {
        if (System.getProperty(INITIALIZED_PROP) == null) {
            configureTrustStore(config, configPrefix);
            System.setProperty(INITIALIZED_PROP, "true");
        }
    }

    private static void configureTrustStore(Properties config,
            String configPrefix) {
        // if a system property was already explicitly set, make no changes
        if (System.getProperty(TRUST_STORE_FILE) != null) {
            RuntimeUtils.addPropagatedSystemProperty(TRUST_STORE_FILE, null);
            RuntimeUtils.addPropagatedSystemProperty(TRUST_STORE_TYPE, null);
            return;
        }

        // check the user preference for TLS truststore. If "off", abort
        String pref = getSetting(config, configPrefix, "truststore", "auto");
        if ("off".equalsIgnoreCase(pref))
            return;

        // otherwise, look for a manually-installed trust store
        File appDir = DirectoryPreferences.getApplicationDirectory(true);
        File tlsDir = new File(appDir, "tls");
        File truststore = new File(tlsDir, "truststore.jks");
        if (truststore.isFile() && truststore.canRead()) {
            System.setProperty(TRUST_STORE_TYPE, "JKS");
            System.setProperty(TRUST_STORE_FILE, truststore.getPath());
            return;
        }

        // on Windows computers, possibly use the operating system truststore
        String os = System.getProperty("os.name").toLowerCase();
        if ("auto".equalsIgnoreCase(pref) && os.contains("windows")) {
            System.setProperty(TRUST_STORE_TYPE, "Windows-ROOT");
            System.setProperty(TRUST_STORE_FILE, "NONE");
            return;
        }
    }

    private static String getSetting(Properties config, String configPrefix,
            String name, String defaultValue) {
        // see if a system property was set to control this property
        String sysPropName = SETTINGS_PREFIX + name;
        String result = System.getProperty(sysPropName);

        // if no system property was found, check the provided configuration
        if (result == null && config != null)
            result = config.getProperty(configPrefix + name);

        // if we found a set value, propagate it to child processes
        if (result != null)
            RuntimeUtils.addPropagatedSystemProperty(sysPropName, result);
        else
            // if no value found yet, check user preferences
            result = PREFERENCES.get(name, defaultValue);

        return result;
    }

    private static final String SETTINGS_PREFIX = "net.sourceforge.processdash.tls.";

    private static final String INITIALIZED_PROP = SETTINGS_PREFIX
            + "initialized_";

    private static final String TRUST_STORE_FILE = "javax.net.ssl.trustStore";

    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    private static final Preferences PREFERENCES = Preferences.userRoot()
            .node("net/sourceforge/processdash/userPrefs/tls");

}
