// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import pspdash.PSPProperties.Event;


public class HierarchyTreeModel extends DefaultTreeModel
    implements PSPProperties.Listener
{
    protected PSPProperties tree;
    protected Map nodes;


    public HierarchyTreeModel(PSPProperties hierarchy) {
        super(new DefaultMutableTreeNode());
        tree = hierarchy;
        nodes = new HashMap();
        setRoot(getNodeForKey(PropertyKey.ROOT));
        tree.addHierarchyListener(this);
    }

    protected synchronized HierarchyTreeNode getNodeForKey(PropertyKey key) {
        if (key == null)
            return null;
        HierarchyTreeNode result = (HierarchyTreeNode) nodes.get(key);
        if (result == null) {
            result = new HierarchyTreeNode(key);
            nodes.put(key, result);
        }
        return result;
    }

    protected PropertyKey key(Object htn) {
        return ((HierarchyTreeNode) htn).key;
    }

    public void hierarchyChanged(Event e) {
        if (e.getSource() == tree)
            reload((TreeNode) getRoot());
    }



    public class HierarchyTreeNode implements TreeNode {

        protected PropertyKey key;

        public HierarchyTreeNode(PropertyKey key) {
            this.key = key;
        }

        public TreeNode getChildAt(int childIndex) {
            return getNodeForKey(tree.getChildKey(key, childIndex));
        }

        public int getChildCount() {
            return tree.getNumChildren(key);
        }

        public TreeNode getParent() {
            if (key.equals(PropertyKey.ROOT))
                return null;
            else
                return getNodeForKey(key.getParent());
        }

        public int getIndex(TreeNode node) {
            if (node == null)
                return -1;

            PropertyKey childKey = ((HierarchyTreeNode) node).key;
            PropertyKey children[] = tree.pget(key).children;
            for (int i = 0; i < children.length; i++) {
                if (childKey.equals(children[i]))
                    return i;
            }
            return -1;
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public boolean isLeaf() {
            return getChildCount() == 0;
        }

        public Enumeration children() {
            return new Enumeration() {
                int next = 0;
                public boolean hasMoreElements() {
                    return next < getChildCount();
                }
                public Object nextElement() {
                    return getChildAt(next++);
                }
            };
        }

        public boolean equals(Object obj) {
            return (obj instanceof HierarchyTreeNode) &&
                ((HierarchyTreeNode) obj).key.equals(key);
        }

        public int hashCode() {
            return key.hashCode();
        }

        public String toString() {
            return key.name();
        }

        public String getPath() {
            return key.path();
        }

    }

}
