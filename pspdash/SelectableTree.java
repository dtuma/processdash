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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

/** class SelectableTree implements a JTree with JCheckBox displayed nodes.
 *  The User Object for the tree nodes <b>must</B> be a JCheckBox.
 */
public class SelectableTree extends JTree implements MouseListener {

    /** Constructor - tm (required), tcr (may be null)
     *
     */
    public SelectableTree (TreeModel tm,
                           TreeCellRenderer tcr) {
        super (tm);

        if (tcr != null)
            setCellRenderer (tcr);
        else
            setCellRenderer (new SelectableTreeCellRenderer());
        expandRow (0);
        setShowsRootHandles (true);
        setEditable(false);
        addMouseListener (this);
        setRootVisible(false);
        setRowHeight(-1);		// Make tree ask for the height of each row.
    }

    protected void setNodeAndChildrenSelectTo (DefaultMutableTreeNode dmn,
                                               boolean isSelected) {
        DefaultMutableTreeNode child;
        JCheckBox jcb = (JCheckBox)(dmn.getUserObject());
        jcb.setSelected (isSelected);
        Enumeration children = dmn.children();
        while (children.hasMoreElements()) {
            child = (DefaultMutableTreeNode)children.nextElement();
            setNodeAndChildrenSelectTo (child, isSelected);
        }
    }

    /**
     * The next 5 methods implement the MouseListener interface
     * to deal with mouse clicks on the tree.
     */
    public void mouseClicked (MouseEvent e) {
        int selRow = getRowForLocation(e.getX(), e.getY());
        if (selRow == -1)		// if not on row
            return;
        TreePath tp = getPathForLocation(e.getX(), e.getY());

        if (isRowSelected (selRow)) { // deselection
            removeSelectionRow (selRow);
        }
        if (tp == null)
            return;
        DefaultMutableTreeNode dmn =
            (DefaultMutableTreeNode)tp.getLastPathComponent();
        JCheckBox jcb = (JCheckBox)(dmn.getUserObject());
        boolean setTo = !jcb.isSelected();
        //set children of tp to setTo
        setNodeAndChildrenSelectTo (dmn, setTo);
        if (setTo == false) {
            Object [] items = dmn.getUserObjectPath();
            for (int ii = 0; ii < items.length; ii++)
                ((JCheckBox) items[ii]).setSelected (false);
        }
        repaint(getVisibleRect());
    }
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}

}
