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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberFilter;
import teamdash.team.TeamMemberList;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSFilenameConstants;

public class UserGroupManagerWBS extends UserGroupManager {

    private static Resources resources = Resources.getDashBundle("WBSEditor");

    public static UserGroupManagerWBS getInstance() {
        return (UserGroupManagerWBS) UserGroupManager.getInstance();
    }


    private Map<Integer, String> datasetIDMap;
    
    private Map<Integer, String> datasetIDMapExt;

    private TeamMemberList teamMemberList;

    private TeamMemberFilter teamMemberFilter;

    private boolean includeRelated;


    private UserGroupManagerWBS() {
        super(true);
        datasetIDMap = new HashMap<Integer, String>();
        datasetIDMapExt = Collections.unmodifiableMap(datasetIDMap);
    }

    public static void init(TeamProject proj) {
        // get the file containing group data, and the dataset ID
        File settingsFile = new File(proj.getStorageDirectory(),
                WBSFilenameConstants.SETTINGS_FILENAME);
        String datasetID = proj.getDatasetID();
        if (datasetID == null)
            datasetID = proj.getProjectID();

        // initialize the user group manager
        new UserGroupManagerWBS().init(settingsFile, datasetID);
        getInstance().teamMemberList = proj.getTeamMemberList();
    }

    @Override
    public String getReadOnlyCode() {
        return "WBS_Editor";
    }

    public void reload() {
        super.reloadAll();
    }

    public UserGroupMember getMe() {
        TeamMember me = teamMemberList.getTeamMemberForCurrentUser();
        if (me == null)
            return null;
        else
            return new UserGroupMember(resources.getString("Filter.Me"),
                    getIdForFilter(me));
    }

    @Override
    public Set<UserGroupMember> getAllKnownPeople() {
        Set<UserGroupMember> result = new TreeSet<UserGroupMember>();
        for (TeamMember m : teamMemberList.getTeamMembers())
            result.add(new UserGroupMember(m.getName(), getIdForFilter(m)));
        return result;
    }

    private String getIdForFilter(TeamMember m) {
        String result = m.getDatasetID();
        if (result == null)
            result = TeamMemberFilter.getTeamMemberPseudoID(m);
        return result;
    }

    public Map<Integer, String> getDatasetIDMap() {
        return datasetIDMapExt;
    }

    public void addDatasetIDMappings(Map<Integer, String> map) {
        this.datasetIDMap.putAll(map);
        if (teamMemberFilter != null)
            fireFilterChangedEvent();
    }

    public void setFilter(UserFilter f, boolean includeRelated) {
        this.includeRelated = includeRelated;
        setGlobalFilter(f);
    }

    @Override
    public void setGlobalFilter(UserFilter f) {
        if (f == null)
            f = UserGroup.EVERYONE;
        super.setGlobalFilter(f);
        rebuildTeamMemberFilter();
    }

    private void rebuildTeamMemberFilter() {
        UserFilter f = getGlobalFilter();
        if (UserGroup.isEveryone(f))
            teamMemberFilter = null;
        else
            teamMemberFilter = new TeamMemberFilter(teamMemberList, f);
        fireFilterChangedEvent();
    }

    public TeamMemberFilter getTeamMemberFilter() {
        return teamMemberFilter;
    }

    public boolean isIncludeRelated() {
        return includeRelated;
    }

    public void setIncludeRelated(boolean includeRelated) {
        if (this.includeRelated != includeRelated) {
            this.includeRelated = includeRelated;
            fireFilterChangedEvent();
        }
    }

    public void addUserGroupFilterListener(UserGroupFilterListener l) {
        listeners.add(UserGroupFilterListener.class, l);
    }

    public void removeUserGroupFilterListener(UserGroupFilterListener l) {
        listeners.remove(UserGroupFilterListener.class, l);
    }

    private void fireFilterChangedEvent() {
        UserGroupFilterEvent e = new UserGroupFilterEvent(this);
        for (UserGroupFilterListener l : listeners
                .getListeners(UserGroupFilterListener.class)) {
            l.groupFilterChanged(e);
        }
    }

}
