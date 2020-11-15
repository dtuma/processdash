// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


public class IncludeTeamTools extends TinyCGIBase {

    private static final String WBS_EDITOR_URL =
        "../../team/tools/index.shtm?directory=";
    private static final String WBS_EDITOR_URL_B =
        "../../team/toolsB/index.shtm?directory=";
    private static final String MASTER_PARAM = "&master";
    private static final String SYNC_PARAM = "&syncURL=";
    private static final String SYNC_URL = "sync.class?run";

    protected void writeContents() throws IOException {
        try {
            if (getPrefix() == null)
                return;

            String directory = getTeamDataDirectory();
            if (directory == null) {
                out.print(TEAM_DIR_MISSING_MSG);
                return;
            }

            String wbsURL = isLegacySizeMetricsProject() //
                    ? WBS_EDITOR_URL : WBS_EDITOR_URL_B;
            wbsURL = wbsURL + HTMLUtils.urlEncode(directory);
            String scriptPath = (String) env.get("SCRIPT_PATH");
            String uri = resolveRelativeURI(scriptPath, wbsURL);

            if (parameters.containsKey("master"))
                uri = uri + MASTER_PARAM;

            String syncURI = resolveRelativeURI(scriptPath, SYNC_URL);
            uri = uri + SYNC_PARAM + HTMLUtils.urlEncode(syncURI);

            outStream.write(getRequest(uri, true));
        } catch (Exception e) {
            out.print(TOOLS_MISSING_MSG);
        }
    }

    private boolean isLegacySizeMetricsProject() {
        String flag = getValue(TeamDataConstants.WBS_CUSTOM_SIZE_DATA_NAME);
        String master = getValue(TeamDataConstants.MASTER_PROJECT_TAG);
        return flag == null && master == null;
    }

    private String getTeamDataDirectory() {
        String result = getValue("Team_Data_Directory");
        if (result == null || result.trim().length() == 0)
            return null;

        File f = new File(result);
        return f.getPath();
    }

    private String getValue(String name) {
        DataRepository data = getDataRepository();
        String dataName = DataRepository.createDataName(getPrefix(), name);
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null)
            return null;
        String result = d.format();
        if (result == null || result.trim().length() == 0)
            return null;
        else
            return result;
    }

    private static final String TEAM_DIR_MISSING_MSG =
            "<html><body>" +
            "<p><b>The advanced team tools (such as the Custom Process Editor " +
            "and the Work Breakdown Structure Editor) cannot be used until you " +
            "specify a team data directory on the project parameters page.</b>" +
            "</body></html>";

    private static final String TOOLS_MISSING_MSG =
        "<html><body>" +
        "<p><b>The advanced team tools (such as the Custom Process Editor " +
        "and the Work Breakdown Structure Editor) have not been installed " +
        "in this instance of the dashboard.</b>" +
        "</body></html>";

}
