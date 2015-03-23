// Copyright (C) 2003-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import net.sourceforge.processdash.hier.*;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.ui.lib.TreeTableModel;



public class HierarchyTreeModel extends DefaultTreeModel
    implements DashHierarchy.Listener, TreeTableModel
{
    protected DashHierarchy tree;
    protected Map nodes;
    protected String rootName;
    protected String columnName;


    public HierarchyTreeModel(DashHierarchy hierarchy) {
        this(hierarchy, null);
    }

    public HierarchyTreeModel(DashHierarchy hierarchy, String colName) {
        super(new DefaultMutableTreeNode());
        tree = hierarchy;
        columnName = colName;
        nodes = new HashMap();
        setRoot(getNodeForKey(PropertyKey.ROOT));
    }

    public String getRootName() {
        return rootName;
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public Class getColumnClass(int column) {
        return TreeTableModel.class;
    }

    public int getColumnCount() {
        return 1;
    }

    public String getColumnName(int column) {
        return columnName;
    }

    public Object getValueAt(Object node, int column) {
        return node.toString();
    }

    public void setValueAt(Object value, Object node, int column) {}

    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    public void addTreeModelListener(TreeModelListener l) {
        if (listenerList.getListenerCount(TreeModelListener.class) == 0)
            tree.addHierarchyListener(this);
        super.addTreeModelListener(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        super.removeTreeModelListener(l);
        if (listenerList.getListenerCount(TreeModelListener.class) == 0)
            tree.removeHierarchyListener(this);
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
            int childCount = tree.getNumChildren(key);
            for (int i = 0; i < childCount; i++) {
                if (childKey.equals(tree.getChildKey(key, i)))
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
            if (rootName != null && key.equals(PropertyKey.ROOT))
                return rootName;
            else
                return key.name();
        }

        public String getPath() {
            return key.path();
        }

    }

}
