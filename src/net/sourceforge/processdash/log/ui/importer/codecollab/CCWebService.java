// Copyright (C) 2009-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.codecollab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.AbstractBoundConnection;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;
import net.sourceforge.processdash.util.StringUtils;

public class CCWebService extends AbstractBoundConnection<CCClient> {

    public static final String CC_DEFAULT_ID = "codeCollaborator";

    private static final String URL_SUFFIX_3 = "/xmlrpc/server";
    private static final String NAMESPACE3 = "ccollab3.";
    private static final String URL_SUFFIX_4 = "/xmlrpc/secure";
    private static final String NAMESPACE4 = "ccollab4.";
    private static final String TEST_METHOD_NAME = "getServerVersion";


    private DynamicAttributeValue baseUrl;

    private DynamicAttributeValue username;

    private DynamicAttributeValue password;


    public CCWebService(BoundMap map, Element xml) {
        super(map, xml, CC_DEFAULT_ID);

        this.baseUrl = getDynamicValue(xml, "url", NO_URL);
        this.username = getDynamicValue(xml, "username", NO_USERNAME);
        this.password = getDynamicValue(xml, "password", NO_PASSWORD);
    }


    @Override
    protected void disposeConnectionImpl(CCClient connection) {}


    @Override
    protected CCClient openConnectionImpl() throws ErrorDataValueException {
        try {
            // by default, try connecting using the JSON API (supported by
            // Collaborator version 9 and higher)
            return openJsonClient();

        } catch (JsonApiUnsupported jau) {

            // if we are communicating with an older version of the server,
            // try connecting with the legacy XML RPC API.
            return openXmlRpcClient();
        }
    }


    private CCClient.Json openJsonClient()
            throws ErrorDataValueException, JsonApiUnsupported {
        try {
            // create a client for this URL
            String baseUrl = this.baseUrl.getValue();
            CCJsonClient result = new CCJsonClient(baseUrl);

            // abort if the JSON API is not available
            if (result.isServerWithJsonSupport() == false)
                throw new JsonApiUnsupported();

            // retrieve user credentials
            String username = getUsername(this.username.getValue());
            String password = getPassword(this.password.getValue());

            // authenticate
            if (result.authenticate(username, password) == false)
                throw getBadCredentialsException(username, password);

            return new CCClient.Json(result);

        } catch (MalformedURLException mue) {
            throw new ErrorDataValueException(INVALID_URL, ErrorData.SEVERE);

        } catch (JsonApiUnsupported jau) {
            throw jau;

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class JsonApiUnsupported extends Exception {}


    private CCClient.XmlRpc openXmlRpcClient() throws ErrorDataValueException {
        // assume the most recent version of the server, and try to connect.
        CCClient.XmlRpc result = openXmlRpcClientImpl(URL_SUFFIX_4, NAMESPACE4,
            NAMESPACE4 + TEST_METHOD_NAME);

        // if that fails, try connecting to the older server API.
        if (result == null) {
            result = openXmlRpcClientImpl(URL_SUFFIX_3, NAMESPACE3,
                NAMESPACE3 + TEST_METHOD_NAME);
        }

        return result;
    }

    private CCClient.XmlRpc openXmlRpcClientImpl(String urlSuffix,
            String namespace, String testMethodName)
            throws ErrorDataValueException {
        String username = null;
        String password = null;
        try {
            // look up the information needed to make the connection
            String baseUrl = this.baseUrl.getValue();
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

            if (!validateCredentials(connection, username, password, namespace))
                throw new ErrorDataValueException(BAD_USERNAME_PASS,
                        ErrorData.SEVERE);

            setError(null, ErrorData.NO_ERROR);
            return new CCClient.XmlRpc(connection);

        } catch (MalformedURLException mue) {
            throw new ErrorDataValueException(INVALID_URL, ErrorData.SEVERE);

        } catch (XmlRpcNotAuthorizedException nae) {
            throw getBadCredentialsException(username, password);

        } catch (ErrorDataValueException edve) {
            throw edve;

        } catch (Exception e) {
            // we were unable to open a connection; return null.
            e.printStackTrace();
            return null;
        }
    }

    /** Doublecheck the credentials provided, to ensure that they are valid.
     * 
     * @return true if the credentials are valid
     * @throws XmlRpcException if an error is encountered
     */
    private boolean validateCredentials(XmlRpcClient client, String username,
            String password, String namespace) throws XmlRpcException {
        if (!StringUtils.hasValue(username) || !StringUtils.hasValue(password))
            return false;

        // no extra validation is needed for Code Collaborator API v4
        if (namespace == NAMESPACE4)
            return true;

        Hashtable<String, String> activity = new Hashtable<String, String>();
        activity.put( "password-type", "plaintext" );
        activity.put( "password-value", password );
        Map result = (Map) client.execute("ccollab.sessionAffirm",
                new Object[] { username, password, activity });
        return !result.containsKey("error");
    }

    /**
     * Determine the XML namespace that should be used for a particular
     * Code Collaborator server.
     */
    protected static String getNamespace(XmlRpcClient client) {
        XmlRpcClientConfig config = client.getClientConfig();
        if (config instanceof XmlRpcHttpClientConfig) {
            XmlRpcHttpClientConfig hcc = (XmlRpcHttpClientConfig) config;
            URL url = hcc.getServerURL();
            if (url.getFile().equals(URL_SUFFIX_3))
                return NAMESPACE3;
            else
                return NAMESPACE4;
        }
        return NAMESPACE4;
    }

}
