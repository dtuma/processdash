// Copyright (C) 2017 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.team.group.GroupPermission.GROUP_PARAM;

import java.awt.Component;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.tool.perm.PermissionEditor;
import net.sourceforge.processdash.ui.lib.BoxUtils;

public class GroupPermissionEditor
        implements PermissionEditor<GroupPermission> {

    @Override
    public Map<String, String> editPermission(GroupPermission p,
            Component parent, boolean isAdd) {
        // retrieve the list of shared groups known to this dashboard. Custom
        // groups are not included, because they can't form the basis of a
        // meaningful cross-user permission grant.
        Vector<UserGroup> groups = new Vector<UserGroup>();
        UserGroupManager mgr = UserGroupManager.getInstance();
        for (UserGroup g : mgr.getGroups().values()) {
            if (!g.isCustom())
                groups.add(g);
        }

        // sort the list, and insert "Everyone" at the beginning.
        Collections.sort(groups);
        groups.add(0, UserGroup.EVERYONE);

        // retrieve the group named in the given permission. If it was a
        // "missing" permission, it will come back with the custom flag set.
        // In that case, add it to the list we'll display in the combo box.
        UserGroup currentGroup = p.getGroup();
        if (currentGroup.isCustom())
            groups.add(currentGroup);

        // create a combo box for selecting a group
        JComboBox<UserGroup> cb = new JComboBox<UserGroup>(groups);
        cb.setSelectedItem(currentGroup);

        // display a user interface for selecting a group
        String title = UserGroupEditor.resources
                .getString(isAdd ? "Add_Permission" : "Edit_Permission");
        String prompt = p.getSpec().getResources().getString("Edit_Prompt");
        Object[] message = { prompt, BoxUtils.hbox(20, cb) };
        int userChoice = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // if the user pressed OK, return the ID of the group they selected.
        // otherwise return null to indicate that they cancelled the operation.
        if (userChoice == JOptionPane.OK_OPTION) {
            UserGroup selected = (UserGroup) cb.getSelectedItem();
            return Collections.singletonMap(GROUP_PARAM, selected.getId());
        } else {
            return null;
        }
    }

}
