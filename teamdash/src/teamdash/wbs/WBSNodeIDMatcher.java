// Copyright (C) 2012-2025 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * The 3-way merge operation relies upon the unique IDs of WBSNode objects to do
 * its work. This class performs a preliminary examination of the changes to the
 * trees in question, and reassigns unique IDs to the nodes in the incoming
 * tree to ensure a successful merge.
 */
public class WBSNodeIDMatcher {

    /**
     * By default, this class will aggressively assign new IDs to nodes that
     * were added in the incoming WBS to avoid the potential for collisions with
     * main. Provide this value as one of the aliasAttrs to indicate that only
     * <b>active</b> collisions warrant a reassignment.
     */
    public static final String RELAX_INCOMING_ID_REASSIGNMENT = //
            "** RELAX_INCOMING_ID_REASSIGNMENT **";


    /** The base ancestor in the 3-way merge */
    private WBSModel base;

    /** The main branch in the 3-way merge */
    private WBSModel main;

    /** The incoming branch in the 3-way merge */
    private WBSModel incoming;

    /** A list of attributes that can be used to detect common added nodes */
    private String[] aliasAttrs;

    /** true to use relaxed reassignment logic for incoming node IDs */
    private boolean relaxIncomingIdReassignment;

    /** A map of changes that were made to the IDs in the incoming data. */
    private Map<Integer, Integer> remappedIDs;



