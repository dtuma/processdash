// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class SetTeamServerJoinUrl extends TinyCGIBase {

    private static final String SYSTEM_PROPERTY_NAME =
        "teamdash.templates.setup.teamServerJoinUrl";


    @Override
    protected void writeContents() throws IOException {
        String serverUrlPattern = System.getProperty(SYSTEM_PROPERTY_NAME);
        if (!StringUtils.hasValue(serverUrlPattern))
            return;

        SimpleData sd = getDataContext().getSimpleValue(
            TeamDataConstants.PROJECT_ID);
        if (sd == null || !sd.test())
            return;

        String projectId = HTMLUtils.urlEncode(sd.format());
        String serverUrl = StringUtils.findAndReplace(serverUrlPattern,
            "[Project_ID]", projectId);

        out.write("<!--#set var='teamServerJoinUrl' inline='true' -->");
        out.write(serverUrl);
        out.write("<!--#endset-->");
    }

}
