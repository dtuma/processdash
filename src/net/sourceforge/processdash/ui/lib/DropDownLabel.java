// Copyright (C) 2011-2016 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class DropDownLabel extends JPanel {

    private JLabel label;

    private JMenu menu;

    public DropDownLabel() {
        this("");
    }

    public DropDownLabel(String text) {
        super(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        menuBar.setMinimumSize(new Dimension(0, 0));
        menuBar.setPreferredSize(new Dimension(0, 1));
        menuBar.setMaximumSize(new Dimension(0, 100));

        menu = new JMenu();
        menuBar.add(menu);
        add(menuBar, BorderLayout.WEST);

        label = new JLabel(text);
        label.setIcon(new DropDownButton.SmallDownArrow());
        label.setDisabledIcon(new DropDownButton.SmallDisabledDownArrow());
        label.setVerticalAlignment(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.LEFT);
        add(label);

        addMouseListener(new MouseHandler());
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }

    public Icon getIcon() {
        return label.getIcon();
    }

    public JMenu getMenu() {
        return menu;
    }

    public void setEnabled(boolean enabled) {
        label.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return label.isEnabled();
    }

    private class MouseHandler extends MouseAdapter implements Runnable {

        public void mouseClicked(MouseEvent e) {
            if (label.isEnabled())
                SwingUtilities.invokeLater(this);
        }

        public void run() {
            menu.doClick();
        }
    }

}
