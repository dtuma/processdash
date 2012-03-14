// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeNode<ID, Content> {

    private ID id;

    private Content content;

    private TreeNode<ID, Content> parent;

    private List<TreeNode<ID, Content>> children;

    private List<TreeNode<ID, Content>> childrenReadOnly;

    public TreeNode(ID id, Content content) {
        this.id = id;
        this.content = content;
        this.parent = null;
        this.children = new ArrayList<TreeNode<ID, Content>>();
        this.childrenReadOnly = Collections.unmodifiableList(children);
    }


    /** Get the ID for this node. IDs must be unique within a particular tree. */
    public ID getID() {
        return id;
    }


    /** Get the content that is associated with this node. */
    public Content getContent() {
        return content;
    }


    /** Alter the content that is associated with this node. */
    public void setContent(Content content) {
        this.content = content;
    }


    /** Get the parent of this node, or null if this is the root of the tree. */
    public TreeNode<ID, Content> getParent() {
        return parent;
    }

    /** Get the root of the tree that contains this node. */
    public TreeNode<ID, Content> getRoot() {
        if (parent == null)
            return this;
        else
            return parent.getRoot();
    }


    /**
     * Return the depth of this node. The root node has a depth of 0, its
     * children have a depth of 1, etc.
     */
    public int getDepth() {
        if (parent == null)
            return 0;
        else
            return 1 + parent.getDepth();
    }


    /**
     * Return the list of children under this node. For leaf nodes, this will
     * return the empty list (not null).
     */
    public List<TreeNode<ID, Content>> getChildren() {
        return childrenReadOnly;
    }


    /** Delete this node (and any children) from the tree. */
    public void delete() {
        if (parent != null)
            parent.removeChild(this, true);
    }

    private void removeChild(TreeNode<ID, Content> child, boolean adjustIDMap) {
        // if the child was indeed in our child list,
        int pos = children.indexOf(child);
        if (pos != -1) {
            // remove the child
            children.remove(pos);
            // null out its parent
            child.parent = null;
            // null out the cached predecessor information
            child.predecessors = null;
            clearCachedPredecessorInfoForChildren(pos);

            // fix the ID map for this tree, if necessary
            if (adjustIDMap) {
                TreeNode<ID, Content> root = getRoot();
                if (root.idMap != null) {
                    if (child.children.isEmpty())
                        root.idMap.remove(child.getID());
                    else
                        root.idMap = null;
                }
            }
        }
    }


    /** Append a child to this node, at the end of our child list. */
    public void addChild(TreeNode<ID, Content> child) {
        addChild(child, -1);
    }

    /** Insert a child at a particular position in the child list. */
    public void addChild(TreeNode<ID, Content> child, int pos) {
        addChild(child, pos, true);
    }

    private void addChild(TreeNode<ID, Content> child, int pos,
            boolean adjustIDMap) {
        // remove the child from any former parent
        if (child.parent != null) {
            // if the child already has a parent, we assume that they are
            // in the same tree as this node.  Since the nodes are just being
            // moved/rearranged, we will not have to adjust the ID map.
            adjustIDMap = false;

            // check to see if we are already the parent of this child. If so,
            // then we are performing a reordering operation
            if (child.parent == this) {
                int currPos = children.indexOf(child);
                if (currPos != -1) {
                    if (currPos == pos)
                        // if the child is already in the right position,
                        // nothing needs to be done.
                        return;

                    else if (currPos < pos)
                        // if we are moving the child down in our list, the
                        // 'target' position will change as soon as we remove
                        // the child.  Anticipate that.
                        pos--;
                }
            }

            // remove the child from its parent, wherever that may be.
            child.parent.removeChild(child, false);
        }

        child.parent = this;
        child.predecessors = null;
        if (pos == -1) {
            children.add(child);
        } else {
            children.add(pos, child);
            clearCachedPredecessorInfoForChildren(pos+1);
        }

        // fix the ID map for this tree, if necessary
        if (adjustIDMap) {
            TreeNode<ID, Content> root = getRoot();
            if (root.idMap != null)
                child.addIDsToMap(root.idMap);
        }
    }


    /**
     * Make a deep copy of the nodes in this tree.
     */
    public TreeNode<ID, Content> copyTree() {
        TreeNode<ID, Content> result = new TreeNode(this.id, this.content);

        for (TreeNode<ID, Content> child : this.children)
            result.addChild(child.copyTree(), -1, false);

        return result;
    }


    /** Find the node in this tree that has a particular ID */
    public TreeNode<ID, Content> findNode(ID id) {
        return getIDMap().get(id);
    }


    /**
     * Examine the tree that contains this node, and return a map that can be
     * used to look up any node by its ID.
     */
    public Map<ID, TreeNode<ID, Content>> getIDMap() {
        if (parent != null)
            return parent.getIDMap();
        if (idMap == null)
            buildIDMapForRoot();
        return idMapReadOnly;
    }

    private Map<ID, TreeNode<ID, Content>> idMap = null;
    private Map<ID, TreeNode<ID, Content>> idMapReadOnly = null;

    private void buildIDMapForRoot() {
        Map<ID, TreeNode<ID, Content>> result = new HashMap();
        addIDsToMap(result);
        this.idMap = result;
        this.idMapReadOnly = Collections.unmodifiableMap(result);
    }

    private void addIDsToMap(Map<ID, TreeNode<ID, Content>> dest) {
        dest.put(id, this);
        for (TreeNode<ID, Content> child : children)
            child.addIDsToMap(dest);
    }


    /**
     * @return the IDs of the siblings that precede this node, in the order
     * they appear in the tree
     */
    public List<ID> getPredecessorIDs() {
        if (predecessors == null)
            predecessors = calcPredecessorIDs();
        return predecessors;
    }

    private List<ID> predecessors = null;

    private List<ID> calcPredecessorIDs() {
        TreeNode<ID, Content> parent = getParent();
        if (parent == null)
            return Collections.EMPTY_LIST;

        List<ID> result = new ArrayList();
        for (TreeNode<ID, Content> child : parent.getChildren()) {
            if (child == this)
                break;
            else
                result.add(child.getID());
        }
        return Collections.unmodifiableList(result);
    }

    private void clearCachedPredecessorInfoForChildren(int pos) {
        for (; pos < children.size(); pos++)
            children.get(pos).predecessors = null;
    }


    /**
     * Return a string representation of the node structure of this tree, for
     * debugging purposes
     */
    public String toDebugString() {
        StringBuilder result = new StringBuilder();
        toDebugString(result);
        return result.toString();
    }

    private void toDebugString(StringBuilder buf) {
        buf.append(id);
        for (int i = 0;  i < children.size();  i++) {
            buf.append(i == 0 ? "{" : ",");
            children.get(i).toDebugString(buf);
        }
        if (!children.isEmpty())
            buf.append("}");
    }

}
