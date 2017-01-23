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

package net.sourceforge.processdash.tool.perm.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.perm.Role;
import net.sourceforge.processdash.tool.perm.User;

public class EditableUser {

    private User user;

    private String name;

    private String username;

    private Boolean active;

    private String roleNames;

    private List<String> roleIDs;


    public EditableUser(User user) {
        this.user = user;
        recalcRoleNames();
    }


    /**
     * @return the user object upon which this item is based.
     */
    public User getOriginalUser() {
        return user;
    }


    /**
     * @return a new User representing the current/modified state of this
     *         object.
     */
    public User getNewUser() {
        String name = trimToNull(getName());
        if (name == null)
            name = getUsername();
        return new User(name, getUsername(), !getActive(), getRoleIDs());
    }


    /**
     * @return true if this object contains modifications from the original user
     *         that was provided
     */
    public boolean isModified() {
        return name != null || username != null || active != null
                || roleIDs != null;
    }


    /**
     * @return true if this represents a change where the username has been
     *         edited from the original user.
     */
    public boolean isUsernameChange() {
        return username != null && user != null
                && !username.equalsIgnoreCase(user.getUsername());
    }


    /**
     * @return the display name of this user
     */
    public String getName() {
        if (name != null)
            return name;
        else if (user != null)
            return user.getName();
        else
            return null;
    }


    /**
     * Set the display name of this user.
     * 
     * @param name
     *            the display name for this user. Can be null to indicate,
     *            "inherit from original user"
     */
    public void setName(String name) {
        name = trimToNull(name);
        if (user != null && user.getName().equals(name))
            this.name = null;
        else
            this.name = name;
    }


    /**
     * @return the username of this user
     */
    public String getUsername() {
        if (username != null)
            return username;
        else if (user != null)
            return user.getUsername();
        else
            return null;
    }


    /**
     * Set the username of this user.
     * 
     * @param username
     *            the username for this user. Can be null to indicate, "inherit
     *            from original user"
     */
    public void setUsername(String username) {
        username = trimToNull(username);
        if (user != null && user.getUsername().equals(username))
            this.username = null;
        else
            this.username = username;
    }


    /**
     * @return true if this is an active user, false otherwise
     */
    public Boolean getActive() {
        if (active != null)
            return active;
        else if (user != null)
            return !user.isInactive();
        else
            return true;
    }


    /**
     * Set the active flag for this user.
     * 
     * @param active
     *            the new active flag. Can be null to indicate "inherit from
     *            original user"
     */
    public void setActive(Boolean active) {
        if (user != null && active != null && active.equals(!user.isInactive()))
            this.active = null;
        else
            this.active = active;
    }


    /**
     * @return a human-readable, comma-separated list of roles assigned to this
     *         user.
     */
    public String getRoleNames() {
        return roleNames;
    }


    /**
     * Change the set of roles that are assigned to this user.
     * 
     * @param roleNames
     *            a comma-separated list of role names. These will be resolved
     *            (in a case-insensitive manner) against the list of roles known
     *            to this dataset.
     */
    public void setRoleNames(String roleNames) {
        if (roleNames == null || roleNames.trim().length() == 0) {
            setRoleIDs(Collections.EMPTY_LIST);

        } else {
            List<String> roleIDs = new ArrayList<String>();
            for (String oneName : roleNames.split(",")) {
                Role r = PermissionsManager.getInstance()
                        .getRoleByName(oneName.trim());
                if (r != null)
                    roleIDs.add(r.getId());
            }
            setRoleIDs(roleIDs);
        }
    }


    /**
     * @return the unique IDs of the roles assigned to this user.
     */
    public List<String> getRoleIDs() {
        if (roleIDs != null)
            return roleIDs;
        else if (user != null)
            return user.getRoleIDs();
        else
            return Collections.EMPTY_LIST;
    }


    /**
     * Set the list of roles that should be assigned to this user.
     * 
     * @param roleIDs
     *            the unique IDs of the roles to assign. Can be null to indicate
     *            "inherit from original user"
     */
    public void setRoleIDs(List<String> roleIDs) {
        if (user != null && user.getRoleIDs().equals(roleIDs))
            this.roleIDs = null;
        else
            this.roleIDs = roleIDs;

        recalcRoleNames();
    }


    /**
     * Look at the role IDs for this user, and recalculate the comma-separated
     * list of role names that should be displayed as a result.
     */
    private void recalcRoleNames() {
        StringBuilder newRoleNames = new StringBuilder();
        for (String oneRoleID : getRoleIDs()) {
            Role r = PermissionsManager.getInstance().getRoleByID(oneRoleID);
            if (r != null)
                newRoleNames.append(", ").append(r.getName());
        }
        if (newRoleNames.length() == 0)
            this.roleNames = "";
        else
            this.roleNames = newRoleNames.substring(2);
    }


    /**
     * Trim whitespace from a string, and replace an empty result with null.
     */
    private String trimToNull(String s) {
        if (s == null)
            return null;
        s = s.trim();
        return (s.length() == 0 ? null : s);
    }

}