    /**
     * Perform a matching operation, and alter the unique IDs in the incoming
     * data as necessary.
     * 
     * @param base
     *            the base ancestor in the 3-way merge
     * @param main
     *            the main branch in the 3-way merge
     * @param incoming
     *            the incoming branch in the 3-way merge. <b>Important:</b> the
     *            node IDs in this data model will be altered as a result of
     *            this matching operation.
     * @param aliasAttrs
     *            sometimes the same node could be added to both branches by an
     *            external process. If two added nodes share a common value for
     *            one of the attributes in this list, they will be assumed to
     *            represent the same entity and will be assigned the same unique
     *            ID.
     */
    public WBSNodeIDMatcher(WBSModel base, WBSModel main,
            WBSModel incoming, String... aliasAttrs) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;
        this.aliasAttrs = aliasAttrs;
        this.relaxIncomingIdReassignment = Arrays.asList(aliasAttrs)
                .contains(RELAX_INCOMING_ID_REASSIGNMENT);
        performMatch();
    }

    public WBSModel getBase() {
        return base;
    }

    public WBSModel getMain() {
        return main;
    }

    public WBSModel getIncoming() {
        return incoming;
    }

    public Map<Integer, Integer> getRemappedIDs() {
        return remappedIDs;
    }


    //
    // variables used during the matching process
    //

    private Map<Integer, WBSNode> baseNodeMap;

    private Map<Integer, WBSNode> mainNodeMap;

    private Map<Integer, WBSNode> incomingNodeMap;

    private Set<Integer> newMainIDs;

    private Set<Integer> newIncomingIDs;


    private void performMatch() {
        // retrieve node maps for each WBS.
        baseNodeMap = Collections.unmodifiableMap(base.getNodeMap());
        mainNodeMap = Collections.unmodifiableMap(main.getNodeMap());
        incomingNodeMap = incoming.getNodeMap();

        // list the IDs of nodes that were added to the main WBS.
        newMainIDs = new HashSet(mainNodeMap.keySet());
        newMainIDs.removeAll(baseNodeMap.keySet());
        newMainIDs = Collections.unmodifiableSet(newMainIDs);

        // find the nodes that have been added to the incoming tree.  Assign
        // IDs to those nodes as necessary to make them unique.  Then collect
        // the IDs of the added nodes, ordered by their depth in the tree.
        newIncomingIDs = assignUniqueIDsToNewIncomingNodes();

        // perform changes to the IDs in the incoming branch.
        matchNewNodesUsingAliases();
        fixIncomingEditingErrors();
        matchIncomingNodesToMain();
        propagateIDChangesFromMain();

        // final bookkeeping
        recordIncomingIDRemappings();
    }



    /**
     * find the nodes that have been added to the incoming tree. Assign IDs to
     * those nodes as necessary to make them unique. Then collect the IDs of the
     * added nodes into an ordered set.  The resulting set will contain the
     * ID ordered by their node depth in the tree (shallowest nodes first).
     */
    private Set<Integer> assignUniqueIDsToNewIncomingNodes() {
        int maxMainID = getMaxIdInUse(main.getRoot().getUniqueID(), //
            baseNodeMap, mainNodeMap);
        int nextUniqueID = getMaxIdInUse(maxMainID, incomingNodeMap) + 1;

        Set<Integer> idsOfNewIncomingNodes = new LinkedHashSet<Integer>();
        List<WBSNode> parentNodes = Collections.singletonList(incoming
                .getRoot());
        List<WBSNode> nextLevelNodes;

        while (!parentNodes.isEmpty()) {
            nextLevelNodes = new ArrayList<WBSNode>();

            for (WBSNode parentNode : parentNodes) {
                for (WBSNode incomingNode : incoming.getChildren(parentNode)) {
                    nextLevelNodes.add(incomingNode);

                    Integer incomingNodeID = incomingNode.getUniqueID();
                    if (!baseNodeMap.containsKey(incomingNodeID)) {
                        // this node was not present in the base tree, so it
                        // has been added in the incoming branch.

                        // By default, we assign the incoming node a new unique
                        // ID if it falls within the range of IDs the main
                        // branch has *ever* used.  If relaxed mode is enabled,
                        // we only reassign if an active collision is detected.
                        boolean collision = relaxIncomingIdReassignment
                                ? mainNodeMap.containsKey(incomingNodeID)
                                : incomingNodeID <= maxMainID;
                        if (collision) {
                            incomingNodeID = nextUniqueID++;
                            changeIncomingNodeID(incomingNode, incomingNodeID);
                        }

                        idsOfNewIncomingNodes.add(incomingNodeID);
                    }
                }
            }

            // iterate again, this time over the nodes in the next level of
            // the tree.
            parentNodes = nextLevelNodes;
        }

        return idsOfNewIncomingNodes;
    }

    private int getMaxIdInUse(int start, Map<Integer, WBSNode>... nodeMaps) {
        int result = start;
        for (Map<Integer, WBSNode> nodeMap : nodeMaps) {
            for (Integer i : nodeMap.keySet())
                if (i != null && i > result)
                    result = i;
        }
        return result;
    }



    /**
     * There are scenarios where automatic processes create new nodes in the
     * WBS. (Examples could be reverse sync, sync from master, etc.) Those
     * processes could create the same new node in both branches. This method
     * detects that scenario (by finding shared values of "alias" attributes),
     * and reassigns incoming IDs to match the ones that were assigned in main.
     */
    private void matchNewNodesUsingAliases() {

        // iterate over each of the alias attributes we know about.
        for (String aliasAttr : aliasAttrs) {

            // ignore the "relax assignment" pseudo-alias
            if (RELAX_INCOMING_ID_REASSIGNMENT.equals(aliasAttr))
                continue;

            // find new incoming nodes that have a value for this alias attr.
            Map<Object, Integer> incomingAliasMap = buildMapForAliasAttr(
                aliasAttr, newIncomingIDs, incomingNodeMap);
            if (incomingAliasMap.isEmpty())
                continue;

            // find nodes added to main that have a value for this alias attr.
            Map<Object, Integer> mainAliasMap = buildMapForAliasAttr(aliasAttr,
                newMainIDs, mainNodeMap);
            if (mainAliasMap.isEmpty())
                continue;

            // find nodes that were added to both branches, that have the
            // same value for this alias attribute.
            for (Entry<Object, Integer> e : mainAliasMap.entrySet()) {
                Object aliasVal = e.getKey();
                Integer mainID = e.getValue();
                Integer incomingID = incomingAliasMap.get(aliasVal);
                if (incomingID != null) {
                    // when we find a match, change the ID of the incoming
                    // node so it has the same ID as the main branch.
                    assignIncomingNodeID(incomingID, mainID);
                }
            }
        }
    }

    /**
     * Look through a set of new IDs, find the corresponding new nodes, see if
     * they have a value for a given alias attribute, and build a map from alias
     * values to new node IDs.
     * 
     * Alias values should generally be unique within a WBS.  But if they are
     * not, one of the new nodes will be chosen arbitrarily.
     */
    private Map<Object, Integer> buildMapForAliasAttr(String aliasAttr,
            Set<Integer> newIDs, Map<Integer, WBSNode> nodeMap) {
        Map<Object, Integer> result = new HashMap();
        for (Integer newID : newIDs) {
            WBSNode newNode = nodeMap.get(newID);
            if (newNode != null) {
                Object aliasVal = newNode.getAttribute(aliasAttr);
                if (aliasVal != null && !aliasVal.equals(""))
                    result.put(aliasVal, newID);
            }
        }
        return result;
    }



    /**
     * Within the WBS, several editing mistakes are possible, including:
     * <ul>
     * <li>Pressing Enter when you meant to press Insert; then manually
     * editing/swapping WBS node names to fix the mistake</li>
     * <li>Deleting a node by mistake, then fixing the mistake by creating a new
     * node in its place with the exact same name.</li>
     * </ul>
     * 
     * This method attempts to detect these editing patterns in the incoming
     * tree and correct them.
     * 
     * Note that this method can only correct errors on a "best effort" basis.
     * (In particular, editing mistakes can only be detected and fixed if the
     * mistake and the associated correction all occurred during a single
     * editing session with no intervening "Save.") As a result, other WBS
     * synchonization mechanisms must still be prepared to detect and handle
     * these editing errors.
     */
    private void fixIncomingEditingErrors() {
        for (Integer oneIncomingID : new ArrayList<Integer>(newIncomingIDs))
            fixIncomingEditingErrors(oneIncomingID);
    }

    private void fixIncomingEditingErrors(Integer oneIncomingID) {
        // find the node that was added to the incoming tree.
        WBSNode newIncomingNode = incomingNodeMap.get(oneIncomingID);
        if (newIncomingNode == null)
            return;

        // Try to find a node in the base tree whose name and parentID
        // are the same as the node in question. If no such node is present
        // in the base tree, this is "really" a new node, so there is no
        // editing error to fix.
        WBSNode matchingBaseNode = findNodeMatchingNameAndParent(
            newIncomingNode, baseNodeMap);
        if (matchingBaseNode == null)
            return;

        // If we reach this point, there *IS* a node in the base tree with the
        // same name and parent as this node, but a different node ID.
        int matchingBaseID = matchingBaseNode.getUniqueID();
        WBSNode swappedNode = incomingNodeMap.get(matchingBaseID);
        if (swappedNode == null) {
            // No node in the incoming tree has the same ID as the matching
            // node from the base tree. That would seem to suggest the
            // "delete/recreate" operation. Change the ID of this node to
            // match the deleted node from the base.
            assignIncomingNodeID(oneIncomingID, matchingBaseID);

        } else if (swappedNode.getName().equals(newIncomingNode.getName())
                && incoming.getParent(swappedNode) == incoming
                        .getParent(newIncomingNode)) {
            // We were considering the possibility that this new node
            // "exchanged names" with the swapped node. But both nodes have the
            // same name and parent in the incoming tree. So the real truth is
            // that someone created a new node in the incoming branch that has
            // a duplicate name conflict with an existing node.  No changes
            // are warranted.
            ;

        } else {
            // the matching node from the base tree has the same ID as
            // another node from the incoming tree.  This would seem to
            // suggest the "Node swap" misedit.  Swap their IDs.
            changeIncomingNodeID(newIncomingNode, matchingBaseID);
            changeIncomingNodeID(swappedNode, oneIncomingID);

            // For completeness, examine the swapped node (which has now been
            // assigned an ID that makes it a "new incoming" node), and see
            // if appears to exhibit any editing errors.
            fixIncomingEditingErrors(oneIncomingID);
        }
    }



    /**
     * If a node was added to both branches with the same name and the same
     * parent, have the incoming branch adopt the ID used by the main branch.
     * 
     * This policy supports common-sense merging of nodes in usage scenarios
     * such as the following:
     * <ul>
     * 
     * <li>Two people sitting next to each other in a launch and coordinating
     * with each other verbally: "Create such-and-such a component"</li>
     * 
     * <li>Naming conventions used within a team, for example to consistently
     * create buckets for "backlog" or "bugfixing"</li>
     * 
     * <li>Two people adding a common task underneath a component, like
     * "Code Inspect."</li>
     * 
     * </ul>
     */
    private void matchIncomingNodesToMain() {
        for (Integer oneID : new ArrayList<Integer>(newIncomingIDs)) {
            // Iterate over each of the nodes added by the incoming branch
            WBSNode newIncomingNode = incomingNodeMap.get(oneID);
            if (newIncomingNode == null)
                continue;

            // Check to see if the main tree has a matching node with this same
            // name and parent.  If not, leave the incoming node alone.
            WBSNode matchingMainNode = findNodeMatchingNameAndParent(
                newIncomingNode, mainNodeMap);
            if (matchingMainNode == null)
                continue;

            // We are considering adopting the ID of the matching main node.
            // but we don't want to do that if it would cause an ID collision
            // in the incoming tree!  Check to see if the incoming tree already
            // has a node with this same ID.  (This could occur, for example,
            // if the incoming tree has a duplicate name error.)
            int matchingMainID = matchingMainNode.getUniqueID();
            if (incomingNodeMap.containsKey(matchingMainID))
                continue;

            // All our checks pass, so we will reuse the ID from the main tree
            // for this matching incoming node.
            assignIncomingNodeID(oneID, matchingMainID);
        }
    }



    /**
     * It is possible that someone deleted a node from main, then created a new
     * node in main with the same name and parent. That is an editing misuse
     * case, but in this case it was made in the main branch so the damage has
     * already been done. To avoid additional churn, apply that same ID change
     * to the incoming branch.
     */
    private void propagateIDChangesFromMain() {
        // Iterate over each of the nodes that was added to the main branch.
        for (Integer addedMainID : newMainIDs) {
            WBSNode addedMainNode = mainNodeMap.get(addedMainID);
            if (addedMainNode == null)
                continue;

            // check and see if there is already a node with this ID in the
            // incoming branch.  If so, we've probably already matched it
            // based on some other change we detected earlier.
            if (incomingNodeMap.containsKey(addedMainID))
                continue;

            // check and see if there was a node in the base tree that had
            // the same name and parent as this added node.
            WBSNode matchingBaseNode = findNodeMatchingNameAndParent(
                addedMainNode, baseNodeMap);
            if (matchingBaseNode == null)
                continue;

            // If we found a parent/name match in the base tree, see if that
            // base node still exists somewhere else in the main tree.  (That
            // would imply that the main tree moved the base node elsewhere,
            // or that the main tree has simply created a duplicate name
            // error.)  In these cases, do nothing.
            int matchingBaseID = matchingBaseNode.getUniqueID();
            if (mainNodeMap.containsKey(matchingBaseID))
                continue;

            // If we reach this point, the main tree deleted a node from the
            // base and then created a replacement node in the same location
            // with the same name.  We should interpret this as an ID change.
            changeIncomingNodeID(matchingBaseID, addedMainID);
        }
    }

    private void recordIncomingIDRemappings() {
        remappedIDs = new HashMap<Integer, Integer>();
        for (WBSNode n : incoming.getNodeMap().values()) {
            Integer oldID = (Integer) n.getAttribute(SAVED_ORIG_ID_ATTR);
            if (oldID != null) {
                remappedIDs.put(oldID, n.getUniqueID());
                n.setAttribute(SAVED_ORIG_ID_ATTR, null);
            }
        }
    }

    /**
     * Look in the node map for some WBSModel, and see if it contains a node
     * whose name and parent-ID match the given prototype node.
     * 
     * @param prototype
     * @param nodeMap
     * @return
     */
    private WBSNode findNodeMatchingNameAndParent(WBSNode prototype,
            Map<Integer, WBSNode> nodeMap) {
        WBSModel prototypeWbs = prototype.getWbsModel();
        WBSNode parent = prototypeWbs.getParent(prototype);
        if (parent == null)
            return null;

        WBSNode targetParent;
        if (parent == prototypeWbs.getRoot())
            targetParent = nodeMap.get(null);
        else
            targetParent = nodeMap.get(parent.getUniqueID());
        if (targetParent == null)
            return null;

        WBSModel targetWbs = targetParent.getWbsModel();
        WBSNode result = null;
        for (WBSNode targetChild : targetWbs.getChildren(targetParent)) {
            if (targetChild.getName().equals(prototype.getName())) {
                // we found a node with a name that matches the prototype.
                result = targetChild;
                // if this match also has the same unique ID, return it
                // immediately.  Otherwise, keep looking; if the target WBS
                // has a duplicate name error, this may help select the best
                // match from within the duplicately named nodes.
                if (result.getUniqueID() == prototype.getUniqueID())
                    break;
            }
        }
        return result;
    }


    private void assignIncomingNodeID(Integer oldID, Integer newID) {
        changeIncomingNodeID(oldID, newID);
        newIncomingIDs.remove(oldID);
    }

    private void changeIncomingNodeID(Integer oldID, Integer newID) {
        WBSNode incomingNode = incomingNodeMap.remove(oldID);
        if (incomingNode != null)
            changeIncomingNodeID(incomingNode, newID);
    }

    private void changeIncomingNodeID(WBSNode incomingNode, Integer newID) {
        Integer oldID = changeNodeID(incomingNode, newID);

        // register the node by its new ID in the incoming map.
        incomingNodeMap.remove(oldID);
        incomingNodeMap.put(newID, incomingNode);
    }

    private Integer changeNodeID(WBSNode node, Integer newID) {
        // make a note of the original ID, so we can infer all node ID
        // changes at the end of our matching operation.
        Integer oldID = node.getUniqueID();
        if (node.getAttribute(SAVED_ORIG_ID_ATTR) == null)
            node.setAttribute(SAVED_ORIG_ID_ATTR, oldID);

        // Tell the WBSNode object about the new ID.
        node.setUniqueID(newID);
        return oldID;
    }

    private static final String SAVED_ORIG_ID_ATTR = "_Saved_Node_ID_"
            + WBSNodeIDMatcher.class.getSimpleName();

}
