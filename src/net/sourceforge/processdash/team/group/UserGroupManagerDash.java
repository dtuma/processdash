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
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class UserGroupManagerDash extends UserGroupManager {

    public static void install() {
        new UserGroupManagerDash();
    }

    public static UserGroupManagerDash getInstance() {
        return (UserGroupManagerDash) UserGroupManager.getInstance();
    }


    private Map<String, KnownPerson> joinedPeople, allTeamMembers;

    private DataRepository dataRepository;


    private UserGroupManagerDash() {
        // user groups are only relevant for team dashboards, and they require
        // a recent version of the database plugin. If these conditions are not
        // met, disable the user group manager.
        super(Settings.isTeamMode()
                && TemplateLoader.meetsPackageRequirement("tpidw-embedded",
                    "1.5.4.1"));
        ReverseOrder reverse = new ReverseOrder();
        joinedPeople = Collections.synchronizedMap(new TreeMap(reverse));
        allTeamMembers = Collections.synchronizedMap(new TreeMap(reverse));
    }

    public void init(DashboardContext ctx) {
        // ensure calling code has permission to perform initialization
        PERMISSION.checkPermission();

        // no initialization is needed if this object is disabled
        if (!isEnabled())
            return;

        // save the data repository for future use
        dataRepository = ctx.getData();

        // determine the file for storage of shared groups
        File wd = ctx.getWorkingDirectory().getDirectory();
        File sharedFile = new File(wd, "groups.dat");

        // identify the dataset ID to use for custom groups
        String datasetID = DashController.getDatasetID(false);
        if (datasetID == null)
            datasetID = Integer.toString(ctx.getWorkingDirectory() //
                    .getDescription().hashCode());

        // call the parent initialization logic
        init(sharedFile, datasetID);
    }


    /**
     * Shared groups should not be editable in certain circumstances (for
     * example, when the team dashboard is in read-only mode). This method
     * indicates whether shared groups are read only.
     * 
     * @return null if shared groups can be edited; otherwise, a reason code
     *         explaining why they cannot
     */
    @Override
    public String getReadOnlyCode() {
        if (!PermissionsManager.getInstance().hasPermission("pdash.editGroups"))
            return "No_Permission";
        else if (Settings.isReadOnly())
            return "Read_Only";
        else
            return null;
    }



    @Override
    public void setGlobalFilter(UserFilter f) {
        if (isEnabled()) {
            super.setGlobalFilter(f);
            if (f instanceof UserGroupMember)
                saveDataElements(f);
            saveDataElement("/" + FILTER_DATANAME, StringData.create(f.getId()));
        }
    }


    public void setLocalFilter(String path, UserFilter f) {
        if (!isEnabled())
            return;
        if (f instanceof UserGroupMember)
            saveDataElements(f);
        String dataName = DataRepository.createDataName(path, FILTER_DATANAME);
        saveDataElement(dataName,
            f == null ? null : StringData.create(f.getId()));
    }


    public UserFilter getLocalFilter(String path) {
        String dataName = DataRepository.createDataName(path, FILTER_DATANAME);
        String filterId = getString(dataName);
        UserFilter result = getFilterById(filterId);
        if (result == null)
            return getGlobalFilter();
        else if (isPrivacyViolation(path))
            return new UserGroupPrivacyBlock(result);
        else
            return result;
    }


    public boolean isPrivacyViolation(String path) {
        String dataName = DataRepository.createDataName(path, PRIVACY_DATANAME);
        return (getString(dataName) != null);
    }


    @Override
    public UserFilter getFilterById(String filterId) {
        if (filterId != null && filterId.startsWith(UserGroupMember.ID_PREFIX)) {
            // if this is an individual, try looking up their details from the
            // data elements saved in the repository
            String dataName = DATA_PREFIX + filterId + NAME_SUFFIX;
            String displayName = getString(dataName);
            dataName = DATA_PREFIX + filterId + DATASET_IDS_SUFFIX;
            ListData l = ListData.asListData(dataRepository.getValue(dataName));
            if (displayName != null && l != null && l.test())
                return new UserGroupMember(displayName, (String) l.get(0));
        }

        return super.getFilterById(filterId);
    }


    @Override
    void groupWasSaved(UserGroup g) {
        // save data elements for this group
        saveDataElements(g);

        // notify listeners about the change
        super.groupWasSaved(g);
    }

    private void saveDataElements(UserFilter f) {
        saveDataElement(f.getId(), NAME_SUFFIX, StringData.create(f.toString()));

        ListData datasetIDs = new ListData();
        for (String oneID : f.getDatasetIDs())
            datasetIDs.add(oneID);
        if (datasetIDs.test() == false)
            datasetIDs.add(EMPTY_GROUP_TOKEN);
        saveDataElement(f.getId(), DATASET_IDS_SUFFIX, datasetIDs);
    }


    @Override
    void groupWasDeleted(UserGroup g) {
        // discard data elements for this group
        saveDataElement(g.getId(), NAME_SUFFIX, null);
        saveDataElement(g.getId(), DATASET_IDS_SUFFIX, null);

        // notify listeners about the change
        super.groupWasDeleted(g);
    }

    private void saveDataElement(String id, String suffix, SimpleData value) {
        String dataName = "/" + DATA_PREFIX + id + suffix;
        saveDataElement(dataName, value);
    }

    private void saveDataElement(String name, SimpleData value) {
        SimpleData oldValue = dataRepository.getSimpleValue(name);
        if (!simpleDataIsEqual(oldValue, value)) {
            dataRepository.putValue(name, value);
            if (oldValue == null)
                dataRepository.pinElement(name);
        }
    }

    private boolean simpleDataIsEqual(SimpleData a, SimpleData b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return a.equals(b);
    }

    private String getString(String dataName) {
        SimpleData sd = dataRepository.getSimpleValue(dataName);
        if (sd != null && sd.test())
            return sd.format();
        else
            return null;
    }


    /**
     * @return the list of all people known to this Team Dashboard.
     */
    @Override
    public Set<UserGroupMember> getAllKnownPeople() {
        Set<UserGroupMember> result = new HashSet();
        Set<String> namesSeen = new HashSet();

        // create users for each person who has joined a team project. Entries
        // will be processed from newest team project to oldest, so the logic
        // below will prefer the most-recently-written full name for each person
        for (KnownPerson p : joinedPeople.values()) {
            if (result.add(p.asMember()))
                namesSeen.add(p.fullName);
        }

        // now, look through the data for people who have been added to a team,
        // but who might not have joined the project yet. This data isn't as
        // reliable, so we only use it for people we haven't seen yet.
        for (Entry<String, KnownPerson> e : allTeamMembers.entrySet()) {
            String key = e.getKey();
            if (joinedPeople.containsKey(key))
                // this team member (as identified by a projectID/initials pair)
                // has joined the project, so we have their data already.
                continue;

            KnownPerson p = e.getValue();
            if (namesSeen.contains(p.fullName))
                // we've already seen an individual with this exact name. The
                // person we saw already is more reliable (either because they
                // came from joining data, or from a newer project).
                continue;

            if (result.add(p.asMember()))
                namesSeen.add(p.fullName);
        }

        return result;
    }


    /**
     * Make note of the fact that we saw a PDASH file describing a particular
     * individual who has joined a team project.
     */
    public void addPersonFromPdash(String projectID, String initials,
            String fullName, String datasetID) {
        if (XMLUtils.hasValue(projectID) //
                && XMLUtils.hasValue(initials) //
                && XMLUtils.hasValue(fullName) //
                && XMLUtils.hasValue(datasetID)) {
            KnownPerson p = new KnownPerson();
            p.projectID = projectID.toLowerCase();
            p.initials = initials.toLowerCase();
            p.fullName = fullName;
            p.datasetID = datasetID;
            joinedPeople.put(p.getKey(), p);
        }
    }


    /**
     * Scan the team member list of a project for any individuals it contains
     */
    public void addTeamMemberList(File f) {
        // look at the name of the directory containing the import file. If it
        // isn't a bridged import directory, ignore it - because we'll only find
        // server identity info in a PDES-based team member list file.
        String parentDir = f.getParentFile().getName().toLowerCase();
        if (!parentDir.startsWith("http")
                && !CompressedInstanceLauncher.isRunningFromCompressedData())
            return;

        // infer the project ID from the name of the parent directory
        int commaPos = parentDir.lastIndexOf(',');
        String projectID = parentDir.substring(commaPos + 1);

        // parse the data in the team member list. Abort if corrupt
        Element xml;
        try {
            xml = XMLUtils
                    .parse(new BufferedInputStream(new FileInputStream(f)))
                    .getDocumentElement();
        } catch (Exception e) {
            return;
        }

        // find each of the team members, and see if they contain datasetIDs
        NodeList nl = xml.getElementsByTagName("teamMember");
        for (int i = 0; i < nl.getLength(); i++) {
            Element m = (Element) nl.item(i);
            String serverInfo = m.getAttribute("serverIdentityData");
            if (!XMLUtils.hasValue(serverInfo))
                continue;

            String datasetID = (String) HTMLUtils.parseQuery(serverInfo)
                    .get("datasetID");
            if (!XMLUtils.hasValue(datasetID))
                continue;

            KnownPerson p = new KnownPerson();
            p.projectID = projectID;
            p.initials = m.getAttribute("initials").toLowerCase();
            p.fullName = m.getAttribute("name");
            p.datasetID = datasetID;
            allTeamMembers.put(p.getKey(), p);
        }
    }


    private static class KnownPerson {

        String projectID, initials, fullName, datasetID;

        String getKey() {
            return projectID + "/" + initials;
        }

        UserGroupMember asMember() {
            return new UserGroupMember(fullName, datasetID);
        }

    }


    private static class ReverseOrder implements Comparator<String> {
        public int compare(String a, String b) {
            return b.compareTo(a);
        }
    }


    private static final String DATA_PREFIX = "User_Group/";

    public static final String FILTER_DATANAME = DATA_PREFIX + "/Filter";

    private static final String PRIVACY_DATANAME = DATA_PREFIX + "Privacy_Violation";

    private static final String NAME_SUFFIX = "//Name";

    private static final String DATASET_IDS_SUFFIX = "//Dataset_IDs";

    public static final String EMPTY_GROUP_TOKEN = "*empty*";

}
