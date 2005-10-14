// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.log.time;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;


public class MockActiveTaskModel implements ActiveTaskModel {

    DashHierarchy hierarchy;
    PropertyKey node = null;
    PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    public MockActiveTaskModel(DashHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public PropertyKey getNode() {
        return node;
    }

    public boolean setNode(PropertyKey node) {
        if (node != null && hierarchy.containsKey(node)) {
            PropertyKey oldNode = this.node;
            this.node = node;
            propSupport.firePropertyChange("node", oldNode, node);
            return true;
        } else {
            return false;
        }
    }

    public String getPath() {
        return (node == null ? null : node.path());
    }

    public boolean setPath(String path) {
        return setNode(hierarchy.findExistingKey(path));
    }

    public boolean setPhase(String phase) {
        PropertyKey parent = node.getParent();
        for (int i = hierarchy.getNumChildren(parent);  i-- > 0; )
            if (phase.equals(hierarchy.getChildName(parent, i)))
                return setNode(hierarchy.getChildKey(parent, i));

        return false;
    }

    public boolean setNextPhase() {
        PropertyKey parent = node.getParent();
        PropertyKey nextNode = null;
        for (int i = hierarchy.getNumChildren(parent);  i-- > 0; ) {
            PropertyKey childKey = hierarchy.getChildKey(parent, i);
            if (node.equals(childKey))
                return setNode(nextNode);
            else
                nextNode = childKey;
        }

        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propSupport.removePropertyChangeListener(l);
    }

}
