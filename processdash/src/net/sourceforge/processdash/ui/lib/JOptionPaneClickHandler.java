// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** This class listens for clicks on JComponents, and makes a selection in
 * a parent JOptionPane.
 * 
 * This helps with a common usability problem.  Often, a JOptionPane will
 * contain a JList or JTree showing several items, inviting the user to select
 * one and click OK.  Instinctively, many users will attempt to doubleclick
 * on one of the items, only to find that nothing happens.  The standard
 * paradigm for JOptionPane will require them to highlight the item, then click
 * the JOptionPane OK button.
 * 
 * This class can be configured to listen for doubleclicks on the JList or
 * JTree, and programmatically simulate an OK button click.  It can also
 * simulate other JOptionPane selections, after any configurable number of
 * mouse clicks.
 *
 * @author Tuma
 *
 */
public class JOptionPaneClickHandler extends MouseAdapter {

    private int clickCount;

    private Object value;

    public JOptionPaneClickHandler() {
        this(2);
    }

    public JOptionPaneClickHandler(int clickCount) {
        this(clickCount, JOptionPane.OK_OPTION);
    }

    public JOptionPaneClickHandler(int clickCount, int value) {
        this(clickCount, new Integer(value));
    }

    public JOptionPaneClickHandler(int clickCount, Object value) {
        this.clickCount = clickCount;
        this.value = value;
    }

    public void install(JComponent component) {
        component.addMouseListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == clickCount
                && e.getSource() instanceof Container) {
            Container comp = (Container) e.getSource();
            while (comp != null) {
                if (comp instanceof JOptionPane) {
                    setValue((JOptionPane) comp);
                    return;
                } else {
                    comp = comp.getParent();
                }
            }
        }
    }

    private void setValue(final JOptionPane pane) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // invoking this later gives the current click event the
                // opportunity to have other necessary effects (like selecting
                // the item clicked upon, for example).  In the default
                // (double-click) scenario, this is rarely an issue, but it's
                // good to be careful.
                pane.setValue(value);
            }
        });
    }

}
