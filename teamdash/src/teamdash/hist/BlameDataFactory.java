// Copyright (C) 2015-2016 Tuma Solutions, LLC
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
import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX;
import static teamdash.wbs.columns.TeamTimeColumn.RESOURCES_PSEUDO_ATTR;
import static teamdash.wbs.columns.TeamTimeColumn.TEAM_TIME_ATTR;
import static teamdash.wbs.columns.TopDownBottomUpColumn.TOP_DOWN_ATTR_SUFFIX;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ModelType;
import teamdash.merge.TreeDiff;
import teamdash.merge.TreeNode;
import teamdash.merge.TreeNodeChange;
import teamdash.merge.TreeNodeChange.Type;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.ConflictCapableDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSModelMergeConflictNotificationFactory;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamTimeColumn;

public class BlameDataFactory extends ProjectDiff {

    private BlamePoint blamePoint;

    private Map<String, String> indivTimeColumnIDs;

    private Set<String> wbsAttributesWithSpecialAuthors;

    public BlameDataFactory(ProjectHistory hist, Object versionA,
            Object versionB, ProjectDiff other) throws IOException {
        super(hist, versionA, versionB, other);
        prepareTeamMemberTimeColumnIDs();

        WBSBlameCalc wbsBlameCalc = new WBSBlameCalc();
        diff = wbsBlameCalc.getMainDiff();

        blamePoint = new BlamePoint(hist.getVersionDate(versionB),
                hist.getVersionAuthor(versionB));
    }

    private void prepareTeamMemberTimeColumnIDs() {
        indivTimeColumnIDs = new HashMap<String, String>();
        for (String attr : indivTimeAttrs) {
            String columnID = attr.substring(0, attr.length()
                    - TOP_DOWN_ATTR_SUFFIX.length());
            indivTimeColumnIDs.put(attr, columnID);
        }
    }

    public void addChanges(BlameData blameData, DataTableModel wbsDataModel) {
        BlameModelData wbsBlameData = blameData.getOrCreate(ModelType.Wbs);
        maybeRemapInitials(wbsBlameData);
        wbsAttributesWithSpecialAuthors = new HashSet<String>();
        addChanges(wbsBlameData, wbsDataModel, diff, wbsA, wbsB);
        rollUpBlame(wbsBlameData, diff.getModifiedRoot());
    }

    private void maybeRemapInitials(BlameModelData wbsBlameData) {
        if (changedInitialAttrs != null)
            for (BlameNodeData node : wbsBlameData.values())
                node.remapInitials(changedInitialAttrs);
    }

    private void addChanges(BlameModelData blameModelData,
            DataTableModel dataTableModel,
            TreeDiff<Integer, WBSNodeContent> diff, WBSModel modelA,
            WBSModel modelB) {
        for (TreeNodeChange<Integer, WBSNodeContent> tnc : diff.getChanges()) {
            switch (tnc.getType()) {
            case Add:
                recordNodeAdded(blameModelData, tnc, modelB);
                break;
            case Delete:
                recordNodeDeleted(blameModelData, tnc, diff, modelA);
                break;
            case Move:
                recordNodeMoved(blameModelData, tnc, modelA, modelB);
                break;
            case Reorder:
                break;
            case Edit:
                recordNodeEdited(blameModelData, dataTableModel, tnc, diff);
                break;
            }
        }
    }

    private void recordNodeAdded(BlameModelData blameModelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc, WBSModel wbs) {
        Integer nodeID = tnc.getNodeID();
        WBSNode node = wbs.getNodeMap().get(nodeID);
        String effAuthor = getAuthorOfNodeChange(node, Type.Add, author);
        BlamePoint effBlame = (effAuthor.equals(author) ? blamePoint
                : new BlamePoint(timestamp, effAuthor));

        BlameNodeData nodeData = blameModelData.getNodeData(nodeID);
        nodeData.setAddedBy(effBlame);
    }

    private void recordNodeDeleted(BlameModelData blameModelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            TreeDiff<Integer, WBSNodeContent> diff, WBSModel modelA) {

        // since the node has been removed, there won't be a row in the GUI for
        // this node where we can display any past blame changes. Remove its
        // blame data entry from our collection.
        Integer nodeID = tnc.getNodeID();
        BlameNodeData removedNodeData = blameModelData.remove(nodeID);

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
        BlameNodeData parentNodeData = blameModelData.getNodeData(parentID);
        parentNodeData.addDeletedChild(deletedNode, blamePoint);
    }

