// Copyright (C) 2002-2022 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.setup.move;

import java.io.File;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.RepairImportInstruction;
import net.sourceforge.processdash.tool.bridge.impl.SyncClientMappings;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class MoveTeamDirMessageHandler implements MessageHandler {

    private String processID;

    private DashboardContext ctx;

    private static final Logger logger = Logger
            .getLogger(MoveTeamDirMessageHandler.class.getName());

    public void setConfigElement(Element xml, String attrName) {
        this.processID = xml.getAttribute("processID");
    }

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return new String[] { processID + "/moveTeamDir" };
    }

    public void handle(MessageEvent message) {
        Element xml = message.getMessageXml();
        String projectId = getString(xml, PROJECT_ID_ATTR);
        if (!StringUtils.hasValue(projectId))
            return;

        String path = findProject(PropertyKey.ROOT, projectId);
        if (path == null)
            return;

        String directory = getString(xml, DIR_ATTR);
        String directoryENC = getString(xml, DIR_ENC_ATTR);
        String directoryUNC = getString(xml, DIR_UNC_ATTR);
        String url = getString(xml, URL_ATTR);

        // try resolving inbound [Shared Folder] encodings, if provided
        String resolved = maybeResolveSharedFolderPath(directoryENC, projectId);
        if (resolved != null)
            directory = resolved;

        NetworkDriveList dl = new NetworkDriveList();
        if (dl.wasSuccessful()) {
            if (StringUtils.hasValue(directoryUNC)) {
                if (directory == null || !directory.startsWith("\\\\")) {
                    String newDir = dl.fromUNCName(directoryUNC);
                    if (StringUtils.hasValue(newDir))
                        directory = newDir;
                }
            } else if (StringUtils.hasValue(directory)) {
                String newUNC = dl.toUNCName(directory);
                if (StringUtils.hasValue(newUNC))
                    directoryUNC = newUNC;
            }
        }

        // potentially re-encode the directory using local folder mappings
        if (StringUtils.hasValue(directory)) {
            SyncClientMappings.initialize(new File(directory));
            directory = FolderMappingManager.getInstance().encodePath(directory);
        }

        logger.info("Moving team data directory for project '" + path + "' to:\n"
            + "\tdirectory=" + directory + "\n"
            + "\tdirectoryUNC=" + directoryUNC + "\n"
            + "\turl=" + url);

        DataContext data = ctx.getData().getSubcontext(path);
        saveString(data, TeamDataConstants.TEAM_DIRECTORY, directory);
        saveString(data, TeamDataConstants.TEAM_DIRECTORY_UNC, directoryUNC);
        saveString(data, TeamDataConstants.TEAM_DATA_DIRECTORY_URL, url);
        RepairImportInstruction.maybeRepairForIndividual(data);
    }

    private String findProject(PropertyKey node, String id) {
        String templateID = ctx.getHierarchy().getID(node);

        if (StringUtils.hasValue(templateID)
                && INDIV_TEMPLATE_IDS.matches(templateID)) {
            String path = node.path();
            String idDataName = DataRepository.createDataName(path,
                TeamDataConstants.PROJECT_ID);
            SimpleData idValue = ctx.getData().getSimpleValue(idDataName);
            if (idValue != null && id.equals(idValue.format()))
                return path;
            else
                return null;
        }

        for (int i = ctx.getHierarchy().getNumChildren(node); i-- > 0;) {
            PropertyKey child = ctx.getHierarchy().getChildKey(node, i);
            String result = findProject(child, id);
            if (result != null)
                return result;
        }

        return null;
    }

    private String maybeResolveSharedFolderPath(String teamDirectoryENC,
            String projectId) {
        // if the team directory is not using [Shared Folder] encoding, abort.
        if (!FolderMappingManager.isEncodedPath(teamDirectoryENC))
            return null;

        // resolve the encoded path, or search for a directory that matches
        try {
            String teamDataDirPath = teamDirectoryENC + "/data/" + projectId;
            File dir = FolderMappingManager.getInstance()
                    .searchForDirectory(teamDataDirPath, 2);
            return dir.getParentFile().getParentFile().getAbsolutePath();

        } catch (Exception e) {
            // we were unable to find a directory that matches
            return null;
        }
    }

    private String getString(Element xml, String tagName) {
        NodeList nl = xml.getElementsByTagName(tagName);
        if (nl.getLength() == 1)
            return XMLUtils.getTextContents((Element) nl.item(0));
        else
            return null;
    }

    private void saveString(DataContext data, String name, String value) {
        SimpleData d;
        if (StringUtils.hasValue(value))
            d = StringData.create(value);
        else
            d = null;
        data.putValue(name, d);
    }

    static final String PROJECT_ID_ATTR = "projectID";

    static final String DIR_ATTR = "directory";

    static final String DIR_ENC_ATTR = "directoryENC";

    static final String DIR_UNC_ATTR = "directoryUNC";

    static final String URL_ATTR = "url";

    static final PatternList INDIV_TEMPLATE_IDS = new PatternList( //
            "/IndivRoot$", "/Indiv2Root$");

}
