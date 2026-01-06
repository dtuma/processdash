// Copyright (C) 2026 Tuma Solutions, LLC
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

package teamdash.sync.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import teamdash.sync.ExtNodeSet;
import teamdash.sync.ExtSyncDaemon;
import teamdash.sync.ExtSyncUtil;
import teamdash.sync.ExtSystemConnection;
import teamdash.sync.SyncDataFile;

public class SyncDatabaseConnection implements ExtSystemConnection {

    protected Properties config;

    protected Logger log;

    protected String jdbcUrl;

    protected String username;

    protected String password;


    public SyncDatabaseConnection(Properties globalConfig) {
        this.config = ExtSyncUtil.getSystemProperties(globalConfig);
        String systemID = config.getProperty(ExtSyncDaemon.EXT_SYSTEM_ID);

        // write the server URL to the log
        this.log = Logger.getLogger(getClass().getName());
        this.jdbcUrl = config.getProperty("serverUrl");
        log.info("[" + systemID + "] - Using database at " + jdbcUrl);

        // remove sensitive auth information from the configuration
        this.username = (String) config.remove("username");
        this.password = (String) config.remove("password");
    }

    public ExtNodeSet getNodeSet(Element configXml, SyncDataFile syncData) {
        return new SyncDatabaseNodeSet(this,
                Collections.unmodifiableMap(config), configXml, syncData);
    }

    public Connection getConnection() throws SQLException {
        // simple implementation opens a new connection for each run
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public void releaseConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            log.log(Level.WARNING, "Unable to close connection", e);
        }
    }

    public void disconnect() {
        // no-op
    }

}
