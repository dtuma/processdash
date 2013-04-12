// Copyright (C) 2007-2013 Tuma Solutions, LLC
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

import java.io.IOException;
import java.net.InetAddress;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public abstract class AbstractBoundConnection<T> implements
        BoundMap.Disposable, ErrorTokens {

    private static final String NO_CREDENTIAL_TOKEN = "[none]";


    protected BoundMap map;

    protected String destPropName;

    private String error;

    private int errorSeverity;

    protected String unavailableMessage;

    private T connection;


    public AbstractBoundConnection(BoundMap map, Element xml, String defaultId) {
        this.map = map;
        this.destPropName = getXmlAttr(xml, "id", defaultId);
        this.error = null;
        this.errorSeverity = ErrorData.NO_ERROR;
        this.unavailableMessage = map.getAttrOrResource(xml, destPropName,
            "Unavailable_Message", CANNOT_CONNECT);

        this.connection = null;
        map.put(this.destPropName, new PublicValue());
    }


    /**
     * Do the work of actually opening a connection.
     * 
     * @return a successfully opened connection object, or null to indicate a
     *      generic failure.
     * @throws ErrorDataValueException to indicate a specific error that
     *      prevented the connection from being made.
     */
    protected abstract T openConnectionImpl() throws ErrorDataValueException;


    /**
     * Dispose of the connection in an appropriate manner.
     */
    protected abstract void disposeConnectionImpl(T connection)
            throws Exception;



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
                disposeConnectionImpl(connection);
            } catch (Exception e) {}
            connection = null;
        }
    }


    protected void setError(ErrorData errorData) {
        setError(errorData.getError(), errorData.getSeverity());
    }


    protected void setError(String errorToken, int severity) {
        this.error = errorToken;
        this.errorSeverity = severity;
    }


    protected void openConnection() {
        try {
            connection = openConnectionImpl();

            if (connection != null) {
                setError(null, ErrorData.NO_ERROR);
                return;
            }

        } catch (ErrorDataValueException e) {
            setError(e);
            return;
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


    protected DynamicAttributeValue getDynamicValue(Element xml,
            String attrName, String errorToken) {
        return new DynamicAttributeValue(map, xml, this, "recalc",
                destPropName, attrName, errorToken);
    }


    protected String getUsername(String username) {
        if (NO_CREDENTIAL_TOKEN.equals(username))
            return null;
        else
            return username;
    }


    protected String getPassword(String password) {
        if (NO_CREDENTIAL_TOKEN.equals(password))
            return "";
        else
            return map.unhashValue(password);
    }


    protected ErrorDataValueException getBadCredentialsException(
            String username, String password) {
        if (!StringUtils.hasValue(username))
            return new ErrorDataValueException(NO_USERNAME,
                    MISSING_DATA_SEVERITY);

        else if (!StringUtils.hasValue(password))
            return new ErrorDataValueException(NO_USERNAME,
                    MISSING_DATA_SEVERITY);

        else
            return new ErrorDataValueException(BAD_USERNAME_PASS,
                    ErrorData.SEVERE);
    }



    private class PublicValue implements ConnectionSource<T>, ErrorData {

        public T getConnection() {
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



    protected static String getXmlAttr(Element xml, String attr, String defVal) {
        String result = xml.getAttribute(attr);
        if (StringUtils.hasValue(result))
            return result;
        else
            return defVal;
    }

}
