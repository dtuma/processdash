// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.tool.redact.HierarchyInfo.Node;

public class TeamProjectInfo {

    private HierarchyInfo hierarchyInfo;

    private Map<String, String> projectIDs;

    public TeamProjectInfo(RedactFilterData data, HierarchyInfo hierarchyInfo)
            throws IOException {
        this.hierarchyInfo = hierarchyInfo;
        projectIDs = new HashMap<String, String>();

        String line;
        for (ZipEntry e : data.getEntries("/settings.xml$")) {
            BufferedReader in = data.getFile(e);
            while ((line = in.readLine()) != null) {
                String projId = RedactFilterUtils.getXmlAttr(line, "projectID");
                if (projId != null) {
                    projectIDs.put(getExtResDir(e.getName()), projId);
                    break;
                }
            }
            in.close();
        }
    }

    public String getTeamProjectName(String filename) {
        String projectId = getProjectIdForFile(filename);
        Node n = hierarchyInfo.findNodeForTeamProject(projectId);
        return (n == null ? null : n.newName);
    }

    private String getProjectIdForFile(String filename) {
        String extResDir = getExtResDir(filename);
        if (extResDir == null)
            return null;
        else
            return projectIDs.get(extResDir);
    }

    private String getExtResDir(String filename) {
        int slashPos = filename.lastIndexOf('/');
        if (slashPos == -1)
            return null;
        else
            return filename.substring(0, slashPos + 1).toLowerCase();
    }

    public static void scanForSafeWorkflowNames(RedactFilterData data,
            HierarchyNodeMapper nodeMapper) throws IOException {
        String line;
        for (ZipEntry e : data.getEntries("/workflow.xml$")) {
            BufferedReader in = data.getFile(e);
            while ((line = in.readLine()) != null) {
                if (RedactFilterUtils.getIndentLevel(line) > 1)
                    nodeMapper.addSafeName(RedactFilterUtils.getXmlAttr(line,
                        "name"));
            }
            in.close();
        }
    }

}
