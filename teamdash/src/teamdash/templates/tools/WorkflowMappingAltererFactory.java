// Copyright (C) 2016 Tuma Solutions, LLC
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

package teamdash.templates.tools;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;

import teamdash.FilenameMapper;
import teamdash.wbs.WBSEditor;

public class WorkflowMappingAltererFactory {

    public static WorkflowMappingAlterer get(DashboardContext ctx,
            String projectId) {

        // find the root path of the project with the given ID.
        DataContext data = ctx.getData();
        String projPath = findProject(ctx.getHierarchy(), data,
            PropertyKey.ROOT, projectId);
        if (projPath == null)
            return null;

        // identify the name of the user who is running this logic
        String ownerName = WBSEditor.getKnownOwnerName();
        if (ownerName == null)
            ownerName = System.getProperty("user.name");

        // retrieve the location of the data directory, and create an alterer
        String teamDataUrl = getVal(data, projPath, "Team_Data_Directory_URL");
        String teamDataDir = getVal(data, projPath, "Team_Data_Directory");
        return new WorkflowMappingAltererProjectDir(ownerName,
                FilenameMapper.remap(teamDataUrl),
                FilenameMapper.remap(teamDataDir));
    }

    private static String findProject(DashHierarchy hier, DataContext data,
            PropertyKey key, String projectId) {
        String id = hier.getID(key);
        if (id != null && id.endsWith("/TeamRoot")) {
            String path = key.path();
            if (projectId.equals(getVal(data, path, "Project_ID")))
                return path;

        } else {
            for (int i = hier.getNumChildren(key); i-- > 0;) {
                PropertyKey child = hier.getChildKey(key, i);
                String result = findProject(hier, data, child, projectId);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private static String getVal(DataContext data, String path, String name) {
        String dataName = path + "/" + name;
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

}
