// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import static teamdash.wbs.AbstractWBSModelMerger.NODE_NAME;
import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_ASSIGNED_WITH_ZERO_SUFFIX;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teamdash.merge.TreeNode;
import teamdash.merge.TreeNodeChange;
import teamdash.merge.TreeNodeChange.Type;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamTimeColumn;

public class ProjectChangeListFactory extends ProjectDiff {

    public ProjectChangeListFactory(ProjectHistory hist, Object versionA,
            Object versionB, ProjectDiff other) throws IOException {
        super(hist, versionA, versionB, other);

        WBSDiffCalc wbsDiffCalc = new WBSDiffCalc();
        diff = wbsDiffCalc.getMainDiff();
    }

    public List<ProjectChange> getChanges() {
        Map<Integer, ProjectWbsNodeChange> nodeChanges = new HashMap();
        Map<Integer, ProjectWbsTimeChange> timeChanges = new HashMap();

        for (TreeNodeChange<Integer, WBSNodeContent> tnc : diff.getChanges()) {
            switch (tnc.getType()) {
            case Add:
                addNodeChange(tnc, nodeChanges, wbsB);
                break;
            case Delete:
                addNodeChange(tnc, nodeChanges, wbsA);
                break;
            case Move:
                addNodeMove(tnc, nodeChanges);
                break;
            case Reorder:
                break;
            case Edit:
                buildChangesForEditedNode(tnc, nodeChanges, timeChanges);
                break;
            }
        }

        summarizeTimeChanges(timeChanges);

        List<ProjectChange> result = new ArrayList<ProjectChange>();
        result.addAll(timeChanges.values());
        result.addAll(nodeChanges.values());
        if (result.size() > 1) {
            // group changes by author, then sort by WBS node within each author
            // group. (The sort calls are arranged in reverse order to let the
            // later sorts drive the overriding grouping. Since the sort
            // function is stable, earlier sort orderings will be preserved.)
            Collections.sort(result, projectChangeComparator);
            Collections.sort(result, ProjectChange.AUTHOR_COMPARATOR);
        }
        return result;
    }

    private void addNodeChange(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges, WBSModel wbs) {
        Type type = tnc.getType();
        Integer parentID = tnc.getParentID();
        if (diff != null && diff.getChangedNodeIDs(type).contains(parentID)) {
            // When an entire branch of the tree was added or deleted, we
            // only want to report the root node of the change.
            return;
        }

        addNodeChange(tnc, nodeChanges, wbs, type);
    }

    private void addNodeMove(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges) {
        Integer nodeID = tnc.getNodeID();
        WBSNode node = wbsA.getNodeMap().get(nodeID);
        WBSNode oldParent = wbsA.getParent(node);
        Object changeType = new ProjectWbsNodeChange.Moved(oldParent);
        addNodeChange(tnc, nodeChanges, wbsB, changeType);
    }

    private void buildChangesForEditedNode(
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges,
            Map<Integer, ProjectWbsTimeChange> timeChanges) {
        Integer nodeID = tnc.getNodeID();
        if (nodeID < 0)
            return; // don't look for edits on the root node.

        WBSNodeContent base = diff.getBaseRoot().findNode(nodeID).getContent();
        WBSNodeContent mod = tnc.getNode().getContent();

        Set<String> attrNames = new HashSet<String>(base.keySet());
        attrNames.addAll(mod.keySet());
        boolean sawTimeChange = false;
        for (String attr : attrNames) {
            String baseVal = base.get(attr);
            String modVal = mod.get(attr);
            if (baseVal != null && baseVal.equals(modVal)) {
                // no change in this attribute value
            } else if (NODE_NAME.equals(attr)) {
                Object changeType = new ProjectWbsNodeChange.Renamed(baseVal);
                addNodeChange(tnc, nodeChanges, wbsB, changeType);
            } else if (attr.endsWith(TEAM_MEMBER_ASSIGNED_WITH_ZERO_SUFFIX)
                    && memberZeroAttrs.containsValue(attr)) {
                sawTimeChange = true;
            } else if (indivTimeAttrs.contains(attr)
                    || TeamTimeColumn.TEAM_TIME_ATTR.equals(attr)) {
                sawTimeChange = true;
            }
        }

        if (sawTimeChange) {
            WBSNode node = wbsB.getNodeMap().get(nodeID);
            timeChanges.put(nodeID, new ProjectWbsTimeChange(node, base, mod,
                    indivTimeAttrs, memberZeroAttrs, teamMemberNames, author,
                    timestamp));
        }
    }

    private void addNodeChange(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges, WBSModel wbs,
            Object changeType) {
        Integer parentID = tnc.getParentID();
        Integer nodeID = tnc.getNodeID();
        WBSNode parent = wbs.getNodeMap().get(parentID);
        WBSNode node = wbs.getNodeMap().get(nodeID);
        if (node == null || parent == null)
            return; // shouldn't happen

        ProjectWbsNodeChange result = nodeChanges.get(parentID);
        if (result != null) {
            result.addChild(node, changeType);
        } else {
            result = new ProjectWbsNodeChange(parent, node, changeType,
                    indivTimeAttrs, memberZeroAttrs, teamMemberNames, author,
                    timestamp, wbsNodeComparator);
            nodeChanges.put(parentID, result);
        }
    }

