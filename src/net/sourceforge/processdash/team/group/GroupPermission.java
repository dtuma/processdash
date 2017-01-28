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

package net.sourceforge.processdash.team.group;

import java.util.Collections;
import java.util.Map;

import net.sourceforge.processdash.team.group.ui.GroupPermissionEditor;
import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.tool.perm.PermissionEditor;

public class GroupPermission extends Permission {

    public static final String GROUP_PARAM = "group";

    public GroupPermission() {}

    public UserGroup getGroup() {
        // retrieve the group named in this permission
        String groupID = getParams().get(GROUP_PARAM);
        if (groupID == null)
            groupID = UserGroup.EVERYONE_ID;
        UserGroup result = UserGroupManager.getInstance().getGroupByID(groupID);

        // if the group was not found, create a "missing" pseudo-group.
        if (result == null)
            result = new UserGroup(getMissingText(), groupID, true,
                    Collections.EMPTY_SET);

        return result;
    }

    @Override
    protected Map<String, String> getDefaultParams() {
        return Collections.singletonMap(GROUP_PARAM, UserGroup.EVERYONE_ID);
    }

    @Override
    protected Map<String, String> getChildParams(Permission parent) {
        if (parent instanceof GroupPermission) {
            return parent.getParams();
        } else {
            return getDefaultParams();
        }
    }

    @Override
    public PermissionEditor getEditor() {
        return new GroupPermissionEditor();
    }

    @Override
    public String toString() {
        // get the ID of the group associated with this permission.
        String groupID = getParams().get(GROUP_PARAM);

        // if this permission is assigned to the "Everyone" group, use a special
        // display string that is tailored to that scenario
        if (groupID == null || UserGroup.EVERYONE_ID.equals(groupID))
            return getSpec().getResources().getString("Description_Everyone");

        // look up the group in question and get its name. If the group does
        // not exist, use error/placeholder text.
        UserGroup g = UserGroupManager.getInstance().getGroupByID(groupID);
        String groupName = (g != null ? g.getDisplayName() : getMissingText());

        // use the group name in a formatted string
        return getSpec().getResources().format("Description_FMT", groupName);
    }

    private String getMissingText() {
        return UserGroup.resources.getString("Missing_Group");
    }

}
