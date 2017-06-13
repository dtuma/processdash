// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.event.EventListenerList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.security.TamperDeterrent.TamperException;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

public abstract class UserGroupManager {

    private static UserGroupManager INSTANCE;

    public static UserGroupManager getInstance() {
        return INSTANCE;
    }



    protected EventListenerList listeners;

    private UserFilter globalFilter;

    private boolean enabled;

    private TamperDeterrent.FileType tamperFileType;

    private boolean indivFilteringSupported;

    private File sharedFile, customFile;

    private Set<Boolean> needsSave;

    private Map<Boolean, Date> timestamps;

    private Map<String, UserGroup> groups;


    UserGroupManager(boolean enable, TamperDeterrent.FileType fileType) {
        PERMISSION.checkPermission();
        if (INSTANCE != null)
            throw new IllegalStateException();

        listeners = new EventListenerList();
        globalFilter = UserGroup.EVERYONE;
        enabled = enable;
        tamperFileType = fileType;
        indivFilteringSupported = false;
        timestamps = new HashMap<Boolean, Date>();
        groups = Collections.EMPTY_MAP;
        INSTANCE = this;
    }

    void init(File sharedFile, String datasetID) throws TamperException {
        // ensure calling code has permission to perform initialization
        PERMISSION.checkPermission();

        // save the location of the shared file
        this.sharedFile = sharedFile;

        // determine the file for storage of custom/personal groups
        File appDir = DirectoryPreferences.getApplicationDirectory();
        File customDir = new File(appDir, "groups");
        String customFilename = "groups-" + datasetID.toLowerCase() + ".xml";
        customFile = new File(customDir, customFilename);

        // load group data from both files
        groups = Collections.synchronizedMap(new HashMap<String, UserGroup>());
        needsSave = new HashSet<Boolean>();
        reloadAll();
    }

    protected void reloadAll() throws TamperException {
        reloadGroups(true);
        reloadGroups(false);
    }

    /**
     * @return true if user group support is active and enabled in this
     *         dashboard.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return true if filtering by individual is supported
     */
    public boolean isIndivFilteringSupported() {
        return indivFilteringSupported;
    }

    /**
     * Toggle the flag indicating whether filtering by individual is supported
     */
    public void setIndivFilteringSupported(boolean indivFilteringSupported) {
        PERMISSION.checkPermission();
        this.indivFilteringSupported = indivFilteringSupported;
    }

    /**
     * @return true if the user has any options for person-based filtering
     */
    public boolean isFilteringAvailable() {
        return enabled && (indivFilteringSupported || !groups.isEmpty());
    }

    /**
     * Shared groups should not be editable in certain circumstances (for
     * example, when the team dashboard is in read-only mode). This method
     * indicates whether shared groups are read only.
     * 
     * @return null if shared groups can be edited; otherwise, a reason code
     *         explaining why they cannot
     */
    public abstract String getReadOnlyCode();

    /**
     * @return the date/time when shared groups were last modified
     */
    public Date getSharedGroupsTimestamp() {
        // return the timestamp for shared (non-custom) groups
        return timestamps.get(Boolean.FALSE);
    }

    public void addUserGroupEditListener(UserGroupEditListener l) {
        listeners.add(UserGroupEditListener.class, l);
    }

    public void removeUserGroupEditListener(UserGroupEditListener l) {
        listeners.remove(UserGroupEditListener.class, l);
    }

