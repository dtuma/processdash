// Copyright (C) 2009-2010 Tuma Solutions, LLC
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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * An object which knows how to lookup the type of a defect.
 * 
 * Looking up the type of a defect is tricky, and relies on aspects of the code
 * collaborator data schema that are "subject to modification without notice."
 * To respond to this uncertainty, this class uses the strategy pattern to
 * construct an object which can lookup defect types for a particular server.
 * 
 * Note: at the moment, two strategies are provided that appear to work with
 * Code Collaborator version 4 and version 5.
 */
public abstract class CCDefectTypeLookup extends CCQuerySupport {

    /** Retrieve the type of a defect, given its unique ID. */
    public abstract String getType(Integer defectId);

    /**
     * Retrieve an object which is capable of looking up defect types for a
     * particular Code Collaborator server.
     * 
     * @param client
     *                an {@link XmlRpcClient} object pointing to the Code
     *                Collaborator server in question
     * @return an appropriate lookup object, never null. (If we are not capable
     *         of looking up defect types for the given server, a "do nothing"
     *         lookup object will be returned.)
     */
    public static CCDefectTypeLookup getTypeLookup(XmlRpcClient client) {
        try {
            return new DefectTypeLookupImplV4(client);
        } catch (Exception e) {}

        try {
            return new DefectTypeLookupImplV5(client);
        } catch (Exception e) {}

        return new NullDefectTypeLookup();
    }


    /**
     * Run a simple, standalone test to see if our known strategies work against
     * a particular server.
     */
    public static void main(String[] args) {
        URL baseUrl, url;
        try {
            baseUrl = new URL(args[0]);
            url = new URL(baseUrl, "/xmlrpc/secure");
        } catch (IOException ioe) {
            throw new RuntimeException("Malformed URL " + args[0]);
        }

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(url);
        config.setBasicUserName(args[1]);
        config.setBasicPassword(args[2]);

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);

        try {
            execute(client, "getServerVersion");
        } catch (Exception e) {
            throw new RuntimeException("Unable to contact server " + baseUrl
                    + " - cannot validate defect lookup strategy");
        }

        CCDefectTypeLookup lookup = getTypeLookup(client);
        String oneType = lookup.getType(1);

        if (oneType != null) {
            System.out.println("Code Collaborator defect type lookup "
                    + "strategy succeeded against URL " + baseUrl);
            return;
        } else {
            throw new RuntimeException("Code Collaborator defect type lookup "
                    + "strategy failed against URL " + baseUrl);
        }
    }


    private static Integer getMetadataId(XmlRpcClient client,
            Object... criteria) throws Exception {
        return (Integer) lookupSingleValue(client, METADATA_DESCRIPTION_CLASS,
            "id", criteria);
    }


    private static class NullDefectTypeLookup extends CCDefectTypeLookup {

        @Override
        public String getType(Integer defectId) {
            return null;
        }

    }


    private static class DefectTypeLookupImplV4 extends CCDefectTypeLookup {

        private XmlRpcClient client;

        private Integer fieldId;

        private Map<String, String> defectTypes;

        public DefectTypeLookupImplV4(XmlRpcClient client) throws Exception {
            this.client = client;

            Integer dataType = getMetadataId(client, "title", "defectTypes",
                "category", "DataTypes");
            this.fieldId = getMetadataId(client, "title", "Type", "category",
                "AdminDefectFields", "related", dataType);

            Object[] knownTypes = querySimple(client,
                METADATA_DESCRIPTION_CLASS, 200, "category", "defectTypes");
            if (knownTypes == null || knownTypes.length == 0)
                throw new Exception("Expected to find defect types");

            defectTypes = new HashMap<String, String>();
            for (Object o : knownTypes) {
                Map oneType = (Map) o;
                Object typeId = oneType.get("id");
                Object typeText = oneType.get("title");
                defectTypes.put(typeId.toString(), typeText.toString());
            }
        }

        @Override
        public String getType(Integer defectId) {
            try {
                Object typeId = lookupSingleValue(client,
                    STRING_METADATA_CLASS, "value", "targetId", defectId,
                    "fieldId", fieldId);
                return defectTypes.get(typeId.toString());
            } catch (Exception e) {
                return null;
            }
        }

    }

    private static class DefectTypeLookupImplV5 extends CCDefectTypeLookup {

        private XmlRpcClient client;

        private Integer fieldId;

        private Map<String, String> defectTypes;

        public DefectTypeLookupImplV5(XmlRpcClient client) throws Exception {
            this.client = client;

            this.fieldId = getMetadataId(client, "title", "Type", "category",
                "AdminDefectFields", "targetType", "Defect");

            Object[] knownTypes = querySimple(client,
                SELECT_METADATA_CLASS, 200, "fieldId", fieldId);
            if (knownTypes == null || knownTypes.length == 0)
                throw new Exception("Expected to find defect types");

            defectTypes = new HashMap<String, String>();
            for (Object o : knownTypes) {
                Map oneType = (Map) o;
                Object typeId = oneType.get("id");
                Object typeText = oneType.get("title");
                defectTypes.put(typeId.toString(), typeText.toString());
            }
        }

        @Override
        public String getType(Integer defectId) {
            try {
                Object typeId = lookupSingleValue(client,
                    INTEGER_METADATA_CLASS, "value", "targetId", defectId,
                    "fieldId", fieldId);
                return defectTypes.get(typeId.toString());
            } catch (Exception e) {
                return null;
            }
        }

    }

}
