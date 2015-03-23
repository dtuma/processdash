// Copyright (C) 2006-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** When a snippet instance needs to manage a list of data, this class helps
 * with the associated HTML rendering.
 * 
 * This class can either be invoked programatically (via the public static
 * methods it exposes) or as a TinyCGI script (potentially included from
 * a server-parsed HTML file).
 * 
 * In addition, it can either write an entire enumeration of HTML fragments
 * for all the items described by a set of parameters (such as those persisted
 * via the snippet framework), or it can write a single fragment for a single
 * item.  (This second capacity is what sets it apart from a #foreach directive;
 * by placing the fragment in a separate file and calling it through this
 * script, new fragments can be added via AJAX calls.)
 * 
 * Either way, the uri and item type must be provided.  The uri describes a
 * fragment that should be invoked repeatedly to build up content.  That
 * fragment should use the token "$$$_###_" when constructing parameter names
 * for item attributes;  the "$$$_" token will be replaced with the identifier
 * for the snippet, while the "###_" token will be replaced with the identifier
 * for the item within the snippet.
 * 
 */
public class SnippetDataEnumerator extends TinyCGIBase {

    public static final String INSERT_TOKEN = "<!-- ### -->";

    protected void writeContents() throws IOException {
        String uri = getParameter("uri");
        if (uri == null || uri.length() == 0)
            throw new IOException("uri parameter must be specified");

        String itemType = getParameter("type");
        if (itemType == null || itemType.length() == 0)
            throw new IOException("type parameter must be specified");

        String namespace = getParameter("ns");
        if (namespace == null || namespace.length() == 0)
            namespace = "$$$_";

        String itemID = getParameter("item");
        if (itemID == null || itemID.length() == 0)
            writeListOfItems(out, getTinyWebServer(), uri, namespace, itemType,
                    parameters);
        else
            writeOneItem(out, getTinyWebServer(), uri, namespace, itemType,
                    parameters, (String) env.get("QUERY_STRING"), itemID);
    }

    public static void writeListOfItems(Writer out, WebServer webServer,
            String uri, String namespace, String itemType, Map parameters)
            throws IOException {
        String[] idList = (String[]) parameters.get(itemType + "Enum_ALL");
        if (idList != null) {
            for (int i = 0; i < idList.length; i++) {
                String itemID = idList[i];
                writeOneItem(out, webServer, uri, namespace, itemType,
                        parameters, null, itemID);
            }
        }
    }

    public static void writeOneItem(Writer out, WebServer webServer,
            String uri, String namespace, String itemType, Map parameters,
            String queryString, String itemID) throws IOException {

        StringBuffer query = new StringBuffer();
        if (XMLUtils.hasValue(queryString))
            query.append('?').append(queryString);

        String itemSpace = itemType + itemID + "_";
        SnippetInvoker.addNamespacedParameters(parameters, itemSpace, query);

        if ((uri.indexOf('?') != -1) && query.length() > 0)
            query.setCharAt(0, '&');

        String fullURI = uri + query;
        // System.out.println("fullURI=" + fullURI);
        String content = webServer.getRequestAsString(fullURI);

        StringBuffer result = new StringBuffer(content);
        StringUtils.findAndReplace(result, "###_", itemSpace);
        if (!"$$$_".equals(namespace))
            StringUtils.findAndReplace(result, "$$$_", namespace);

        int insertPos = result.indexOf(INSERT_TOKEN);
        if (insertPos == -1)
            insertPos = result.indexOf("</");
        if (insertPos == -1)
            insertPos = 0;

        String elementText = "<input type='hidden' name='" + namespace
                + itemType + "Enum' value='" + itemID + "'/>";
        result.insert(insertPos, elementText);

        out.write(result.toString());
    }

    /** Find an enumerated list in the given parameter map for the given item
     * type, and return information about each enumerated item.
     * 
     * @param parameters a set of snippet parameters (should already be
     *     de-namespaced for consumption by a given snippet)
     * @param itemType the name of the enumerated type
     * @return an array whose length equals the number of items in the
     *      enumeration.  Each array member is a Map, mapping attribute names
     *      to values for the given enumerated object.  If no such enumeration
     *      is found, or if it is of zero length, null will be returned.
     */
    public static Map[] getEnumeratedValues(Map parameters, String itemType) {
        String[] idList = (String[]) parameters.get(itemType + "Enum_ALL");
        if (idList == null || idList.length == 0)
            return null;

        Map[] result = new Map[idList.length];
        for (int i = 0; i < idList.length; i++) {
            String itemID = idList[i];
            String itemSpace = itemType + itemID + "_";
            result[i] = EditedPageDataParser.filterParamMap(parameters,
                    new HashMap(), itemSpace, null, false, true);
        }

        return result;
    }

    /**
     * Write values into a given parameter map to represent the items in an
     * enumeration.
     * 
     * This method performs the inverse operation of
     * {@link #getEnumeratedValues(Map, String)}.  This method is not used by
     * many snippets, because the posting of data from an HTML form will
     * build the same map.  However, this method is provided for use by
     * clients that need to build snippet data from some source other than
     * an HTML form.
     * 
     * @param parameters
     *            the map to store snippet parameters into
     * @param itemType
     *            the name of the enumerated type
     * @param values
     *            an array whose length equals the number of items in the
     *            enumeration. Each array member is a Map, mapping attribute
     *            names to values for the given enumerated object.
     */
    public static void storeEnumeratedValues(Map parameters, String itemType,
            Map[] values) {

        String[] idList = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            String itemID = Integer.toString(i);
            idList[i] = itemID;
            String itemSpace = itemType + itemID + "_";
            for (Iterator j = values[i].entrySet().iterator(); j.hasNext();) {
                Map.Entry e = (Map.Entry) j.next();
                parameters.put(itemSpace + e.getKey(), e.getValue());
            }
        }
        parameters.put(itemType + "Enum_ALL", idList);
    }

    public static void storeEnumeratedValues(Map parameters, String itemType,
            List<Map> values) {
        Map[] valueArray = values.toArray(new Map[values.size()]);
        storeEnumeratedValues(parameters, itemType, valueArray);
    }

}
