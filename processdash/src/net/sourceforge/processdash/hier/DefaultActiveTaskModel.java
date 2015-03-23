// Copyright (C) 2005-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class DefaultActiveTaskModel implements ActiveTaskModel {

    private DashHierarchy hierarchy;

    private PropertyKey node;

    private PropertyChangeSupport changeSupport;

    public DefaultActiveTaskModel(DashHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        this.changeSupport = new PropertyChangeSupport(this);
        this.node = findSelectedLeaf(PropertyKey.ROOT);
    }

    public PropertyKey getNode() {
        return node;
    }

    public boolean setNode(PropertyKey newNode) {
        if (newNode == null || hierarchy.containsKey(newNode)) {
            newNode = findSelectedLeaf(newNode);
            if (newNode != this.node
                    && (newNode == null || !newNode.equals(this.node))) {
                this.node = newNode;
                updateHierarchySelectionToNode(newNode);
                changeSupport.firePropertyChange(null, null, null);
            }
            return true;
        }
        return false;
    }

    private PropertyKey findSelectedLeaf(PropertyKey node) {
        if (hierarchy.getNumChildren(node) == 0)
            return node;
        else {
            int sel = hierarchy.getSelectedChild(node);
            if (sel < 0 || sel >= hierarchy.getNumChildren(node))
                sel = 0;
            node = hierarchy.getChildKey(node, sel);
            return findSelectedLeaf(node);
        }
    }


    private void updateHierarchySelectionToNode(PropertyKey node) {
        if (node == null)
            return;
        PropertyKey parent = node.getParent();
        if (parent == null)
            return;
        Prop parentProp = hierarchy.pget(parent);
        if (parentProp != null) {
            int numChildren = parentProp.getNumChildren();
            for (int i = 0; i < numChildren; i++) {
                if (node.equals(parentProp.getChild(i))) {
                    parentProp.setSelectedChild(i);
                    break;
                }
            }
        }
        updateHierarchySelectionToNode(parent);
    }

    public String getPath() {
        if (node == null)
            return null;
        else
            return node.path();
    }

    public boolean setPath(String path) {
        PropertyKey key = null;
        if (path != null)
            key = PropertyKey.fromPath(path);
        return setNode(key);
    }

    public boolean setPhase(String phase) {
        if (node == null || phase == null || phase.length() == 0)
            return false;
        return setPhase(node.getParent(), phase);
    }

    private boolean setPhase(PropertyKey parent, String phase) {
        if (parent == null)
            return false;
        Prop parentProp = hierarchy.pget(parent);
        if (parentProp == null)
            return false;

        for (int i = 0; i < parentProp.getNumChildren(); i++) {
            PropertyKey child = parentProp.getChild(i);
            if (phase.equals(child.name()))
                return setNode(child);
        }

        return setPhase(parent.getParent(), phase);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }

}
