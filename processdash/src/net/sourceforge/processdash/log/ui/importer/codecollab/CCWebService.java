// Copyright (C) 2009-2011 Tuma Solutions, LLC
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

import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.BoundXmlRpcConnection;
import net.sourceforge.processdash.ui.lib.binding.DynamicAttributeValue;
import net.sourceforge.processdash.ui.lib.binding.ErrorDataValueException;
import net.sourceforge.processdash.util.StringUtils;

public class CCWebService extends BoundXmlRpcConnection {

    public static final String CC_DEFAULT_ID = "codeCollaborator";

    private static final String URL_SUFFIX_3 = "/xmlrpc/server";
    private static final String NAMESPACE3 = "ccollab3.";
    private static final String URL_SUFFIX_4 = "/xmlrpc/secure";
    private static final String NAMESPACE4 = "ccollab4.";
    private static final String TEST_METHOD_NAME = "getServerVersion";

    private String namespace;

    public CCWebService(BoundMap map, Element xml) {
        super(map, xml, CC_DEFAULT_ID);
    }

    @Override
    protected XmlRpcClient openConnectionImpl() throws ErrorDataValueException {
        // assume the most recent version of the server, and try to connect.
        this.urlSuffix = new DynamicAttributeValue(URL_SUFFIX_4);
        this.namespace = NAMESPACE4;
        this.testMethodName = NAMESPACE4 + TEST_METHOD_NAME;
        XmlRpcClient result = super.openConnectionImpl(true);

        // if that fails, try connecting to the older server API.
        if (result == null) {
            this.urlSuffix = new DynamicAttributeValue(URL_SUFFIX_3);
            this.namespace = NAMESPACE3;
            this.testMethodName = NAMESPACE3 + TEST_METHOD_NAME;
            result = super.openConnectionImpl(true);
        }

        return result;
    }

    @Override
    protected boolean validateCredentials(XmlRpcClient client, String username,
            String password) throws XmlRpcException {
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
