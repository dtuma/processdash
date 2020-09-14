// Copyright (C) 2017-2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.InterpolatingProperties;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.WBSNode;

public class ExtSyncUtil {

    public static final String EXT_SPEC_FILE = "externals.xml";

    public static final String EXT_SYSTEM_ID_ATTR = "External System ID";

    public static final String EXT_NODE_TYPE_ATTR = "External Node Type";

    public static final String EXT_NODE_TYPE_ID_ATTR = "External Node Type ID";

    public static final String INCOMING_PARENT_ID = "incoming";

    public static final String NODE_TYPE_PREFIX = "nodeType";

    public static final String NAME_ATTR = "name";

    static final String EXT_ATTR_PREFIX = "Ext-";

    public static boolean isExtNode(WBSNode node) {
        return node.getAttribute(EXT_SYSTEM_ID_ATTR) != null;
    }

    public static void removeExtNodeAttributes(WBSNode node) {
        Object extSystemID = node.removeAttribute(EXT_SYSTEM_ID_ATTR);
        if (extSystemID != null) {
            node.removeAttributes(new PatternList().addLiteralStartsWith( //
                EXT_ATTR_PREFIX + extSystemID + " "));
            node.setReadOnly(false);
        }
        node.removeAttribute(EXT_NODE_TYPE_ATTR);
        node.removeAttribute(EXT_NODE_TYPE_ID_ATTR);
    }

    public static void removeExtIDAttributes(WBSNode node) {
        node.removeAttributes(EXT_ID_ATTR_PAT);
    }
    private static final PatternList EXT_ID_ATTR_PAT = new PatternList(
            "^" + EXT_ATTR_PREFIX + ".* (ID|Key)$");

    public static String getExtIDAttr(String systemID) {
        return EXT_ATTR_PREFIX + systemID + " ID";
    }

    public static String getExtKeyAttr(String systemID) {
        return EXT_ATTR_PREFIX + systemID + " Key";
    }

    public static String getExtUrlAttr(String systemID) {
        return EXT_ATTR_PREFIX + systemID + " Script URL";
    }

    public static String getExtOwnerAttr(String systemID) {
        return EXT_ATTR_PREFIX + systemID + " Owner";
    }

    /** @since 5.2.1 */
    public static Properties getSystemProperties(Properties globalConfig) {
        // read the system ID from the global configuration
        String systemID = globalConfig.getProperty(ExtSyncDaemon.EXT_SYSTEM_ID);

        // retrieve the items from the global config that match our system ID
        Properties result = filterProperties(globalConfig,
            systemID.toLowerCase() + ".");

        // write the active system name/ID back into the configuration
        result.put(ExtSyncDaemon.EXT_SYSTEM_ID, systemID);
        result.put(ExtSyncDaemon.EXT_SYSTEM_NAME,
            globalConfig.getProperty(ExtSyncDaemon.EXT_SYSTEM_NAME));

        // return the result
        return result;
    }

    /** @since 5.2.1 */
    public static Properties filterProperties(Properties props, String prefix) {
        Properties result = new Properties();
        for (Entry<Object, Object> e : props.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith(prefix))
                result.put(key.substring(prefix.length()), e.getValue());
        }
        return result;
    }

    /** @since 5.2.1 */
    public static InterpolatingProperties getTargetProperties(
            InputStream appDefaults, Map configDefaults, Element configXml) {

        // load application-driven default property values
        Properties defaults = new Properties();
        try {
            if (appDefaults != null) {
                defaults.load(appDefaults);
                appDefaults.close();
            }
        } catch (IOException ioe) {
        }

        // supplement the application defaults with values from the global
        // properties that were provided to this object
        defaults.putAll(configDefaults);

        // read configuration properties from the XML tag
        InterpolatingProperties result = new InterpolatingProperties(defaults);
        try {
            result.load(new StringReader(XMLUtils.getTextContents(configXml)));
        } catch (IOException e) {
        }
        copyAttr(result, configXml, "name", "systemName");
        copyAttr(result, configXml, "id", "systemID");

        // return the final result
        return result;
    }

    private static void copyAttr(Properties p, Element xml, String attr,
            String prop) {
        String value = xml.getAttribute(attr);
        if (StringUtils.hasValue(value))
            p.put(prop, value);
    }

    /**
     * Retrieve a named property, interpret it as fractional seconds, and return
     * the equivalent number of milliseconds.
     * 
     * @param properties
     *            a set of Properties to consult
     * @param propName
     *            the name of the desired property
     * @param defaultMillis
     *            a default value to return, if the the properties are null, if
     *            they don't contain the named property, or if the property
     *            value can't be parsed as a number.
     * @since 2.5.7
     */
    public static int getParamAsMillis(Properties properties, String propName,
            int defaultMillis) {
        try {
            String value = properties.getProperty(propName);
            return (int) (Double.parseDouble(value) * 1000);
        } catch (Exception e) {
            return defaultMillis;
        }
    }

}
