// Copyright (C) 2007-2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundSqlConnection extends AbstractBoundConnection<Connection> {

    public static final String DEFAULT_ID = "sqlConnection";


    protected DynamicAttributeValue jdbcURL;

    protected DynamicAttributeValue username;

    protected DynamicAttributeValue password;

    protected DynamicAttributeValue schema;


    public BoundSqlConnection(BoundMap map, Element xml) {
        super(map, xml, DEFAULT_ID);

        this.jdbcURL = getDynamicValue(xml, "url", CANNOT_CONNECT);
        this.username = getDynamicValue(xml, "username", NO_USERNAME);
        this.password = getDynamicValue(xml, "password", NO_PASSWORD);
        this.schema = getDynamicValue(xml, "schema", null);
    }


    @Override
    protected void disposeConnectionImpl(Connection connection)
            throws Exception {
        connection.close();
    }


    @Override
    protected Connection openConnectionImpl() throws ErrorDataValueException {
        ErrorDataValueException passwordException = null;
        try {
            // look up the information needed to make the connection
            String jdbcURL = this.jdbcURL.getValue();
            String username = getUsername(this.username.getValue());
            String password;
            try {
                password = getPassword(this.password.getValue());
            } catch (ErrorDataValueException e) {
                // if no password was provided, try to proceed without one.
                password = "";
                passwordException = e;
            }

            // make the connection
            Connection result = DriverManager.getConnection(jdbcURL,
                username, password);

            // optionally set the schema
            String schema = this.schema.getValue();
            if (StringUtils.hasValue(schema))
                setSchema(result, schema);

            return result;

        } catch (SQLException e) {
            // if the login failed because no password was set, rethrow the
            // "missing password" error.
            if (passwordException != null)
                throw passwordException;
            // we were unable to open a connection; return null.
            e.printStackTrace();
            return null;
        }
    }


    private void setSchema(Connection conn, String schemaName) {
        if (!VALID_SCHEMA_NAME.matcher(schemaName).matches()) {
            System.out.println("Ignoring invalid schema name '" + schemaName
                    + "'");
            return;
        }
        try {
            conn.createStatement().execute("set schema " + schemaName);
        } catch (SQLException e) {
            System.out.println("Failed to set schema '" + schemaName
                    + "' - attempting to continue anyway");
        }
    }
    private static final Pattern VALID_SCHEMA_NAME = Pattern.compile(
        "[a-z][a-z0-9_.]*", Pattern.CASE_INSENSITIVE);

}
