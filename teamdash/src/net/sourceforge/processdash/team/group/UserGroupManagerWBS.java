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
import java.util.Map;
import java.util.Set;

import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSFilenameConstants;

public class UserGroupManagerWBS extends UserGroupManager {

    public static UserGroupManagerWBS getInstance() {
        return (UserGroupManagerWBS) UserGroupManager.getInstance();
    }


    private Map<String, String> datasetIDMap;

    private UserGroupManagerWBS() {
        super(true);
        datasetIDMap = Collections.EMPTY_MAP;
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
    }

    @Override
    public String getReadOnlyCode() {
        return "WBS_Editor";
    }

    @Override
    public Set<UserGroupMember> getAllKnownPeople() {
        return Collections.EMPTY_SET;
    }

    public Map<String, String> getDatasetIDMap() {
        return datasetIDMap;
    }

    public void setDatasetIDMap(Map<String, String> datasetIDMap) {
        this.datasetIDMap = Collections.unmodifiableMap(datasetIDMap);
    }

}
