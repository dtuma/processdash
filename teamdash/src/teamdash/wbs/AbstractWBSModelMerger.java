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

package teamdash.wbs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.AttributeMerger;
import teamdash.merge.DefaultAttributeMerger;
import teamdash.merge.MapContentMerger;
import teamdash.merge.MergeWarning;
import teamdash.merge.ModelType;
import teamdash.merge.TreeDiff;
import teamdash.merge.TreeMerger;
import teamdash.merge.TreeNode;
import teamdash.merge.ui.MergeConflictNotification;

public abstract class AbstractWBSModelMerger<W extends WBSModel> {

    W base;

    W main;

    W incoming;

    MapContentMerger<Integer> contentMerger;

    TreeMerger<Integer, WBSNodeContent> treeMerger;

    W merged;

    protected AbstractWBSModelMerger(W base, W main, W incoming) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;
        this.contentMerger = new MapContentMerger<Integer>();
        this.contentMerger.setMapClass(WBSNodeContent.class);
    }

    public W getBase() {
        return base;
    }

    public W getMain() {
        return main;
    }

    public W getIncoming() {
        return incoming;
    }

    public W getMerged() {
        return merged;
    }

    public Set<MergeWarning<Integer>> getMergeWarnings() {
        if (treeMerger == null)
            return Collections.EMPTY_SET;
        else
            return treeMerger.getMergeWarnings();
    }

    public List<MergeConflictNotification> getConflictNotifications() {
        if (treeMerger == null)
            return Collections.EMPTY_LIST;

        List<MergeConflictNotification> result =
            WBSModelMergeConflictNotificationFactory.createAll(this);

        return result;
    }

    public Set<Integer> getMergedUndeletedNodeIDs() {
        if (treeMerger == null)
            return Collections.EMPTY_SET;
        else
            return treeMerger.getMergedUndeletedNodeIDs();
    }

    public void run() {
        if (main.isEqualTo(base)) {
            merged = incoming;

        } else if (incoming.isEqualTo(base)) {
            merged = main;

        } else {
            treeMerger = new TreeMerger(buildTree(base.getRoot()), //
                    buildTree(main.getRoot()), //
                    buildTree(incoming.getRoot()), //
                    contentMerger);
            treeMerger.run();
            merged = buildWBS(treeMerger.getMergedTree());
        }
    }

    public TreeDiff<Integer, WBSNodeContent> getMainDiff() {
        return new TreeDiff(buildTree(base.getRoot()),
                buildTree(main.getRoot()), contentMerger);
    }

    protected TreeNode<Integer, WBSNodeContent> buildTree(WBSNode node) {
        int nodeId = node.getTreeNodeID();
        WBSNodeContent content = new WBSNodeContent(node);
        tweakTreeNodeContent(content);
        TreeNode<Integer, WBSNodeContent> result = new TreeNode(nodeId, content);
        for (WBSNode child : node.getWbsModel().getChildren(node))
            result.addChild(buildTree(child));
        return result;
    }

    protected void tweakTreeNodeContent(WBSNodeContent content) {}

    protected W buildWBS(TreeNode<Integer, WBSNodeContent> treeNode) {
        // Create a new, empty WBS model
        W model = createWbsModel();
        model.deleteNodes(Arrays.asList(model.getDescendants(model.getRoot())));

        // modify the name and attributes of the root node of the WBS
        treeNode.getContent().storeData(model.getRoot());

        // add children (recursively) to the WBS
        buildWBSChildren(model, treeNode, 1);
        model.recalcRows();

        // calculate the max ID in use, and update the root node accordingly
        int maxID = 1;
        for (WBSNode node : model.getDescendants(model.getRoot()))
            maxID = Math.max(maxID, node.getUniqueID());
        model.getRoot().setUniqueID(maxID+1);

        // return the result
        return model;
    }

    protected abstract W createWbsModel();

    protected abstract ModelType getModelType();

    protected void ignoreAttributeConflicts(String... attributeNames) {
        PatternList pattern = new PatternList();
        for (String attr : attributeNames) {
            if (isRegexp(attr))
                pattern.addRegexp(attr);
            else
                pattern.addLiteralEquals(attr);
        }
        contentMerger.addHandler(pattern, SILENTLY_PREFER_MAIN);
    }

    private boolean isRegexp(String s) {
        if (s.startsWith("^")) return true;
        if (s.endsWith("$")) return true;
        if (s.indexOf('*') != -1) return true;
        return false;
    }

    private void buildWBSChildren(W model,
            TreeNode<Integer, WBSNodeContent> treeNode, int depth) {
        // iterate over the children of this tree node.
        for (TreeNode<Integer, WBSNodeContent> treeChild : treeNode.getChildren()) {
            // build a WBSNode for each child, and add it to the WBS.
            WBSNode wbsChild = new WBSNode(model, "x", "x", depth, false);
            treeChild.getContent().storeData(wbsChild);
            wbsChild.setUniqueID(treeChild.getID());
            model.addImpl(wbsChild);

            // recursively add the children of this child.
            buildWBSChildren(model, treeChild, depth+1);
        }
    }


    public static class WBSNodeContent extends HashMap<String, String> {

        WBSNode node;

        boolean readOnly;

        public WBSNodeContent() {}

        public WBSNodeContent(WBSNode node) {
            this.node = node;
            this.readOnly = node.isReadOnly();
            put(NODE_NAME, node.getName());
            put(NODE_TYPE, node.getType());

            Map<String, Object> attrs = node.getAttributeMap(true, true);
            for (Map.Entry<String, Object> e : attrs.entrySet()) {
                String attrName = e.getKey();
                Object value = e.getValue();
                if (value != null) {
                    String valueStr = value.toString();
                    if (valueStr != null && valueStr.length() > 0)
                        put(attrName, valueStr);
                }
            }
        }

        public WBSNode getWBSNode() {
            return node;
        }

        public void storeData(WBSNode dest) {
            for (Map.Entry<String, String> e : entrySet()) {
                String attrName = e.getKey();
                String value = e.getValue();
                if (attrName.equals(NODE_NAME))
                    dest.setName((String) value);
                else if (attrName.equals(NODE_TYPE))
                    dest.setType((String) value);
                else
                    dest.setAttribute(attrName, value);
            }
            dest.setReadOnly(this.readOnly);
        }
    }

    public static final String NODE_NAME = "WBSNode_Name";
    public static final String NODE_TYPE = "WBSNode_Type";

    protected static final AttributeMerger SILENTLY_PREFER_MAIN =
        DefaultAttributeMerger.SILENTLY_PREFER_MAIN;

}
