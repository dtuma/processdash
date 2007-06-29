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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.sourceforge.processdash.util.Base64;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundSqlConnection implements BoundMap.Disposable, ErrorTokens {

    private static final String NO_PASSWORD_TOKEN = "[none]";

    public static final String DEFAULT_ID = "sqlConnection";

    public interface ConnectionSource {
        public Connection getConnection();
    }


    protected String jdbcURL;

    protected String usernameProperty;

    protected String username;

    protected String passwordProperty;

    protected String password;

    protected BoundMap map;

    protected String destPropName;

    protected String error;

    protected int errorSeverity;

    protected String unavailableMessage;

    protected Connection connection;

    public BoundSqlConnection(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"), //
                xml.getAttribute("url"), //
                xml.getAttribute("username"), //
                xml.getAttribute("usernameId"), //
                decodePassword(xml.getAttribute("password")), //
                xml.getAttribute("passwordId"), //
                xml.getAttribute("unavailableMessage"));


    }

    public BoundSqlConnection(BoundMap map, String destProp,
            String jdbcURL, String username, String usernameProp,
            String password, String passwordProp, String unavailableMsg) {
        this.map = map;

        if (StringUtils.hasValue(destProp))
            this.destPropName = destProp;
        else
            this.destPropName = DEFAULT_ID;

        this.jdbcURL = jdbcURL;

        if (StringUtils.hasValue(usernameProp))
            this.usernameProperty = usernameProp;
        else
            this.username = username;

        if (StringUtils.hasValue(passwordProp))
            this.passwordProperty = passwordProp;
        else
            this.password = password;

        if (StringUtils.hasValue(unavailableMsg))
            this.unavailableMessage = unavailableMsg;
        else
            this.unavailableMessage = CANNOT_CONNECT;

        map.addPropertyChangeListener( //
                new String[] { usernameProp, passwordProp }, this, "recalc");

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


    protected void openConnection() {
        // lookup the username
        String username;
        if (usernameProperty == null)
            username = this.username;
        else {
            username = null;
            Object usernameVal = map.get(usernameProperty);
            if (usernameVal instanceof String)
                username = (String) usernameVal;
            if (!StringUtils.hasValue(username)) {
                setMissingAttrError(usernameProperty, NO_USERNAME);
                return;
            }
        }

        // lookup the password.
        String password;
        if (passwordProperty == null)
            password = this.password;
        else {
            password = null;
            Object passwordVal = map.get(passwordProperty);
            if (passwordVal instanceof String)
                password = (String) passwordVal;
            if (!StringUtils.hasValue(password)) {
                setMissingAttrError(passwordProperty, NO_PASSWORD);
                return;
            }
        }
        if (NO_PASSWORD_TOKEN.equals(password))
            password = "";

        // make the connection
        try {
            connection = DriverManager.getConnection(jdbcURL, username,
                    password);
            setError(null, ErrorData.NO_ERROR);
            return;
        } catch (SQLException e) {
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

    protected void setMissingAttrError(String attrName, String errorToken) {
        ErrorData errorData = map.getErrorDataForAttr(attrName);
        if (errorData != null)
            setError(errorData.getError(), errorData.getSeverity());
        else
            setError(errorToken, ErrorTokens.MISSING_DATA_SEVERITY);
    }

    protected void setError(String errorToken, int severity) {
        this.error = errorToken;
        this.errorSeverity = severity;
    }

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

    private static String decodePassword(String passwordHash) {
        if (!StringUtils.hasValue(passwordHash))
            return "";
        if (NO_PASSWORD_TOKEN.equals(passwordHash))
            return NO_PASSWORD_TOKEN;

        byte[] bytes = Base64.decode(passwordHash);
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) ((bytes[i] ^ XOR_BITS) & 0xff);
        try {
            return new String(bytes, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
    }

    private static String encodePassword(String password) {
        byte[] bytes;
        try {
            bytes = password.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }

        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) ((bytes[i] ^ XOR_BITS) & 0xff);
        return Base64.encodeBytes(bytes);
    }

    private static final int XOR_BITS = 55;

    public static void main(String[] args) {
        System.out.println(encodePassword(args[0]));
    }

}