    private void summarizeTimeChanges(
            Map<Integer, ProjectWbsTimeChange> timeChanges) {
        summarizeTimeChanges(timeChanges, diff.getModifiedRoot());
    }

    private boolean summarizeTimeChanges(
            Map<Integer, ProjectWbsTimeChange> timeChanges,
            TreeNode<Integer, WBSNodeContent> node) {

        Map<Integer, ProjectWbsTimeChange> childChanges = new HashMap();
        for (TreeNode<Integer, WBSNodeContent> child : node.getChildren()) {
            // recursively summarize time changes for this branch of the tree
            boolean childIsZero = summarizeTimeChanges(timeChanges, child);

            if (childChanges != null) {
                Integer childID = child.getID();
                ProjectWbsTimeChange childChange = timeChanges.get(childID);
                if (childChange != null)
                    // collect the changes for all of our children
                    childChanges.put(childID, childChange);

                else if (!childIsZero)
                    // if this child has time but no time change, do not
                    // consider creating a summary at the parent node level
                    childChanges = null;
            }
        }

        // if we found a child with no time changes, don't summarize this node
        if (childChanges == null)
            return false;

        if (childChanges.isEmpty()) {
            // If this is a parent node over a branch with zero time, return
            // true to indicate that this node is zero as well.
            if (!node.getChildren().isEmpty())
                return true;

            // this is a leaf node. Return true if we have zero time.
            ProjectWbsTimeChange test = new ProjectWbsTimeChange(null,
                    node.getContent(), node.getContent(), indivTimeAttrs,
                    memberZeroAttrs, teamMemberNames, author, timestamp);
            return test.getNewTotalTime() == 0;
        }

        // if we only found a single child, there is no need to summarize it.
        // Instead, adopt its change as our own to allow our parent's potential
        // summarization to succeed.
        if (childChanges.size() == 1) {
            Integer childID = childChanges.keySet().iterator().next();
            ProjectWbsTimeChange single = timeChanges.remove(childID);
            timeChanges.put(node.getID(), single);
            return false;
        }

        // if all of our children had time changes, this is indicative of a
        // top-down edit (such as scaling, task assignment, etc). create a
        // summarized change to represent the time changes that have occurred
        // in this branch of the tree.
        WBSNode wbsNode = node.getContent().getWBSNode();
        ProjectWbsTimeChange summarized = new ProjectWbsTimeChange(wbsNode,
                new ArrayList(childChanges.values()), indivTimeAttrs,
                memberZeroAttrs, teamMemberNames, author, timestamp);
        timeChanges.put(node.getID(), summarized);
        // remove the children's now-redundant changes from the change list
        for (Integer childID : childChanges.keySet())
            timeChanges.remove(childID);
        return false;
    }


    private Comparator<ProjectChange> projectChangeComparator = new Comparator<ProjectChange>() {
        public int compare(ProjectChange a, ProjectChange b) {
            // At the moment, we are only reporting WBS changes. If we report
            // other changes (milestones, workflows, etc) in the future, this
            // logic will have to handle those cases too.
            ProjectWbsChange wa = (ProjectWbsChange) a;
            ProjectWbsChange wb = (ProjectWbsChange) b;
            return wbsNodeComparator.compare(wa.getNode(), wb.getNode());
        }
    };



    public static ProjectChangeList getChanges(ProjectHistory hist,
            Date onOrBeforeDate, int minNumChanges, boolean forceFullDays,
            boolean mergeConsecutiveChangesFromSameAuthor) throws IOException {
        ProjectChangeList result = new ProjectChangeList();
        List versions = hist.getVersions();
        String lastDateStr = null;
        ProjectDiff lastDiff = null;
        for (int i = versions.size(); i-- > 1;) {
            Object oneVersion = versions.get(i);
            Date versionDate = hist.getVersionDate(oneVersion);
            if (onOrBeforeDate != null && versionDate.after(onOrBeforeDate))
                continue;

            String thisDateStr = ProjectChange.DATE_FMT.format(versionDate);
            if (minNumChanges > 0 && result.size() >= minNumChanges
                    && !(forceFullDays && thisDateStr.equals(lastDateStr))) {
                result.setFollowupTimestamp(versionDate);
                return result;
            }

            Object prevVersion = versions.get(i - 1);
            if (mergeConsecutiveChangesFromSameAuthor) {
                String thisAuthor = hist.getVersionAuthor(oneVersion);
                while (thisAuthor.equals(hist.getVersionAuthor(prevVersion))
                        && thisDateStr.equals(ProjectChange.DATE_FMT
                                .format(hist.getVersionDate(prevVersion)))
                        && i > 1) {
                    prevVersion = versions.get(--i - 1);
                }
            }

            try {
                ProjectChangeListFactory diff = new ProjectChangeListFactory(
                        hist, prevVersion, oneVersion, lastDiff);
                result.addAll(diff.getChanges());
                lastDiff = diff;
            } catch (FileNotFoundException fnfe) {
                // the change history file can sometimes contain more history
                // than is actually stored in the PDES (for example, when a
                // legacy project has been migrated into the PDES). In these
                // cases, scrolling back far enough will generate a "file not
                // found" exception. Catch this exception and treat it as the
                // end of searchable history.
                break;
            }
            lastDateStr = thisDateStr;
        }
        return result;
    }

}
