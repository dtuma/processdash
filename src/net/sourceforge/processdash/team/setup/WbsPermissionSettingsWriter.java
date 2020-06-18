// Copyright (C) 2017-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.perm.Permission;
import net.sourceforge.processdash.tool.perm.PermissionsChangeEvent;
import net.sourceforge.processdash.tool.perm.PermissionsChangeListener;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.tool.perm.User;
import net.sourceforge.processdash.util.StringUtils;

public class WbsPermissionSettingsWriter
        implements TeamSettingsDataWriter, PermissionsChangeListener {

    private Set<String> wbsPermissionIDs;

    private List<User> allUsers;

    private List<Role> allRoles;


    public WbsPermissionSettingsWriter() {
        wbsPermissionIDs = Collections.unmodifiableSet(PermissionsManager
                .getInstance().getPermissionsImpliedBy("wbs.all"));
        PermissionsManager.getInstance().addPermissionsChangeListener(this);
    }


    @Override
    public String getFormatVersion() {
        return "2.3.1.2";
    }


    @Override
    public Date getDataTimestamp(String projectID) {
        return PermissionsManager.getInstance().getPermissionsTimestamp();
    }


    @Override
    public void writeTeamSettings(String projectID, XmlSerializer xml)
            throws IOException {
        // start a <roles> tag
        xml.startTag(null, ROLES_TAG);

        // gather the list of users known to this team dashboard
        List<User> allUsers = getAllUsers();
        Set<String> usersToWrite = new HashSet();
        for (User u : allUsers)
            usersToWrite.add(u.getUsername());

        // write data for each role
        for (Role r : getAllRoles())
            writeRoleSettings(xml, r, allUsers, usersToWrite);

        // write an empty role for users with no WBS permissions
        if (!usersToWrite.isEmpty())
            writeEmptyRole(xml, usersToWrite);

        // end the </roles> tag
        xml.endTag(null, ROLES_TAG);
    }

    private void writeRoleSettings(XmlSerializer xml, Role r,
            List<User> allUsers, Set<String> usersToWrite) throws IOException {
        // get a list of the active permissions that are assigned to this role
        User prototypeUser = new User(null, null, false,
                Collections.singletonList(r.getId()));
        Set<Permission> perms = PermissionsManager.getInstance()
                .getPermissionsForUser(prototypeUser, true);

        // filter the list to contain only wbs-specific permissions
        for (Iterator i = perms.iterator(); i.hasNext();) {
            Permission p = (Permission) i.next();
            if (!wbsPermissionIDs.contains(p.getSpec().getId()))
                i.remove();
        }

        // if this role doesn't grant any wbs-specific permissions, then
        // the WBS Editor doesn't need to know about it.
        if (perms.isEmpty())
            return;

        // find the users who are assigned to this permission
        List<String> userIDs = new ArrayList<String>();
        for (User u : allUsers) {
            if (u.getRoleIDs().contains(r.getId()))
                userIDs.add(u.getUsername());
        }

        // if no users are assigned to this permission, the WBS Editor
        // doesn't need to know about it.
        if (userIDs.isEmpty())
            return;

        // write XML data for this role
        xml.startTag(null, ROLE_TAG);
        xml.attribute(null, NAME_ATTR, r.getName());
        xml.attribute(null, ID_ATTR, r.getId());

        // write the list of users, as a comma-separated attribute
        xml.attribute(null, USERS_ATTR, StringUtils.join(userIDs, ","));
        usersToWrite.removeAll(userIDs);

        // write the permissions granted by this role
        for (Permission p : perms)
            PermissionsManager.writePermission(xml, p);

        // end the role tag
        xml.endTag(null, ROLE_TAG);
    }

    private void writeEmptyRole(XmlSerializer xml, Set<String> usersToWrite)
            throws IOException {
        // write a role tag granting no permissions to the given people
        xml.startTag(null, ROLE_TAG);
        xml.attribute(null, USERS_ATTR, StringUtils.join(usersToWrite, ","));
        xml.endTag(null, ROLE_TAG);
    }



    @Override
    public void permissionsChanged(PermissionsChangeEvent event) {
        allUsers = null;
        allRoles = null;
        TeamSettingsRepublisher.getInstance().requestRepublish();
    }

    private List<User> getAllUsers() {
        if (allUsers == null)
            allUsers = PermissionsManager.getInstance().getAllUsers();
        return allUsers;
    }

    private List<Role> getAllRoles() {
        if (allRoles == null)
            allRoles = PermissionsManager.getInstance().getAllRoles();
        return allRoles;
    }



    private static final String ROLES_TAG = "roles";

    private static final String ROLE_TAG = "role";

    private static final String NAME_ATTR = "name";

    private static final String ID_ATTR = "id";

    private static final String USERS_ATTR = "users";

}
