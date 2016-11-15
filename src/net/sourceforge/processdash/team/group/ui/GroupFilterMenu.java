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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupEditEvent;
import net.sourceforge.processdash.team.group.UserGroupEditListener;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

public class GroupFilterMenu extends JMenu implements UserGroupEditListener {

    private Icon groupIcon, personIcon;

    private EventListenerList listeners;

    private UserFilter selectedItem;

    public GroupFilterMenu(UserFilter initialSelection) {
        groupIcon = DashboardIconFactory.getGroupIcon();
        personIcon = DashboardIconFactory.getIndividualIcon();
        listeners = new EventListenerList();
        setSelectedItem(initialSelection);

        UserGroupManager.getInstance().addUserGroupEditListener(this);
        getPopupMenu().addPopupMenuListener(new Handler());
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(ChangeListener.class, l);
    }

    public UserFilter getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(UserFilter selection) {
        if (selection instanceof UserGroup) {
            UserGroup g = (UserGroup) selection;
            setIcon(groupIcon);
            setText(g.toString());

        } else if (selection instanceof UserGroupMember) {
            UserGroupMember m = (UserGroupMember) selection;
            setIcon(personIcon);
            setText(m.toString());
        }

        boolean isChange = !NullSafeObjectUtils.EQ(selectedItem, selection);
        this.selectedItem = selection;
        if (isChange) {
            for (ChangeListener l : listeners
                    .getListeners(ChangeListener.class))
                l.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public void userGroupEdited(UserGroupEditEvent e) {
        if (selectedItem instanceof UserGroup) {
            UserGroup selectedGroup = (UserGroup) selectedItem;
            UserGroup changedGroup = e.getGroup();
            if (selectedGroup.getId().equals(changedGroup.getId())) {
                final UserGroup newGroup = (e.isDelete() ? UserGroupManager
                        .getEveryonePseudoGroup() : changedGroup);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setSelectedItem(newGroup);
                    }
                });
            }
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
            UserFilter selectedItem = new UserGroupSelector(
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
