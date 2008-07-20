// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.tool.probe;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.ui.SelectableHierarchyTree;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.help.PCSH;


public class FilterDialog extends JDialog implements ActionListener {

    DashHierarchy props;
    SelectableHierarchyTree tree;

    ActionListener      l;

    Resources resources = Resources.getDashBundle("PROBE");


    public FilterDialog (DashHierarchy props,
                         Frame probeWindow,
                         ActionListener l) {
        super (probeWindow);
        setTitle(resources.getString("Filter_Window_Title"));
        PCSH.enableHelpKey(this, "UsingProbeTool.filter");

        this.props = props;
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
