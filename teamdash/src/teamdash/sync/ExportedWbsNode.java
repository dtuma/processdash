// Copyright (C) 2020 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.WBSNode;

/**
 * A node in the WBS that should be exported to an external system.
 */
public class ExportedWbsNode {

    private WBSNode node;

    private String extSystemID;

    private SyncMetadata metadata;

    public ExportedWbsNode(WBSNode node, String extSystemID,
            SyncMetadata metadata) {
        this.node = node;
        this.extSystemID = extSystemID;
        this.metadata = metadata;
    }

    /**
     * @return the unique ID of this node in the WBS
     */
    public int getWbsID() {
        return node.getUniqueID();
    }

    /**
     * @return the name this node should have in the external system
     */
    public String getName() {
        return node.getName();
    }

    /**
     * @return the human-readable type this node should have in the external
     *         system
     */
    public String getType() {
        return (String) node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR);
    }

    /**
     * @return the ID of the type this node should have in the external system
     */
    public String getTypeID() {
        return (String) node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR);
    }


    /**
     * Make a record that the node was successfully created in the external
     * system, and store the unique ID and key that were assigned.
     */
    public void nodeWasCreated(String extUniqueID, String extKey) {
        String wbsID = Integer.toString(node.getUniqueID());
        metadata.discardAttrs(EXPORTED_PREFIX, wbsID);
        metadata.setStr(extUniqueID, EXPORTED_PREFIX, wbsID, EXT_ID);
        metadata.setStr(extKey, EXPORTED_PREFIX, wbsID, EXT_KEY);
    }

    /**
     * Record an error that occurred when attempting to create this node.
     */
    public void recordCreationError(String errorMessage) {
        String wbsID = Integer.toString(node.getUniqueID());
        metadata.setStr(errorMessage, EXPORTED_PREFIX, wbsID, "errorMessage");
    }


    /**
     * Read the external unique ID and key from metadata and set it on the WBS
     * node
     * 
     * @return true if an ID was found and stored, false if no changes were made
     */
    protected boolean loadExtIDFromMetadata() {
        String wbsID = Integer.toString(node.getUniqueID());

        // retrieve the external node ID from metadata and store in the WBS
        String extID = metadata.getStr(EXPORTED_PREFIX, wbsID, EXT_ID);
        if (StringUtils.hasValue(extID) == false)
            return false;
        node.setAttribute(ExtSyncUtil.getExtIDAttr(extSystemID), extID);

        // retrieve the external node key from metadata and store in the WBS
        String extKey = metadata.getStr(EXPORTED_PREFIX, wbsID, EXT_KEY);
        if (StringUtils.hasValue(extKey))
            node.setAttribute(ExtSyncUtil.getExtKeyAttr(extSystemID), extKey);

        // changes were made to the WBS
        return true;
    }

    private static final String EXPORTED_PREFIX = "exportedNode";

    private static final String EXT_ID = "extId";

    private static final String EXT_KEY = "extKey";

}
