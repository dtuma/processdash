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

import static teamdash.wbs.WBSFilenameConstants.DATA_DUMP_FILE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.TeamProcess;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSClipSelection;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer;

public class ExtSynchronizer {

    private TeamProjectDataTarget dataTarget;

    private TeamProject teamProject;

    private WBSModel wbs;

    private String extSystemName;

    private String extSystemID;

    private String extIDAttr;

    private SyncMetadata metadata;

    private Map<String, WBSNode> extNodeMap;

    private WBSNode incomingNodeParent;

    private List<ExtChange> extChangesNeeded;

    private Map<String, String> nameChanges;

    private Map<String, String> timeChanges;

    private boolean reverseSyncedTime;

    private boolean wbsChanged;

    public ExtSynchronizer(TeamProjectDataTarget dataTarget,
            TeamProject teamProject, String extSystemName, String extSystemID,
            SyncMetadata metadata, boolean wbsDirty) {
        this.dataTarget = dataTarget;
        this.teamProject = teamProject;
        this.wbs = this.teamProject.getWBS();
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.extIDAttr = ExtSyncUtil.getExtIDAttr(extSystemID);
        this.metadata = metadata;
        this.reverseSyncedTime = false;
        this.wbsChanged = wbsDirty;

        // if the sync metadata holds external ID info for any recently exported
        // nodes, apply those to the WBS
        for (ExportedWbsNode node : getWbsNodesToExport()) {
            if (node.loadExtIDFromMetadata())
                wbsChanged = true;
        }

        this.extNodeMap = buildExtNodeMap();
    }


    public List<ExportedWbsNode> getWbsNodesToExport() {
        List<ExportedWbsNode> result = new ArrayList();
        getWbsNodesToExport(result, wbs.getRoot());
        return result;
    }

    private void getWbsNodesToExport(List<ExportedWbsNode> result,
            WBSNode parentNode) {
        for (WBSNode node : wbs.getChildren(parentNode)) {
            if (needsExport(node))
                result.add(new ExportedWbsNode(node, extSystemID, metadata));
            getWbsNodesToExport(result, node);
        }
    }

    private boolean needsExport(WBSNode node) {
        // abort if this node doesn't belong to our external system
        Object nodeSystemID = node.getAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR);
        if (!extSystemID.equals(nodeSystemID))
            return false;

        // abort if this node already has an ID from the ext system
        String nodeExtID = (String) node.getAttribute(extIDAttr);
        if (StringUtils.hasValue(nodeExtID))
            return false;