    private void recordNodeMoved(BlameModelData blameModelData,
            TreeNodeChange<Integer, WBSNodeContent> tnc, WBSModel modelA,
            WBSModel modelB) {
        Integer nodeID = tnc.getNodeID();
        String oldParentName = getParentName(modelA, nodeID);
        String newParentName = getParentName(modelB, nodeID);
        BlameNodeData nodeData = blameModelData.getNodeData(nodeID);
        nodeData.addParentPathChange(blamePoint, oldParentName, newParentName);
    }

    private String getParentName(WBSModel model, Integer nodeID) {
        WBSNode node = model.getNodeMap().get(nodeID);
        WBSNode parent = model.getParent(node);
        String parentName = model.getFullName(parent);
        return parentName;
    }

    private void recordNodeEdited(BlameModelData blameModelData,
            DataTableModel dataTableModel,
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            TreeDiff<Integer, WBSNodeContent> diff) {
        Integer nodeID = tnc.getNodeID();
        WBSNodeContent base = diff.getBaseRoot().findNode(nodeID).getContent();
        WBSNodeContent mod = tnc.getNode().getContent();
        BlameNodeData nodeData = blameModelData.getNodeData(nodeID);
        boolean isWBS = (diff == this.diff);

        Set<String> attrNames = new HashSet<String>(base.keySet());
        attrNames.addAll(mod.keySet());
        Set<String> indivTimeAuthors = null;
        boolean hasTotalTimeChange = false;
        boolean hasExplicitTotalTimeChange = false;
        for (String attr : attrNames) {
            String baseVal = base.get(attr);
            String modVal = mod.get(attr);
            if (NullSafeObjectUtils.EQ(baseVal, modVal)) {
                // no changes in this attr

            } else if (attr == TEAM_TIME_EXPLICIT_FLAG) {
                // no need to record changes to this pseudo flag

            } else if (isWBS && attr.equals(TEAM_TIME_ATTR)) {
                // the team time value was changed
                hasTotalTimeChange = true;
                // remember whether the team time value was explicit (not just
                // the sum of indiv times)
                if (base.containsKey(TEAM_TIME_EXPLICIT_FLAG)
                        || mod.containsKey(TEAM_TIME_EXPLICIT_FLAG))
                    hasExplicitTotalTimeChange = true;

            } else if (isWBS && attr.endsWith(TEAM_MEMBER_TIME_SUFFIX)) {
                // team member time changes can be reverse-synced, so we must
                // determine the appropriate author for this change.
                BlamePoint timeAuthor = getTimeAuthor(mod, attr);
                if (indivTimeAuthors == null)
                    indivTimeAuthors = new TreeSet<String>();
                indivTimeAuthors.add(timeAuthor.getAuthor());
                String baseDisp = fmtTime(baseVal, UNASSIGNED);
                String modDisp = fmtTime(modVal, UNASSIGNED);
                nodeData.addAttributeChange(attr, indivTimeColumnIDs.get(attr),
                    timeAuthor, baseDisp, modDisp);

            } else if (attr.equals(NODE_NAME)) {
                // the name of this node has changed
                nodeData.addNodeNameChange(blamePoint, baseVal, modVal);

            } else {
                // possibly record an attribute change for this node
                ConflictCapableDataColumn column = //
                WBSModelMergeConflictNotificationFactory
                        .findColumnForAttribute(dataTableModel, attr);
                if (column != null) {
                    String baseDisp = fmt(column.getConflictDisplayValue(
                        baseVal, base.getWBSNode()));
                    String modDisp = fmt(column.getConflictDisplayValue(modVal,
                        mod.getWBSNode()));
                    nodeData.addAttributeChange(attr, column.getColumnID(),
                        blamePoint, baseDisp, modDisp);
                }

            }
        }

        // if any individuals changed time, record a change to the value in the
        // "Assigned To" column
        if (indivTimeAuthors != null && tnc.getNode().getChildren().isEmpty()) {
            String oldWho = getAssignedToString(base, true);
            String newWho = getAssignedToString(mod, false);
            if (!NullSafeObjectUtils.EQ(oldWho, newWho))
                nodeData.addAttributeChange(RESOURCES_PSEUDO_ATTR,
                    TeamTimeColumn.RESOURCES_COL_ID,
                    getMergedBlame(indivTimeAuthors), oldWho, newWho);
        }

        // record a change to the team time attribute if needed
        if (hasTotalTimeChange) {
            BlamePoint blame = blamePoint;
            if (indivTimeAuthors != null) {
                Set<String> allAuthors = indivTimeAuthors;
                if (hasExplicitTotalTimeChange && !allAuthors.contains(author)) {
                    allAuthors = new TreeSet(indivTimeAuthors);
                    allAuthors.add(author);
                }
                blame = getMergedBlame(allAuthors);
            }
            if (blame != blamePoint)
                wbsAttributesWithSpecialAuthors.add(TEAM_TIME_ATTR);

            String oldTime = base.get(TEAM_TIME_ATTR);
            String newTime = mod.get(TEAM_TIME_ATTR);
            nodeData.addAttributeChange(TEAM_TIME_ATTR,
                TeamTimeColumn.COLUMN_ID, blame, fmtTime(oldTime, "0"),
                fmtTime(newTime, "0"));
        }
    }

