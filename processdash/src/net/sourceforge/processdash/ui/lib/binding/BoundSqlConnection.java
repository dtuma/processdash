// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundSqlConnection implements BoundMap.Disposable, ErrorTokens {

    private static final String NO_PASSWORD_TOKEN = "[none]";

    public static final String DEFAULT_ID = "sqlConnection";

    public interface ConnectionSource {
        public Connection getConnection();
    }

    protected AttributeValue jdbcURL;

    protected AttributeValue username;

    protected AttributeValue password;

    protected AttributeValue schema;

    protected BoundMap map;

    protected String destPropName;

    protected String error;

    protected int errorSeverity;

    protected String unavailableMessage;

    protected Connection connection;

    public BoundSqlConnection(BoundMap map, Element xml) {
        this.map = map;
        this.destPropName = getXmlAttr(xml, "id", DEFAULT_ID);

        this.jdbcURL = new AttributeValue(xml, "url", CANNOT_CONNECT);
        this.username = new AttributeValue(xml, "username", NO_USERNAME);
        this.password = new AttributeValue(xml, "password", NO_PASSWORD);
        this.schema = new AttributeValue(xml, "schema", null);

        this.unavailableMessage = getXmlAttr(xml, "unavailableMessage",
            CANNOT_CONNECT);

        recalc();
    }



    public void disposeBoundItem() {
        disposeConnection();
    }


    public void recalc() {
        disposeConnection();

        setError(null, ErrorData.NO_ERROR);
        map.put(destPropName, new PublicValue());
    }


    private void disposeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {}
            connection = null;
        }
    }

    protected void setError(String errorToken, int severity) {
        this.error = errorToken;
        this.errorSeverity = severity;
    }



    protected void openConnection() {
        String password = null;
        try {
            // look up the information needed to make the connection
            String jdbcURL = this.jdbcURL.getValue();
            String username = this.username.getValue();
            password = getPassword();

            // make the connection
            connection = DriverManager.getConnection(jdbcURL, username,
                password);

            // optionally set the schema
            String schema = this.schema.getValue();
            if (StringUtils.hasValue(schema))
                setSchema(connection, schema);

            setError(null, ErrorData.NO_ERROR);
            return;
        } catch (AttributeValueMissingException e) {
            // The error will have already been set at this point. Nothing
            // else needs to be done.
            return;
        } catch (SQLException e) {
            if ("".equals(password))
                // if the login failed because no password was set, the error
                // fields will already contain the "missing password" info.
                return;
            e.printStackTrace();
        }

        // see if a network connection appears to be available.
        try {
            if (InetAddress.getLocalHost().isLoopbackAddress()) {
                setError(NO_NETWORK, ErrorData.SEVERE);
                return;
            }
        } catch (IOException ioe) {}

        // connection failed.
        setError(unavailableMessage, ErrorData.SEVERE);
    }

    private String getPassword() {
        try {
            String password = this.password.getValue();
            if (NO_PASSWORD_TOKEN.equals(password))
                return "";
            else
                return map.unhashValue(password);
        } catch (AttributeValueMissingException e) {
            // let's see if the login succeeds with an empty password
            return "";
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



    protected class AttributeValue {
        private String explicitValue;
        private String propName;
        private String errorToken;

        public AttributeValue(Element xml, String name, String errorToken) {
            if (xml.hasAttribute(name))
                this.explicitValue = xml.getAttribute(name);

            else {
                this.propName = getXmlAttr(xml, name + "Id",
                    destPropName + "." + name);
                map.addPropertyChangeListener(this.propName,
                    BoundSqlConnection.this, "recalc");
            }

            this.errorToken = errorToken;
        }

        public String getValue() throws AttributeValueMissingException {
            if (explicitValue != null)
                return explicitValue;

            Object propVal = map.get(propName);
            if (propVal instanceof String) {
                String result = (String) propVal;
                if (StringUtils.hasValue(result))
                    // the map contained a valid string property with a
                    // non-empty value. return it.
                    return result;
            }

            if (errorToken == null)
                // no data was found, but for this attribute, that isn't
                // an error. Just return null.
                return null;

            // no data was found for this attribute.
            ErrorData errorData = map.getErrorDataForAttr(propName);
            if (errorData != null)
                // if the attribute itself had associated error data,
                // propagate it along.
                setError(errorData.getError(), errorData.getSeverity());
            else
                // otherwise, use our default error token.
                setError(errorToken, ErrorTokens.MISSING_DATA_SEVERITY);
            // throw an exception to indicate missing data
            throw new AttributeValueMissingException();
        }
    }

    private class AttributeValueMissingException extends Exception {}



    private class PublicValue implements ConnectionSource, ErrorData {

        public Connection getConnection() {
            if (connection == null)
                openConnection();

            return connection;
        }

        public String getError() {
            return error;
        }

        public int getSeverity() {
            return errorSeverity;
        }

    }



    private static String getXmlAttr(Element xml, String attr, String defVal) {
        String result = xml.getAttribute(attr);
        if (StringUtils.hasValue(result))
            return result;
        else
            return defVal;
    }

}
