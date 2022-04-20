// Copyright (C) 2021-2022 Tuma Solutions, LLC
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.MockMap;
import net.sourceforge.processdash.util.RuntimeUtils;

import com.tuma_solutions.teamserver.jnlp.client.JarVerifier;

public class TLSConfig {

    private static File configFile;

    private static final Logger logger = Logger
            .getLogger(TLSConfig.class.getName());

    public static void autoConfigure() {
        autoConfigure(getInstallationConfig(), "");
    }

    private static Properties getInstallationConfig() {
        try {
            configFile = getConfigFile("config.ini");
            if (configFile == null)
                return null;

            Properties result = new Properties();
            InputStream in = new FileInputStream(configFile);
            result.load(in);
            in.close();
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void autoConfigure(Properties config, String configPrefix) {
        if (System.getProperty(INITIALIZED_PROP) == null) {
            configureSocketTimeouts(config, configPrefix);
            configureSystemProperties(config, configPrefix);
            configureTrustStore(config, configPrefix);
            runNetworkInitializer(config, configPrefix);
            logConfigState();
            System.setProperty(INITIALIZED_PROP, "true");
        }
    }

    private static void configureSocketTimeouts(Properties config,
            String configPrefix) {
        configureSocketTimeout(config, configPrefix, CONNECT_TIMEOUT_PROP,
            "connectTimeout", 10);
        configureSocketTimeout(config, configPrefix, READ_TIMEOUT_PROP,
            "readTimeout", 30);
    }

    private static boolean configureSocketTimeout(Properties config,
            String configPrefix, String sysProp, String configKey,
            int defaultSeconds) {
        // propagate this system property into forked child processes
        RuntimeUtils.addPropagatedSystemProperty(sysProp, null);

        // if the property is already present, do nothing
        if (System.getProperty(sysProp) != null)
            return false;

        // read the timeout preference from config, or use default seconds
        float seconds;
        try {
            String pref = getSetting(config, configPrefix, configKey, null);
            seconds = Float.parseFloat(pref);
        } catch (Exception e) {
            seconds = defaultSeconds;
        }

        // store the number of milliseconds in the target system property
        int milliseconds = (int) (seconds * 1000);
        String newValue = Integer.toString(milliseconds);
        System.setProperty(sysProp, newValue);
        return true;
    }

    private static void configureSystemProperties(Properties config,
            String configPrefix) {
        configureSystemProp(config, configPrefix, KEEP_ALIVE_PROP, //
            "keepAlive", null);
        configureSystemProp(config, configPrefix, POST_PREFLIGHT_PROP, //
            "postPreflight", null);
    }

    private static boolean configureSystemProp(Properties config,
            String configPrefix, String sysProp, String configKey,
            String defaultValue) {
        // if the system property is not set, check for a configured value
        if (System.getProperty(sysProp) == null) {
            String configVal = getSetting(config, configPrefix, configKey,
                defaultValue);
            if (configVal == null)
                // if no configured value was given, abort
                return false;

            // set the system property from the configured value
            System.setProperty(sysProp, configVal);
        }

        // propagate this system property into forked child processes
        RuntimeUtils.addPropagatedSystemProperty(sysProp, null);
        return true;
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
        File truststore = getConfigFile("truststore.jks");
        if (truststore != null && truststore.canRead()) {
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

    private static void runNetworkInitializer(final Properties config,
            final String configPrefix) {
        try {
            File networkInitJar = getConfigFile("network-init.jar");
            if (networkInitJar == null || !JarVerifier.verify(networkInitJar))
                return;

            @SuppressWarnings("resource")
            URLClassLoader cl = new URLClassLoader(
                    new URL[] { networkInitJar.toURI().toURL() },
                    TLSConfig.class.getClassLoader().getParent());
            Class clazz = cl.loadClass(NETWORK_INITIALIZER_CLASSNAME);
            Constructor<Runnable> cstr = clazz.getConstructor(Map.class);
            Runnable init = cstr.newInstance(new MockMap() {
                public Object get(Object key) {
                    return getSetting(config, configPrefix, (String) key, null);
                }});
            init.run();
        } catch (Throwable t) {
            System.err.println("Couldn't run network initializer:");
            t.printStackTrace();
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
            result = PREFERENCES.get("tls." + name, defaultValue);

        return result;
    }

    private static File getConfigFile(String name) {
        // check for a file in the same directory as this application
        if (SELF_JAR != null && SELF_JAR.isFile()) {
            File appDir = SELF_JAR.getParentFile();
            File localResult = new File(appDir, name);
            if (localResult.isFile())
                return localResult;
        }

        // check for a file in the global TLS configuration directory
        File appDir = DirectoryPreferences.getApplicationDirectory(true);
        File tlsDir = new File(appDir, "tls");
        File globalResult = new File(tlsDir, name);
        if (globalResult.isFile())
            return globalResult;

        // no file with the given name was found
        return null;
    }

    public static void logConfigState() {
        // log a message indicating which configuration file we used
        if (configFile != null)
            logger.config("Using TLS config: " + configFile);

        // log a message if a proxy server was installed
        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector != null)
            logger.info("Using proxy selector: " + proxySelector);
    }

    private static final String SETTINGS_PREFIX = "net.sourceforge.processdash.tls.";

    private static final String INITIALIZED_PROP = SETTINGS_PREFIX
            + "initialized_";

    private static final String CONNECT_TIMEOUT_PROP = "sun.net.client.defaultConnectTimeout";

    private static final String READ_TIMEOUT_PROP = "sun.net.client.defaultReadTimeout";

    private static final String KEEP_ALIVE_PROP = "http.keepAlive";

    public static final String POST_PREFLIGHT_PROP = SETTINGS_PREFIX
            + "postPreflight";

    private static final String TRUST_STORE_FILE = "javax.net.ssl.trustStore";

    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    private static final String NETWORK_INITIALIZER_CLASSNAME = //
            TLSConfig.class.getPackage().getName() + ".NetworkInitializer";

    private static final Preferences PREFERENCES = Preferences.userRoot()
            .node("net/sourceforge/processdash/userPrefs");

    private static final File SELF_JAR = RuntimeUtils
            .getClasspathFile(TLSConfig.class);

}
