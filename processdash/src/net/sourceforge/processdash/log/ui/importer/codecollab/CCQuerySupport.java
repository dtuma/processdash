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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

public class CCQuerySupport {

    private static final Logger logger = Logger.getLogger(CCQuerySupport.class
            .getName());

    /**
     * Execute an RPC call and return the result.
     * 
     * @param client
     *                a preconfigured {@link XmlRpcClient} object to use
     * @param name
     *                the name of the method, without the "ccollab*" namespace
     * @param params
     *                the parameters to pass
     * @return the result returned from the RPC
     * @throws XmlRpcException
     *                 if an error is encountered
     */
    public static Object execute(XmlRpcClient client, String name,
            Object... params) throws XmlRpcException {
        try {
            String namespace = CCWebService.getNamespace(client);
            return client.execute(namespace + name, params);
        } catch (XmlRpcException x) {
            String paramStr = (params == null ? "()"
                    : Arrays.asList(params).toString());
            logger.log(Level.WARNING,
                "Encountered problem when executing XML RPC Query " + name
                        + paramStr, x);
            throw x;
        }
    }

    /**
     * Retrieve a list of objects meeting a certain criteria
     * 
     * @param client
     *                a preconfigured {@link XmlRpcClient} object to use
     * @param clazz
     *                the type of object to retrieve
     * @param max
     *                the maximum number of results to return
     * @param criteria
     *                the filter criteria for the object
     * @return a list of objects returned by the query
     * @throws XmlRpcException
     *                 if an error is encountered
     */
    public static Object[] querySimple(XmlRpcClient client, String clazz,
            int max, Object... criteria) throws XmlRpcException {
        Map criteriaMap = new HashMap();
        for (int i = 0; i < criteria.length; i += 2) {
            criteriaMap.put(criteria[i], criteria[i + 1]);
        }
        return (Object[]) execute(client, "queryObjectsSimple", clazz,
            criteriaMap, max);
    }

    /**
     * Look up a unique object on the server, and return an attribute of that
     * object.
     * 
     * @param client
     *                a preconfigured {@link XmlRpcClient} object to use
     * @param clazz
     *                the type of object to retrieve
     * @param attr
     *                the name of the attribute to return
     * @param criteria
     *                a set of criteria that will select a unique object on the
     *                server.
     * @return the value of the given attribute for the unique object found
     * @throws XmlRpcException
     *                 if an error occurs during the RPC call
     * @throws SingleValueNotFoundException
     *                 if the criteria does not return exactly one object or if
     *                 the object found does not have a value for the given
     *                 attribute.
     */
    public static Object lookupSingleValue(XmlRpcClient client, String clazz,
            String attr, Object... criteria) throws XmlRpcException,
            SingleValueNotFoundException {
        Object[] list = querySimple(client, clazz, 2, criteria);
        if (list == null || list.length != 1)
            throw new SingleValueNotFoundException("Expected a single element");
        Map element = (Map) list[0];
        Object result = element.get(attr);
        if (result != null)
            return result;
        else
            throw new SingleValueNotFoundException("Expected " + clazz
                    + " item to have an " + attr);
    }

    public static class SingleValueNotFoundException extends Exception {

        private SingleValueNotFoundException(String message) {
            super(message);
        }

    }


    private static final String NAMESPACE = "com.smartbear.ccollab.datamodel.";

    public static final String USER_CLASS = NAMESPACE + "UserData";

    public static final String REVIEW_CLASS = NAMESPACE + "ReviewData";

    public static final String DEFECT_CLASS = NAMESPACE + "DefectData";

    public static final String METADATA_DESCRIPTION_CLASS = NAMESPACE
            + "MetaDataDescriptionData";

    public static final String STRING_METADATA_CLASS = NAMESPACE
            + "MetaDataValueStringData";

    public static final String INTEGER_METADATA_CLASS = NAMESPACE
            + "MetaDataValueIntegerData";

    public static final String SELECT_METADATA_CLASS = NAMESPACE
            + "MetaDataSelectItemData";

}
