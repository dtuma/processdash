// Copyright (C) 2018 Tuma Solutions, LLC
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

public interface SyncXmlConstants {

    /**
     * Encoding used for XML documents written by the sync logic
     */
    String ENCODING = "UTF-8";


    /**
     * Root tag for a document containing information about external nodes
     */
    String EXT_NODES_TAG = "extNodes";

    /**
     * Attribute holding the ID of an external system
     */
    String SYSTEM_ID_ATTR = "extSystemID";

    /**
     * Attribute holding the name of an external system
     */
    String SYSTEM_NAME_ATTR = "extSystemName";


    /**
     * XML tag representing a node in an external system
     */
    String NODE_TAG = "node";

    /**
     * Attribute holding the ID of an external node
     */
    String NODE_ID_ATTR = "id";

    /**
     * Attribute holding the name of an external node
     */
    String NODE_NAME_ATTR = "name";

    /**
     * Attribute holding the owner of an external node
     */
    String OWNER_ATTR = "owner";

    /**
     * Attribute holding the URL of an external node
     */
    String URL_ATTR = "url";

    /**
     * Attribute holding the estimated time (in hours) of an external node
     */
    String EST_HOURS_ATTR = "estHours";

    /**
     * Attribute holding the remaining time (in hours) of an external node
     */
    String REM_HOURS_ATTR = "remHours";

    /**
     * Attribute holding the actual time (in hours) of an external node
     */
    String ACT_HOURS_ATTR = "actHours";


    /**
     * Root tag for a document containing information about outbound changes
     */
    String CHANGES_TAG = "extChanges";

    /**
     * XML tag holding outbound change information for a particular node
     */
    String CHANGE_TAG = "change";

    /**
     * Attribute holding the external ID of a node that needs changing
     */
    String CHANGE_NODE_ID_ATTR = "nodeId";

    /**
     * XML tag indicating a change to an attribute for a particular node
     */
    String ATTR_CHANGE_TAG = "attr";

    /**
     * XML attribute holding the name of a node attribute that needs changing
     */
    String ATTR_CHANGE_NAME_ATTR = "name";

    /**
     * XML attribute holding the new value of a node attribute
     */
    String ATTR_CHANGE_VALUE_ATTR = "value";

}
