// Copyright (C) 2017-2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;
import java.util.Properties;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;



/** @since 5.2.1 */
public abstract class ExtSyncDaemon {

    public static final String EXT_SYSTEM_NAME = "systemName";

    public static final String EXT_SYSTEM_ID = "systemID";


    protected Logger log;

    protected Properties globalConfig;

    protected String systemName, systemID;

    protected ExtSystemConnection connection;



    public ExtSyncDaemon(String globalConfigFilename) throws Exception {
        // create an object for logging
        this.log = Logger.getLogger(getClass().getName());
        String version = ExtSyncDaemon.class.getPackage().getImplementationVersion();
        String extVersion = getClass().getPackage().getImplementationVersion();
        log.info(getClass().getSimpleName() + " version [" + extVersion + " / "
                + version + "]");

        // load global config parameters based on command line arg
        this.globalConfig = loadGlobalConfig(globalConfigFilename);
        this.systemName = globalConfig.getProperty(EXT_SYSTEM_NAME);
        this.systemID = globalConfig.getProperty(EXT_SYSTEM_ID);

        // save and pass session cookies for improved performance
        CookieHandler
                .setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        // create an object for managing a connection to the external system
        this.connection = openConnection(globalConfig);

        // ensure we can connect to any PDES-based data
        configurePdesCredentials(globalConfig);
    }



    /**
     * Retrieve default configuration values for this daemon.
     * 
     * Subclasses must override, and provide (at a minimum) a mapping for
     * {@link #EXT_SYSTEM_NAME} and {@link #EXT_SYSTEM_ID}
     */
    protected abstract Properties getGlobalDefaults();


    /**
     * Open a connection to the external system specified by the global
     * configuration.
     */
    protected abstract ExtSystemConnection openConnection(
            Properties globalConfig) throws Exception;



    /**
     * Load global configuration parameters from a properties file
     */
    private Properties loadGlobalConfig(String propertiesFile)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties(getGlobalDefaults());
        InputStream in = new FileInputStream(propertiesFile);
        properties.load(in);
        in.close();
        return properties;
    }

    /**
     * Configure credentials for PDES connectivity
     */
    private void configurePdesCredentials(Properties properties) {
        String strategy = properties.getProperty("pdes.auth");
        if ("keyring".equalsIgnoreCase(strategy)) {
            // prompt the user for credentials, and save them in OS-provided
            // cryptographically secure storage
            System.setProperty(HttpAuthenticator.REMEMBER_ME_SETTING_NAME,
                "36500"); // 100 years
            HttpAuthenticator
                    .maybeInitialize("PD/" + systemName + " Synchronizer");
        } else {
            // read the username and password from the configuration file
            Authenticator.setDefault(new HttpAuth(properties));
        }
    }

    private static class HttpAuth extends Authenticator {

        private String username, password;

        public HttpAuth(Properties p) {
            username = p.getProperty("pdes.username");
            password = p.getProperty("pdes.password");
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }


    public void run() throws Exception {
        // create a data target for synchronization
        String location = globalConfig.getProperty("wbs.location");

        try {
            // run a loop to synchronize this WBS
            makeWBSDaemon(location).run();
        } finally {
            // dispose of resources when finished
            connection.disconnect();
        }
    }

    protected ExtSyncDaemonWBS makeWBSDaemon(String wbsLocation) {
        return new ExtSyncDaemonWBS(this, wbsLocation);
    }

}
