// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeModelListener;


public class PropTreeModel extends DefaultTreeModel
{
    TreeModelListener listener;

    public PropTreeModel (DefaultMutableTreeNode root,
                          TreeModelListener      l) {
        super (root);
        listener = l;
    }

    public void useTreeModelListener (boolean listen) {
        if (listen)
            addTreeModelListener (listener);
        else
            removeTreeModelListener (listener);
    }

    private void fill (DefaultMutableTreeNode parent,
                       PSPProperties props, PropertyKey key) {
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

    public void fill (PSPProperties props) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        fill (root, props, PropertyKey.ROOT);
    }

    public void reload (PSPProperties          props,
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

    public void reload (PSPProperties props) {
        // the root node is assumed to be non-null!
        reload (props, (DefaultMutableTreeNode)getRoot(), PropertyKey.ROOT);
    }

    public PropertyKey getPropKey (PSPProperties props, Object [] path) {
        PropertyKey key = PropertyKey.ROOT;
        if (path != null)
            for (int i = 1; i < path.length; i++) {
                int index = getIndexOfChild (path [i - 1], path [i]);
                key = props.getChildKey (key, index);
            }
        return key;
    }

}
