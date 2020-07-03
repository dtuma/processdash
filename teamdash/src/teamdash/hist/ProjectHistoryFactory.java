// Copyright (C) 2015-2020 Tuma Solutions, LLC
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

package teamdash.hist;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.StringUtils;

public class ProjectHistoryFactory {

    public static ProjectHistory getProjectHistory(PDashData data)
            throws ProjectHistoryException {
        return getProjectHistory(Settings.getVal("wbsChangeHistory.testFile"),
            data.getString(TeamDataConstants.TEAM_DATA_DIRECTORY_URL),
            data.getString(TeamDataConstants.TEAM_DATA_DIRECTORY));
    }

    public static ProjectHistory getProjectHistory(String... locations)
            throws ProjectHistoryException {

        for (String oneLocation : locations) {
            if (!StringUtils.hasValue(oneLocation))
                continue;

            oneLocation = ExternalLocationMapper.getInstance().remapFilename(
                oneLocation);

            if (oneLocation.startsWith("http"))
                return getProjectHistoryForUrl(oneLocation);
            else if (oneLocation.endsWith(".zip"))
                return getProjectHistoryForFile(oneLocation);
            else
                return getProjectHistoryForDir(oneLocation);
        }

        return null;
    }

    private static ProjectHistory getProjectHistoryForUrl(String url)
            throws ProjectHistoryException {
        URL serverUrl = TeamServerSelector.resolveServerURL(url);
        if (serverUrl == null)
            throw new ProjectHistoryException("Server.Unavailable_HTML_FMT",
                    url);

        try {
            URL collectionUrl = new URL(serverUrl.toString() + "/");
            return new ProjectHistoryBridged(collectionUrl, //
                    getBool("wbsChangeHistory.precacheAll", false));
        } catch (HttpException.Forbidden f) {
            throw new ProjectHistoryException(f,
                "Server.No_Permission_HTML_FMT", url);
        } catch (IOException ioe) {
            throw new ProjectHistoryException(ioe,
                    "Server.Cannot_Read_HTML_FMT", url);
        }
    }

    private static ProjectHistory getProjectHistoryForDir(String path)
            throws ProjectHistoryException {
        File dir = new File(path);
        if (!dir.isDirectory())
            throw new ProjectHistoryException("Dir.Unavailable_HTML_FMT", path);

        try {
            return new ProjectHistoryLocal(dir);
        } catch (IOException ioe) {
            throw new ProjectHistoryException(ioe, "Dir.Cannot_Read_HTML_FMT",
                    path);

        }
    }

    private static ProjectHistory getProjectHistoryForFile(String path)
            throws ProjectHistoryException {
        File zipFile = new File(path);
        if (!zipFile.isFile())
            throw new ProjectHistoryException("Dir.Unavailable_HTML_FMT", path);

        try {
            return new ProjectHistoryBridgedFile(zipFile);
        } catch (IOException ioe) {
            throw new ProjectHistoryException(ioe, "Dir.Cannot_Read_HTML_FMT",
                    path);
        }
    }

    private static boolean getBool(String name, boolean defaultValue) {
        try {
            return Settings.getBool(name, defaultValue);
        } catch (Throwable t) {
            // when running in the WBS Editor, the Settings class will not be
            // present. Fall back to the default.
            return defaultValue;
        }
    }

}
