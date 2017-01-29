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

package net.sourceforge.processdash.tool.perm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.security.TamperDeterrent.TamperException;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class PermissionsManager {

    public static final DashboardPermission PERMISSION = new DashboardPermission(
            "permissionsManager.config");

    public static final DashboardPermission EDIT_PERMISSION = new DashboardPermission(
            "permissionsManager.edit");

    private static final Resources resources = Resources
            .getDashBundle("Permissions");

    private static final Logger logger = Logger
            .getLogger(PermissionsManager.class.getName());


    private static final PermissionsManager INSTANCE = new PermissionsManager();

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }



    private Map<String, PermissionSpec> specs;

    private WorkingDirectory workingDir;

    private String bridgedUrl;

    private File rolesFile;

    private boolean rolesDirty;

    private Map<String, Role> roles;

    private File usersFile;

    private boolean usersDirty;

    private Map<String, User> users;

    private String currentUsername;

    private Set<Permission> currentPermissions;

    private boolean legacyPdesMode;


    private PermissionsManager() {}


    public void init(DashboardContext ctx) throws IOException, TamperException {
        PERMISSION.checkPermission();

        // load the definitions for known permission types
        this.specs = Collections.unmodifiableMap(loadPermissionSpecs());

        // get the directory where permission-based files are stored
        workingDir = ctx.getWorkingDirectory();
        File storageDir = workingDir.getDirectory();

        // if we are using a bridged working directory, store the URL
        if (workingDir instanceof BridgedWorkingDirectory)
            bridgedUrl = workingDir.getDescription();

        // identify the current user
        identifyCurrentUser();

        // load the roles for this dashboard, and the permissions they map to
        this.rolesFile = new File(storageDir, "roles.dat");
        readRoles();

        // load the users for this dashboard, and the roles they map to
        this.usersFile = new File(storageDir, "users.dat");
        readUsers();
        updateExternalUsers();

        // evaluate the permissions associated with the current user
        evaluateCurrentUserPermissions();
    }


    /**
     * Save all in-memory changes to disk.
     * 
     * @return true if all changes have been saved persistently, false if some
     *         in-memory changes could not be saved (for example, due to
     *         filesystem/network problems)
     */
    public boolean saveAll() {
        if (Settings.isReadOnly() || Settings.isPersonalMode())
            return true;

        if (rolesDirty)
            saveRoles();

        if (usersDirty)
            saveUsers();

        return !rolesDirty && !usersDirty;
    }


    /**
     * Get a list of permission specs that are directly implied by a given
     * parent.
     * 
     * This could be called recursively to build a tree of permission specs, or
     * to enumerate all of the permissions that are implied by a given grant.
     * 
     * @param parent
     *            a parent spec to retrieve children for, or null to retrieve
     *            all of the "root" specs (i.e., specs with no parent)
     * @return the specs that are immediately implied by the given spec
     */
    public List<PermissionSpec> getChildSpecsFor(PermissionSpec parent) {
        List<PermissionSpec> result = new ArrayList<PermissionSpec>();
        for (PermissionSpec oneSpec : specs.values()) {
            boolean isChild;
            if (parent == null)
                isChild = oneSpec.getImpliedBy().isEmpty();
            else
                isChild = oneSpec.getImpliedBy().contains(parent.getId())
                        || parent.getImplies().contains(oneSpec.getId());
            if (isChild)
                result.add(oneSpec);
        }
        Collections.sort(result);
        return result;
    }


    /**
     * @return a list of all roles known to this dataset
     */
    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList(this.roles.values());
        Collections.sort(roles);
        return roles;
    }


    /**
     * Find the role that has a particular ID.
     * 
     * @param id
     *            the unique ID of a role
     * @return the role with the given ID, or null if no such role can be found
     */
    public Role getRoleByID(String id) {
        return id == null ? null : roles.get(id);
    }


    /**
     * Find the role that has a particular name.
     * 
     * @param name
     *            the name of a role
     * @return the role with the given name (using a case-insensitive
     *         comparison), or null if no such role can be found
     */
    public Role getRoleByName(String name) {
        if (StringUtils.hasValue(name)) {
            for (Role r : roles.values()) {
                if (name.equalsIgnoreCase(r.getName()))
                    return r;
            }
        }
        return null;
    }


    /**
     * Make changes to the roles in this dataset.
     * 
     * @param rolesToSave
     *            a collection of new or changed roles
     * @param rolesToDelete
     *            a collection of existing roles that should be deleted
     */
    public void alterRoles(Collection<Role> rolesToSave,
            Collection<Role> rolesToDelete) {
        EDIT_PERMISSION.checkPermission();

        // delete roles as requested
        if (rolesToDelete != null) {
            for (Role r : rolesToDelete) {
                if (roles.remove(r.getId()) != null)
                    rolesDirty = true;
            }
        }

        // save new and changed roles as requested
        if (rolesToSave != null) {
            for (Role r : rolesToSave) {
                if (r.getId() == null)
                    r.id = "r." + generateUniqueID();
                roles.put(r.getId(), r);
                rolesDirty = true;
            }
        }

        // save the changes
        if (rolesDirty)
            saveRoles();
    }


    /**
     * Return the username of the current user.
     */
    public String getCurrentUsername() {
        return currentUsername;
    }


    /**
     * Return the User object for the current user.
     */
    public User getCurrentUser() {
        // identify the User object for the currently logged in user
        User user = getUserByUsername(currentUsername);
        if (user == null)
            user = users.get(CATCH_ALL_USER_ID);
        return user;
    }


    /**
     * Return the permissions of the current user.
     */
    public Set<Permission> getCurrentPermissions() {
        return currentPermissions;
    }


    /**
     * Return the permissions of the current user that have one of the given
     * IDs.
     */
    public <T extends Permission> Set<T> getCurrentPermissions(
            String... permissionIDs) {
        Set<T> result = new HashSet<T>();
        for (Permission p : currentPermissions) {
            for (String oneID : permissionIDs)
                if (p.getSpec().getId().equals(oneID))
                    result.add((T) p);
        }
        return result;
    }


    /**
     * @return true if the current user has a permission with one of the given
     *         IDs.
     */
    public boolean hasPermission(String... permissionIDs) {
        for (Permission p : currentPermissions) {
            for (String oneID : permissionIDs)
                if (p.getSpec().getId().equals(oneID))
                    return true;
        }
        return false;
    }


    /**
     * @return a list of all users known to this dataset
     */
    public List<User> getAllUsers() {
        return new ArrayList<User>(this.users.values());
    }


    /**
     * Find the user that has a particular username.
     * 
     * @param username
     *            the username of a user
     * @return the user with the given username, or null if no such user can be
     *         found
     */
    public User getUserByUsername(String username) {
        return username == null ? null : users.get(username.toLowerCase());
    }


    /**
     * Make changes to the users in this dataset.
     * 
     * @param usersToSave
     *            a collection of new or changed users
     * @param usersToDelete
     *            a collection of existing users that should be deleted
     */
    public void alterUsers(Collection<User> usersToSave,
            Collection<User> usersToDelete) {
        EDIT_PERMISSION.checkPermission();

        // delete users as requested
        if (usersToDelete != null) {
            for (User u : usersToDelete) {
                if (users.remove(u.getUsernameLC()) != null)
                    usersDirty = true;
            }
        }

        // save new and changed users as requested
        if (usersToSave != null) {
            for (User u : usersToSave) {
                String id = u.getUsernameLC();
                if (StringUtils.hasValue(id)) {
                    users.put(id, u);
                    usersDirty = true;
                }
            }
        }

        // save the changes
        if (usersDirty)
            saveUsers();
    }


    /**
     * Update our user list based on externally available data.
     */
    public void updateExternalUsers() {
        try {
            if (Settings.isTeamMode() && bridgedUrl != null)
                updateBridgedUsers();
        } catch (Exception e) {
            logger.log(Level.FINE, "Unable to update bridged users", e);
        }
    }


    /**
     * Retrieve the permissions that have been granted to a particular user.
     * 
     * @param user
     *            a user
     * @param deep
     *            false to return explicitly granted permissions; true to return
     *            all of the transitively implied permissions as well
     * @return a collection of permissions granted to the given user
     */
    public Set<Permission> getPermissionsForUser(User user, boolean deep) {
        Set<Permission> result = new LinkedHashSet<Permission>();

        // grant the "active user" permission if applicable
        if (!user.isInactive() || legacyPdesMode) {
            PermissionSpec spec = specs.get(ACTIVE_USER_PERMISSION_ID);
            Permission p = spec.createPermission(false, null);
            result.add(p);
            if (deep)
                enumerateImpliedPermissions(result, p);
        }

        // iterate over the roles for this user, and list granted permissions
        for (Role r : user.getRoles()) {
            if (!r.isInactive()) {
                for (Permission p : r.getPermissions()) {
                    if (!p.isInactive()) {
                        if (result.add(p)) {
                            if (deep)
                                enumerateImpliedPermissions(result, p);
                        }
                    }
                }
            }
        }
        return result;
    }

    private void enumerateImpliedPermissions(Set<Permission> result,
            Permission parent) {
        for (PermissionSpec childSpec : getChildSpecsFor(parent.getSpec())) {
            Permission childPerm = childSpec.createChildPermission(parent);
            if (result.add(childPerm))
                enumerateImpliedPermissions(result, childPerm);
        }
    }


    /**
     * @return true if this dataset is being managed by an older version of the
     *         PDES, which has not been upgraded to include user sync support
     */
    public boolean isLegacyPdesMode() {
        return legacyPdesMode;
    }



    /**
     * Scan extension points and load the permission specs they declare.
     */
    private Map loadPermissionSpecs() {
        Map result = new HashMap();
        for (Element xml : ExtensionManager
                .getXmlConfigurationElements(PERMISSION_TAG)) {

            // only consider <permission> tags that are direct children of the
            // enclosing document root tag. (This avoids <permission> tags that
            // appear within a <standardRoles> definition.)
            if (xml.getParentNode() != xml.getOwnerDocument()
                    .getDocumentElement())
                continue;

            try {
                PermissionSpec oneSpec = new PermissionSpec(xml);

                if (result.containsKey(oneSpec.getId()))
                    logger.severe("Conflicting definitions were provided "
                            + "for the permission with id '" + oneSpec.getId()
                            + "'; discarding the definition in "
                            + ExtensionManager
                                    .getDebugDescriptionOfSource(xml));
                else
                    result.put(oneSpec.getId(), oneSpec);

            } catch (Exception e) {
                logger.warning("Could not create permission with id '"
                        + xml.getAttribute(ID_ATTR) + "' in "
                        + ExtensionManager.getDebugDescriptionOfSource(xml)
                        + ": " + e.getMessage());
            }
        }
        return result;
    }


    /**
     * @return a unique ID that can be assigned to an object
     */
    private String generateUniqueID() {
        StringBuilder result = new StringBuilder();

        int i = Math.abs((new Random()).nextInt());
        result.append(Integer.toString(i, Character.MAX_RADIX)).append('.');
        result.append(
            Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));

        return result.toString();
    }


    /**
     * Read the roles that are defined in this dataset.
     */
    private void readRoles() throws IOException, TamperException {
        Map roles = new TreeMap();
        if (Settings.isTeamMode() && rolesFile.isFile()) {
            verifyFile(rolesFile);
            try {
                // open, parse, and load the roles XML file
                InputStream in = new BufferedInputStream(
                        new FileInputStream(rolesFile));
                Element xml = XMLUtils.parse(in).getDocumentElement();
                readRolesFromXml(roles, xml);

            } catch (Exception e) {
                throw new IOException(rolesFile.getName(), e);
            }

        } else {
            // no file present? read default roles from extension points
            for (Element xml : ExtensionManager
                    .getXmlConfigurationElements(STANDARD_ROLES_TAG)) {
                readRolesFromXml(roles, xml);

                // in personal mode, only read the role definitions that are
                // built-in to the dashboard. (Don't load any organizational
                // overrides to the standard roles.)
                if (Settings.isPersonalMode())
                    break;
            }
        }

        this.roles = Collections.synchronizedMap(roles);
        this.rolesDirty = false;
    }

    private void readRolesFromXml(Map roles, Element xml) {
        // find and parse the <role> tags in the document
        NodeList nl = xml.getElementsByTagName(ROLE_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Role r = readRole((Element) nl.item(i));
            roles.put(r.getId(), r);
        }
    }


    /**
     * Save in-memory role information to disk.
     */
    private void saveRoles() {
        try {
            if (Settings.isReadOnly() || Settings.isPersonalMode())
                return;

            // open a file to save the roles
            File tmp = TempFileFactory.get().createTempFile("roles", ".dat");
            OutputStream out = new BufferedOutputStream(
                    new RobustFileOutputStream(tmp));

            // start an XML document
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(out, "UTF-8");
            xml.startDocument("UTF-8", null);
            xml.startTag(null, ROLES_TAG);

            // write XML for each role
            List<Role> roles = new ArrayList(this.roles.values());
            for (Role r : roles) {
                writeRole(xml, r);
            }

            // end the document and close the file
            xml.endTag(null, ROLES_TAG);
            xml.ignorableWhitespace(System.lineSeparator());
            xml.endDocument();
            out.close();

            // add a tamper-deterrent thumbprint to the file
            TamperDeterrent.getInstance().addThumbprint(tmp, rolesFile,
                TamperDeterrent.FileType.XML);
            tmp.delete();

            this.rolesDirty = false;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not save roles to " + rolesFile,
                ioe);
        }
    }


    /**
     * Read a single role from an XML document
     */
    private Role readRole(Element xml) {
        // read the attributes for the role
        String id = xml.getAttribute(ID_ATTR);
        String name = xml.getAttribute(NAME_ATTR);
        if (name.startsWith("${"))
            name = resources.interpolate(name);
        boolean inactive = "true".equals(xml.getAttribute(INACTIVE_ATTR));

        // read the list of permissions granted by the role
        NodeList nl = xml.getElementsByTagName(PERMISSION_TAG);
        List<Permission> perms = new ArrayList<Permission>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Permission p = readPermission((Element) nl.item(i));
            if (p != null)
                perms.add(p);
        }

        // create the role and return it
        return new Role(id, name, inactive, perms);
    }


    /**
     * Write a single role definition as XML
     */
    private void writeRole(XmlSerializer xml, Role r) throws IOException {
        // start a <role> tag, and write its attributes
        xml.startTag(null, ROLE_TAG);
        xml.attribute(null, ID_ATTR, r.getId());
        xml.attribute(null, NAME_ATTR, r.getName());
        if (r.isInactive())
            xml.attribute(null, INACTIVE_ATTR, "true");

        // write each of the permissions granted by the rol
        for (Permission p : r.getPermissions())
            writePermission(xml, p);

        // end the <role> tag
        xml.endTag(null, ROLE_TAG);
    }


    /**
     * Read a single permission grant from an XML document
     */
    private Permission readPermission(Element xml) {
        // read the ID and find the spec for this permission
        String id = xml.getAttribute(ID_ATTR);
        PermissionSpec spec = specs.get(id);

        // if this permission was defined in a future version of the dashboard,
        // or by some extension that we don't have installed, we might not
        // recognize the ID. In that case, create a placeholder spec.
        if (spec == null)
            spec = new PermissionSpec(id);

        // read the inactive flag
        boolean inactive = "true".equals(xml.getAttribute(INACTIVE_ATTR));

        // read the parameters from the XML
        Map<String, String> params = new HashMap();
        NodeList nl = xml.getElementsByTagName(PARAM_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Element p = (Element) nl.item(i);
            String name = p.getAttribute(NAME_ATTR);
            String value = p.getAttribute(VALUE_ATTR);
            if (XMLUtils.hasValue(name))
                params.put(name, value);
        }

        // create the permission and return it.
        return spec.createPermission(inactive, params);
    }


    /**
     * Write a single permission grant to XML
     */
    private void writePermission(XmlSerializer xml, Permission p)
            throws IOException {
        // start a <permission> tag
        xml.startTag(null, PERMISSION_TAG);

        // write the ID of the permission
        xml.attribute(null, ID_ATTR, p.getSpec().getId());

        // write an inactive attribute if necessary
        if (p.isInactive())
            xml.attribute(null, INACTIVE_ATTR, "true");

        // write any parameters specified for this permission
        if (!p.getParams().isEmpty()) {
            List<String> names = new ArrayList<String>(p.getParams().keySet());
            Collections.sort(names);
            for (String name : names) {
                xml.startTag(null, PARAM_TAG);
                xml.attribute(null, NAME_ATTR, name);
                xml.attribute(null, VALUE_ATTR, p.getParams().get(name));
                xml.endTag(null, PARAM_TAG);
            }
        }

        // end the <permission> tag
        xml.endTag(null, PERMISSION_TAG);
    }


    /**
     * Read the users that are defined in this dataset.
     */
    private void readUsers() throws IOException, TamperException {
        Map users = new TreeMap();
        if (Settings.isTeamMode() && usersFile.isFile()) {
            verifyFile(usersFile);
            try {
                // open and parse the users XML file
                InputStream in = new BufferedInputStream(
                        new FileInputStream(usersFile));
                Element xml = XMLUtils.parse(in).getDocumentElement();

                // find and parse the <user> tags in the document
                NodeList nl = xml.getElementsByTagName(USER_TAG);
                for (int i = 0; i < nl.getLength(); i++) {
                    User u = readUser((Element) nl.item(i));
                    users.put(u.getUsernameLC(), u);
                }

            } catch (Exception e) {
                throw new IOException(usersFile.getName(), e);
            }

        } else {
            // no file present? create a catch-all user definition
            String catchAllName = resources.getString("All_Other_Users");
            User u = new User(catchAllName, CATCH_ALL_USER_ID, false,
                    Collections.singletonList(STANDARD_ROLE_ID));
            users.put(u.getUsernameLC(), u);
        }

        this.users = Collections.synchronizedMap(users);
        this.usersDirty = false;
    }


    /**
     * Save in-memory user information to disk.
     */
    private void saveUsers() {
        try {
            if (Settings.isReadOnly() || Settings.isPersonalMode())
                return;

            // open a file to save the users
            File tmp = TempFileFactory.get().createTempFile("users", ".dat");
            OutputStream out = new BufferedOutputStream(
                    new RobustFileOutputStream(tmp));

            // start an XML document
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(out, "UTF-8");
            xml.startDocument("UTF-8", null);
            xml.startTag(null, USERS_TAG);

            // write XML for each user
            List<User> users = new ArrayList(this.users.values());
            for (User u : users) {
                writeUser(xml, u);
            }

            // end the document and close the file
            xml.endTag(null, USERS_TAG);
            xml.ignorableWhitespace(System.lineSeparator());
            xml.endDocument();
            out.close();

            // add a tamper-deterrent thumbprint to the file
            TamperDeterrent.getInstance().addThumbprint(tmp, usersFile,
                TamperDeterrent.FileType.XML);
            tmp.delete();

            // flush data so the PDES can update server-side permissions
            if (bridgedUrl != null)
                workingDir.flushData();

            this.usersDirty = false;

        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not save users to " + usersFile,
                ioe);
        } catch (LockFailureException lfe) {
            logger.log(Level.WARNING, "Could not flush user data to server",
                lfe);
        }
    }


    /**
     * Read a single user from an XML document.
     */
    private User readUser(Element xml) {
        // get the attributes for the user
        String name = xml.getAttribute(NAME_ATTR);
        String username = xml.getAttribute(ID_ATTR);
        boolean inactive = "true".equals(xml.getAttribute(INACTIVE_ATTR));

        // add special handling for the name of the catch-all user, to handle
        // datasets that are shared by people speaking different languages.
        if (CATCH_ALL_USER_ID.equals(username))
            name = resources.getString("All_Other_Users");

        // read the list of roles assigned to the user
        NodeList nl = xml.getElementsByTagName(ROLE_TAG);
        List<String> roleIDs = new ArrayList<String>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Element roleTag = (Element) nl.item(i);
            String roleID = roleTag.getAttribute(ID_ATTR);
            if (roles.containsKey(roleID))
                roleIDs.add(roleID);
        }

        // create the user object and return it
        return new User(name, username, inactive, roleIDs);
    }


    /**
     * Write a single user to XML
     */
    private void writeUser(XmlSerializer xml, User u) throws IOException {
        // start a <user> tag
        xml.startTag(null, USER_TAG);

        // write attributes for the user
        xml.attribute(null, NAME_ATTR, u.getName());
        xml.attribute(null, ID_ATTR, u.getUsername());
        if (u.isInactive())
            xml.attribute(null, INACTIVE_ATTR, "true");

        // write the roles that have been assigned to the user
        for (String id : u.getRoleIDs()) {
            xml.startTag(null, ROLE_TAG);
            xml.attribute(null, ID_ATTR, id);
            xml.endTag(null, ROLE_TAG);
        }

        // end the <user> tag
        xml.endTag(null, USER_TAG);
    }


    /**
     * Contact the PDES to retrieve a list of users for this dataset, and update
     * our user list to match.
     */
    private void updateBridgedUsers() throws IOException {
        // if we have local changes that the server doesn't know about, make
        // certain to save them before proceeding. Otherwise, the logic below
        // will clobber local changes with stale data from the server.
        if (usersDirty)
            saveUsers();
        if (usersDirty)
            return;

        // construct the URL for retrieving permissions
        Matcher m = DATA_BRIDGE_URL_PAT.matcher(bridgedUrl);
        if (!m.matches())
            return;

        // contact the server and get the list of users for this dataset. If
        // the server is running older software, this will fail and we will
        // return without making changes.
        JSONObject json = makeRestApiCall(m.group(1) + "/api/v1/datasets/" //
                + m.group(2) + "/permissions/");
        List<Map> pdesUsers = (List<Map>) json.get("users");

        // make a copy of the users data structure, and prepare for changes
        Map<String, User> oldUsers = new HashMap<String, User>(this.users);
        Map<String, User> newUsers = new TreeMap<String, User>();
        User catchAllUser = oldUsers.remove(CATCH_ALL_USER_ID);
        if (catchAllUser != null)
            newUsers.put(CATCH_ALL_USER_ID, catchAllUser);

        // scan the list of PDES users, and reconcile them with the local users
        for (Map onePdesUser : pdesUsers) {
            String name = (String) onePdesUser.get("name");
            String username = (String) onePdesUser.get("username");
            if (!XMLUtils.hasValue(username)
                    || CATCH_ALL_USER_ID.equals(username))
                continue;
            User oldUser = oldUsers.remove(username.toLowerCase());

            List<String> roleIDs;
            if (oldUser != null) {
                name = getBestNameForUser(oldUser.getName(), name, username);
                roleIDs = oldUser.getRoleIDs();
            } else {
                name = getBestNameForUser(name, username);
                roleIDs = getBestRolesForUser(onePdesUser, catchAllUser);
            }

            User newUser = new User(name, username, false, roleIDs);
            newUsers.put(newUser.getUsernameLC(), newUser);
        }

        // if any local users are no longer in the PDES list, make them inactive
        for (User oldUser : oldUsers.values()) {
            User newUser = new User(oldUser.getName(), oldUser.getUsername(),
                    true, oldUser.getRoleIDs());
            newUsers.put(newUser.getUsernameLC(), newUser);
        }

        // save the new user data structure
        this.users = Collections.synchronizedMap(newUsers);
    }

    private String getBestNameForUser(String... names) {
        for (String oneName : names)
            if (XMLUtils.hasValue(oneName))
                return oneName;
        return null;
    }

    private List<String> getBestRolesForUser(Map pdesUser, User catchAllUser) {
        List<String> result = null;

        // if this user is the owner of the dataset, assign an owner role
        List<String> permissions = (List<String>) pdesUser.get("permissions");
        if (permissions != null && permissions.contains("dataset-owner"))
            result = getDatasetOwnerRoles();

        // if this is not the owner, or if no owner role was found, try using
        // the roles from the catch-all user.
        if (result == null && catchAllUser != null)
            result = catchAllUser.getRoleIDs();

        // if we still don't have any roles, assign the standard user role.
        if (result == null)
            result = Collections.singletonList(STANDARD_ROLE_ID);

        return result;
    }

    private List<String> getDatasetOwnerRoles() {
        // look through the standard role definitions, searching for ones that
        // contain a <datasetOwnerRole/> tag. If we find it, return the IDs
        // of the enclosing <role> tags.
        Set<String> result = new HashSet();
        for (Element datasetOwnerRoleTag : ExtensionManager
                .getXmlConfigurationElements(DATASET_OWNER_ROLE_TAG)) {
            Element roleTag = (Element) datasetOwnerRoleTag.getParentNode();
            if (ROLE_TAG.equals(roleTag.getTagName())) {
                String roleID = roleTag.getAttribute(ID_ATTR);
                if (XMLUtils.hasValue(roleID))
                    result.add(roleID);
            }
        }

        return (result.isEmpty() ? null : new ArrayList(result));
    }


    /**
     * Determine the identity of the current user
     */
    private void identifyCurrentUser() throws HttpException {
        currentUsername = null;
        currentPermissions = Collections.EMPTY_SET;

        // try a number of ways to figure the current user, until one succeeds
        String pdesUrl = ExternalResourceManager.getInstance().getDatasetUrl();
        if (pdesUrl != null && Settings.isTeamMode())
            identifyUserFromPDES(pdesUrl);
        if (!StringUtils.hasValue(currentUsername))
            identifyUserFromFileIO();
        if (!StringUtils.hasValue(currentUsername))
            identifyUserFromWhoamiCall();
        if (!StringUtils.hasValue(currentUsername))
            identifyUserFromSystemProperties();
    }

    private void identifyUserFromPDES(String datasetUrl) throws HttpException {
        Matcher m = DATA_BRIDGE_URL_PAT.matcher(datasetUrl);
        if (!m.matches())
            return;

        try {
            // make a REST API call to the server to identify the current user
            String whoamiUrl = m.group(1) + "/api/v1/users/whoami/";
            JSONObject json = makeRestApiCall(whoamiUrl);
            Map user = (Map) json.get("user");
            this.currentUsername = (String) user.get("username");
            logger.info("From PDES, current user is " + currentUsername);

            // see if this user should have extra permissions
            List<String> permIDs = (List<String>) user.get("clientPermissions");
            if (permIDs == null || permIDs.isEmpty())
                return;

            // calculate the implied permissions named by the whoami call
            Set<Permission> permissions = new LinkedHashSet<Permission>();
            for (String onePermissionID : permIDs) {
                PermissionSpec oneSpec = specs.get(onePermissionID);
                if (oneSpec != null) {
                    Permission p = oneSpec.createPermission(false, null);
                    if (p != null && permissions.add(p))
                        enumerateImpliedPermissions(permissions, p);
                }
            }
            this.currentPermissions = permissions;

        } catch (HttpException.Unauthorized he) {
            // if the attempt to contact the "whoami" API triggered a password
            // challenge that the user failed, propagate that failure along.
            throw he;

        } catch (Exception e) {
            // older versions of the PDES will not have the whoami REST API.
            // fall back and try retrieving the HTTP Auth username that was
            // used to authenticate to the DataBridge
            this.currentUsername = HttpAuthenticator.getLastUsername();
            if (StringUtils.hasValue(currentUsername)) {
                logger.info("From HTTP, current user is " + currentUsername);
                legacyPdesMode = true;
            }
        }
    }

    private void identifyUserFromFileIO() {
        try {
            File f = File.createTempFile("whoami", ".tmp");
            Path path = Paths.get(f.getAbsolutePath());
            FileOwnerAttributeView ownerAttributeView = Files
                    .getFileAttributeView(path, FileOwnerAttributeView.class);
            UserPrincipal owner = ownerAttributeView.getOwner();
            this.currentUsername = discardDomain(owner.getName());
            f.delete();
            logger.info("From NIO, current user is " + currentUsername);

        } catch (Throwable t) {
            // this will fail on Java 1.6. Try the next option
        }
    }

    private void identifyUserFromWhoamiCall() {
        try {
            // execute the "whoami" command
            Process p = Runtime.getRuntime().exec("whoami");
            byte[] out = RuntimeUtils.collectOutput(p, true, false);
            String result = new String(out).trim();
            this.currentUsername = discardDomain(result);
            logger.info("From whoami, current user is " + currentUsername);

        } catch (Throwable t) {
        }
    }

    private void identifyUserFromSystemProperties() {
        this.currentUsername = discardDomain(System.getProperty("user.name"));
        logger.info("From system, current user is " + currentUsername);
    }

    private String discardDomain(String username) {
        if (!StringUtils.hasValue(username))
            return null;
        int pos = username.lastIndexOf('\\');
        return (pos == -1 ? username : username.substring(pos + 1));
    }


    /**
     * Evaluate the permissions that should be assigned to the current user.
     */
    private void evaluateCurrentUserPermissions() {
        User user = getCurrentUser();
        if (user != null && (!user.isInactive() || legacyPdesMode)) {
            // if we found an active user, calculate the resulting permissions.
            Set<Permission> permissions = getPermissionsForUser(user, true);
            permissions.addAll(currentPermissions);
            this.currentPermissions = Collections.unmodifiableSet(permissions);

        } else if (hasPermission(ACTIVE_USER_PERMISSION_ID)) {
            // we didn't find this user in the list, or they are inactive. But
            // the PDES assigned them the "active user" permission (for example,
            // because they are a data admin), and that overrides the user list.
            // Go ahead and give them the permissions the PDES granted.
            this.currentPermissions = Collections
                    .unmodifiableSet(currentPermissions);

        } else {
            // if we did not find an active User object, we have no permissions
            this.currentPermissions = Collections.EMPTY_SET;
        }

        // debug print the effective permission list, if desired
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder permlist = new StringBuilder();
            permlist.append("current user effective permissions:");
            for (Permission p : currentPermissions)
                permlist.append("\n        " + p);
            logger.fine(permlist.toString());
        }
    }


    /**
     * Make a REST API call, and return the result as JSON.
     */
    private JSONObject makeRestApiCall(String urlStr) throws IOException {
        InputStream in = null;
        try {
            URLConnection conn = new URL(urlStr).openConnection();
            HttpException.checkValid(conn);
            in = conn.getInputStream();
            return (JSONObject) new JSONParser().parse(
                new InputStreamReader(new BufferedInputStream(in), "UTF-8"));
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception pe) {
            throw new IOException(pe);
        } finally {
            FileUtils.safelyClose(in);
        }
    }


    /**
     * Throw an exception if one of our files has been tampered with.
     * 
     * To avoid denial-of-service problems, people who have been granted "all
     * permissions" by a PDES are still allowed to open the tampered files. This
     * allows them to review the configuration, fix any problems, and save a
     * new/good version of the files.
     */
    private void verifyFile(File file) throws IOException, TamperException {
        if (hasPermission(ALL_PERMISSION_ID) == false)
            TamperDeterrent.getInstance().verifyThumbprint(file,
                TamperDeterrent.FileType.XML);
    }



    private static final Pattern DATA_BRIDGE_URL_PAT = Pattern
            .compile("(http.*)/DataBridge/([\\w-]+)");

    public static final String STANDARD_ROLE_ID = "r.standard";

    public static final String ALL_PERMISSION_ID = "pdash.all";

    public static final String ACTIVE_USER_PERMISSION_ID = "pdash.active";

    public static final String CATCH_ALL_USER_ID = "*";

    private static final String STANDARD_ROLES_TAG = "standardRoles";

    private static final String DATASET_OWNER_ROLE_TAG = "datasetOwnerRole";

    private static final String USERS_TAG = "users";

    private static final String USER_TAG = "user";

    private static final String ROLES_TAG = "roles";

    private static final String ROLE_TAG = "role";

    private static final String PERMISSION_TAG = "permission";

    private static final String PARAM_TAG = "param";

    private static final String ID_ATTR = "id";

    private static final String NAME_ATTR = "name";

    private static final String VALUE_ATTR = "value";

    private static final String INACTIVE_ATTR = "inactive";

}