    private BlamePoint getTimeAuthor(WBSNodeContent mod, String timeAttrName) {
        if (ProjectWbsTimeChange.timeEqualsSyncTime(mod, timeAttrName)) {
            String indivName = teamMemberNames.get(timeAttrName);
            if (indivName != null && !indivName.equals(author)) {
                wbsAttributesWithSpecialAuthors.add(timeAttrName);
                return new BlamePoint(timestamp, indivName);
            }
        }
        return blamePoint;
    }

    private String fmtTime(String rawTime, String defaultVal) {
        if (rawTime == null)
            return defaultVal;
        else
            return NumericDataValue.format(Double.parseDouble(rawTime));
    }

    private String fmt(Object obj) {
        return (obj == null ? "" : obj.toString());
    }

    private static final String UNASSIGNED = resources
            .getString("Blame.Unassigned");

    private BlamePoint getMergedBlame(Set<String> authors) {
        if (authors.size() == 1 && authors.contains(author))
            return blamePoint;
        else
            return new BlamePoint(timestamp, authors);
    }

    private String getAssignedToString(WBSNodeContent data,
            boolean includeDeleted) {
        Set<String> result = new TreeSet();
        getAssignedToTokens(result, data, indivTimeAttrs);
        if (includeDeleted)
            getAssignedToTokens(result, data, deletedIndivAttrs);
        return (result.isEmpty() ? UNASSIGNED : StringUtils.join(result, ", "));
    }

    private void getAssignedToTokens(Set<String> result, WBSNodeContent data,
            Set<String> timeAttrs) {
        for (String indivAttr : timeAttrs) {
            String val = data.get(indivAttr);
            if (val != null) {
                String initials = indivAttr.substring(0, indivAttr.length()
                        - TEAM_MEMBER_TIME_SUFFIX.length());
                String time = NumericDataValue.format(Double.parseDouble(val));
                result.add(initials + "(" + time + ")");
            }
        }
    }

    private void rollUpBlame(BlameModelData wbsBlameData,
            TreeNode<Integer, WBSNodeContent> node) {
        for (String attr : wbsAttributesWithSpecialAuthors)
            rollUpBlame(wbsBlameData, node, attr);
    }

    private BlamePoint rollUpBlame(BlameModelData wbsBlameData,
            TreeNode<Integer, WBSNodeContent> node, String attr) {
        // get the blame annotations for the node in question
        BlameNodeData blameNodeData = wbsBlameData.get(node.getID());
        if (blameNodeData == null || blameNodeData.getAttributes() == null)
            return null;

        // now look up the annotations on this node for the given attribute
        BlameValueList values = blameNodeData.getAttributes().get(attr);
        if (values == null)
            return null;

        // find the change in the value list corresponding to the current diff
        BlamePoint thisBlamePoint = null;
        for (Entry<BlamePoint, String> e : values.entrySet()) {
            if (e.getKey().equals(blamePoint)) {
                thisBlamePoint = e.getKey();
                break;
            }
        }
        if (thisBlamePoint == null)
            return null;

        // Now, recursively find blame data for all of our children
        Set<String> childAuthors = null;
        for (TreeNode<Integer, WBSNodeContent> child : node.getChildren()) {
            BlamePoint childBlame = rollUpBlame(wbsBlameData, child, attr);
            if (childBlame != null) {
                if (childAuthors == null)
                    childAuthors = new TreeSet<String>();
                childAuthors.addAll(childBlame.getAuthors());
            }
        }

        // when we initially created the annotation for this change, we assigned
        // the blame to the default author, but yhat's very possibly incorrect.
        // When a rolled up value changes, that change will be a result of
        // changes in our children. So if we find a list of blame authors for
        // our children, adopt it as the list for the rolled-up value too.
        if (childAuthors != null && !childAuthors.isEmpty())
            thisBlamePoint.setAuthors(childAuthors);

        return thisBlamePoint;
    }

