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

package teamdash.wbs;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.security.TamperDeterrent.FileType;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
import net.sourceforge.processdash.tool.perm.WhoAmI;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class WBSPermissionManager {

    private static String version = "";

    private static String currentUser = null;

    private static Set<WBSPermission> permissions = Collections.EMPTY_SET;

    private static final Logger logger = Logger
            .getLogger(WBSPermissionManager.class.getName());


    /**
     * @return the username of the current user
     */
    public static String getCurrentUser() {
        return currentUser;
    }


    /**
     * Return true if the current user has a permission with the given ID.
     * 
     * @param permissionID
     *            the ID of a permission
     * @param version
     *            the version of the Process Dashboard software when the
     *            definition of permission was added. If the team project
     *            settings file was written by an older version of the
     *            dashboard, the permission will be implicitly granted by
     *            omission.
     */
    public static boolean hasPerm(String permissionID,
            String permissionVersion) {
        if (VersionUtils.compareVersions(version, permissionVersion) < 0)
            return true;

        for (WBSPermission wbsPerm : permissions) {
            String permID = wbsPerm.getId();
            if (permissionID.equals(permID) || ALL_PERMISSIONS.equals(permID))
                return true;
        }

        return false;
    }


    /**
     * Evaluate whether the current user has been assigned a group permission.
     * 
     * @param permissionID
     *            the ID of a permission
     * @param version
     *            the version of the Process Dashboard software when the
     *            definition of permission was added. If the team project
     *            settings file was written by an older version of the
     *            dashboard, the permission will be implicitly granted for
     *            "Everyone" by omission.
     * @return null if the user is granted permission to everyone; an empty list
     *         if they have not been granted permission; otherwise, a list of
     *         the datasetIDs to which they have been granted permission
     */
    public static Set<String> getGroupPerm(String permissionID,
            String permissionVersion) {
        if (VersionUtils.compareVersions(version, permissionVersion) < 0)
            return null;

        Set<String> result = new HashSet();
        for (WBSPermission wbsPerm : permissions) {
            String permID = wbsPerm.getId();
            if (ALL_PERMISSIONS.equals(permID)) {
                return null;
            } else if (permissionID.equals(permID)) {
                String groupID = wbsPerm.getParams().get("group");
                if (UserGroup.EVERYONE_ID.equals(groupID))
                    return null;

                UserGroup group = UserGroupManager.getInstance()
                        .getGroupByID(groupID);
                if (group != null)
                    result.addAll(group.getDatasetIDs());
            }
        }
        return result;
    }


    /**
     * Identify the current user and determine their permissions.
     */
    public static void init(WorkingDirectory workingDirectory, TeamProject proj)
            throws HttpException.Unauthorized {

        // determine the identity of the current user.
        String pdesUrl;
        if (workingDirectory instanceof BridgedWorkingDirectory)
            pdesUrl = workingDirectory.getDescription();
        else
            pdesUrl = ExternalLocationMapper.getInstance().getDatasetUrl();
        WhoAmI whoAmI = new WhoAmI(pdesUrl);
        currentUser = whoAmI.getUsername();

        // look at the team settings.xml file. if it was written by an old
        // version of the dashboard, it will not contain a version. In that
        // case, exit without any further analysis.
        Element settings = proj.getProjectSettings();
        if (settings == null)
            return;
        version = settings.getAttribute("version");
        if (!XMLUtils.hasValue(version))
            return;

        // the settings.xml file was written by a version of the dashboard that
        // knows how to publish permissions. Check to ensure that it has not
        // been tampered with. If it has, grant no permissions.
        File settingsFile = new File(proj.getStorageDirectory(),
                WBSFilenameConstants.SETTINGS_FILENAME);
        try {
            TamperDeterrent.getInstance().verifyThumbprint(settingsFile,
                FileType.WBS);
        } catch (Exception te) {
            System.err.println("The file " + settingsFile + " is corrupt; "
                    + "locking down WBS permissions");
            version = "999.999";
            return;
        }

        // find the permissions granted in the settings.xml file
        Set<WBSPermission> perms = getPermissionsForUser(settings,
            currentUser.toLowerCase());
        perms.remove(null);

        // add the externally granted permissions of the current user
        for (String onePerm : whoAmI.getExternalPermissionGrants()) {
            if ("pdash.all".equals(onePerm))
                onePerm = ALL_PERMISSIONS;
            perms.add(new WBSPermission(onePerm));
        }

        // store these permissions for future use
        permissions = Collections.unmodifiableSet(perms);
        debugLogPermissions();
    }

    private static Set<WBSPermission> getPermissionsForUser(Element settings,
            String username) {

        // create separate collections of permissions for this specific user,
        // and for the catch-all user. The catch-all collection will only be
        // used if we don't find any mention of this user in the settings file.
        Set<WBSPermission> userPerms = new HashSet();
        Set<WBSPermission> catchAllPerms = new HashSet();
        String usernameHash = WhoAmI.hashUsername(username);

        // find the <roles> tag in the settings document. If the tag is not
        // found, return an empty set.
        List<Element> roles = getChildren(settings, "roles");
        if (roles.size() != 1)
            return userPerms;

        // iterate over the different <role> tags in the document
        for (Element role : getChildren(roles.get(0), "role")) {

            // get the list of users who share this role
            List<String> usersForRole = Arrays.asList(
                role.getAttribute("users").toLowerCase().split(","));
            Set<WBSPermission> perms;
            if (usersForRole.contains(username)
                    || usersForRole.contains(usernameHash)) {
                // if our target user has this role, store permissions in
                // the userPerms collection
                perms = userPerms;

            } else if (usersForRole.contains("*")) {
                // if this role is assigned to the catch-all user, store the
                // permissions in the catchAllPerms collection
                perms = catchAllPerms;

            } else {
                // if this role is not assigned to anyone relevant, skip it
                continue;
            }

            // add a null value to the target collection, just to keep track
            // of the fact that we found a role that named it.
            perms.add(null);

            // now, add each of the permissions associated with this role.
            for (Element permXml : getChildren(role, "permission")) {
                WBSPermission wbsPerm = new WBSPermission(permXml);
                if (XMLUtils.hasValue(wbsPerm.getId()))
                    perms.add(wbsPerm);
            }
        }

        // if we found any roles that named the target user, return that
        // collection. If the target user was not named, they receive the
        // catch-all permissions
        if (!userPerms.isEmpty())
            return userPerms;
        else
            return catchAllPerms;
    }

    private static List<Element> getChildren(Element xml, String tagName) {
        List<Element> children = XMLUtils.getChildElements(xml);
        for (Iterator<Element> i = children.iterator(); i.hasNext();) {
            if (!i.next().getTagName().equals(tagName))
                i.remove();
        }
        return children;
    }

    private static void debugLogPermissions() {
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder msg = new StringBuilder();
            msg.append("Effective WBS permissions are:");
            for (WBSPermission p : permissions)
                msg.append("\n\t" + p.toString());
            if (permissions.isEmpty())
                msg.append("\n\t- none -");
            logger.fine(msg.toString());
        }
    }

    private static final String ALL_PERMISSIONS = "wbs.all";

}