    private void fireUserGroupEditEvent(UserGroupEditEvent event) {
        for (UserGroupEditListener l : listeners
                .getListeners(UserGroupEditListener.class)) {
            try {
                if (event != null)
                    l.userGroupEdited(event);
                else
                    l.userGroupsChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public Map<String, UserGroup> getGroups() {
        return Collections.unmodifiableMap(groups);
    }


    public UserGroup getGroupByID(String groupID) {
        if (UserGroup.EVERYONE_ID.equals(groupID))
            return UserGroup.EVERYONE;
        else if (groups == null)
            return null;
        else
            return groups.get(groupID);
    }


    public void setGlobalFilter(UserFilter f) {
        if (!enabled)
            return;
        if (f == null)
            throw new NullPointerException();
        this.globalFilter = f;
    }


    public UserFilter getGlobalFilter() {
        return globalFilter;
    }


    public UserFilter getFilterById(String filterId) {
        if (filterId == null || filterId.length() == 0) {
            return null;

        } else if (UserGroup.EVERYONE_ID.equals(filterId)) {
            return UserGroup.EVERYONE;

        } else if (filterId.startsWith(UserGroupMember.ID_PREFIX)) {
            for (UserGroupMember m : getAllKnownPeople()) {
                if (m.getId().equals(filterId))
                    return m;
            }

            // no luck, no such individual
            return null;

        } else {
            return groups.get(filterId);
        }
    }


    public void alterGroups(Collection<UserGroup> groupsToSave,
            Collection<UserGroup> groupsToDelete) {
        // delete the requested set of groups
        if (groupsToDelete != null) {
            for (UserGroup g : groupsToDelete)
                if (g.getId() != null)
                    deleteGroupImpl(g);
        }

        // save the requested set of groups
        if (groupsToSave != null) {
            for (UserGroup g : groupsToSave)
                saveGroupImpl(g);
        }

        // notify listeners if changes were made
        if (!needsSave.isEmpty())
            fireUserGroupEditEvent(null);

        // flush in-memory changes to disk
        saveAll();
    }


    private void saveGroupImpl(UserGroup g) {
        prepareForModification(g);

        // if this is a new group, assign it a unique group ID
        if (g.getId() == null)
            g.id = generateUniqueID(g.isCustom());

        // add or replace the given group.
        groups.put(g.getId(), g);

        // make a note that this file needs saving.
        needsSave.add(g.isCustom());
        groupWasSaved(g);
    }

    private String generateUniqueID(boolean custom) {
        StringBuilder result = new StringBuilder();

        if (custom)
            result.append(CUSTOM_ID_PREFIX);
        int i = Math.abs((new Random()).nextInt());
        result.append(Integer.toString(i, Character.MAX_RADIX)).append('.');
        result.append(Long.toString(System.currentTimeMillis(),
            Character.MAX_RADIX));

        return result.toString();
    }

    protected void groupWasSaved(UserGroup g) {
        // notify listeners about the change
        fireUserGroupEditEvent(new UserGroupEditEvent(this, g, false));
    }



    private void deleteGroupImpl(UserGroup g) {
        prepareForModification(g);

        // delete the requested group
        if (groups.remove(g.getId()) != null) {
            // make a note that this file needs saving.
            needsSave.add(g.isCustom());
            groupWasDeleted(g);
        }
    }

    protected void groupWasDeleted(UserGroup g) {
        // notify listeners about the change
        fireUserGroupEditEvent(new UserGroupEditEvent(this, g, true));
    }


    public boolean saveAll() {
        if (needsSave == null || needsSave.isEmpty())
            return true;

        for (Boolean custom : new HashSet<Boolean>(needsSave)) {
            try {
                saveFile(custom);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return needsSave.isEmpty();
    }

    private void prepareForModification(UserGroup g) {
        // ensure calling code has permission to make changes
        PERMISSION.checkPermission();

        // Groups are only used in the Team Dashboard. If a caller tries to make
        // changes within a personal dashboard, throw an exception.
        if (!enabled || needsSave == null)
            throw new IllegalStateException("Group modification not allowed");

        // validate that the ID of this group is appropriate
        String id = g.getId();
        if ((id != null && (id.startsWith(CUSTOM_ID_PREFIX) != g.isCustom()))
                || UserGroup.EVERYONE_ID.equals(id))
            throw new IllegalArgumentException("Invalid group ID");

        // don't allow modification of non-custom groups in read-only mode
        if (!g.isCustom() && getReadOnlyCode() != null)
            throw new IllegalStateException("Shared groups are read-only: "
                    + getReadOnlyCode());

        // make a note of the time we are changing the data in the given file
        timestamps.put(g.isCustom(), new Date());

        // custom groups could be altered simultaneously by different processes.
        // to be on the safe side, try reloading the custom groups file before
        // we modify it (as long as we don't have unsaved changes).
        if (g.isCustom() && !needsSave.contains(Boolean.TRUE)) {
            try {
                reloadGroups(true);
            } catch (TamperException e) {
                // can't happen: the custom file is not checked for tampering
            }
        }
    }

    private void reloadGroups(boolean custom) throws TamperException {
        File targetFile = custom ? customFile : sharedFile;
        if (!targetFile.isFile())
            return;

        try {
            // read the groups from the file
            Map<String, UserGroup> groupsRead = readFile(targetFile, custom);

            // delete in-memory groups that are no longer in the file
            for (Iterator i = new HashSet(groups.values()).iterator(); i
                    .hasNext();) {
                UserGroup oneGroup = (UserGroup) i.next();
                String oneID = oneGroup.getId();
                if (oneGroup.isCustom() == custom
                        && !groupsRead.containsKey(oneID)) {
                    groups.remove(oneID);
                    groupWasDeleted(oneGroup);
                }
            }

            // replace in-memory groups with those just read
            for (UserGroup oneGroup : groupsRead.values()) {
                UserGroup oldGroup = groups.put(oneGroup.getId(), oneGroup);
                if (!oneGroup.equals(oldGroup))
                    groupWasSaved(oneGroup);
            }

        } catch (TamperException te) {
            throw te;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, UserGroup> readFile(File f, boolean custom)
            throws SAXException, IOException, TamperException {
        // check to see if the shared groups file has been tampered with. To
        // avoid denial-of-service problems, people who have permission to
        // edit groups are still allowed to open the tampered files. This
        // allows them to review the configuration, fix any problems, and save
        // a new/good version of the file.
        if (!custom && getReadOnlyCode() != null)
            TamperDeterrent.getInstance().verifyThumbprint(f, tamperFileType);

        // open the file and parse as XML
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        Element xml = XMLUtils.parse(in).getDocumentElement();

        // if the <groups> tag isn't the root of the document, find it
        if (!GROUPS_TAG.equals(xml.getTagName())) {
            NodeList nl = xml.getElementsByTagName(GROUPS_TAG);
            if (nl.getLength() > 0)
                xml = (Element) nl.item(0);
        }

        // extract each of the groups from the XML file
        Map<String, UserGroup> result = new HashMap();
        NodeList nl = xml.getElementsByTagName(UserGroup.GROUP_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            UserGroup oneGroup = new UserGroup(e, custom);
            String id = oneGroup.getId();
            if (custom == id.startsWith(CUSTOM_ID_PREFIX))
                result.put(id, oneGroup);
        }

        // update the data timestamp for this type
        Date fileTs = XMLUtils.getXMLDate(xml, TIMESTAMP_ATTR);
        if (fileTs == null)
            fileTs = new Date(f.lastModified());
        Date memoryTs = timestamps.get(custom);
        if (memoryTs == null || fileTs.compareTo(memoryTs) > 0)
            timestamps.put(custom, fileTs);

        return result;
    }

    private void saveFile(boolean custom) throws IOException {
        // find the file that should be modified
        File targetFile = custom ? customFile : sharedFile;
        targetFile.getParentFile().mkdirs();
        OutputStream out = new BufferedOutputStream(new RobustFileOutputStream(
                targetFile));

        // start an XML document
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, "UTF-8");
        xml.startDocument("UTF-8", null);
        xml.startTag(null, GROUPS_TAG);

        // write the data timestamp, if we have one
        Date ts = timestamps.get(custom);
        if (ts != null)
            xml.attribute(null, TIMESTAMP_ATTR, XMLUtils.saveDate(ts));

        // write XML for each group
        List<UserGroup> groups = new ArrayList<UserGroup>(this.groups.values());
        Collections.sort(groups);
        for (UserGroup oneGroup : groups) {
            if (oneGroup.isCustom() == custom)
                oneGroup.getAsXml(xml);
        }

        // end the document and close the file
        xml.endTag(null, GROUPS_TAG);
        xml.ignorableWhitespace(System.lineSeparator());
        xml.endDocument();
        out.close();

        // add a tamper-deterrent thumbprint to the shared groups file.
        if (!custom) {
            TamperDeterrent.getInstance().addThumbprint(targetFile, targetFile,
                TamperDeterrent.FileType.XML);
        }

        // if we saved the file successfully, clear its dirty flag
        needsSave.remove(custom);
    }


    /**
     * @return the list of all people known to this Team Dashboard. <b>Note:</b>
     *         this list cannot be generated until all project data has been
     *         loaded; so if this method is called shortly after startup in a
     *         large Team Dashboard, it may take a long time to return.
     */
    public abstract Set<UserGroupMember> getAllKnownPeople();


    private static final String CUSTOM_ID_PREFIX = "c.";

    private static final String GROUPS_TAG = "groups";

    private static final String TIMESTAMP_ATTR = "timestamp";

    static DashboardPermission PERMISSION = new DashboardPermission(
            "userGroupManager");

}