    private static final String TEAM_TIME_EXPLICIT_FLAG = TEAM_TIME_ATTR
            + "_Is_Explicit";

    private class WBSBlameCalc extends WBSDiffCalc {
        @Override
        protected void tweakTreeNodeContent(WBSNodeContent content) {
            String teamTime = content.get(TEAM_TIME_ATTR);
            super.tweakTreeNodeContent(content);
            if (content.get(TEAM_TIME_ATTR) != null)
                content.put(TEAM_TIME_EXPLICIT_FLAG, "t");
            else if (teamTime != null)
                content.put(TEAM_TIME_ATTR, teamTime);
        }

        @Override
        protected TreeNode<Integer, WBSNodeContent> buildTree(WBSNode node) {
            TreeNode<Integer, WBSNodeContent> result = super.buildTree(node);
            calcBottomUpSums(result);
            return result;
        }

        private Map<String, Double> calcBottomUpSums(
                TreeNode<Integer, WBSNodeContent> node) {
            Map<String, Double> result = null;

            // get the bottom up sums for each child, and sum those together
            for (TreeNode<Integer, WBSNodeContent> child : node.getChildren()) {
                Map<String, Double> childSums = calcBottomUpSums(child);
                if (result == null) {
                    result = childSums;

                } else if (childSums != null) {
                    for (Entry<String, Double> e : childSums.entrySet()) {
                        String attr = e.getKey();
                        Double childValue = e.getValue();
                        Double prevTotal = result.get(attr);
                        Double sum = (prevTotal == null ? childValue
                                : prevTotal + childValue);
                        result.put(attr, sum);
                    }
                }
            }

            // now find all top-down values stored on this node
            for (Entry<String, String> e : node.getContent().entrySet()) {
                String attr = e.getKey();
                if (attr.endsWith(TOP_DOWN_ATTR_SUFFIX) && e.getValue() != null) {
                    Double value = Double.valueOf(e.getValue());
                    if (value > 0) {
                        if (result == null)
                            result = new HashMap<String, Double>();
                        if (result.get(attr) != null) {
                            // if we have both a nonzero child sum and a
                            // top-down value, there is a top-down-bottom-up
                            // mismatch. Follow the WBS Editor's lead and
                            // use the nonzero child sum.
                        } else {
                            result.put(attr, value);
                        }
                    }
                }
            }

            // finally, store bottom-up child sums into this node if needed
            if (result != null) {
                for (Entry<String, Double> e : result.entrySet()) {
                    String attr = e.getKey();
                    if (node.getContent().get(attr) == null)
                        node.getContent().put(attr, e.getValue().toString());
                }
            }

            return result;
        }

    }


    public static BlameData getBlameData(ProjectHistory hist,
            Date onOrAfterDate, DataTableModel dataTableModel, Future f)
            throws IOException {
        if (hist instanceof ProjectHistoryBridged)
            ((ProjectHistoryBridged) hist).cacheFileRevisions(onOrAfterDate,
                null);

        BlameData result = new BlameData();
        List versions = hist.getVersions();
        ProjectDiff prevDiff = null;
        for (int i = 1; i < versions.size(); i++) {
            if (f != null && f.isDone())
                return null;

            Object oneVersion = versions.get(i);
            Date versionDate = hist.getVersionDate(oneVersion);
            if (onOrAfterDate != null && versionDate.before(onOrAfterDate))
                continue;

            try {
                Object prevVersion = versions.get(i - 1);
                BlameDataFactory diff = new BlameDataFactory(hist, prevVersion,
                        oneVersion, prevDiff);
                diff.addChanges(result, dataTableModel);
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