        // return true if we have the minimal info needed to create an ext node
        String nodeName = node.getName();
        Object nodeType = node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR);
        return StringUtils.hasValue(nodeName) && nodeType != null;
    }


    public void sync(List<ExtNode> extNodes) {
        this.extChangesNeeded = new ArrayList<ExtChange>();
        runReverseSync();
        createRenameAndDeleteNodes(extNodes);
        syncReadOnlyFields(extNodes);
        syncTimeValues(extNodes);
    }

    public boolean wasWbsChanged() {
        return wbsChanged;
    }

    public List<ExtChange> getExtChangesNeeded() {
        return extChangesNeeded;
    }

    /**
     * Get the IDs of nodes that were copied into the WBS in the past, which the
     * user has moved out from under the "Incoming" node (implying they are
     * being used within the project)
     */
    public Set<String> getIDsOfNodesUsedInWbs() {
        // create a set of all known external IDs in the WBS
        Set<String> result = new HashSet<String>(extNodeMap.keySet());

        // discard the ID of the incoming node
        result.remove(ExtSyncUtil.INCOMING_PARENT_ID);

        // discard IDs of nodes that appear under the incoming node
        WBSNode incoming = extNodeMap.get(ExtSyncUtil.INCOMING_PARENT_ID);
        if (incoming != null) {
            Map<String, WBSNode> incomingNodeMap = buildExtNodeMap(
                Arrays.asList(wbs.getDescendants(incoming)));
            result.removeAll(incomingNodeMap.keySet());
        }

        // return the result
        return result;
    }


    private Map<String, WBSNode> buildExtNodeMap() {
        return buildExtNodeMap(wbs.getWbsNodes());
    }

    private Map<String, WBSNode> buildExtNodeMap(List<WBSNode> nodesToScan) {
        Map<String, WBSNode> result = new HashMap<String, WBSNode>();
        for (WBSNode node : nodesToScan) {
            String oneID = (String) node.getAttribute(extIDAttr);
            if (StringUtils.hasValue(oneID)) {
                if (result.containsKey(oneID)) {
                    // if the WBS contains two copies of a given external node,
                    // make the second one "normal."
                    ExtSyncUtil.removeExtNodeAttributes(node);
                    this.wbsChanged = true;
                } else {
                    result.put(oneID, node);
                }
            }
        }
        return result;
    }


    private void runReverseSync() {
        // If PDASH files contain unpublished modifications, apply them. This
        // will ensure that the estimate sync logic will be working against the
        // most up-to-date values.
        WBSSynchronizer reverseSync = new WBSSynchronizer(teamProject, null);
        reverseSync.run();
        reverseSyncedTime = reverseSync.getChangedTimeEstimates();
    }


    private void createRenameAndDeleteNodes(List<ExtNode> extNodes) {
        HashMap<String, WBSNode> oldWbsNodes = new HashMap<String, WBSNode>(
                extNodeMap);
        oldWbsNodes.remove(ExtSyncUtil.INCOMING_PARENT_ID);
        List<ExportedWbsNode> exportedNodes = getWbsNodesToExport();

        this.nameChanges = new HashMap<String, String>();
        WBSNode parent = incomingNodeParent = getIncomingNodeParent();
        WBSNode previousSibling = null;
        for (ExtNode extNode : extNodes) {
            WBSNode node = createOrRenameNode(extNode, parent, previousSibling,
                oldWbsNodes, exportedNodes);
            previousSibling = node;
        }

        // if any WBS nodes were created by a previous sync but are no longer
        // present in the external system, clean them up
        if (!oldWbsNodes.isEmpty()) {
            // scan the "incoming" node for children that are no longer needed
            scrubOrDeleteObsoleteIncomingNodes(incomingNodeParent, oldWbsNodes);

            // scrub unneeded nodes that appear elsewhere in the WBS
            for (Entry<String, WBSNode> e : oldWbsNodes.entrySet()) {
                String extID = e.getKey();
                WBSNode oldNode = e.getValue();
                scrubOrDeleteNode(false, extID, oldNode);
            }
        }
    }

    private WBSNode getIncomingNodeParent() {
        WBSNode result = extNodeMap.get(ExtSyncUtil.INCOMING_PARENT_ID);
        if (result == null) {
            result = new WBSNode(wbs, "Incoming Items from " + extSystemName,
                    TeamProcess.COMPONENT_TYPE, 1, true);
            result.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR, extSystemID);
            result.setAttribute(extIDAttr, ExtSyncUtil.INCOMING_PARENT_ID);
            wbs.add(result);
        }
        result.setAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR, "Incoming Items");
        result.setReadOnly(true);
        return result;
    }

    private WBSNode createOrRenameNode(ExtNode extNode, WBSNode parent,
            WBSNode previousSibling, HashMap<String, WBSNode> oldWbsNodes,
            List<ExportedWbsNode> exportedNodes) {
        // retrieve information about an external node
        String extID = extNode.getID();
        String extName = WBSClipSelection.scrubName(extNode.getName());
        oldWbsNodes.remove(extID);

        // look for the external node in our WBS
        WBSNode node = extNodeMap.get(extID);
        if (node == null)
            node = findExportedNodeToClaim(extNode, exportedNodes);

        if (node == null) {
            // if the node does not exist, create it.
            node = new WBSNode(wbs, extName, TeamProcess.COMPONENT_TYPE, 2,
                    false);
            node.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR, extSystemID);
            node.setAttribute(extIDAttr, extID);
            node.setReadOnly(true);
            wbs.addChild(parent, node);
            extNodeMap.put(extID, node);
            metadata.discardAttrs(extID);
            wbsChanged = true;

        } else {
            // the WBS already contains an item for the given external node.

            // if the WBS node name doesn't match the name in the external
            // system, rename the node in the WBS
            if (!extName.equals(node.getName())) {
                node.setName(extName);
                nameChanges.put(Integer.toString(node.getUniqueID()), extName);
                wbsChanged = true;
            }

            // if the node isn't read-only, it represents an external node that
            // disappeared from our query in the past (and was "scrubbed" as a
            // result), but has just returned. Add the attributes we need to
            // mark it as an external node again.
            if (node.isReadOnly() == false) {
                node.setType(TeamProcess.COMPONENT_TYPE);
                node.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR, extSystemID);
                node.setReadOnly(true);
                wbsChanged = true;
            }

            // if the WBS node has the wrong parent, move it under the right
            // parent. Note that we skip this change in certain situations:
            //
            // 1) If the "target parent" is the "incoming" node. If the user
            // manually moved this WBS node out from under the "incoming" bucket
            // in the past, there is no reason to move it back.
            //
            // 2) if the WBS node has a parent that didn't come from the
            // external system. That would imply that the user manually moved it
            // out from under the external hierarchy in the past (perhaps to
            // cherry pick items that are relevant to the current project
            // cycle).
            //
            // In other cases, we reparent the WBS node as needed. Note that
            // this logic should generally only be triggered if nodes got
            // reparented in the external system. (But it could also run if
            // users mucked up the WBS ancestry unintentionally.)
            WBSNode currentParent = wbs.getParent(node);
            if (currentParent != parent && parent != incomingNodeParent
                    && isExtNode(currentParent)) {
                wbs.deleteNodes(Collections.singletonList(node), false);
                wbs.addChild(parent, node);
                wbsChanged = true;
            }
        }

        // move the node up/down if necessary, to preserve sibling order.
        maybeReorderNodeRelativeToSiblings(parent, node, previousSibling);

        // recurse over children, if any are present
        WBSNode previousChild = null;
        for (ExtNode child : getChildren(extNode)) {
            previousChild = createOrRenameNode(child, node, previousChild,
                oldWbsNodes, exportedNodes);
        }

        // our calling logic uses the return value from this method to track
        // the "previous sibling" of the target parent, for ordering purposes.
        if (wbs.getParent(node) == parent) {
            // If the node we just created/updated is under the target parent,
            // return it so the next sibling can be placed after it.
            return node;
        } else {
            // But if this node is under a different parent (because the user
            // moved it), tell our caller to place subsequent children after
            // the previous sibling it gave us.
            return previousSibling;
        }
    }

    private WBSNode findExportedNodeToClaim(ExtNode extNode,
            List<ExportedWbsNode> exportedNodes) {
        // iterate over the exported nodes, looking for one that could be a
        // match for the given external node
        for (Iterator i = exportedNodes.iterator(); i.hasNext();) {
            ExportedWbsNode expNode = (ExportedWbsNode) i.next();
            if (extNode.getSimpleName().equals(expNode.getName())
                    && NullSafeObjectUtils.EQ(extNode.getTypeID(),
                        expNode.getTypeID())) {
                // if we find a "pending export" in the WBS that matches this
                // external node, look up the corresponding WBS node.
                WBSNode wbsNode = wbs.getNodeMap().get(expNode.getWbsID());
                if (wbsNode == null)
                    continue;

                // claim the WBS node as a match, by storing the external ID
                // and saving it in the extNodeMap.
                wbsNode.setAttribute(extIDAttr, extNode.getID());
                extNodeMap.put(extNode.getID(), wbsNode);
                i.remove();
                return wbsNode;
            }
        }

        // no matching exported node was found.
        return null;
    }

    /**
     * Move the given node up or down relative to its siblings to ensure that if
     * comes after the desired sibling, and that no other external nodes appear
     * between the two.
     * 
     * The given parent might include non-external nodes that were manually
     * created by the user. We want to disturb these existing nodes as little as
     * possible; so this method only moves the target node around, and moves it
     * the shortest distance needed to ensure that previous sibling constraints
     * are met.
     */
    private void maybeReorderNodeRelativeToSiblings(WBSNode parent,
            WBSNode node, WBSNode desiredPreviousSibling) {
        // get the children of the parent node
        List<WBSNode> children = Arrays.asList(wbs.getChildren(parent));

        // find the ordinal position of the given node under this parent.
        // if this node isn't under the given parent (because the user
        // moved it elsewhere), don't perform any reordering.
        int nodePos = children.indexOf(node);
        if (nodePos == -1)
            return;

        // find the ordinal position of the previous sibling. The previous
        // sibling could be null (if "node" is the first child under this
        // parent), in which case this will be -1
        int prevSiblingPos = children.indexOf(desiredPreviousSibling);

        while (nodePos < prevSiblingPos) {
            // if the node in question comes before its desired previous
            // sibling, move the node down.
            wbs.moveNodeDown(node);
            wbsChanged = true;
            nodePos++;
        }

        while (nodePos > prevSiblingPos + 1
                && isExtNodeBetween(children, prevSiblingPos, nodePos)) {
            // if the node in question comes after its desired previous
            // sibling, but there is at least one other external node
            // between the two, move the node up.
            wbs.moveNodeUp(node);
            wbsChanged = true;
            nodePos--;
        }
    }

    private boolean isExtNodeBetween(List<WBSNode> nodes, int posA, int posB) {
        for (int i = posA + 1; i < posB; i++) {
            if (isExtNode(nodes.get(i)))
                return true;
        }
        return false;
    }

    private void scrubOrDeleteObsoleteIncomingNodes(WBSNode node,
            HashMap<String, WBSNode> deletableNodes) {
        // recurse over children first, deleting any that are no longer needed.
        // this will enable trees of obsolete nodes to be eligible for deletion
        for (WBSNode child : wbs.getChildren(node)) {
            scrubOrDeleteObsoleteIncomingNodes(child, deletableNodes);
        }

        // if the current node is deletable, clean it up
        String nodeExtID = (String) node.getAttribute(extIDAttr);
        if (deletableNodes.remove(nodeExtID) != null)
            scrubOrDeleteNode(true, nodeExtID, node);
    }

    private void scrubOrDeleteNode(boolean isIncomingNode, String extID,
            WBSNode oldNode) {
        if (isIncomingNode && wbs.isLeaf(oldNode)) {
            // if this node is still in the "incoming items" bucket and is
            // still a leaf, delete it from the WBS entirely.
            wbs.deleteNodes(Collections.singletonList(oldNode));

        } else if (ExtSyncUtil.isExtNode(oldNode) == false) {
            // if this node was already scrubbed at some time in the past,
            // return without making any changes to the WBS.
            return;

        } else {
            // if this node has been moved out from the "incoming items" bucket,
            // or if children have been created underneath it, don't delete it.
            // Instead, turn it back into a "normal" node.
            ExtSyncUtil.removeExtNodeAttributes(oldNode);

            // write the extID back into the node. This will make it possible
            // for a future run to reclaim this WBS node if the given ext node
            // reappears in the query.
            oldNode.setAttribute(extIDAttr, extID);
        }

        metadata.discardAttrs(extID);
        wbsChanged = true;
    }


    private void syncReadOnlyFields(List<ExtNode> extNodes) {
        String keyAttr = ExtSyncUtil.getExtKeyAttr(extSystemID);
        String urlAttr = ExtSyncUtil.getExtUrlAttr(extSystemID);
        String ownerAttr = ExtSyncUtil.getExtOwnerAttr(extSystemID);
        for (ExtNode extNode : extNodes) {
            // iterate over each of the external nodes
            WBSNode node = extNodeMap.get(extNode.getID());
            if (node == null)
                continue;

            // save the user-facing key for the given node
            saveReadOnlyAttr(node, keyAttr, extNode.getKey());

            // save the external type of the given node
            saveReadOnlyAttr(node, ExtSyncUtil.EXT_NODE_TYPE_ATTR,
                extNode.getType());
            saveReadOnlyAttr(node, ExtSyncUtil.EXT_NODE_TYPE_ID_ATTR,
                extNode.getTypeID());

            // save the external URL for the given node
            saveReadOnlyAttr(node, urlAttr, extNode.getUrl());

            // save the external owner for the given node
            saveReadOnlyAttr(node, ownerAttr, extNode.getOwner());

            // recurse over children
            syncReadOnlyFields(getChildren(extNode));
        }
    }

    private void saveReadOnlyAttr(WBSNode node, String attrName,
            Object newValue) {
        // normalize missing attribute values
        if ("".equals(newValue))
            newValue = null;

        // compare the current and incoming value, and change if needed
        Object oldValue = node.getAttribute(attrName);
        if (!NullSafeObjectUtils.EQ(newValue, oldValue)) {
            node.setAttribute(attrName, newValue);
            wbsChanged = true;
        }
    }


    private void syncTimeValues(List<ExtNode> extNodes) {
        this.timeChanges = new HashMap<String, String>();
        WBSUtil wbsUtil = new WBSUtil(wbs, timeChanges,
                teamProject.getTeamMemberList());
        syncTimeValues(extNodes, wbsUtil);
    }

    private void syncTimeValues(List<ExtNode> extNodes, WBSUtil wbsUtil) {
        for (ExtNode extNode : extNodes) {
            syncTimeEstimate(wbsUtil, extNode);
            syncActualTime(wbsUtil, extNode);

            // recurse over children
            syncTimeValues(getChildren(extNode), wbsUtil);
        }
    }

    private void syncTimeEstimate(WBSUtil wbsUtil, ExtNode extNode) {
        // see if the external system has a time estimate for this node. If
        // not, make no changes.
        Double extTime = extNode.getEstimatedHours();
        Double extRem = extNode.getRemainingHours();
        if (extTime == null && extRem == null)
            return;

        // Look up the WBS node corresponding to this external node
        String extID = extNode.getID();
        WBSNode node = extNodeMap.get(extID);
        if (node == null)
            return;

        // if the user has placed one external node inside another in the WBS,
        // don't try to sync both time estimates.
        if (isNestedExtNode(node))
            return;

        // retrieve the WBS time estimate and the last sync values
        double wbsPriorTime = ExtSyncUtil.getPriorProjectTime(node, true);
        double wbsPriorAct = ExtSyncUtil.getPriorProjectTime(node, false);
        double wbsCurrTime = wbsUtil.getEstimatedTime(node);
        double wbsCurrAct = wbsUtil.getActualTime(node);
        double wbsTime = wbsCurrTime + wbsPriorTime;
        double wbsAct = wbsCurrAct + wbsPriorAct;
        double newRem = Math.max(0, wbsTime - wbsAct);
        double lastSyncTime = metadata.getNum(0.0, extID, EST_TIME, LAST_SYNC);
        double lastSyncRem = metadata.getNum(0.0, extID, REM_TIME, LAST_SYNC);

        // if the user edited the "remaining time" in the external system,
        // propagate that change as needed
        boolean remEdited = (extRem != null && !timeEq(extRem, lastSyncRem));
        if (remEdited) {
            // calculate the new total estimated time
            double newTime = extRem + wbsAct;

            // if this new time estimate is less than the total estimates for
            // prior projects, bump it back up to that min value
            if (newTime < wbsPriorTime) {
                newTime = wbsPriorTime;
                newRem = Math.max(0, newTime - wbsAct);
                recordOutboundChange(extNode, REM_TIME, newRem);
                wbsChanged |= reverseSyncedTime;
            } else {
                // if we were able to honor the new remaining time value,
                // write metadata to record that fact
                metadata.setNum(extRem, extID, REM_TIME, LAST_SYNC);
                metadata.setNum(null, extID, REM_TIME, OUTBOUND_VALUE);
            }

            // update the estimate in the WBS with the new total time
            if (!timeEq(wbsTime, newTime)) {
                double newCurrTime = Math.max(0, newTime - wbsPriorTime);
                wbsUtil.changeTimeEstimate(node, wbsCurrTime, newCurrTime);
                metadata.setNum(newTime, extID, EST_TIME, LAST_SYNC);
                metadata.setNum(null, extID, EST_TIME, OUTBOUND_VALUE);
                wbsChanged = true;
            }

            // copy this change back to the external system if needed
            if (extTime != null && !timeEq(extTime, newTime)) {
                recordOutboundChange(extNode, EST_TIME, newTime);
                wbsChanged |= reverseSyncedTime;
            }

            // this new change has been propagated everywhere, so no additional
            // synchronization logic is needed.
            return;
        }

        // perform a bidirectional sync of total estimated time
        if (extTime != null) {
            // compare value pairs to identify changes that have been made
            boolean valuesMatch = timeEq(extTime, wbsTime);
            boolean extEdited = !timeEq(extTime, lastSyncTime);
            boolean wbsEdited = !timeEq(wbsTime, lastSyncTime);

            // compare estimates and determine if changes are needed
            if (valuesMatch) {
                // the time in the WBS agrees with the external system, so no
                // changes are needed to the estimates themselves. Update the
                // tracking metdata if needed.
                if (!timeEq(wbsTime, lastSyncTime))
                    metadata.setNum(wbsTime, extID, EST_TIME, LAST_SYNC);
                metadata.setNum(null, extID, EST_TIME, OUTBOUND_VALUE);

            } else if (extEdited) {
                // the value in the external system has been edited since the
                // last sync. Copy the new value into the WBS.
                double newCurrTime = Math.max(0, extTime - wbsPriorTime);
                wbsUtil.changeTimeEstimate(node, wbsCurrTime, newCurrTime);
                metadata.setNum(extTime, extID, EST_TIME, LAST_SYNC);
                // if the new external estimate is lower than total estimates
                // for prior projects, bump it back up to that min value
                if (wbsPriorTime > extTime) {
                    recordOutboundChange(extNode, EST_TIME, wbsPriorTime);
                    newRem = Math.max(0, wbsPriorTime - wbsAct);
                } else {
                    metadata.setNum(null, extID, EST_TIME, OUTBOUND_VALUE);
                    newRem = Math.max(0, extTime - wbsAct);
                }
                wbsChanged = true;

            } else if (wbsEdited) {
                // if the value in the WBS has been edited since the last sync,
                // copy that value back to the external system.
                recordOutboundChange(extNode, EST_TIME, wbsTime);
                wbsChanged |= reverseSyncedTime;
            }
        }

        // update the remaining time in the external system if needed
        if (extRem != null) {
            boolean remChanged = !timeEq(newRem, lastSyncRem);
            if (remChanged) {
                recordOutboundChange(extNode, REM_TIME, newRem);
                wbsChanged |= reverseSyncedTime;
            } else {
                metadata.setNum(null, extID, REM_TIME, OUTBOUND_VALUE);
            }
        }
    }

    /**
     * If the external system does not have the correct value for a particular
     * attribute, record the changes necessary to rectify the problem
     * 
     * @param extNode
     *            the node where the change should be made
     * @param attrName
     *            the name of the attribute to change
     * @param newValue
     *            the new value which should be set for the attribute
     */
    private void recordOutboundChange(ExtNode extNode, String attrName,
            double newValue) {
        // Record a "pending outbound value" in the metadata
        String extID = extNode.getID();
        metadata.setNum(newValue, extID, attrName, OUTBOUND_VALUE);

        // create an object to record the external change that is needed
        ExtChange change = getExtChange(extNode);
        change.attrValues.put(attrName, newValue);
        change.metadata.setNum(newValue, extID, attrName, LAST_SYNC);
        change.metadata.setStr(SyncMetadata.DELETE_METADATA, extID, attrName,
            OUTBOUND_VALUE);
    }

    private void syncActualTime(WBSUtil wbsUtil, ExtNode extNode) {
        // see if the external system tracks actual time for this node. If
        // not, make no changes.
        Double extTime = extNode.getActualHours();
        if (extTime == null)
            return;

        // Look up the WBS node corresponding to this external node
        String extID = extNode.getID();
        WBSNode node = extNodeMap.get(extID);
        if (node == null)
            return;

        // if the user has placed one external node inside another in the WBS,
        // don't try to sync both time values.
        if (isNestedExtNode(node))
            return;

        // retrieve the WBS actual time
        double wbsPriorTime = ExtSyncUtil.getPriorProjectTime(node, false);
        double wbsCurrTime = wbsUtil.getActualTime(node);
        double wbsTime = wbsCurrTime + wbsPriorTime;
        if (!timeEq(wbsTime, extTime)) {
            // create an object to record the external change that is needed
            ExtChange change = getExtChange(extNode);
            change.attrValues.put(ExtChange.ACT_TIME_ATTR, wbsTime);
        }
    }

    private boolean isNestedExtNode(WBSNode node) {
        // scan the WBS descendants of this node to see if any came from the
        // external system. If so, we will sync estimates for those children,
        // but not for this parent.
        for (WBSNode desc : wbs.getDescendants(node)) {
            if (isExtNode(desc))
                return true;
        }
        return false;
    }

    private boolean isExtNode(WBSNode node) {
        String extID = (String) node.getAttribute(extIDAttr);
        return StringUtils.hasValue(extID);
    }

    private List<ExtNode> getChildren(ExtNode node) {
        List<ExtNode> result = node.getChildren();
        return (result == null ? Collections.EMPTY_LIST : result);
    }

    /**
     * Find a change object for the given external node, or create an empty
     * change object if none exists.
     */
    private ExtChange getExtChange(ExtNode node) {
        for (ExtChange existingChange : extChangesNeeded) {
            if (existingChange.extNode == node)
                return existingChange;
        }

        ExtChange newChange = new ExtChange();
        newChange.extNode = node;
        newChange.attrValues = new HashMap<String, Object>();
        newChange.metadata = new SyncMetadata();
        extChangesNeeded.add(newChange);
        return newChange;
    }

    private boolean timeEq(double a, double b) {
        return (Math.abs(a - b) < ONE_MINUTE);
    }
    private static final double ONE_MINUTE = 1.0/60;


    public void updateProjDump() throws IOException {
        // get a collection of filters that should be applied to projDump.xml
        List<ProjDumpFilter> filters = getDumpFilters();
        if (filters.isEmpty())
            return;

        // open the dump file for reading/writing
        ResourceCollection coll = dataTarget.getCollection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                coll.getInputStream(DATA_DUMP_FILE), "UTF-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                coll.getOutputStream(DATA_DUMP_FILE, 0), "UTF-8"));

        // scan the lines in the dump file, filtering as we go
        String line;
        while ((line = in.readLine()) != null) {
            if (line.contains(" tid=")) {
                String nodeID = FilterUtils.getXmlAttr(line, "id");
                for (ProjDumpFilter f : filters)
                    line = f.filterLine(line, nodeID);
            }
            out.write(line);
            out.write("\n");
        }

        // close the streams, saving our changes
        in.close();
        out.close();
    }

    private List<ProjDumpFilter> getDumpFilters() {
        List<ProjDumpFilter> result = new ArrayList<ProjDumpFilter>();
        if (!nameChanges.isEmpty())
            result.add(new NodeRenameFilter(nameChanges));
        if (!timeChanges.isEmpty())
            result.add(new TimeChangeFilter(timeChanges));
        return result;
    }


    static final String EXT_ATTR_PREFIX = ExtSyncUtil.EXT_ATTR_PREFIX;

    private static final String LAST_SYNC = "lastSyncValue";

    private static final String OUTBOUND_VALUE = "pendingOutboundValue";

    private static final String EST_TIME = ExtChange.EST_TIME_ATTR;

    private static final String REM_TIME = ExtChange.REM_TIME_ATTR;

    static final Logger log = Logger.getLogger(ExtSynchronizer.class.getName());

    static final Logger debug = Logger
            .getLogger("debug." + ExtSynchronizer.class.getName());

}
