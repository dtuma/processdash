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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class PermissionsManager {

    public static final DashboardPermission PERMISSION = new DashboardPermission(
            "permissionsManager.config");

    private static final Resources resources = Resources
            .getDashBundle("Permissions");

    private static final Logger logger = Logger
            .getLogger(PermissionsManager.class.getName());


    private static final PermissionsManager INSTANCE = new PermissionsManager();

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }



    private Map<String, PermissionSpec> specs;

    private File rolesFile;

    private boolean rolesDirty;

    private Map<String, Role> roles;

    private File usersFile;

    private boolean usersDirty;

    private Map<String, User> users;

    private PermissionsManager() {}


    public void init(DashboardContext ctx) throws IOException {
        PERMISSION.checkPermission();

        // load the definitions for known permission types
        this.specs = Collections.unmodifiableMap(loadPermissionSpecs());

        // get the directory where permission-based files are stored
        File storageDir = ((ProcessDashboard) ctx).getWorkingDirectory()
                .getDirectory();

        // load the roles for this dashboard, and the permissions they map to
        this.rolesFile = new File(storageDir, "roles.dat");
        readRoles();

        // load the users for this dashboard, and the roles they map to
        this.usersFile = new File(storageDir, "users.dat");
        readUsers();
    }


    /**
     * Save all in-memory changes to disk.
     * 
     * @return true if all changes have been saved persistently, false if some
     *         in-memory changes could not be saved (for example, due to
     *         filesystem/network problems)
     */
    public boolean saveAll() {
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
        return roles.get(id);
    }


    /**
     * Make changes to the roles in this dataset.
     * 
     * @param rolesToSave
     *            a list of new or changed roles
     * @param rolesToDelete
     *            a list of existing roles that should be deleted
     */
    public void alterRoles(List<Role> rolesToSave, List<Role> rolesToDelete) {
        // delete roles as requested
        for (Role r : rolesToDelete) {
            if (roles.remove(r.getId()) != null)
                rolesDirty = true;
        }

        // save new and changed roles as requested
        for (Role r : rolesToSave) {
            if (r.getId() == null)
                r.id = "r." + generateUniqueID();
            roles.put(r.getId(), r);
            rolesDirty = true;
        }

        // save the changes
        saveRoles();
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
        return users.get(username.toLowerCase());
    }


    /**
     * Make changes to the users in this dataset.
     * 
     * @param usersToSave
     *            a list of new or changed users
     * @param usersToDelete
     *            a list of existing users that should be deleted
     */
    public void alterUsers(List<User> usersToSave, List<User> usersToDelete) {
        // delete users as requested
        for (User u : usersToDelete) {
            if (users.remove(u.getUsernameLC()) != null)
                usersDirty = true;
        }

        // save new and changed users as requested
        for (User u : usersToSave) {
            String id = u.getUsernameLC();
            if (StringUtils.hasValue(id)) {
                users.put(id, u);
                usersDirty = true;
            }
        }

        // save the changes
        saveUsers();
    }



    /**
     * Scan extension points and load the permission specs they declare.
     */
    private Map loadPermissionSpecs() {
        Map result = new HashMap();
        for (Element xml : ExtensionManager
                .getXmlConfigurationElements(PERMISSION_TAG)) {
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
    private void readRoles() throws IOException {
        Map roles = new TreeMap();
        if (rolesFile.isFile()) {
            try {
                // open and parse the roles XML file
                InputStream in = new BufferedInputStream(
                        new FileInputStream(rolesFile));
                Element xml = XMLUtils.parse(in).getDocumentElement();

                // find and parse the <role> tags in the document
                NodeList nl = xml.getElementsByTagName(ROLE_TAG);
                for (int i = 0; i < nl.getLength(); i++) {
                    Role r = readRole((Element) nl.item(i));
                    roles.put(r.getId(), r);
                }
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException(e);
            }

        } else {
            // no file present? create a single, standard role
            PermissionSpec spec = specs.get(ALL_PERMISSION_ID);
            Permission p = spec.createPermission(false, null);
            String standardRoleName = resources.getString("Standard_User");
            Role r = new Role(STANDARD_ROLE_ID, standardRoleName, false,
                    Collections.singletonList(p));
            roles.put(r.getId(), r);
        }

        this.roles = Collections.synchronizedMap(roles);
        this.rolesDirty = false;
    }


    /**
     * Save in-memory role information to disk.
     */
    private void saveRoles() {
        try {
            // open a file to save the roles
            OutputStream out = new BufferedOutputStream(
                    new RobustFileOutputStream(rolesFile));

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
            xml.endDocument();
            out.close();

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
    private void readUsers() throws IOException {
        Map users = new TreeMap();
        if (usersFile.isFile()) {
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
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException(e);
            }

        } else {
            // no file present? create a catch-all user definition
            String catchAllName = resources.getString("All_Other_Users");
            Role standardRole = roles.get(STANDARD_ROLE_ID);
            User u = new User(catchAllName, CATCH_ALL_USER_ID, false,
                    standardRole == null ? Collections.EMPTY_LIST
                            : Collections.singletonList(standardRole));
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
            // open a file to save the users
            OutputStream out = new BufferedOutputStream(
                    new RobustFileOutputStream(usersFile));

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
            xml.endDocument();
            out.close();

            this.usersDirty = false;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not save users to " + usersFile,
                ioe);
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



    public static final String ALL_PERMISSION_ID = "pdash.all";

    public static final String CATCH_ALL_USER_ID = "*";

    private static final String STANDARD_ROLE_ID = "r.0";

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
