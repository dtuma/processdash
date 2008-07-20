// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class JOptionPaneActionHandler implements ActionListener {

    private Object value;

    public JOptionPaneActionHandler() {
        this(JOptionPane.OK_OPTION);
    }

    public JOptionPaneActionHandler(int value) {
        this(new Integer(value));
    }

    public JOptionPaneActionHandler(Object value) {
        this.value = value;
    }

    public void install(Object component) {
        try {
            Method m = component.getClass().getMethod("addActionListener",
                    new Class[] { ActionListener.class });
            m.invoke(component, new Object[] { this });
        } catch (Exception e) {
            throw new IllegalArgumentException("Object does not support "
                    + "action listeners: " + component);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof Container) {
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
                // the item clicked upon, for example). In the default
                // (double-click) scenario, this is rarely an issue, but it's
                // good to be careful.
                pane.setValue(value);
            }
        });
    }

}
