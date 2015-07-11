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

import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX;
import static teamdash.wbs.columns.TeamTimeColumn.TEAM_TIME_ATTR;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ModelType;
import teamdash.merge.TreeDiff;
import teamdash.merge.TreeNodeChange;
import teamdash.merge.TreeNodeChange.Type;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class BlameDataFactory extends ProjectDiff {

    private BlamePoint blamePoint;

    public BlameDataFactory(ProjectHistory hist, Object versionA,
            Object versionB, ProjectDiff other) throws IOException {
        super(hist, versionA, versionB, other);

        WBSBlameCalc wbsBlameCalc = new WBSBlameCalc();
        diff = wbsBlameCalc.getMainDiff();

        blamePoint = new BlamePoint(hist.getVersionDate(versionB),
                hist.getVersionAuthor(versionB));
    }

    public void addChanges(BlameData blameData) {
        BlameModelData wbsData = blameData.getOrCreate(ModelType.Wbs);
        maybeRemapInitials(wbsData);
        addChanges(wbsData, diff, wbsA, wbsB);
    }

    private void maybeRemapInitials(BlameModelData wbsData) {
        if (changedInitialAttrs != null)
            for (BlameNodeData node : wbsData.values())
                node.remapInitials(changedInitialAttrs);
    }

    private void addChanges(BlameModelData modelData,
            TreeDiff<Integer, WBSNodeContent> diff, WBSModel modelA,
            WBSModel modelB) {
        for (TreeNodeChange<Integer, WBSNodeContent> tnc : diff.getChanges()) {
            switch (tnc.getType()) {
            case Add:
                recordNodeAdded(modelData, tnc);
                break;
            case Delete:
                recordNodeDeleted(modelData, tnc, diff, modelA);
                break;
            case Move:
                recordNodeMoved(modelData, tnc, modelA, modelB);
                break;
            case Reorder:
                break;
            case Edit:
                recordNodeEdited(modelData, tnc, diff);
                break;
            }
        }
    }

    private void recordNodeAdded(BlameModelData modelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc) {
        BlameNodeData nodeData = modelData.getNodeData(tnc.getNodeID());
        nodeData.setAddedBy(blamePoint);
    }

    private void recordNodeDeleted(BlameModelData modelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            TreeDiff<Integer, WBSNodeContent> diff, WBSModel modelA) {

        // since the node has been removed, there won't be a row in the GUI for
        // this node where we can display any past blame changes. Remove its
        // blame data entry from our collection.
        Integer nodeID = tnc.getNodeID();
        BlameNodeData removedNodeData = modelData.remove(nodeID);

        // if this node was added and then deleted within the time period
        // of our blame analysis, treat these as a no-op, and don't report
        // any details about this transient node to the user.
        if (removedNodeData != null && removedNodeData.getAddedBy() != null)
            return;

        // when an entire branch of the tree was deleted, we only need to report
        // the deletion of the branch's top-level parent.
        Integer parentID = tnc.getParentID();
        if (diff.getChangedNodeIDs(Type.Delete).contains(parentID))
            return;

        // Record an entry on the parent that this child was deleted.
        WBSNode deletedNode = modelA.getNodeMap().get(nodeID);
        BlameNodeData parentNodeData = modelData.getNodeData(parentID);
        parentNodeData.addDeletedChild(deletedNode, blamePoint);
    }

    private void recordNodeMoved(BlameModelData modelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc, WBSModel modelA,
            WBSModel modelB) {
        Integer nodeID = tnc.getNodeID();
        String oldParentName = getParentName(modelA, nodeID);
        String newParentName = getParentName(modelB, nodeID);
        BlameNodeData nodeData = modelData.getNodeData(nodeID);
        nodeData.addParentPathChange(blamePoint, oldParentName, newParentName);
    }

    private String getParentName(WBSModel model, Integer nodeID) {
        WBSNode node = model.getNodeMap().get(nodeID);
        WBSNode parent = model.getParent(node);
        String parentName = model.getFullName(parent);
        return parentName;
    }

    private void recordNodeEdited(BlameModelData modelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            TreeDiff<Integer, WBSNodeContent> diff) {
        Integer nodeID = tnc.getNodeID();
        WBSNodeContent base = diff.getBaseRoot().findNode(nodeID).getContent();
        WBSNodeContent mod = tnc.getNode().getContent();
        BlameNodeData nodeData = modelData.getNodeData(nodeID);
        boolean isWBS = (diff == this.diff);

        Set<String> attrNames = new HashSet<String>(base.keySet());
        attrNames.addAll(mod.keySet());
        Set<String> indivTimeAuthors = null;
        boolean totalTimeExplicitlyChanged = false;
        for (String attr : attrNames) {
            String baseVal = base.get(attr);
            String modVal = mod.get(attr);
            if (NullSafeObjectUtils.EQ(baseVal, modVal)) {
                // no changes in this attr

            } else if (isWBS && attr.equals(TEAM_TIME_ATTR)) {
                // the team time value might have been explicitly changed
                baseVal = base.get(SAVED_TEAM_TIME_ATTR);
                modVal = mod.get(SAVED_TEAM_TIME_ATTR);
                if (!NullSafeObjectUtils.EQ(baseVal, modVal)) {
                    totalTimeExplicitlyChanged = true;
                    nodeData.addAttributeChange(attr, blamePoint, baseVal,
                        modVal);
                }

            } else if (isWBS && attr.endsWith(TEAM_MEMBER_TIME_SUFFIX)) {
                // team member time changes can be reverse-synced, so we must
                // determine the appropriate author for this change.
                BlamePoint timeAuthor = getTimeAuthor(mod, attr);
                if (indivTimeAuthors == null)
                    indivTimeAuthors = new TreeSet<String>();
                indivTimeAuthors.add(timeAuthor.getAuthor());
                nodeData.addAttributeChange(attr, timeAuthor, baseVal, modVal);

            } else {
                // record an attribute change for this node
                nodeData.addAttributeChange(attr, blamePoint, baseVal, modVal);
            }
        }

        // if any individuals changed time, record downstream attribute changes
        if (indivTimeAuthors != null) {

            // if we haven't logged a team time change yet, consider logging one
            if (!totalTimeExplicitlyChanged) {
                String oldTime = base.get(SAVED_TEAM_TIME_ATTR);
                String newTime = mod.get(SAVED_TEAM_TIME_ATTR);
                if (!NullSafeObjectUtils.EQ(oldTime, newTime)) {
                    nodeData.addAttributeChange(TEAM_TIME_ATTR,
                        getMergedBlame(indivTimeAuthors), oldTime, newTime);
                }
            }
        }
    }

    private BlamePoint getTimeAuthor(WBSNodeContent mod, String timeAttrName) {
        if (ProjectWbsTimeChange.timeEqualsSyncTime(mod, timeAttrName)) {
            String initials = timeAttrName.substring(timeAttrName.length()
                    - TEAM_MEMBER_TIME_SUFFIX.length());
            String indivName = this.teamMemberNames.get(initials);
            if (indivName != null)
                return new BlamePoint(timestamp, indivName);
        }
        return blamePoint;
    }

    private BlamePoint getMergedBlame(Collection<String> authors) {
        return new BlamePoint(timestamp, StringUtils.join(authors, ", "));
    }

    private static final String SAVED_TEAM_TIME_ATTR = TEAM_TIME_ATTR + "_Save";

    private class WBSBlameCalc extends WBSDiffCalc {
        @Override
        protected void tweakTreeNodeContent(WBSNodeContent content) {
            String teamTime = content.get(TEAM_TIME_ATTR);
            super.tweakTreeNodeContent(content);
            if (teamTime != null)
                content.put(SAVED_TEAM_TIME_ATTR, teamTime);
        }
    }


    public static BlameData getBlameData(ProjectHistory hist, Date onOrAfterDate)
            throws IOException {
        BlameData result = new BlameData();
        List versions = hist.getVersions();
        ProjectDiff prevDiff = null;
        for (int i = 1; i < versions.size(); i++) {
            Object oneVersion = versions.get(i);
            Date versionDate = hist.getVersionDate(oneVersion);
            if (onOrAfterDate != null && versionDate.before(onOrAfterDate))
                continue;

            try {
                Object prevVersion = versions.get(i - 1);
                BlameDataFactory diff = new BlameDataFactory(hist, prevVersion,
                        oneVersion, prevDiff);
                diff.addChanges(result);
                prevDiff = diff;
            } catch (FileNotFoundException fnfe) {
                // the change history file can sometimes contain more history
                // than is actually stored in the PDES (for example, when a
                // legacy project has been migrated into the PDES). In these
                // cases, starting the analysis too far back will generate a
                // "file not found" exception. Catch this exception and skip
                // forward to the next historical change.
                continue;
            }
        }

        return result;
    }

}
