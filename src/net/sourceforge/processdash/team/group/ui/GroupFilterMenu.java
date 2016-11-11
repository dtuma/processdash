// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group.ui;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.DashboardIconFactory;

public class GroupFilterMenu extends JMenu {

    private Icon groupIcon, personIcon;

    public GroupFilterMenu(Object initialSelection) {
        groupIcon = DashboardIconFactory.getGroupIcon();
        personIcon = DashboardIconFactory.getIndividualIcon();
        setSelectedItem(initialSelection);

        getPopupMenu().addPopupMenuListener(new Handler());
    }

    public void setSelectedItem(Object selection) {
        if (selection instanceof UserGroup) {
            UserGroup g = (UserGroup) selection;
            setIcon(groupIcon);
            setText(g.toString());

        } else if (selection instanceof UserGroupMember) {
            UserGroupMember m = (UserGroupMember) selection;
            setIcon(personIcon);
            setText(m.toString());
        }
    }


    private class Handler implements PopupMenuListener, Runnable {

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            SwingUtilities.invokeLater(this);
        }

        @Override
        public void run() {
            getPopupMenu().setVisible(false);
            Object selectedItem = new UserGroupSelector(
                    SwingUtilities.getWindowAncestor(GroupFilterMenu.this),
                    "Filter_Prompt").getSelectedItem();
            setSelectedItem(selectedItem);
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}

    }

}
