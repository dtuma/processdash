// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package teamdash.team;

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import teamdash.merge.AttributeMerger;
import teamdash.merge.DefaultAttributeMerger;
import teamdash.merge.MapContentMerger;
import teamdash.merge.MergeWarning;
import teamdash.merge.TreeMerger;
import teamdash.merge.TreeNode;
import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.MergeWarning.Severity;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.wbs.TeamProject;

public class TeamMemberListMerger {

    /** The team member list from the base revision */
    TeamMemberList base;

    /** The team member list from the main revision */
    TeamMemberList main;

    /** The team member list from the incoming revision */
    TeamMemberList incoming;

    /** The merged team member list */
    TeamMemberList merged;

    /** A collection of warnings produced during the merge operation */
    private Set<MergeWarning<Integer>> mergeWarnings;

    /** A collection of changes that were made to the IDs of individuals in team
     * member list from the incoming revision */
    private Map<Integer, Integer> changesMadeToIncomingIDs;

    /** A collection of changes that need to be made to team member initials
     * in the main WBS */
    private Map<String, String> changesNeededToMainInitials;

    /** A collection of changes that need to be made to team member initials
     * in the incoming WBS */
    private Map<String, String> changesNeededToIncomingInitials;


    public TeamMemberListMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this(base.getTeamMemberList(), main.getTeamMemberList(), incoming
                .getTeamMemberList());
    }

    public TeamMemberListMerger(TeamMemberList base, TeamMemberList main,
            TeamMemberList incoming) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;

        performMerge();
    }

    public TeamMemberList getMerged() {
        return merged;
    }

    public Set<MergeWarning<Integer>> getMergeWarnings() {
        return mergeWarnings;
    }

    public List<MergeConflictNotification> getConflictNotifications() {
        return TeamMemberListMergeConflictNotificationFactory.createAll(this);
    }

    public Map<Integer, Integer> getChangesMadeToIncomingIDs() {
        return changesMadeToIncomingIDs;
    }

    public Map<String, String> getChangesNeededToMainInitials() {
        return changesNeededToMainInitials;
    }

    public Map<String, String> getChangesNeededToIncomingInitials() {
        return changesNeededToIncomingInitials;
    }


    /**
     * Do the work of merging two team member lists.
     */
    private void performMerge() {
        // perform a matching step to see if any individuals in the incoming
        // branch are duplicates of individuals added by main
        rematchIncomingIndividuals();

        // merge the data from the two teams
        TreeMerger<Integer, Map> teamMerger = new TreeMerger<Integer, Map>(
                toTree(base), toTree(main), toTree(incoming), buildMerger());
        teamMerger.run();

        // check for and resolve conflicts in name or initials
        checkForConflictsInNameOrInitials(teamMerger);

        // record the effective changes that the merge made to the initials
        // in the two team member lists.
        recordChangesToInitials(teamMerger);

        // Create the merged team member list
        merged = getMergedTeam(teamMerger);
        mergeWarnings = teamMerger.getMergeWarnings();
    }


    /** Look to see if any identical individuals were added in both branches.
     * If so, make a note to change the IDs of the incoming individuals to
     * match the IDs of the individuals in the main revision. */
    private void rematchIncomingIndividuals() {
        changesMadeToIncomingIDs = new HashMap();

        // make a list of the IDs of individuals in the base team list.
        Set<Integer> baseAndMainIndividuals = new HashSet();
        for (TeamMember m : base.getTeamMembers())
            baseAndMainIndividuals.add(m.getId());

        // Look through the individuals that were added in the main branch.
        // make a cross-reference of their names/initials and server identities.
        Map<String, Integer> addedInMain = new HashMap();
        for (TeamMember m : main.getTeamMembers()) {
            if (!baseAndMainIndividuals.contains(m.getId())) {
                addedInMain.put(m.getServerIdentityInfo(), m.getId());
                addedInMain.put(getNameInitialsKey(m), m.getId());
                baseAndMainIndividuals.add(m.getId());
            }
        }
        addedInMain.remove(null);
        baseAndMainIndividuals.remove(-1);

        // Now look through the incoming list.  If any added individuals have
        // matching data, record a change to their IDs.
        for (TeamMember m : incoming.getTeamMembers()) {
            if (!baseAndMainIndividuals.contains(m.getId())) {
                Integer newID = addedInMain.get(m.getServerIdentityInfo());
                if (newID == null)
                    newID = addedInMain.get(getNameInitialsKey(m));
                if (newID != null)
                    changesMadeToIncomingIDs.put(m.getId(), newID);
            }
        }
    }

    private String getNameInitialsKey(TeamMember m) {
        return m.getName().toLowerCase() + "|" + m.getInitials().toLowerCase();
    }


    /**
     * Translate a team member list into a Tree that can be passed to the
     * TreeMerger logic
     */
    private TreeNode<Integer, Map> toTree(TeamMemberList team) {
        Map rootData = new HashMap();
        rootData.put(ZERO_DAY, team.getZeroDay());
        TreeNode<Integer, Map> root = new TreeNode<Integer, Map>(-100, rootData);
        for (TeamMember m : team.getTeamMembers()) {
            int id = m.getId();
            if (changesMadeToIncomingIDs.containsKey(id))
                id = changesMadeToIncomingIDs.get(id);
            Map<String, Object> map = m.getAsMap();
            for (String subteamName : team.getSubteamModel()
                    .getSubteamsForIndividual(m.getId()))
                map.put(SUBTEAM_PREFIX + subteamName, Boolean.TRUE);
            root.addChild(new TreeNode<Integer, Map>(id, map));
        }
        return root;
    }


    /** Build a object that knows how to merge changes to our attributes. */
    private MapContentMerger<Integer> buildMerger() {
        MapContentMerger<Integer> result = new MapContentMerger<Integer>();

        // if both branches edit the color of a team member, don't flag a
        // conflict. Just accept the color of the incoming branch.
        result.addHandler(TeamMember.COLOR_ATTR,
            DefaultAttributeMerger.SILENTLY_PREFER_INCOMING);

        // if both branches edit the server identity of a team member, don't
        // flag a conflict. Just accept the identity of the main branch. The
        // rationale here is that a change to a server identity will also
        // change the display name of the user, and the two values need to stay
        // in sync with each other. Since the merge conflict resolution
        // preferred the name from the main branch, we must prefer the server
        // identity too. We make this preference silent, because the user
        // will already have received an alert about the name conflict, and
        // they won't understand the distinction between name/server identity.
        result.addHandler(TeamMember.SERVER_IDENTITY_ATTR,
            DefaultAttributeMerger.SILENTLY_PREFER_MAIN);

        // if there are multiple conflicting edits to schedule exceptions,
        // just report a single error
        result.addHandler(WeeklySchedule.EXCEPTION_TAG + "_.*",
            new ScheduleExceptionsMerger());

        // register a special merger for any extra attributes that are
        // associated with a team member.
        result.addHandler(TeamMember.EXTRA_ATTRS, new ExtraAttrsMerger());

        return result;
    }


    /**
     * Look through the merged team member list to see if there are any people
     * with duplicated names or initials. If so, modify the person who came from
     * the incoming branch, giving them a unique value.
     */
    private void checkForConflictsInNameOrInitials(
            TreeMerger<Integer, Map> teamMerger) {
        Map<String, TreeNode<Integer, Map>> nameMap = new HashMap();
        Map<String, TreeNode<Integer, Map>> initialsMap = new HashMap();

        TreeNode<Integer, Map> mergedTree = teamMerger.getMergedTree();
        for (TreeNode<Integer, Map> node : mergedTree.getChildren()) {
            checkForAttrConflict(teamMerger, node, TeamMember.NAME_ATTR,
                nameMap, "Attribute.Name_Conflict");
            checkForAttrConflict(teamMerger, node, INITIALS,
                initialsMap, "Attribute.Initials_Conflict");
        }
    }

    /** Look at the node for a single individual to see if it is duplicating
     * the value of a particular attribute.  If so, adjust this node or the
     * node it is conflicting with (depending on which node came from the
     * incoming branch.)
     */
    private void checkForAttrConflict(TreeMerger<Integer, Map> teamMerger,
            TreeNode<Integer, Map> node, String attrName,
            Map<String, TreeNode<Integer, Map>> seenMap, String conflictKey) {

        String attrVal = (String) node.getContent().get(attrName);
        TreeNode<Integer, Map> otherNode = seenMap.get(attrVal);
        if (otherNode == null) {
            // this is the first time we've seen a team member with this value
            // for the attr.  Record it into our the list of items we've seen.
            seenMap.put(attrVal, node);

        } else {
            // Two nodes have the same value for this attribute.  Our save
            // logic never allows this within a single team list, so we know
            // the conflicting values must have come from different branches.
            // Figure out which node came from the main tree, and fix the value
            // in the other node to be unique.
            TreeNode<Integer, Map> nodeToFix;
            TreeNode<Integer, Map> nodeToKeep;

            if (attrValueMatches(teamMerger.getMainTree(), node, attrName)) {
                // if the merged value of this node matches the main branch,
                // our value must have come from there.  The other node is
                // the one that needs fixing.
                nodeToFix = otherNode;
                nodeToKeep = node;
            } else {
                // the merged value of this node does not match the main
                // branch.  This node must have come from the incoming branch,
                // so it is the one that needs to be fixed.
                nodeToFix = node;
                nodeToKeep = otherNode;
            }

            // Find a new/unique value to use for this attribute.
            String newValue = attrVal;
            do {
                newValue += "x";
            } while (seenMap.containsKey(newValue));

            // fix the value in the incoming node.  Note that we must create a
            // new Map to hold the content, because it might be a shared object
            // taken directly from the incoming tree.
            Map newContent = new HashMap(nodeToFix.getContent());
            newContent.put(attrName, newValue);
            nodeToFix.setContent(newContent);
            // register both nodes in our map of seen objects.
            seenMap.put(newValue, nodeToFix);
            seenMap.put(attrVal, nodeToKeep);

            // log a conflict about this problem
            teamMerger.getMergeWarnings().add(
                new MergeWarning<Integer>(Severity.CONFLICT, conflictKey,
                        nodeToKeep.getID(), nodeToFix.getID()));
        }
    }


    /**
     * Look through the merged team member list, and compare each person's
     * final initials to the initials they had in either branch.  When the
     * initials differ, make a note of the remapping for use by our clients.
     */
    private void recordChangesToInitials(TreeMerger<Integer, Map> teamMerger) {
        // if the merged tree ended up using different initials from a
        // particular branch, make a record that the initials in the branch
        // need to change.
        changesNeededToMainInitials = getChangesToInitials(teamMerger
                .getMainTree(), teamMerger.getMergedTree());
        changesNeededToIncomingInitials = getChangesToInitials(teamMerger
                .getIncomingTree(), teamMerger.getMergedTree());
    }

    private Map<String, String> getChangesToInitials(
            TreeNode<Integer, Map> branch, TreeNode<Integer, Map> merged) {
        Map<String, String> result = new HashMap();

        for (TreeNode<Integer, Map> m : merged.getChildren()) {
            Object mergedValue = m.getContent().get(INITIALS);
            Object branchValue = getAttrValueForBranch(branch, m, INITIALS);
            if (mergedValue != null && branchValue != null
                    && !mergedValue.equals(branchValue))
                result.put((String) branchValue, (String) mergedValue);
        }

        return result;
    }


    /**
     * Construct a TeamMemberList object representing the merged data.
     */
    private TeamMemberList getMergedTeam(TreeMerger<Integer, Map> merger) {
        // determine whether the merged zero day came from the main branch or
        // the incoming branch.  Based on the source, make a copy of the
        // corresponding team member list.  This will set the appropriate values
        // for zeroDay, referenceDate, and startOnDayOfWeek
        TeamMemberList result;
        Object mainZeroDay = merger.getMainTree().getContent().get(ZERO_DAY);
        Object mergedZeroDay = merger.getMergedTree().getContent().get(ZERO_DAY);
        if (EQ(mainZeroDay, mergedZeroDay))
            result = new TeamMemberList(main);
        else
            result = new TeamMemberList(incoming);

        // create the new list of team members, and store it in our result.
        List mergedMembers = new ArrayList();
        for (TreeNode<Integer, Map> node : merger.getMergedTree().getChildren()) {
            Map attrs = node.getContent();
            result.getSubteamModel().addSubteamsForIndividual(node.getID(),
                extractSubteamNames(attrs));
            TeamMember m = new TeamMember(node.getID(), attrs,
                    result.getZeroDay());
            mergedMembers.add(m);
        }
        result.setTeamMembers(mergedMembers);
        result.getSubteamModel().copySubteamNameCapitalization(
            main.getSubteamModel());

        return result;
    }

    private List<String> extractSubteamNames(Map<String, Object> attrs) {
        List<String> result = new ArrayList();
        for (Iterator<Entry<String, Object>> i = attrs.entrySet().iterator(); i
                .hasNext();) {
            Entry<String, Object> e = i.next();
            if (e.getKey().startsWith(SUBTEAM_PREFIX)) {
                if (e.getValue() != null)
                    result.add(e.getKey().substring(SUBTEAM_PREFIX.length()));
                i.remove();
            }
        }
        return result;
    }

    /** Find the node in the branch whose ID matches a given node.  Return true
     * if such a node exists, and if it had the same value for a particular
     * attribute as the given node. */
    private boolean attrValueMatches(TreeNode<Integer, Map> branch,
            TreeNode<Integer, Map> mergedNode, String attrName) {
        Object thisValue = mergedNode.getContent().get(attrName);
        Object branchValue = getAttrValueForBranch(branch, mergedNode, attrName);
        return EQ(thisValue, branchValue);
    }

    /** Find the node in the branch whose ID matches a given node.  Then return
     * the value of a particular attribute for that branch-node.  Returns null
     * if the branch did not contain a node with a matching ID. */
    private Object getAttrValueForBranch(TreeNode<Integer, Map> branch,
            TreeNode<Integer, Map> mergedNode, String attrName) {
        int id = mergedNode.getID();
        TreeNode<Integer, Map> branchNode = branch.findNode(id);
        if (branchNode == null) {
            // this branch does not have a node with this ID, so the node
            // we are examining must have been added by the other branch.
            // this branch is not providing any value for the attribute.
            return null;
        } else {
            // return the value this branch is providing for the attribute.
            return branchNode.getContent().get(attrName);
        }
    }


    /**
     * Special class for merging schedule exceptions.  This maps all schedule
     * exception attributes into a single warning key, so that multiple
     * schedule conflicts will only generate a single warning to the user.
     */
    private class ScheduleExceptionsMerger extends
            DefaultAttributeMerger<Integer, Object> {

        ScheduleExceptionsMerger() {
            super(Severity.CONFLICT);
        }

        @Override
        public Object mergeAttribute(Integer nodeID, String attrName,
                Object base, Object main, Object incoming,
                ErrorReporter<Integer> err) {
            return super.mergeAttribute(nodeID, WeeklySchedule.EXCEPTION_TAG,
                base, main, incoming, err);
        }

    }


    /**
     * Special class for merging the "extra attributes" of a team member.
     * This just merges the attributes from both branches, without reporting
     * any errors.
     */
    private class ExtraAttrsMerger implements AttributeMerger<Integer, Map> {

        public Map mergeAttribute(Integer nodeID, String attrName, Map base,
                Map main, Map incoming, ErrorReporter<Integer> err) {
            // preserve all of the extra attributes in either branch,
            // preferring the main branch if there is a conflict.
            Map result = new HashMap();
            result.putAll(incoming);
            result.putAll(main);
            return result;
        }

    }

    static final String ZERO_DAY = "zeroDay";
    private static final String INITIALS = TeamMember.INITIALS_ATTR;
    private static final String SUBTEAM_PREFIX = "In Subteam ";


}
