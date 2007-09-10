// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
            else if (item instanceof JMenuItem)
                add(new DuplicatedMenuItem(((JMenuItem) item)));
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
