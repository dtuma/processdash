// Copyright (C) 2009-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.binding;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import net.sourceforge.processdash.util.StringUtils;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.w3c.dom.Element;

public class BoundXmlRpcConnection extends AbstractBoundConnection<XmlRpcClient> {

    public static final String DEFAULT_ID = "xmlRpcConnection";


    protected DynamicAttributeValue baseUrl;

    protected DynamicAttributeValue urlSuffix;

    protected DynamicAttributeValue username;

    protected DynamicAttributeValue password;

    protected String testMethodName;


    public BoundXmlRpcConnection(BoundMap map, Element xml) {
        this(map, xml, DEFAULT_ID);
    }


    public BoundXmlRpcConnection(BoundMap map, Element xml, String defaultId) {
        super(map, xml, defaultId);

        this.baseUrl = getDynamicValue(xml, "url", NO_URL);
        this.urlSuffix = getDynamicValue(xml, "urlSuffix", null);
        this.username = getDynamicValue(xml, "username", NO_USERNAME);
        this.password = getDynamicValue(xml, "password", NO_PASSWORD);
        this.testMethodName = getXmlAttr(xml, "testMethodName", null);
    }


    @Override
    protected void disposeConnectionImpl(XmlRpcClient connection) {}


    @Override
    protected XmlRpcClient openConnectionImpl() throws ErrorDataValueException {
        return openConnectionImpl(true);
    }

    protected XmlRpcClient openConnectionImpl(boolean printException)
            throws ErrorDataValueException {
        String username = null;
        String password = null;
        try {
            // look up the information needed to make the connection
            String baseUrl = this.baseUrl.getValue();
            String urlSuffix = this.urlSuffix.getValue();
            username = getUsername(this.username.getValue());
            password = getPassword(this.password.getValue());

            URL url = new URL(baseUrl.trim());
            if (StringUtils.hasValue(urlSuffix))
                url = new URL(url, urlSuffix);

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(url);
            config.setEnabledForExtensions(true);
            if (username != null && password != null) {
                config.setBasicUserName(username);
                config.setBasicPassword(password);
            }

            XmlRpcClient connection = new XmlRpcClient();
            connection.setConfig(config);

            if (StringUtils.hasValue(testMethodName))
                connection.execute(testMethodName, Collections.EMPTY_LIST);

            if (!validateCredentials(connection, username, password))
                throw new ErrorDataValueException(BAD_USERNAME_PASS,
                        ErrorData.SEVERE);

            setError(null, ErrorData.NO_ERROR);
            return connection;

        } catch (MalformedURLException mue) {
            throw new ErrorDataValueException(INVALID_URL, ErrorData.SEVERE);

        } catch (XmlRpcNotAuthorizedException nae) {
            throw getBadCredentialsException(username, password);

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            // we were unable to open a connection; return null.
            if (printException)
                e.printStackTrace();
            return null;
        }
    }

    /** Doublecheck the credentials provided, to ensure that they are valid.
     * 
     * @param client the XML RPC connection
     * @param username the username
     * @param password the password
     * @return true if the credentials are valid
     * @throws XmlRpcException if an error is encountered
     */
    protected boolean validateCredentials(XmlRpcClient client, String username,
            String password) throws XmlRpcException {
        return true;
    }

}
