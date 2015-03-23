// Copyright (C) 2007-2013 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class DuplicatedMenu extends Menu {

    public DuplicatedMenu(JMenu source) {
        this(source.getText(), source);
    }

    public DuplicatedMenu(String label, JMenu source) {
        super(label);

        for (int i = 0; i < source.getMenuComponentCount(); i++) {
            Component item = source.getMenuComponent(i);
            if (item instanceof JMenu)
                add(new DuplicatedMenu((JMenu) item));
            else if (item instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) item;
                if (menuItem.isVisible())
                    add(new DuplicatedMenuItem(menuItem));
            }
        }
    }

    private class DuplicatedMenuItem extends MenuItem {

        JMenuItem target;

        public DuplicatedMenuItem(JMenuItem target) {
            super(target.getText());
            this.target = target;
            addActionListener(DISPATCHER);
        }

        public void delegate() {
            target.doClick();
        }

    }


    private static ActionListener DISPATCHER = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            ((DuplicatedMenuItem) e.getSource()).delegate();
        }

    };
}
