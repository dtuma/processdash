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

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeModelListener;
import java.awt.event.*;


public class PropSelectTreeModel extends DefaultTreeModel
{
                                // filter criteria constants
    public static final int NO_FILTER = 0;
    public static final int NO_LEAVES = 1;

    PSPProperties     props;
    int               criteria;

    public PropSelectTreeModel (DefaultMutableTreeNode root,
                                PSPProperties          props,
                                int                    filterCriteria) {
        super (root);
        this.props = props;
        this.criteria = filterCriteria;

        fill (props);
    }

    private boolean matchesFilter (PropertyKey key) {
        boolean ok = true;
        if ((criteria & NO_LEAVES) != 0) {
            ok = ok && (props.getNumChildren (key) > 0);
        }
        return ok;
    }

    private void fill (DefaultMutableTreeNode parent,
                       PSPProperties props, PropertyKey key) {
        DefaultMutableTreeNode child;
        String name;
        JCheckBox jcb;
        int childIndex = 0;

        // read in from properties
        int numChildren = props.getNumChildren (key);

        for (int i=0; i<numChildren; i++) {
            if (matchesFilter (props.getChildKey (key, i))) {
                name = props.getChildName (key, i);
                jcb = new JCheckBox (name);
                jcb.setSelected (true);
                child = new DefaultMutableTreeNode (jcb);
                insertNodeInto(child, parent, childIndex++);
                fill (child, props, props.getChildKey (key, i));
            }
        }
    }

    private void fill (PSPProperties props) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();
        fill (root, props, PropertyKey.ROOT);
    }

    public void setFilterCriteria (int filterCriteria) {
        this.criteria = filterCriteria;
        reload ();
    }

    protected int findMatchingChildIndex (DefaultMutableTreeNode parentNode,
                                          PropertyKey            toFind,
                                          int                    startIndex) {
        JCheckBox jcb;
        String name = toFind.name();
        int numChildren = getChildCount (parentNode);
        for (int ii = startIndex; ii < numChildren; ii++) {
            jcb = (JCheckBox)
                ((DefaultMutableTreeNode) getChild (parentNode, ii)).getUserObject();
            if (name.equals (jcb.getText()))
                return ii;
        }
        return -1;
    }

    // Assumed:
    // 1) The node ordering has not been changed since initial 'fill'.
    // 2) nodes to be added will be set to their parent's "selected" state.
    // 3) node & key parameters *match* (if a parent's name changes, children
    //    will get default isSelected attribute).
    private void reload (DefaultMutableTreeNode node,
                         PropertyKey            key) {
        DefaultMutableTreeNode child;
        int childIndex = 0, matchIndex;
        PropertyKey childKey;
        String name;
        JCheckBox jcbParent = null, jcb;
        boolean defaultState = false;

        int numChildren = props.getNumChildren (key);
        for (int ii=0; ii<numChildren; ii++) {
            childKey = props.getChildKey (key, ii);
            matchIndex = findMatchingChildIndex (node, childKey, childIndex);

            // take care of nodes deleted via the PropertyFrame dialog
            if (matchIndex > childIndex) {
                for (int jj = matchIndex - 1; jj >= childIndex; jj--) {
                    removeNodeFromParent ((MutableTreeNode)getChild (node, jj));
                }
                                        // now recalc matchIndex (sibs deleted)
                matchIndex = findMatchingChildIndex (node, childKey, childIndex);
            }

            if (matchesFilter (childKey)) {
                if (matchIndex == -1) {
                    if (jcbParent == null) { // only do this once
                        jcbParent = (JCheckBox)node.getUserObject();
                        defaultState = jcbParent.isSelected();
                    }
                    name = childKey.name();
                    jcb = new JCheckBox (name);
                    jcb.setSelected (defaultState);
                    child = new DefaultMutableTreeNode (jcb);
                    insertNodeInto(child, node, childIndex);
                }
                child = (DefaultMutableTreeNode)getChild (node, childIndex);
                reload (child, props.getChildKey(key, childIndex));
                childIndex++;
            } else {
                if (matchIndex != -1) {
                    removeNodeFromParent ((MutableTreeNode)getChild (node, matchIndex));
                }
            }
        }
        while (getChildCount (node) > childIndex)
            removeNodeFromParent((MutableTreeNode)getChild
                                 (node, getChildCount (node) - 1));
    }

    public void reload () {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)getRoot();
        if (root != null)		// make sure the root node is not null!
            reload (root, PropertyKey.ROOT);
    }

    public PropertyKey getPropKey (PSPProperties props, Object [] path) {
        PropertyKey key = PropertyKey.ROOT;
        int index;
        if (path != null)
            for (int i = 1; i < path.length; i++) {
                index = getIndexOfChild (path [i - 1], path [i]);
                key = props.getChildKey (key, index);
            }
        return key;
    }

}
