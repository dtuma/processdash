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

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.util.*;

public class FilterDialog extends JDialog implements ActionListener {

    PSPDashboard  parent;
    PSPProperties props;
    SelectableHierarchyTree tree;

    ActionListener      l;

    Resources resources = Resources.getDashBundle("pspdash.PROBE");


    public FilterDialog (PSPDashboard dash,
                         Frame probeWindow,
                         ActionListener l) {
        super (probeWindow);
        setTitle(resources.getString("Filter_Window_Title"));
        PCSH.enableHelpKey(this, "UsingProbeTool.filter");

        parent = dash;
        props = parent.props;
        this.l = l;

        /* Create the tree */
        tree = new SelectableHierarchyTree(props);

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
//    sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        getContentPane().add(sp, "Center");

        Box mainBox = new Box(BoxLayout.Y_AXIS);
        mainBox.add (Box.createVerticalStrut(2));

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add (Box.createGlue());
        JButton button;
        button = new JButton (resources.getString("Apply_Filter_Button"));
        button.setActionCommand("applyFilter");
        button.addActionListener(this);
        buttonBox.add (button);
        buttonBox.add (Box.createGlue());
        button = new JButton (resources.getString("Close"));
        button.setActionCommand("close");
        button.addActionListener(this);
        buttonBox.add (button);

        buttonBox.add (Box.createGlue());
        mainBox.add (buttonBox);
        mainBox.add (Box.createVerticalStrut(2));

        getContentPane().add(mainBox, "South");
        pack();
        show();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("close")) {
            setVisible(false);
        } else if (cmd.equals("applyFilter")) {
            Vector v = tree.getSelectedPaths();

//        System.out.println ("Selected nodes =");
//        for (int jj = 0; jj < v.size(); jj++) {
//        System.out.println ("  " + v.elementAt(jj));
//        }
            if (l != null)
                l.actionPerformed (new ActionEvent (v,
                                                    ActionEvent.ACTION_PERFORMED,
                                                    cmd));
        }
    }

}
