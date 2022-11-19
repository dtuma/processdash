// Copyright (C) 2002-2022 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class TeamMemberDataStatus implements Comparable<TeamMemberDataStatus> {


    /**
     * Get the information about the PDASH data files exported by members of a
     * team project
     * 
     * @param data
     *            the data repository
     * @param projectPath
     *            the path of a team project
     */
    public static List<TeamMemberDataStatus> get(DataRepository data,
            String projectPath) {
        return get(data, projectPath, "Corresponding_Project_Nodes");
    }


    public static List<TeamMemberDataStatus> get(DataRepository data,
            String projectPath, String prefixListDataName) {
        ListData prefixList = ListData.asListData(data.getSimpleValue( //
            DataRepository.createDataName(projectPath, prefixListDataName)));

        List<TeamMemberDataStatus> result = new ArrayList();
        Map<String, TeamMemberDataStatus> datasetIdMap = new HashMap();
        if (prefixList != null) {
            for (int i = prefixList.size(); i-- > 0;) {
                String onePrefix = StringUtils.asString(prefixList.get(i));
                String metadataPrefix = getMetadataPrefix(data, onePrefix);
                if (metadataPrefix != null)
                    result.add(new TeamMemberDataStatus(data, projectPath,
                            onePrefix, metadataPrefix, datasetIdMap));
            }
        }

        return result;
    }


    private static String getMetadataPrefix(DataRepository data,
            String prefix) {
        if (prefix == null)
            return null;
        StringBuffer result = new StringBuffer(prefix);
        if (data.getInheritableValue(result, METADATA) != null)
            return result.toString();
        else
            return null;
    }



    private DataContext data;

    public String projectPath;

    private String metadataPrefix;

    public String ownerName;

    public String datasetID;

    public boolean hasDatasetIdCollision;

    public Date exportDate;

    public String dashVersion;

    public Date wbsLastSync;

    private TeamMemberDataStatus(DataContext data, String projectPath,
            String prefix, String metadataPrefix,
            Map<String, TeamMemberDataStatus> datasetIdMap) {
        this.data = data;
        this.projectPath = projectPath;
        this.metadataPrefix = metadataPrefix;
        this.ownerName = getString(OWNER_VAR, metadataPrefix);
        this.datasetID = getString(DATASET_ID_VAR, metadataPrefix);
        this.exportDate = getDate(TIMESTAMP_VAR, metadataPrefix);
        this.dashVersion = getVersion(DASH_PKG_ID);
        this.wbsLastSync = getDate(TeamDataConstants.LAST_SYNC_TIMESTAMP,
            prefix);

        TeamMemberDataStatus other = datasetIdMap.put(datasetID, this);
        if (other != null && XMLUtils.hasValue(datasetID)
                && !this.ownerName.equals(other.ownerName))
            this.hasDatasetIdCollision = other.hasDatasetIdCollision = true;
    }

    private SimpleData get(String name, String prefix) {
        String dataName = DataRepository.createDataName(prefix, name);
        return data.getSimpleValue(dataName);
    }

    private String getString(String name, String prefix) {
        SimpleData val = get(name, prefix);
        return (val == null ? "" : val.format());
    }

    private Date getDate(String name, String prefix) {
        SimpleData val = get(name, prefix);
        if (val instanceof DateData) {
            return ((DateData) val).getValue();
        } else {
            return null;
        }
    }

    public String getVersion(String packageID) {
        return getString(VERSION_PREFIX + packageID, metadataPrefix);
    }

    public String getVersionSortKey() {
        if (dashVersion == null)
            return null;

        StringBuilder result = new StringBuilder("v");
        for (String num : dashVersion.split("\\."))
            result.append(num.length() == 1 ? "0" : "").append(num);
        return result.toString();
    }

    public int compareTo(TeamMemberDataStatus that) {
        return this.ownerName.compareTo(that.ownerName);
    }



    private static final String DASH_PKG_ID = "pspdash";

    private static final String METADATA = "Import_Metadata";

    private static final String EXPORTED = METADATA + "/exported.";

    private static final String OWNER_VAR = EXPORTED + "byOwner";

    private static final String DATASET_ID_VAR = EXPORTED + "datasetID";

    private static final String TIMESTAMP_VAR = EXPORTED + "when";

    private static final String VERSION_PREFIX = EXPORTED + "withPackage/";

}
