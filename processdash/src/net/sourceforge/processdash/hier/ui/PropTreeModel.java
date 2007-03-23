// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier.ui;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeModelListener;

import net.sourceforge.processdash.hier.*;


public class PropTreeModel extends DefaultTreeModel
{
    TreeModelListener listener;

    private PropertyKey rootKey = PropertyKey.ROOT;

    public PropTreeModel (DefaultMutableTreeNode root,
                          TreeModelListener      l) {
        super (root);
        listener = l;
    }

    public PropertyKey getRootKey() {
        return rootKey;
    }

    public void setRootKey(PropertyKey rootNode) {
        this.rootKey = rootNode;
    }

    public void useTreeModelListener (boolean listen) {
        if (listen)
            addTreeModelListener (listener);
        else
            removeTreeModelListener (listener);
    }

    private void fill (DefaultMutableTreeNode parent,
                       DashHierarchy props, PropertyKey key) {
        int numChildren, i;
        DefaultMutableTreeNode child;

        // read in from properties
        numChildren = props.getNumChildren (key);

        for (i=0; i<numChildren; i++) {
            child = new DefaultMutableTreeNode (props.getChildName (key, i));
            insertNodeInto(child, parent, i);
            fill (child, props, props.getChildKey (key, i));
        }
    }

    public void fill (DashHierarchy props) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        fill (root, props, rootKey);
        root.setUserObject(rootKey.path());
    }

    private void reload (DashHierarchy          props,
                         DefaultMutableTreeNode node,
                         PropertyKey            key) {
        DefaultMutableTreeNode child;
        int childIndex = 0;
        //set the current node name
        Prop value = props.pget (key);
        node.setUserObject(key.name());
        int numPropChildren = value.getNumChildren();
        while (numPropChildren > childIndex) {
            if (getChildCount (node) <= childIndex) {
                child = new DefaultMutableTreeNode ("changeMeLater");
                insertNodeInto(child, node, childIndex);
            } else
                child = (DefaultMutableTreeNode)getChild (node, childIndex);
            reload (props, child, props.getChildKey(key, childIndex));
            childIndex++;
        }
        while (getChildCount (node) > numPropChildren)
            removeNodeFromParent((MutableTreeNode)getChild
                                 (node, getChildCount (node) - 1));
    }

    public void reload (DashHierarchy props) {
        // the root node is assumed to be non-null!
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)getRoot();
        reload (props, root, rootKey);
        root.setUserObject(rootKey.path());
    }

    public PropertyKey getPropKey (DashHierarchy props, Object [] path) {
        PropertyKey key = rootKey;
        if (path != null)
            for (int i = 1; i < path.length; i++) {
                int index = getIndexOfChild (path [i - 1], path [i]);
                key = props.getChildKey (key, index);
            }
        return key;
    }

    public TreeNode getNodeForKey (DashHierarchy props, PropertyKey key) {
        if (key == null)
            return null;
        if (rootKey.equals(key))
            return (TreeNode) getRoot();
        PropertyKey parentKey = key.getParent();
        TreeNode parent = getNodeForKey(props, parentKey);
        PropertyKey childKey;
        for (int i = props.getNumChildren(parentKey);  i-- > 0; ) {
            childKey = props.getChildKey(parentKey, i);
            if (childKey.equals(key))
                return parent.getChildAt(i);
        }
        return null;
    }

    public Object [] getPathToKey (DashHierarchy props, PropertyKey key) {
        return getPathToRoot(getNodeForKey(props, key));
    }

}
